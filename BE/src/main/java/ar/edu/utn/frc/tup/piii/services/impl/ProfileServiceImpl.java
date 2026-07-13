package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.HonorType;
import ar.edu.utn.frc.tup.piii.dtos.UpdateProfileRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.UpdateShowcaseRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.UserProfileResponseDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserShowcaseEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.DeckEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserShowcaseRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserShowcaseInventoryRepository;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserShowcaseInventoryEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.DeckRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserCardStatRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserEnergyStatRepository;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserCardStatEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEnergyStatEntity;
import ar.edu.utn.frc.tup.piii.persistence.mapper.CardMapper;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.services.HonorService;
import ar.edu.utn.frc.tup.piii.services.ProfileService;
import ar.edu.utn.frc.tup.piii.services.ProfanityFilterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    /**
     * Single source of truth for every achievement: title, category, unlock requirement text,
     * target, reward, and how to read its progress out of an {@link AchievementContext}.
     * Both {@link #checkAndUnlockTitles} and {@link #getAchievementsProgress} iterate this list
     * instead of hand-duplicating each achievement's data in two (or three) places.
     */
    private record AchievementDefinition(
            String title, String category, String requirement, int target,
            String rewardType, String rewardValue, ToIntFunction<AchievementContext> progress) {
    }

    /** Card/energy usage breakdown shared by unlock-checking and progress-listing. */
    private record ElementalStats(
            int versatilityCount,
            int pikachuPlays, int charizardPlays, int blastoisePlays, int venusaurPlays, int mewtwoPlays,
            int fireAttached, int waterAttached, int grassAttached, int lightningAttached,
            int psychicAttached, int fightingAttached, int colorlessAttached) {
    }

    /** All the stats an {@link AchievementDefinition#progress()} function may need to read. */
    private record AchievementContext(
            int level, int matchesWon, int losses, int completedMatches, int uniqueCardsCount,
            int totalHonors, int mmr, int pokecoins, int battlePoints, int totalDamageDealt,
            int totalKos, int perfectWins, int comebackWins, int trainerCardsPlayed,
            ElementalStats elemental, Set<String> unlockedTitles) {
    }

    private static final List<AchievementDefinition> ACHIEVEMENTS = List.of(
        new AchievementDefinition("Novato", "DEFECTO", "Título inicial por defecto", 1, "TITULO", "Novato", ctx -> 1),
        new AchievementDefinition("Entrenador", "DEFECTO", "Título inicial por defecto", 1, "TITULO", "Entrenador", ctx -> 1),

        new AchievementDefinition("Estratega en Crecimiento", "NIVEL", "Alcanzar nivel 5", 5, "TITULO", "Estratega en Crecimiento", AchievementContext::level),
        new AchievementDefinition("Maestro de Cartas", "NIVEL", "Alcanzar nivel 10", 10, "TITULO", "Maestro de Cartas", AchievementContext::level),
        new AchievementDefinition("Gran Mentor", "NIVEL", "Alcanzar nivel 20", 20, "FOTO_PERFIL", "avatar_bulbasaur", AchievementContext::level),
        new AchievementDefinition("Líder de Élite", "NIVEL", "Alcanzar nivel 30", 30, "FOTO_PERFIL", "avatar_charizard_3d", AchievementContext::level),
        new AchievementDefinition("Leyenda Viviente", "NIVEL", "Alcanzar nivel 50", 50, "MEDALLA", "medal_legend", AchievementContext::level),
        new AchievementDefinition("Maestro de Kanto", "NIVEL", "Alcanzar nivel 100", 100, "FOTO_PERFIL", "avatar_mewtwo_3d", AchievementContext::level),

        new AchievementDefinition("Ganador Prometedor", "VICTORIAS", "Ganar 5 partidas", 5, "TITULO", "Ganador Prometedor", AchievementContext::matchesWon),
        new AchievementDefinition("Ganador Implacable", "VICTORIAS", "Ganar 20 partidas", 20, "FOTO_PERFIL", "avatar_winner_badge", AchievementContext::matchesWon),
        new AchievementDefinition("Campeón del Tablero", "VICTORIAS", "Ganar 50 partidas", 50, "MEDALLA", "medal_champion", AchievementContext::matchesWon),
        new AchievementDefinition("Leyenda del Tablero", "VICTORIAS", "Ganar 100 partidas", 100, "MEDALLA", "medal_board_legend", AchievementContext::matchesWon),
        new AchievementDefinition("Inmortal del Tablero", "VICTORIAS", "Ganar 250 partidas", 250, "FOTO_PERFIL", "avatar_red_champ", AchievementContext::matchesWon),

        new AchievementDefinition("Espíritu Resiliente", "RESILIENCIA", "Perder 10 partidas en total", 10, "TITULO", "Espíritu Resiliente", AchievementContext::losses),
        new AchievementDefinition("Fuerza de Voluntad", "RESILIENCIA", "Perder 50 partidas en total", 50, "FOTO_PERFIL", "avatar_resilience_mid", AchievementContext::losses),

        new AchievementDefinition("Combatiente", "PARTIDAS_JUGADAS", "Jugar 10 partidas completas", 10, "TITULO", "Combatiente", AchievementContext::completedMatches),
        new AchievementDefinition("Combatiente Tenaz", "PARTIDAS_JUGADAS", "Jugar 25 partidas completas", 25, "TITULO", "Combatiente Tenaz", AchievementContext::completedMatches),
        new AchievementDefinition("Veterano de Batallas", "PARTIDAS_JUGADAS", "Jugar 50 partidas completas", 50, "MEDALLA", "medal_veteran", AchievementContext::completedMatches),
        new AchievementDefinition("Leyenda de Batallas", "PARTIDAS_JUGADAS", "Jugar 100 partidas completas", 100, "FOTO_PERFIL", "avatar_lucas_legend", AchievementContext::completedMatches),
        new AchievementDefinition("Espíritu Inquebrantable", "PARTIDAS_JUGADAS", "Jugar 250 partidas completas", 250, "FOTO_PERFIL", "avatar_eevee_3d", AchievementContext::completedMatches),

        new AchievementDefinition("Coleccionista Novato", "COLECCION", "Tener 30 cartas distintas en tus mazos", 30, "TITULO", "Coleccionista Novato", AchievementContext::uniqueCardsCount),
        new AchievementDefinition("Coleccionista Experto", "COLECCION", "Tener 50 cartas distintas en tus mazos", 50, "TITULO", "Coleccionista Experto", AchievementContext::uniqueCardsCount),
        new AchievementDefinition("Coleccionista de Élite", "COLECCION", "Tener 100 cartas distintas en tus mazos", 100, "MEDALLA", "medal_collector_elite", AchievementContext::uniqueCardsCount),
        new AchievementDefinition("Maestro Coleccionista", "COLECCION", "Tener 150 cartas distintas en tus mazos", 150, "MEDALLA", "medal_collector_legend", AchievementContext::uniqueCardsCount),
        new AchievementDefinition("Curador del Museo", "COLECCION", "Tener 200 cartas distintas en tus mazos", 200, "FOTO_PERFIL", "avatar_collector_legend", AchievementContext::uniqueCardsCount),

        new AchievementDefinition("Compañero Amigable", "HONORES", "Recibir 5 honores de otros jugadores", 5, "TITULO", "Compañero Amigable", AchievementContext::totalHonors),
        new AchievementDefinition("Entrenador Respetado", "HONORES", "Recibir 15 honores de otros jugadores", 15, "TITULO", "Entrenador Respetado", AchievementContext::totalHonors),
        new AchievementDefinition("Héroe del Fair Play", "HONORES", "Recibir 30 honores de otros jugadores", 30, "MEDALLA", "medal_fair_play_legend", AchievementContext::totalHonors),

        new AchievementDefinition("Entrenador Destacado", "COMPETITIVO", "Alcanzar 1200 de MMR", 1200, "TITULO", "Entrenador Destacado", AchievementContext::mmr),
        new AchievementDefinition("Líder de Gimnasio", "COMPETITIVO", "Alcanzar 1500 de MMR", 1500, "FOTO_PERFIL", "avatar_gym_leader", AchievementContext::mmr),
        new AchievementDefinition("Alto Mando", "COMPETITIVO", "Alcanzar 1800 de MMR", 1800, "TITULO", "Alto Mando", AchievementContext::mmr),
        new AchievementDefinition("Campeón de la Liga", "COMPETITIVO", "Alcanzar 2000 de MMR", 2000, "MEDALLA", "medal_league_champion", AchievementContext::mmr),

        new AchievementDefinition("Estratega Versátil", "VERSATILIDAD", "Jugar 20 cartas diferentes en partidas", 20, "FOTO_PERFIL", "avatar_versatility_mid", ctx -> ctx.elemental().versatilityCount()),
        new AchievementDefinition("Maestro Adaptable", "VERSATILIDAD", "Jugar 50 cartas diferentes en partidas", 50, "FOTO_PERFIL", "avatar_versatility_3d", ctx -> ctx.elemental().versatilityCount()),

        new AchievementDefinition("Súper Nerd de las Ventas", "ECONOMIA", "Acumular 1,000 Pokecoins", 1000, "MEDALLA", "medal_coins_1k", AchievementContext::pokecoins),
        new AchievementDefinition("Magnate de Kanto", "ECONOMIA", "Acumular 5,000 Pokecoins", 5000, "MEDALLA", "medal_magnate_gold", AchievementContext::pokecoins),
        new AchievementDefinition("Gladiador del Tablero", "ECONOMIA", "Acumular 500 Battle Points", 500, "TITULO", "Gladiador del Tablero", AchievementContext::battlePoints),
        new AchievementDefinition("Campeón del Coliseo", "ECONOMIA", "Acumular 2,000 Battle Points", 2000, "MEDALLA", "medal_colosseum_legend", AchievementContext::battlePoints),

        new AchievementDefinition("Poder Eléctrico", "COMBATE", "Infligir 1,000 puntos de daño en total", 1000, "MEDALLA", "medal_power_1k", AchievementContext::totalDamageDealt),
        new AchievementDefinition("Fuerza Brutal", "COMBATE", "Infligir 5,000 puntos de daño en total", 5000, "TITULO", "Fuerza Brutal", AchievementContext::totalDamageDealt),
        new AchievementDefinition("Fuerza de la Naturaleza", "COMBATE", "Infligir 15,000 puntos de daño en total", 15000, "FOTO_PERFIL", "avatar_nature_force", AchievementContext::totalDamageDealt),
        new AchievementDefinition("Destructor Cósmico", "COMBATE", "Infligir 50,000 puntos de daño en total", 50000, "TITULO", "Destructor Cósmico", AchievementContext::totalDamageDealt),

        new AchievementDefinition("Derribador", "COMBATE", "Realizar 10 KOs en total", 10, "MEDALLA", "medal_kos_10", AchievementContext::totalKos),
        new AchievementDefinition("Cazador de KOs", "COMBATE", "Realizar 50 KOs en total", 50, "TITULO", "Cazador de KOs", AchievementContext::totalKos),
        new AchievementDefinition("Ejecutor Implacable", "COMBATE", "Realizar 150 KOs en total", 150, "FOTO_PERFIL", "avatar_executor_mid", AchievementContext::totalKos),
        new AchievementDefinition("Verdugo Supremo", "COMBATE", "Realizar 300 KOs en total", 300, "TITULO", "Verdugo Supremo", AchievementContext::totalKos),

        new AchievementDefinition("Estratega Imbatible", "COMBATE", "Conseguir 1 victoria perfecta (sin sufrir KOs)", 1, "MEDALLA", "medal_perfect_1", AchievementContext::perfectWins),
        new AchievementDefinition("Intocable", "COMBATE", "Conseguir 5 victorias perfectas", 5, "TITULO", "Intocable", AchievementContext::perfectWins),
        new AchievementDefinition("Inmaculado", "COMBATE", "Conseguir 15 victorias perfectas", 15, "TITULO", "Inmaculado", AchievementContext::perfectWins),

        new AchievementDefinition("Rey del Clímax", "COMBATE", "Conseguir 1 victoria tras remontada", 1, "MEDALLA", "medal_comeback_1", AchievementContext::comebackWins),
        new AchievementDefinition("Espíritu de Remontada", "COMBATE", "Conseguir 5 victorias tras remontada", 5, "TITULO", "Espíritu de Remontada", AchievementContext::comebackWins),
        new AchievementDefinition("Fénix del Tablero", "COMBATE", "Conseguir 15 victorias tras remontada", 15, "TITULO", "Fénix del Tablero", AchievementContext::comebackWins),

        new AchievementDefinition("Estudioso de Reglas", "COMBATE", "Jugar 50 cartas de Entrenador", 50, "FOTO_PERFIL", "avatar_rules_student", AchievementContext::trainerCardsPlayed),
        new AchievementDefinition("Maestro Táctico", "COMBATE", "Jugar 200 cartas de Entrenador", 200, "TITULO", "Maestro Táctico", AchievementContext::trainerCardsPlayed),
        new AchievementDefinition("Gran Sabio", "COMBATE", "Jugar 500 cartas de Entrenador", 500, "TITULO", "Gran Sabio", AchievementContext::trainerCardsPlayed),

        new AchievementDefinition("Piro-Novato", "ELEMENTAL", "Unir 50 energías de Fuego", 50, "MEDALLA", "medal_fire_50", ctx -> ctx.elemental().fireAttached()),
        new AchievementDefinition("Piro-Maestro", "ELEMENTAL", "Unir 200 energías de Fuego", 200, "MEDALLA", "medal_fire_200", ctx -> ctx.elemental().fireAttached()),
        new AchievementDefinition("Llama de Kanto", "ELEMENTAL", "Unir 500 energías de Fuego", 500, "FOTO_PERFIL", "avatar_fire_kanto", ctx -> ctx.elemental().fireAttached()),

        new AchievementDefinition("Hidro-Novato", "ELEMENTAL", "Unir 50 energías de Agua", 50, "MEDALLA", "medal_water_50", ctx -> ctx.elemental().waterAttached()),
        new AchievementDefinition("Maestro del Surf", "ELEMENTAL", "Unir 200 energías de Agua", 200, "MEDALLA", "medal_water_200", ctx -> ctx.elemental().waterAttached()),
        new AchievementDefinition("Tsunami Viviente", "ELEMENTAL", "Unir 500 energías de Agua", 500, "FOTO_PERFIL", "avatar_water_kanto", ctx -> ctx.elemental().waterAttached()),

        new AchievementDefinition("Brote Verde", "ELEMENTAL", "Unir 50 energías de Planta", 50, "MEDALLA", "medal_grass_50", ctx -> ctx.elemental().grassAttached()),
        new AchievementDefinition("Guardián de la Selva", "ELEMENTAL", "Unir 200 energías de Planta", 200, "MEDALLA", "medal_grass_200", ctx -> ctx.elemental().grassAttached()),
        new AchievementDefinition("Espíritu del Bosque", "ELEMENTAL", "Unir 500 energías de Planta", 500, "FOTO_PERFIL", "avatar_grass_kanto", ctx -> ctx.elemental().grassAttached()),

        new AchievementDefinition("Chispa Inicial", "ELEMENTAL", "Unir 50 energías de Rayo", 50, "MEDALLA", "medal_lightning_50", ctx -> ctx.elemental().lightningAttached()),
        new AchievementDefinition("Voltaje Máximo", "ELEMENTAL", "Unir 200 energías de Rayo", 200, "MEDALLA", "medal_lightning_200", ctx -> ctx.elemental().lightningAttached()),
        new AchievementDefinition("Tormenta Perpetua", "ELEMENTAL", "Unir 500 energías de Rayo", 500, "FOTO_PERFIL", "avatar_lightning_kanto", ctx -> ctx.elemental().lightningAttached()),

        new AchievementDefinition("Sensitivo", "ELEMENTAL", "Unir 50 energías Psíquicas", 50, "MEDALLA", "medal_psychic_50", ctx -> ctx.elemental().psychicAttached()),
        new AchievementDefinition("Mente Mística", "ELEMENTAL", "Unir 200 energías Psíquicas", 200, "MEDALLA", "medal_psychic_200", ctx -> ctx.elemental().psychicAttached()),
        new AchievementDefinition("Poder Cósmico", "ELEMENTAL", "Unir 500 energías Psíquicas", 500, "FOTO_PERFIL", "avatar_psychic_kanto", ctx -> ctx.elemental().psychicAttached()),

        new AchievementDefinition("Cinturón Blanco", "ELEMENTAL", "Unir 50 energías de Lucha", 50, "FOTO_PERFIL", "avatar_belt_white", ctx -> ctx.elemental().fightingAttached()),
        new AchievementDefinition("Cinturón Negro", "ELEMENTAL", "Unir 200 energías de Lucha", 200, "TITULO", "Cinturón Negro", ctx -> ctx.elemental().fightingAttached()),
        new AchievementDefinition("Fuerza Sísmica", "ELEMENTAL", "Unir 500 energías de Lucha", 500, "FOTO_PERFIL", "avatar_fighting_kanto", ctx -> ctx.elemental().fightingAttached()),

        new AchievementDefinition("Equilibrio", "ELEMENTAL", "Unir 50 energías Incoloras", 50, "FOTO_PERFIL", "avatar_neutral_balance", ctx -> ctx.elemental().colorlessAttached()),
        new AchievementDefinition("Estratega Neutral", "ELEMENTAL", "Unir 200 energías Incoloras", 200, "TITULO", "Estratega Neutral", ctx -> ctx.elemental().colorlessAttached()),
        new AchievementDefinition("Armonía Pura", "ELEMENTAL", "Unir 500 energías Incoloras", 500, "FOTO_PERFIL", "avatar_colorless_kanto", ctx -> ctx.elemental().colorlessAttached()),

        new AchievementDefinition("Amigo del Ratón", "LEALTAD", "Jugar cartas de Pikachu 15 veces", 15, "FOTO_PERFIL", "avatar_pikachu_cute", ctx -> ctx.elemental().pikachuPlays()),
        new AchievementDefinition("Compañero Fiel", "LEALTAD", "Jugar cartas de Pikachu 50 veces", 50, "TITULO", "Compañero Fiel", ctx -> ctx.elemental().pikachuPlays()),

        new AchievementDefinition("Aliento Ígneo", "LEALTAD", "Jugar cartas de Charizard 10 veces", 10, "FOTO_PERFIL", "avatar_charizard_cute", ctx -> ctx.elemental().charizardPlays()),
        new AchievementDefinition("Llama Ancestral", "LEALTAD", "Jugar cartas de Charizard 30 veces", 30, "TITULO", "Llama Ancestral", ctx -> ctx.elemental().charizardPlays()),

        new AchievementDefinition("Presión de Agua", "LEALTAD", "Jugar cartas de Blastoise 10 veces", 10, "FOTO_PERFIL", "avatar_blastoise_cute", ctx -> ctx.elemental().blastoisePlays()),
        new AchievementDefinition("Tsunami de Kanto", "LEALTAD", "Jugar cartas de Blastoise 30 veces", 30, "TITULO", "Tsunami de Kanto", ctx -> ctx.elemental().blastoisePlays()),

        new AchievementDefinition("Floración Rápida", "LEALTAD", "Jugar cartas de Venusaur 10 veces", 10, "FOTO_PERFIL", "avatar_venusaur_cute", ctx -> ctx.elemental().venusaurPlays()),
        new AchievementDefinition("Semilla de la Vida", "LEALTAD", "Jugar cartas de Venusaur 30 veces", 30, "TITULO", "Semilla de la Vida", ctx -> ctx.elemental().venusaurPlays()),

        new AchievementDefinition("Mirada Mental", "LEALTAD", "Jugar cartas de Mewtwo 10 veces", 10, "FOTO_PERFIL", "avatar_mewtwo_cute", ctx -> ctx.elemental().mewtwoPlays()),
        new AchievementDefinition("Fuerza Psíquica", "LEALTAD", "Jugar cartas de Mewtwo 30 veces", 30, "TITULO", "Fuerza Psíquica", ctx -> ctx.elemental().mewtwoPlays()),

        // Evaluated last on purpose: progress reads titles.size(), so it must see every title
        // unlocked earlier in this same pass (see checkAndUnlockTitles).
        new AchievementDefinition("Multifacético", "TITULOS", "Desbloquear 10 títulos", 10, "FOTO_PERFIL", "avatar_multifaceted", ctx -> ctx.unlockedTitles().size()),
        new AchievementDefinition("Celebridad de Kanto", "TITULOS", "Desbloquear 20 títulos", 20, "FOTO_PERFIL", "avatar_celebrity", ctx -> ctx.unlockedTitles().size())
    );

    private final UserRepository userRepository;
    private final UserShowcaseRepository userShowcaseRepository;
    private final MatchRepository matchRepository;
    private final CardRepository cardRepository;
    private final HonorService honorService;
    private final DeckRepository deckRepository;
    private final ProfanityFilterService profanityFilterService;
    private final UserCardStatRepository userCardStatRepository;
    private final UserEnergyStatRepository userEnergyStatRepository;
    private final UserShowcaseInventoryRepository userShowcaseInventoryRepository;
    private final CardMapper cardMapper;

    public ProfileServiceImpl(final UserRepository userRepository,
                              final UserShowcaseRepository userShowcaseRepository,
                              final MatchRepository matchRepository,
                              final CardRepository cardRepository,
                              final HonorService honorService,
                              final DeckRepository deckRepository,
                              final ProfanityFilterService profanityFilterService,
                              final UserCardStatRepository userCardStatRepository,
                              final UserEnergyStatRepository userEnergyStatRepository,
                              final UserShowcaseInventoryRepository userShowcaseInventoryRepository,
                              final CardMapper cardMapper) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.userShowcaseRepository = Objects.requireNonNull(userShowcaseRepository, "userShowcaseRepository must not be null");
        this.matchRepository = Objects.requireNonNull(matchRepository, "matchRepository must not be null");
        this.cardRepository = Objects.requireNonNull(cardRepository, "cardRepository must not be null");
        this.honorService = Objects.requireNonNull(honorService, "honorService must not be null");
        this.deckRepository = Objects.requireNonNull(deckRepository, "deckRepository must not be null");
        this.profanityFilterService = Objects.requireNonNull(profanityFilterService, "profanityFilterService must not be null");
        this.userCardStatRepository = Objects.requireNonNull(userCardStatRepository, "userCardStatRepository must not be null");
        this.userEnergyStatRepository = Objects.requireNonNull(userEnergyStatRepository, "userEnergyStatRepository must not be null");
        this.userShowcaseInventoryRepository = Objects.requireNonNull(userShowcaseInventoryRepository, "userShowcaseInventoryRepository must not be null");
        this.cardMapper = Objects.requireNonNull(cardMapper, "cardMapper must not be null");
    }

    @Override
    @Transactional
    public UserProfileResponseDTO getProfile(final String username) {
        final Optional<UserEntity> userOpt = userRepository.findFirstByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        final UserEntity user = userOpt.get();

        // 1. Estadísticas de partidas
        final List<ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto> matches = matchRepository.findMatchesByUsername(username);
        int matchesPlayed = matches.size();
        int matchesWon = 0;
        for (final ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto m : matches) {
            if (m.winnerUsername() != null && m.winnerUsername().equalsIgnoreCase(username)) {
                matchesWon++;
            }
        }
        int matchesLost = matchesPlayed - matchesWon;
        double winRate = matchesPlayed > 0 ? (matchesWon * 100.0) / matchesPlayed : 0.0;
        // Redondear a dos decimales
        winRate = Math.round(winRate * 100.0) / 100.0;

        // Calcular racha de victorias actual
        int winStreak = 0;
        for (final ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto m : matches) {
            if (m.status() != null && (m.status().equalsIgnoreCase("FINISHED") || m.status().equalsIgnoreCase("COMPLETED"))) {
                if (m.winnerUsername() != null) {
                    if (m.winnerUsername().equalsIgnoreCase(username)) {
                        winStreak++;
                    } else {
                        break; // Se cortó la racha
                    }
                }
            }
        }

        final UserProfileResponseDTO.Statistics stats = UserProfileResponseDTO.Statistics.builder()
                .matchesPlayed(matchesPlayed)
                .matchesWon(matchesWon)
                .matchesLost(matchesLost)
                .winRate(winRate)
                .perfectWins(user.getPerfectWins() != null ? user.getPerfectWins() : 0)
                .comebackWins(user.getComebackWins() != null ? user.getComebackWins() : 0)
                .totalKos(user.getTotalKos() != null ? user.getTotalKos() : 0)
                .trainerCardsPlayed(user.getTrainerCardsPlayed() != null ? user.getTrainerCardsPlayed() : 0)
                .totalDamageDealt(user.getTotalDamageDealt() != null ? user.getTotalDamageDealt() : 0)
                .winStreak(winStreak)
                .build();

        // 2. Honores recibidos
        final Map<HonorType, Integer> honors = honorService.getHonors(username);
        final int totalHonors = honors.values().stream().mapToInt(Integer::intValue).sum();

        // 2.1. Calcular partidas terminadas totalmente
        int completedMatchesPlayed = 0;
        for (final ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto m : matches) {
            if (m.status() != null && (m.status().equalsIgnoreCase("FINISHED") || m.status().equalsIgnoreCase("COMPLETED"))) {
                completedMatchesPlayed++;
            }
        }

        // 2.2. Chequear y Desbloquear Títulos antes de armar la respuesta
        final int uniqueCardsCount = deckRepository.countUniqueCardsByUserId(user.getId());
        checkAndUnlockTitles(user, completedMatchesPlayed, matchesWon, totalHonors, uniqueCardsCount);
        userRepository.save(user);

        // 3. Showcase (Vitrina)
        final List<UserShowcaseEntity> showcaseEntities = userShowcaseRepository.findByUserUsernameOrderBySlotPositionAsc(username);
        final List<UserProfileResponseDTO.ShowcaseSlot> showcaseSlots = new ArrayList<>();
        for (final UserShowcaseEntity entity : showcaseEntities) {
            String cardName = "Carta Desconocida";
            final Optional<CardEntity> cardOpt = cardRepository.findById(entity.getCardId());
            if (cardOpt.isPresent()) {
                cardName = cardOpt.get().getName();
            }
            showcaseSlots.add(UserProfileResponseDTO.ShowcaseSlot.builder()
                    .slotPosition(entity.getSlotPosition())
                    .cardId(entity.getCardId())
                    .cardName(cardName)
                    .build());
        }

        // 4. XP necesario para el próximo nivel
        final int currentLevel = user.getLevel() != null ? user.getLevel() : 1;
        final int currentXp = user.getXp() != null ? user.getXp() : 0;
        final int xpToNext = getXpNeededForNextLevel(currentLevel);

        UserProfileResponseDTO.ShowcasedDeck showcasedDeckDto = null;
        if (user.getShowcasedDeck() != null) {
            List<UserProfileResponseDTO.ShowcasedDeckCard> cardsList = new ArrayList<>();
            if (user.getShowcasedDeck().getCards() != null) {
                for (DeckCardEntity dce : user.getShowcasedDeck().getCards()) {
                    cardsList.add(UserProfileResponseDTO.ShowcasedDeckCard.builder()
                            .cardId(dce.getCard().getId())
                            .cardName(dce.getCard().getName())
                            .quantity(dce.getQuantity())
                            .build());
                }
            }
            showcasedDeckDto = UserProfileResponseDTO.ShowcasedDeck.builder()
                    .id(user.getShowcasedDeck().getId())
                    .name(user.getShowcasedDeck().getName())
                    .cards(cardsList)
                    .build();
        }

        // 5. Advanced statistics
        final List<UserCardStatEntity> cardStatEntities = userCardStatRepository.findByUserId(user.getId());
        final List<UserProfileResponseDTO.CardStatDTO> cardStatDTOs = new ArrayList<>();
        int totalDmgDealt = 0;
        int totalDmgReceived = 0;
        int totalKOsMade = 0;
        int totalKOsSuffered = 0;

        for (final UserCardStatEntity statEntity : cardStatEntities) {
            String cardName = "Carta Desconocida";
            String pokemonType = "COLORLESS";
            
            final Optional<CardEntity> cardEntityOpt = cardRepository.findById(statEntity.getCardId());
            if (cardEntityOpt.isPresent()) {
                final CardEntity cardEntity = cardEntityOpt.get();
                cardName = cardEntity.getName();
                
                try {
                    final Card domainCard = cardMapper.map(cardEntity);
                    if (domainCard instanceof PokemonCard pc) {
                        pokemonType = pc.getPokemonType().name();
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Could not map card {} to a domain Card for pokemonType lookup: {}", cardEntity.getId(), e.getMessage());
                }
            }

            totalDmgDealt += statEntity.getDamageDealt();
            totalDmgReceived += statEntity.getDamageReceived();
            totalKOsMade += statEntity.getKosMade();
            totalKOsSuffered += statEntity.getKosSuffered();

            cardStatDTOs.add(UserProfileResponseDTO.CardStatDTO.builder()
                    .cardId(statEntity.getCardId())
                    .cardName(cardName)
                    .pokemonType(pokemonType)
                    .timesPlayed(statEntity.getTimesPlayed())
                    .damageDealt(statEntity.getDamageDealt())
                    .damageReceived(statEntity.getDamageReceived())
                    .kosMade(statEntity.getKosMade())
                    .kosSuffered(statEntity.getKosSuffered())
                    .build());
        }

        final List<UserEnergyStatEntity> energyStatEntities = userEnergyStatRepository.findByUserId(user.getId());
        final List<UserProfileResponseDTO.EnergyStatDTO> energyStatDTOs = new ArrayList<>();
        for (final UserEnergyStatEntity energyEntity : energyStatEntities) {
            energyStatDTOs.add(UserProfileResponseDTO.EnergyStatDTO.builder()
                    .energyType(energyEntity.getEnergyType())
                    .count(energyEntity.getTimesPlayed())
                    .build());
        }

        final UserProfileResponseDTO.AdvancedStatsDTO advancedStatsDTO = UserProfileResponseDTO.AdvancedStatsDTO.builder()
                .pokemonStats(cardStatDTOs)
                .energyStats(energyStatDTOs)
                .totalDamageDealt(totalDmgDealt)
                .totalDamageReceived(totalDmgReceived)
                .totalKOsMade(totalKOsMade)
                .totalKOsSuffered(totalKOsSuffered)
                .build();

        Map<String, Integer> pInv = user.getPacksInventory() != null ? new java.util.HashMap<>(user.getPacksInventory()) : new java.util.HashMap<>();
        int totalPacks = user.getPacks() != null ? user.getPacks() : 0;

        List<UserShowcaseInventoryEntity> userInventory = userShowcaseInventoryRepository.findByUserId(user.getId());
        List<UserProfileResponseDTO.CollectedCardDTO> packCollection = new ArrayList<>();
        
        for (UserShowcaseInventoryEntity invEntity : userInventory) {
            String cardName = "Carta Desconocida";
            String rarity = "COMUN";
            Optional<CardEntity> cardEntityOpt = cardRepository.findById(invEntity.getCardId());
            if (cardEntityOpt.isPresent()) {
                CardEntity cardEntity = cardEntityOpt.get();
                cardName = cardEntity.getName();
                String subtype = cardEntity.getSubtype() != null ? cardEntity.getSubtype().toUpperCase() : "";
                
                if (subtype.contains("EX") || subtype.contains("GX") || subtype.contains(" V") || subtype.equals("V") || subtype.contains("MEGA") || subtype.contains("LEGEND")) {
                    rarity = "LEGENDARIA";
                } else if (subtype.contains("STAGE 2")) {
                    rarity = "EPICA";
                } else if (subtype.contains("STAGE 1")) {
                    rarity = "RARA";
                }
            }
            packCollection.add(UserProfileResponseDTO.CollectedCardDTO.builder()
                    .cardId(invEntity.getCardId())
                    .cardName(cardName)
                    .isFoil(invEntity.getIsFoil() != null ? invEntity.getIsFoil() : false)
                    .rarity(rarity)
                    .build());
        }

        return UserProfileResponseDTO.builder()
                .username(user.getUsername())
                .createdAt(user.getCreatedAt())
                .avatarIcon(user.getAvatarIcon())
                .description(user.getDescription())
                .activeTitle(user.getActiveTitle())
                .selectedMedals(user.getSelectedMedals())
                .level(currentLevel)
                .xp(currentXp)
                .xpToNextLevel(xpToNext)
                .mmr(user.getMmr() != null ? user.getMmr() : 1000)
                .pokecoins(user.getPokecoins() != null ? user.getPokecoins() : 0)
                .battlePoints(user.getBattlePoints() != null ? user.getBattlePoints() : 0)
                .packs(totalPacks)
                .packsInventory(pInv)
                .statistics(stats)
                .honors(honors.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), java.util.Map.Entry::getValue)))
                .unlockedTitles(user.getUnlockedTitles() != null ? new java.util.ArrayList<>(user.getUnlockedTitles()) : new java.util.ArrayList<>())
                .unlockedAvatars(user.getUnlockedAvatars() != null ? new java.util.ArrayList<>(user.getUnlockedAvatars()) : new java.util.ArrayList<>())
                .showcase(showcaseSlots)
                .packCollection(packCollection)
                .showcasedDeck(showcasedDeckDto)
                .advancedStats(advancedStatsDTO)
                .build();
    }

    @Override
    public void updateProfile(final String username, final UpdateProfileRequestDTO request) {
        if (username == null || request == null) {
            return;
        }
        final Optional<UserEntity> userOpt = userRepository.findFirstByUsername(username);
        if (userOpt.isPresent()) {
            final UserEntity user = userOpt.get();
            if (request.getAvatarIcon() != null) {
                user.setAvatarIcon(request.getAvatarIcon());
            }
            if (request.getDescription() != null) {
                if (request.getDescription().length() > 150) {
                    throw new IllegalArgumentException("La descripción no puede superar los 150 caracteres.");
                }
                final List<String> profanities = profanityFilterService.getProfaneWords(request.getDescription());
                if (!profanities.isEmpty()) {
                    throw new IllegalArgumentException("La descripción contiene palabras no permitidas: " + String.join(", ", profanities));
                }
                user.setDescription(request.getDescription());
            }
            if (request.getActiveTitle() != null) {
                if (request.getActiveTitle().trim().isEmpty()) {
                    user.setActiveTitle(null);
                } else if (user.getUnlockedTitles().contains(request.getActiveTitle())) {
                    user.setActiveTitle(request.getActiveTitle());
                }
            }
            if (request.getSelectedMedals() != null) {
                user.setSelectedMedals(request.getSelectedMedals());
            }
            userRepository.save(user);
        }
    }

    @Override
    public void updateShowcase(final String username, final UpdateShowcaseRequestDTO request) {
        if (username == null || request == null || request.getSlots() == null) {
            return;
        }
        final Optional<UserEntity> userOpt = userRepository.findFirstByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }
        final UserEntity user = userOpt.get();

        for (final UpdateShowcaseRequestDTO.ShowcaseSlot slot : request.getSlots()) {
            if (slot.getSlotPosition() == null || slot.getSlotPosition() < 1 || slot.getSlotPosition() > 3) {
                continue;
            }

            final Optional<UserShowcaseEntity> existingOpt = userShowcaseRepository.findByUserAndSlotPosition(user, slot.getSlotPosition());
            if (slot.getCardId() == null || slot.getCardId().trim().isEmpty()) {
                // Si la carta viene vacía, elimina ese slot de la vitrina
                existingOpt.ifPresent(userShowcaseRepository::delete);
            } else {
                // Verificar si la carta existe en base de datos y si el usuario la posee
                if (!cardRepository.existsById(slot.getCardId())) {
                    throw new IllegalArgumentException("La carta no existe: " + slot.getCardId());
                }
                if (existingOpt.isPresent()) {
                    final UserShowcaseEntity showcase = existingOpt.get();
                    showcase.setCardId(slot.getCardId());
                    userShowcaseRepository.save(showcase);
                } else {
                    userShowcaseRepository.save(UserShowcaseEntity.builder()
                            .user(user)
                            .cardId(slot.getCardId())
                            .slotPosition(slot.getSlotPosition())
                            .build());
                }
            }
        }
    }

    @Override
    public void awardXpAndCheckAchievements(final Long userId, final boolean won, final boolean isPerfectWin, final boolean isComebackWin, final int kos) {
        if (userId == null) {
            return;
        }
        final Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return;
        }
        final UserEntity user = userOpt.get();

        // Increment statistics
        if (isPerfectWin) {
            user.setPerfectWins((user.getPerfectWins() != null ? user.getPerfectWins() : 0) + 1);
        }
        if (isComebackWin) {
            user.setComebackWins((user.getComebackWins() != null ? user.getComebackWins() : 0) + 1);
        }
        user.setTotalKos((user.getTotalKos() != null ? user.getTotalKos() : 0) + kos);

        // 1. Asignar XP y Pokecoins
        final int xpGained = won ? 50 : 25;
        final int coinsGained = won ? 50 : 10;
        user.setPokecoins((user.getPokecoins() != null ? user.getPokecoins() : 0) + coinsGained);

        int currentXp = user.getXp() != null ? user.getXp() : 0;
        int currentLevel = user.getLevel() != null ? user.getLevel() : 1;

        currentXp += xpGained;

        // 2. Subida de Nivel
        int needed = getXpNeededForNextLevel(currentLevel);
        while (currentXp >= needed) {
            currentXp -= needed;
            currentLevel++;
            needed = getXpNeededForNextLevel(currentLevel);
        }
        
        user.setXp(currentXp);
        user.setLevel(currentLevel);

        // 3. Chequear y Desbloquear Títulos
        final List<ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto> matches = matchRepository.findMatchesByUsername(user.getUsername());
        int matchesWon = 0;
        int completedMatchesPlayed = 0;
        for (final ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto m : matches) {
            if (m.winnerUsername() != null && m.winnerUsername().equalsIgnoreCase(user.getUsername())) {
                matchesWon++;
            }
            if (m.status() != null && (m.status().equalsIgnoreCase("FINISHED") || m.status().equalsIgnoreCase("COMPLETED"))) {
                completedMatchesPlayed++;
            }
        }
        final Map<HonorType, Integer> honors = honorService.getHonors(user.getUsername());
        final int totalHonors = honors.values().stream().mapToInt(Integer::intValue).sum();
        final int uniqueCardsCount = deckRepository.countUniqueCardsByUserId(userId);

        checkAndUnlockTitles(user, completedMatchesPlayed, matchesWon, totalHonors, uniqueCardsCount);

        userRepository.save(user);
    }

    private int getXpNeededForNextLevel(final int currentLevel) {
        if (currentLevel <= 10) {
            return 100;
        } else if (currentLevel <= 20) {
            return 120;
        } else if (currentLevel <= 30) {
            return 150;
        } else {
            return 200;
        }
    }

    private ElementalStats computeElementalStats(final Long userId) {
        final List<UserCardStatEntity> cardStats = userCardStatRepository.findByUserId(userId);
        final List<UserEnergyStatEntity> energyStats = userEnergyStatRepository.findByUserId(userId);

        final Map<String, String> cardNamesMap = cardRepository.findAll().stream()
                .collect(Collectors.toMap(CardEntity::getId, CardEntity::getName, (a, b) -> a));

        final int versatilityCount = (int) cardStats.stream()
                .filter(stat -> stat.getTimesPlayed() > 0)
                .count();

        int pikachuPlays = 0;
        int charizardPlays = 0;
        int blastoisePlays = 0;
        int venusaurPlays = 0;
        int mewtwoPlays = 0;

        for (final UserCardStatEntity stat : cardStats) {
            final String name = cardNamesMap.get(stat.getCardId());
            if (name == null) {
                continue;
            }
            final String nameLower = name.toLowerCase(Locale.ROOT);
            if (nameLower.contains("pikachu")) {
                pikachuPlays += stat.getTimesPlayed();
            } else if (nameLower.contains("charizard")) {
                charizardPlays += stat.getTimesPlayed();
            } else if (nameLower.contains("blastoise")) {
                blastoisePlays += stat.getTimesPlayed();
            } else if (nameLower.contains("venusaur")) {
                venusaurPlays += stat.getTimesPlayed();
            } else if (nameLower.contains("mewtwo")) {
                mewtwoPlays += stat.getTimesPlayed();
            }
        }

        int fireAttached = 0;
        int waterAttached = 0;
        int grassAttached = 0;
        int lightningAttached = 0;
        int psychicAttached = 0;
        int fightingAttached = 0;
        int colorlessAttached = 0;

        for (final UserEnergyStatEntity stat : energyStats) {
            if (stat.getEnergyType() == null) {
                continue;
            }
            switch (stat.getEnergyType().toUpperCase(Locale.ROOT)) {
                case "FIRE" -> fireAttached += stat.getTimesPlayed();
                case "WATER" -> waterAttached += stat.getTimesPlayed();
                case "GRASS" -> grassAttached += stat.getTimesPlayed();
                case "LIGHTNING" -> lightningAttached += stat.getTimesPlayed();
                case "PSYCHIC" -> psychicAttached += stat.getTimesPlayed();
                case "FIGHTING" -> fightingAttached += stat.getTimesPlayed();
                case "COLORLESS" -> colorlessAttached += stat.getTimesPlayed();
                default -> { }
            }
        }

        return new ElementalStats(versatilityCount, pikachuPlays, charizardPlays, blastoisePlays, venusaurPlays, mewtwoPlays,
                fireAttached, waterAttached, grassAttached, lightningAttached, psychicAttached, fightingAttached, colorlessAttached);
    }

    private AchievementContext buildAchievementContext(final UserEntity user, final int matchesWon, final int losses,
            final int completedMatches, final int uniqueCardsCount, final int totalHonors,
            final ElementalStats elemental, final Set<String> unlockedTitles) {
        final int currentLevel = user.getLevel() != null ? user.getLevel() : 1;
        final int mmr = user.getMmr() != null ? user.getMmr() : 1000;
        final int pokecoins = user.getPokecoins() != null ? user.getPokecoins() : 0;
        final int battlePoints = user.getBattlePoints() != null ? user.getBattlePoints() : 0;
        final int totalDamageDealt = user.getTotalDamageDealt() != null ? user.getTotalDamageDealt() : 0;
        final int totalKos = user.getTotalKos() != null ? user.getTotalKos() : 0;
        final int perfectWins = user.getPerfectWins() != null ? user.getPerfectWins() : 0;
        final int comebackWins = user.getComebackWins() != null ? user.getComebackWins() : 0;
        final int trainerCardsPlayed = user.getTrainerCardsPlayed() != null ? user.getTrainerCardsPlayed() : 0;
        return new AchievementContext(currentLevel, matchesWon, losses, completedMatches, uniqueCardsCount,
                totalHonors, mmr, pokecoins, battlePoints, totalDamageDealt, totalKos, perfectWins, comebackWins,
                trainerCardsPlayed, elemental, unlockedTitles);
    }

    /**
     * Checks every {@link #ACHIEVEMENTS} definition against the user's current stats and unlocks
     * whichever ones now qualify. Definitions are evaluated in list order, which matters for
     * "Multifacético"/"Celebridad de Kanto": their progress reads {@code titles.size()}, so they
     * must run after every other title has had a chance to be added in this same pass.
     */
    private void checkAndUnlockTitles(final UserEntity user, final int matchesPlayed, final int matchesWon,
            final int totalHonors, final int uniqueCardsCount) {
        Set<String> titles = user.getUnlockedTitles();
        if (titles == null) {
            titles = new HashSet<>();
        }

        final ElementalStats elemental = computeElementalStats(user.getId());
        final AchievementContext ctx = buildAchievementContext(user, matchesWon, matchesPlayed - matchesWon,
                matchesPlayed, uniqueCardsCount, totalHonors, elemental, titles);

        boolean changed = false;
        for (final AchievementDefinition def : ACHIEVEMENTS) {
            if (def.progress().applyAsInt(ctx) >= def.target() && titles.add(def.title())) {
                changed = true;
            }
        }

        if (changed) {
            user.setUnlockedTitles(titles);
        }
    }

    @Override
    public void updateShowcaseDeck(final String username, final Long deckId) {
        if (username == null) {
            return;
        }
        final Optional<UserEntity> userOpt = userRepository.findFirstByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado: " + username);
        }
        final UserEntity user = userOpt.get();

        if (deckId == null) {
            user.setShowcasedDeck(null);
        } else {
            final DeckEntity deck = deckRepository.findById(deckId)
                    .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado: " + deckId));
            if (!deck.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("El mazo no pertenece al usuario.");
            }
            user.setShowcasedDeck(deck);
        }
        userRepository.save(user);
    }

    @Override
    public void trackDamageDealt(final String username, final int damage) {
        if (username == null) {
            return;
        }
        userRepository.findFirstByUsername(username).ifPresent(user -> {
            user.setTotalDamageDealt((user.getTotalDamageDealt() != null ? user.getTotalDamageDealt() : 0) + damage);
            userRepository.save(user);
        });
    }

    @Override
    public void trackTrainerCardPlayed(final String username) {
        if (username == null) {
            return;
        }
        userRepository.findFirstByUsername(username).ifPresent(user -> {
            user.setTrainerCardsPlayed((user.getTrainerCardsPlayed() != null ? user.getTrainerCardsPlayed() : 0) + 1);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ar.edu.utn.frc.tup.piii.dtos.UserAchievementProgressDTO> getAchievementsProgress(final String username) {
        if (username == null) {
            return java.util.Collections.emptyList();
        }
        final Optional<UserEntity> userOpt = userRepository.findFirstByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado: " + username);
        }
        final UserEntity user = userOpt.get();

        final List<ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto> matches = matchRepository.findMatchesByUsername(username);
        int matchesWon = 0;
        int completedMatches = 0;
        for (final ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto m : matches) {
            if (m.winnerUsername() != null && m.winnerUsername().equalsIgnoreCase(username)) {
                matchesWon++;
            }
            if (m.status() != null && (m.status().equalsIgnoreCase("FINISHED") || m.status().equalsIgnoreCase("COMPLETED"))) {
                completedMatches++;
            }
        }

        final Map<HonorType, Integer> honors = honorService.getHonors(username);
        final int totalHonors = honors.values().stream().mapToInt(Integer::intValue).sum();
        final int uniqueCardsCount = deckRepository.countUniqueCardsByUserId(user.getId());

        Set<String> unlocked = user.getUnlockedTitles();
        if (unlocked == null) {
            unlocked = new HashSet<>();
        }

        final ElementalStats elemental = computeElementalStats(user.getId());
        final AchievementContext ctx = buildAchievementContext(user, matchesWon, completedMatches - matchesWon,
                completedMatches, uniqueCardsCount, totalHonors, elemental, unlocked);

        final List<ar.edu.utn.frc.tup.piii.dtos.UserAchievementProgressDTO> list = new ArrayList<>(ACHIEVEMENTS.size());
        for (final AchievementDefinition def : ACHIEVEMENTS) {
            list.add(createProgressDTO(def, unlocked.contains(def.title()), def.progress().applyAsInt(ctx)));
        }
        return list;
    }

    private ar.edu.utn.frc.tup.piii.dtos.UserAchievementProgressDTO createProgressDTO(
            final AchievementDefinition def, final boolean isUnlocked, final int progress) {
        return ar.edu.utn.frc.tup.piii.dtos.UserAchievementProgressDTO.builder()
                .title(def.title())
                .category(def.category())
                .unlocked(isUnlocked)
                .requirement(def.requirement())
                .progress(isUnlocked ? def.target() : Math.min(progress, def.target()))
                .target(def.target())
                .rewardType(def.rewardType())
                .rewardValue(def.rewardValue())
                .build();
    }
}
