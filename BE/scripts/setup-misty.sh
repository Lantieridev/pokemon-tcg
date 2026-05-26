#!/usr/bin/env bash
#
# setup-misty.sh
# --------------
# Idempotent bootstrap helper for the second test player in the Pokemon TCG project.
#
# Flow:
#   1. POST /api/auth/register        -> create user "Misty" (tolerates "already exists")
#   2. psql lookup against pokemon-tcg-db -> resolve users.id for "Misty"
#   3. POST /api/auth/login           -> capture JWT for Misty
#   4. POST /api/decks  (Bearer JWT)  -> create a legal 60-card Water deck
#                                        (4x Blastoise-EX [xy1-29] + 56x Water Energy [xy1-134])
#   5. POST /api/matches (Bearer JWT) -> optionally create a match Ash vs Misty
#
# Requirements on PATH: bash >= 4, curl, jq, docker.
#
# References:
#   - docs/SKILLS/game-rules-reference.md (deck composition rules)
#   - BE/src/main/java/ar/edu/utn/frc/tup/piii/services/deck/DeckBuilderValidator.java
#
# Usage:
#   chmod +x BE/scripts/setup-misty.sh
#   ./BE/scripts/setup-misty.sh
#
# Optional environment overrides:
#   API_BASE          (default: http://localhost:8081)
#   DB_CONTAINER      (default: pokemon-tcg-db)
#   DB_NAME           (default: pokemon_tcg)
#   DB_USER           (default: admin)
#   MISTY_USERNAME    (default: Misty)
#   MISTY_PASSWORD    (default: mistypassword123)
#   MISTY_EMAIL       (default: misty@cerulean.gym)
#   DECK_NAME         (default: Mistys Water Tide)
#   ASH_USERNAME      (default: AshKetchum)
#   ASH_DECK_ID       (default: 1)
#   CREATE_MATCH      (default: true; set to "false" to skip the match step)
#
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8081}"
DB_CONTAINER="${DB_CONTAINER:-pokemon-tcg-db}"
DB_NAME="${DB_NAME:-pokemon_tcg}"
DB_USER="${DB_USER:-admin}"

MISTY_USERNAME="${MISTY_USERNAME:-Misty}"
MISTY_PASSWORD="${MISTY_PASSWORD:-mistypassword123}"
MISTY_EMAIL="${MISTY_EMAIL:-misty@cerulean.gym}"
DECK_NAME="${DECK_NAME:-Mistys Water Tide}"
ASH_USERNAME="${ASH_USERNAME:-AshKetchum}"
ASH_DECK_ID="${ASH_DECK_ID:-1}"
CREATE_MATCH="${CREATE_MATCH:-true}"

EX_CARD_ID="xy1-29"        # Blastoise-EX (Basic, EX, 180 HP)
ENERGY_CARD_ID="xy1-134"   # Water Energy

# ---- ANSI colors (only when stdout is a TTY) --------------------------------
if [[ -t 1 ]]; then
  C_RESET=$'\033[0m'; C_BOLD=$'\033[1m'
  C_GREEN=$'\033[32m'; C_YELLOW=$'\033[33m'; C_RED=$'\033[31m'; C_BLUE=$'\033[34m'
else
  C_RESET=""; C_BOLD=""; C_GREEN=""; C_YELLOW=""; C_RED=""; C_BLUE=""
fi

log()  { printf "%s==>%s %s\n" "${C_BOLD}${C_BLUE}" "${C_RESET}" "$*"; }
ok()   { printf "%sOK%s  %s\n" "${C_GREEN}" "${C_RESET}" "$*"; }
warn() { printf "%s!%s   %s\n" "${C_YELLOW}" "${C_RESET}" "$*"; }
die()  { printf "%sX%s   %s\n" "${C_RED}" "${C_RESET}" "$*" >&2; exit 1; }

# ---- Preflight --------------------------------------------------------------
for bin in curl jq docker; do
  command -v "${bin}" >/dev/null 2>&1 || die "Missing required binary: ${bin}"
done

log "Pinging backend at ${API_BASE}"
if ! curl -fsS -o /dev/null "${API_BASE}/api/rankings"; then
  die "Backend not reachable at ${API_BASE}. Start it with 'mvn spring-boot:run' in BE/."
fi
ok "Backend is up"

