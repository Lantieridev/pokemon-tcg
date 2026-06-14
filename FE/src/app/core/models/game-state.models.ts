/**
 * Tipos TypeScript que reflejan EXACTAMENTE los contratos del backend.
 * Fuente de verdad: BattlePokemonDTO.java, GameStateResponseDTO.java,
 * ActionRequestDTO.java, ActionType.java, AttackDTO.java
 */

// ─── Enums (reflejan los del backend) ────────────────────────────────────────

/** Condiciones especiales — valores exactos que envía statusConditions[] del backend */
export type SpecialCondition = 'ASLEEP' | 'CONFUSED' | 'PARALYZED' | 'BURNED' | 'POISONED';

/** Tipos de acción — refleja ActionType.java */
export type ActionType =
  | 'DECLARE_ATTACK'
  | 'RETREAT'
  | 'PLAY_TRAINER'
  | 'ATTACH_ENERGY'
  | 'EVOLVE'
  | 'PLACE_BASIC_POKEMON'
  | 'USE_ABILITY'
  | 'END_TURN'
  | 'PROMOTE_ACTIVE'
  | 'SELECT_CARDS';

/** Fases del turno — currentPhase del GameStateResponseDTO */
export type TurnPhase = 'DRAW' | 'MAIN' | 'ATTACK' | 'BETWEEN_TURNS' | 'SETUP' | 'FINISHED';

/** Tipos de Pokémon (energía) — PokemonType.java */
export type PokemonType =
  | 'FIRE' | 'WATER' | 'GRASS' | 'LIGHTNING' | 'PSYCHIC'
  | 'FIGHTING' | 'DARKNESS' | 'METAL' | 'FAIRY' | 'DRAGON' | 'COLORLESS';

// ─── DTOs del backend ─────────────────────────────────────────────────────────

/** Refleja AttackDTO.java */
export interface AttackDTO {
  name: string;
  baseDamage: number;
  energyCost: PokemonType[];
}

/** Refleja BattlePokemonDTO.java */
export interface BattlePokemonDTO {
  cardId: string;           // ej: "xy1-46"
  name: string;             // ej: "Charmander"
  pokemonType: PokemonType;
  maxHp: number;
  damageCounters: number;   // cada contador = 10 daño
  isEx: boolean;
  weaknessType: PokemonType | null;
  resistanceType: PokemonType | null;
  attachedEnergies: PokemonType[];
  retreatCost: number;
  hasToolAttached: boolean;
  attachedToolCardId?: string | null;
  attacks: AttackDTO[];
  statusConditions: SpecialCondition[];  // List<String> en Java
}

/** Refleja GameStateResponseDTO.PlayerView — estado PROPIO (mano completa) */
export interface PlayerView {
  playerId: string;
  active: BattlePokemonDTO | null;
  bench: BattlePokemonDTO[];
  hand: string[];           // IDs de cartas — solo para el jugador propio
  deckSize: number;
  prizeCount: number;
}

/** Refleja GameStateResponseDTO.OpponentView — niebla de guerra aplicada */
export interface OpponentView {
  playerId: string;
  active: BattlePokemonDTO | null;
  bench: BattlePokemonDTO[];
  handSize: number;         // Solo el conteo, NUNCA los IDs
  deckSize: number;
  prizeCount: number;
}

/** Refleja PendingSelectionRequestDTO.java */
export interface PendingSelectionRequest {
  sourceEffect: string;
  targetId: string;
  maxSelections: number;
  source: string;
  options: string[];
}

/** Refleja GameStateResponseDTO.java (record raíz) */
export interface GameStateResponseDTO {
  matchId: string;
  version: number;
  turnNumber: number;
  activePlayerIndex: number;  // 0 o 1
  currentPhase: TurnPhase;
  pendingSelectionRequest: PendingSelectionRequest | null;
  activeStadiumCardId?: string | null;
  winnerId?: string | null;
  victoryReason?: string | null;
  mvpCardId?: string | null;
  mvpCardDamage?: number | null;
  self: PlayerView;
  opponent: OpponentView;
  lastCoinFlips?: boolean[] | null;
  mmrChange?: number | null;
  coinsGained?: number | null;
  xpGained?: number | null;
}

// ─── DTOs de acción (cliente → servidor) ─────────────────────────────────────

/** Refleja ActionRequestDTO.java */
export interface ActionRequestDTO {
  type: ActionType;
  cardId?: string | null;
  targetId?: string | null;
  targetIndex?: number | null;
  trainerType?: string | null;
  attackIndex?: number | null;
  energyType?: PokemonType | null;
  selectedEnergyIndices?: number[];
  sourceIndex?: number | null;
  selectedCardIds?: string[];
}

// ─── DTOs de Auth ─────────────────────────────────────────────────────────────

/** Refleja AuthResponseDTO.java */
export interface AuthResponseDTO {
  token: string;
  username: string;
  userId: number;
}

// ─── DTOs de Deck ────────────────────────────────────────────────────────────

/** Refleja DeckCardRequestDTO.java */
export interface DeckCardRequestDTO {
  cardId: string;
  quantity: number;
}

/** Refleja DeckRequestDTO.java */
export interface DeckRequestDTO {
  userId: number;
  name: string;
  cards: DeckCardRequestDTO[];
}

/** Refleja DeckResponseDTO.java */
export interface DeckResponseDTO {
  id: number;
  name: string;
  createdAt: string;
  cards: { cardId: string; quantity: number }[];
}

/** Refleja DeckSummaryDTO.java */
export interface DeckSummaryDTO {
  id: number;
  name: string;
  createdAt: string;
  totalCards: number;
}

// ─── Modelos de la API pokemontcg.io (solo para Deck Builder) ────────────────

/** Estructura de carta de pokemontcg.io/v2/cards */
export interface PokemonTcgCard {
  id: string;           // ej: "xy1-1"
  name: string;
  supertype: 'Pokémon' | 'Trainer' | 'Energy';
  subtypes: string[];   // ej: ["Basic"], ["ACE SPEC"], ["Stage 1"], ["Basic", "EX"]
  types?: string[];     // ej: ["Fire"]
  hp?: string;
  images: {
    small: string;
    large: string;
  };
  set: {
    id: string;         // "xy1"
  };
}