# ---- 1. Register Misty (idempotent) ----------------------------------------
log "Registering ${MISTY_USERNAME}"
REGISTER_PAYLOAD=$(jq -nc \
  --arg u "${MISTY_USERNAME}" \
  --arg e "${MISTY_EMAIL}" \
  --arg p "${MISTY_PASSWORD}" \
  '{username:$u, email:$e, password:$p}')

REGISTER_HTTP=$(curl -sS -o /tmp/misty_register.out -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -X POST "${API_BASE}/api/auth/register" \
  -d "${REGISTER_PAYLOAD}")

case "${REGISTER_HTTP}" in
  200|201) ok "Registered ${MISTY_USERNAME}" ;;
  400)
    BODY=$(cat /tmp/misty_register.out)
    if [[ "${BODY}" == *"already exists"* ]]; then
      warn "${MISTY_USERNAME} already exists - continuing"
    else
      die "Registration failed (HTTP 400): ${BODY}"
    fi
    ;;
  *) die "Registration failed (HTTP ${REGISTER_HTTP}): $(cat /tmp/misty_register.out)" ;;
esac

# ---- 2. Resolve userId via Postgres ----------------------------------------
log "Resolving users.id for ${MISTY_USERNAME} (psql in ${DB_CONTAINER})"
USER_ID=$(docker exec -i "${DB_CONTAINER}" \
  psql -U "${DB_USER}" -d "${DB_NAME}" -tA \
  -c "SELECT id FROM users WHERE username = '${MISTY_USERNAME}';" | tr -d '[:space:]')

[[ -n "${USER_ID}" ]] || die "Could not resolve userId for ${MISTY_USERNAME}"
ok "User id = ${USER_ID}"

# ---- 3. Login (fresh JWT) --------------------------------------------------
log "Logging in to get JWT"
LOGIN_PAYLOAD=$(jq -nc \
  --arg u "${MISTY_USERNAME}" \
  --arg p "${MISTY_PASSWORD}" \
  '{username:$u, password:$p}')

LOGIN_BODY=$(curl -fsS \
  -H "Content-Type: application/json" \
  -X POST "${API_BASE}/api/auth/login" \
  -d "${LOGIN_PAYLOAD}")

JWT=$(printf "%s" "${LOGIN_BODY}" | jq -r '.token')
[[ "${JWT}" != "null" && -n "${JWT}" ]] || die "Login failed: ${LOGIN_BODY}"
ok "JWT acquired"

# ---- 4. Build deck payload (4 EX + 56 Energy = 60) --------------------------
log "Creating deck '${DECK_NAME}' (4x ${EX_CARD_ID} + 56x ${ENERGY_CARD_ID})"
DECK_PAYLOAD=$(jq -nc \
  --argjson uid "${USER_ID}" \
  --arg name "${DECK_NAME}" \
  --arg ex "${EX_CARD_ID}" \
  --arg en "${ENERGY_CARD_ID}" \
  '{
     userId: $uid,
     name: $name,
     cards: [
       {cardId: $ex, quantity: 4},
       {cardId: $en, quantity: 56}
     ]
   }')

DECK_HTTP=$(curl -sS -o /tmp/misty_deck.out -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT}" \
  -X POST "${API_BASE}/api/decks" \
  -d "${DECK_PAYLOAD}")

if [[ "${DECK_HTTP}" != "201" && "${DECK_HTTP}" != "200" ]]; then
  die "Deck creation failed (HTTP ${DECK_HTTP}): $(cat /tmp/misty_deck.out)"
fi

DECK_ID=$(jq -r '.id // .deckId // empty' /tmp/misty_deck.out)
ok "Deck created (id = ${DECK_ID:-unknown})"

# ---- 5. (Optional) Create a match between Ash and Misty --------------------
MATCH_ID=""
if [[ "${CREATE_MATCH}" == "true" ]]; then
  log "Creating match: ${ASH_USERNAME} (deck ${ASH_DECK_ID}) vs ${MISTY_USERNAME} (deck ${DECK_ID})"

  if [[ -z "${DECK_ID}" ]]; then
    warn "Misty deck id is unknown - skipping match creation"
  else
    MATCH_PAYLOAD=$(jq -nc \
      --arg a "${ASH_USERNAME}" \
      --arg b "${MISTY_USERNAME}" \
      --argjson da "${ASH_DECK_ID}" \
      --argjson db "${DECK_ID}" \
      '{playerAId:$a, playerBId:$b, deckAId:$da, deckBId:$db}')

    MATCH_HTTP=$(curl -sS -o /tmp/misty_match.out -w "%{http_code}" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${JWT}" \
      -X POST "${API_BASE}/api/matches" \
      -d "${MATCH_PAYLOAD}")

    if [[ "${MATCH_HTTP}" != "201" && "${MATCH_HTTP}" != "200" ]]; then
      warn "Match creation failed (HTTP ${MATCH_HTTP}): $(cat /tmp/misty_match.out)"
      warn "Common cause: Ash deck id ${ASH_DECK_ID} does not exist. Run the equivalent setup for Ash first."
    else
      MATCH_ID=$(jq -r '.matchId // empty' /tmp/misty_match.out)
      ok "Match created (id = ${MATCH_ID:-unknown})"
    fi
  fi
else
  warn "CREATE_MATCH=false - skipping match creation"
fi

# ---- Summary ---------------------------------------------------------------
printf "\n%sSummary%s\n" "${C_BOLD}" "${C_RESET}"
printf "  Username  : %s\n" "${MISTY_USERNAME}"
printf "  User id   : %s\n" "${USER_ID}"
printf "  Deck id   : %s\n" "${DECK_ID:-unknown}"
printf "  Deck size : 60 (4x %s + 56x %s)\n" "${EX_CARD_ID}" "${ENERGY_CARD_ID}"
printf "  Match id  : %s\n" "${MATCH_ID:-not created}"
printf "  JWT       : %s\n" "${JWT}"
printf "\nExport for follow-up curls:\n"
printf "  export MISTY_JWT=%s\n" "${JWT}"
if [[ -n "${MATCH_ID}" ]]; then
  printf "  export MATCH_ID=%s\n" "${MATCH_ID}"
fi
]]; then
  log "Creating match: ${ASH_USERNAME} (deck ${ASH_DECK_ID}) vs ${MISTY_USERNAME} (deck ${DECK_ID})"

  if [[ -z "${DECK_ID}" ]]; then
    warn "Misty deck id is unknown - skipping match creation"
  else
    MATCH_PAYLOAD=$(jq -nc \
      --arg a "${ASH_USERNAME}" \
      --arg b "${MISTY_USERNAME}" \
      --argjson da "${ASH_DECK_ID}" \
      --argjson db "${DECK_ID}" \
      '{playerAId:$a, playerBId:$b, deckAId:$da, deckBId:$db}')

    MATCH_HTTP=$(curl -sS -o /tmp/misty_match.out -w "%{http_code}" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${JWT}" \
      -X POST "${API_BASE}/api/matches" \
      -d "${MATCH_PAYLOAD}")

    if [[ "${MATCH_HTTP}" != "201" && "${MATCH_HTTP}" != "200" ]]; then
      warn "Match creation failed (HTTP ${MATCH_HTTP}): $(cat /tmp/misty_match.out)"
      warn "Common cause: Ash deck id ${ASH_DECK_ID} does not exist. Run the equivalent setup for Ash first."
    else
      MATCH_ID=$(jq -r '.matchId // empty' /tmp/misty_match.out)
      ok "Match created (id = ${MATCH_ID:-unknown})"
    fi
  fi
else
  warn "CREATE_MATCH=false - skipping match creation"
fi

# ---- Summary ---------------------------------------------------------------
printf "\n%sSummary%s\n" "${C_BOLD}" "${C_RESET}"
printf "  Username  : %s\n" "${MISTY_USERNAME}"
printf "  User id   : %s\n" "${USER_ID}"
printf "  Deck id   : %s\n" "${DECK_ID:-unknown}"
printf "  Deck size : 60 (4x %s + 56x %s)\n" "${EX_CARD_ID}" "${ENERGY_CARD_ID}"
printf "  Match id  : %s\n" "${MATCH_ID:-not created}"
printf "  JWT       : %s\n" "${JWT}"
printf "\nExport for follow-up curls:\n"
printf "  export MISTY_JWT=%s\n" "${JWT}"
if [[ -n "${MATCH_ID}" ]]; then
  printf "  export MATCH_ID=%s\n" "${MATCH_ID}"
fi
