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
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.TooManyMethods",
        "PMD.ExcessiveImports", "PMD.CouplingBetweenObjects"})
// Profile domain service: player stats, showcase, achievements, and XP/leveling all read and
// write the same UserEntity aggregate, so they live together; per-method complexity is kept low
// (highest cyclomatic complexity across all methods is 10) via the compute*/build* extraction
// throughout this class — the aggregate class-level metrics only reflect method/collaborator
// count, not entangled logic.
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

    private static final String REWARD_TITULO = "TITULO";
    private static final String REWARD_FOTO_PERFIL = "FOTO_PERFIL";
    private static final String REWARD_MEDALLA = "MEDALLA";

    private static final String CAT_NIVEL = "NIVEL";
    private static final String CAT_VICTORIAS = "VICTORIAS";
    private static final String CAT_PARTIDAS_JUGADAS = "PARTIDAS_JUGADAS";
    private static final String CAT_COLECCION = "COLECCION";
    private static final String CAT_COMPETITIVO = "COMPETITIVO";
    private static final String CAT_ECONOMIA = "ECONOMIA";
    private static final String CAT_COMBATE = "COMBATE";
    private static final String CAT_ELEMENTAL = "ELEMENTAL";
    private static final String CAT_LEALTAD = "LEALTAD";

    private static final String MATCH_STATUS_FINISHED = "FINISHED";
    private static final String MATCH_STATUS_COMPLETED = "COMPLETED";

    private static final String RARITY_LEGENDARIA = "LEGENDARIA";
    private static final String RARITY_EPICA = "EPICA";
    private static final String RARITY_RARA = "RARA";
    private static final String RARITY_COMUN = "COMUN";

    private static final int MAX_DESCRIPTION_LENGTH = 150;
    private static final int MIN_SHOWCASE_SLOT = 1;
    private static final int MAX_SHOWCASE_SLOT = 3;
    private static final int XP_GAIN_WIN = 50;
    private static final int XP_GAIN_LOSS = 25;
    private static final int COINS_GAIN_WIN = 50;
    private static final int COINS_GAIN_LOSS = 10;

    private static final int LEVEL_TIER_1_MAX = 10;
    private static final int LEVEL_TIER_2_MAX = 20;
    private static final int LEVEL_TIER_3_MAX = 30;
    private static final int XP_NEEDED_TIER_1 = 100;
    private static final int XP_NEEDED_TIER_2 = 120;
    private static final int XP_NEEDED_TIER_3 = 150;
    private static final int XP_NEEDED_TIER_4 = 200;

    private static final List<AchievementDefinition> ACHIEVEMENTS = List.of(
        new AchievementDefinition("Novato", "DEFECTO", "Título inicial por defecto", 1, REWARD_TITULO, "Novato", ctx -> 1),
        new AchievementDefinition("Entrenador", "DEFECTO", "Título inicial por defecto", 1, REWARD_TITULO, "Entrenador", ctx -> 1),

        new AchievementDefinition("Estratega en Crecimiento", CAT_NIVEL, "Alcanzar nivel 5", 5, REWARD_TITULO, "Estratega en Crecimiento", AchievementContext::level),
        new AchievementDefinition("Maestro de Cartas", CAT_NIVEL, "Alcanzar nivel 10", 10, REWARD_TITULO, "Maestro de Cartas", AchievementContext::level),
        new AchievementDefinition("Gran Mentor", CAT_NIVEL, "Alcanzar nivel 20", 20, REWARD_FOTO_PERFIL, "avatar_bulbasaur", AchievementContext::level),
        new AchievementDefinition("Líder de Élite", CAT_NIVEL, "Alcanzar nivel 30", 30, REWARD_FOTO_PERFIL, "avatar_charizard_3d", AchievementContext::level),
        new AchievementDefinition("Leyenda Viviente", CAT_NIVEL, "Alcanzar nivel 50", 50, REWARD_MEDALLA, "medal_legend", AchievementContext::level),
        new AchievementDefinition("Maestro de Kanto", CAT_NIVEL, "Alcanzar nivel 100", 100, REWARD_FOTO_PERFIL, "avatar_mewtwo_3d", AchievementContext::level),

        new AchievementDefinition("Ganador Prometedor", CAT_VICTORIAS, "Ganar 5 partidas", 5, REWARD_TITULO, "Ganador Prometedor", AchievementContext::matchesWon),
        new AchievementDefinition("Ganador Implacable", CAT_VICTORIAS, "Ganar 20 partidas", 20, REWARD_FOTO_PERFIL, "avatar_winner_badge", AchievementContext::matchesWon),
        new AchievementDefinition("Campeón del Tablero", CAT_VICTORIAS, "Ganar 50 partidas", 50, REWARD_MEDALLA, "medal_champion", AchievementContext::matchesWon),
        new AchievementDefinition("Leyenda del Tablero", CAT_VICTORIAS, "Ganar 100 partidas", 100, REWARD_MEDALLA, "medal_board_legend", AchievementContext::matchesWon),
        new AchievementDefinition("Inmortal del Tablero", CAT_VICTORIAS, "Ganar 250 partidas", 250, REWARD_FOTO_PERFIL, "avatar_red_champ", AchievementContext::matchesWon),

        new AchievementDefinition("Espíritu Resiliente", "RESILIENCIA", "Perder 10 partidas en total", 10, REWARD_TITULO, "Espíritu Resiliente", AchievementContext::losses),
        new AchievementDefinition("Fuerza de Voluntad", "RESILIENCIA", "Perder 50 partidas en total", 50, REWARD_FOTO_PERFIL, "avatar_resilience_mid", AchievementContext::losses),

        new AchievementDefinition("Combatiente", CAT_PARTIDAS_JUGADAS, "Jugar 10 partidas completas", 10, REWARD_TITULO, "Combatiente", AchievementContext::completedMatches),
        new AchievementDefinition("Combatiente Tenaz", CAT_PARTIDAS_JUGADAS, "Jugar 25 partidas completas", 25, REWARD_TITULO, "Combatiente Tenaz", AchievementContext::completedMatches),
        new AchievementDefinition("Veterano de Batallas", CAT_PARTIDAS_JUGADAS, "Jugar 50 partidas completas", 50, REWARD_MEDALLA, "medal_veteran", AchievementContext::completedMatches),
        new AchievementDefinition("Leyenda de Batallas", CAT_PARTIDAS_JUGADAS, "Jugar 100 partidas completas", 100, REWARD_FOTO_PERFIL, "avatar_lucas_legend", AchievementContext::completedMatches),
        new AchievementDefinition("Espíritu Inquebrantable", CAT_PARTIDAS_JUGADAS, "Jugar 250 partidas completas", 250, REWARD_FOTO_PERFIL, "avatar_eevee_3d", AchievementContext::completedMatches),

        new AchievementDefinition("Coleccionista Novato", CAT_COLECCION, "Tener 30 cartas distintas en tus mazos", 30, REWARD_TITULO, "Coleccionista Novato", AchievementContext::uniqueCardsCount),
        new AchievementDefinition("Coleccionista Experto", CAT_COLECCION, "Tener 50 cartas distintas en tus mazos", 50, REWARD_TITULO, "Coleccionista Experto", AchievementContext::uniqueCardsCount),
        new AchievementDefinition("Coleccionista de Élite", CAT_COLECCION, "Tener 100 cartas distintas en tus mazos", 100, REWARD_MEDALLA, "medal_collector_elite", AchievementContext::uniqueCardsCount),
        new AchievementDefinition("Maestro Coleccionista", CAT_COLECCION, "Tener 150 cartas distintas en tus mazos", 150, REWARD_MEDALLA, "medal_collector_legend", AchievementContext::uniqueCardsCount),
        new AchievementDefinition("Curador del Museo", CAT_COLECCION, "Tener 200 cartas distintas en tus mazos", 200, REWARD_FOTO_PERFIL, "avatar_collector_legend", AchievementContext::uniqueCardsCount),

        new AchievementDefinition("Compañero Amigable", "HONORES", "Recibir 5 honores de otros jugadores", 5, REWARD_TITULO, "Compañero Amigable", AchievementContext::totalHonors),
        new AchievementDefinition("Entrenador Respetado", "HONORES", "Recibir 15 honores de otros jugadores", 15, REWARD_TITULO, "Entrenador Respetado", AchievementContext::totalHonors),
        new AchievementDefinition("Héroe del Fair Play", "HONORES", "Recibir 30 honores de otros jugadores", 30, REWARD_MEDALLA, "medal_fair_play_legend", AchievementContext::totalHonors),

        new AchievementDefinition("Entrenador Destacado", CAT_COMPETITIVO, "Alcanzar 1200 de MMR", 1200, REWARD_TITULO, "Entrenador Destacado", AchievementContext::mmr),
        new AchievementDefinition("Líder de Gimnasio", CAT_COMPETITIVO, "Alcanzar 1500 de MMR", 1500, REWARD_FOTO_PERFIL, "avatar_gym_leader", AchievementContext::mmr),
        new AchievementDefinition("Alto Mando", CAT_COMPETITIVO, "Alcanzar 1800 de MMR", 1800, REWARD_TITULO, "Alto Mando", AchievementContext::mmr),
        new AchievementDefinition("Campeón de la Liga", CAT_COMPETITIVO, "Alcanzar 2000 de MMR", 2000, REWARD_MEDALLA, "medal_league_champion", AchievementContext::mmr),

        new AchievementDefinition("Estratega Versátil", "VERSATILIDAD", "Jugar 20 cartas diferentes en partidas", 20, REWARD_FOTO_PERFIL, "avatar_versatility_mid", ctx -> ctx.elemental().versatilityCount()),
        new AchievementDefinition("Maestro Adaptable", "VERSATILIDAD", "Jugar 50 cartas diferentes en partidas", 50, REWARD_FOTO_PERFIL, "avatar_versatility_3d", ctx -> ctx.elemental().versatilityCount()),

        new AchievementDefinition("Súper Nerd de las Ventas", CAT_ECONOMIA, "Acumular 1,000 Pokecoins", 1000, REWARD_MEDALLA, "medal_coins_1k", AchievementContext::pokecoins),
        new AchievementDefinition("Magnate de Kanto", CAT_ECONOMIA, "Acumular 5,000 Pokecoins", 5000, REWARD_MEDALLA, "medal_magnate_gold", AchievementContext::pokecoins),
        new AchievementDefinition("Gladiador del Tablero", CAT_ECONOMIA, "Acumular 500 Battle Points", 500, REWARD_TITULO, "Gladiador del Tablero", AchievementContext::battlePoints),
        new AchievementDefinition("Campeón del Coliseo", CAT_ECONOMIA, "Acumular 2,000 Battle Points", 2000, REWARD_MEDALLA, "medal_colosseum_legend", AchievementContext::battlePoints),

        new AchievementDefinition("Poder Eléctrico", CAT_COMBATE, "Infligir 1,000 puntos de daño en total", 1000, REWARD_MEDALLA, "medal_power_1k", AchievementContext::totalDamageDealt),
        new AchievementDefinition("Fuerza Brutal", CAT_COMBATE, "Infligir 5,000 puntos de daño en total", 5000, REWARD_TITULO, "Fuerza Brutal", AchievementContext::totalDamageDealt),
        new AchievementDefinition("Fuerza de la Naturaleza", CAT_COMBATE, "Infligir 15,000 puntos de daño en total", 15000, REWARD_FOTO_PERFIL, "avatar_nature_force", AchievementContext::totalDamageDealt),
        new AchievementDefinition("Destructor Cósmico", CAT_COMBATE, "Infligir 50,000 puntos de daño en total", 50000, REWARD_TITULO, "Destructor Cósmico", AchievementContext::totalDamageDealt),

        new AchievementDefinition("Derribador", CAT_COMBATE, "Realizar 10 KOs en total", 10, REWARD_MEDALLA, "medal_kos_10", AchievementContext::totalKos),
        new AchievementDefinition("Cazador de KOs", CAT_COMBATE, "Realizar 50 KOs en total", 50, REWARD_TITULO, "Cazador de KOs", AchievementContext::totalKos),
        new AchievementDefinition("Ejecutor Implacable", CAT_COMBATE, "Realizar 150 KOs en total", 150, REWARD_FOTO_PERFIL, "avatar_executor_mid", AchievementContext::totalKos),
        new AchievementDefinition("Verdugo Supremo", CAT_COMBATE, "Realizar 300 KOs en total", 300, REWARD_TITULO, "Verdugo Supremo", AchievementContext::totalKos),

        new AchievementDefinition("Estratega Imbatible", CAT_COMBATE, "Conseguir 1 victoria perfecta (sin sufrir KOs)", 1, REWARD_MEDALLA, "medal_perfect_1", AchievementContext::perfectWins),
        new AchievementDefinition("Intocable", CAT_COMBATE, "Conseguir 5 victorias perfectas", 5, REWARD_TITULO, "Intocable", AchievementContext::perfectWins),
        new AchievementDefinition("Inmaculado", CAT_COMBATE, "Conseguir 15 victorias perfectas", 15, REWARD_TITULO, "Inmaculado", AchievementContext::perfectWins),

        new AchievementDefinition("Rey del Clímax", CAT_COMBATE, "Conseguir 1 victoria tras remontada", 1, REWARD_MEDALLA, "medal_comeback_1", AchievementContext::comebackWins),
        new AchievementDefinition("Espíritu de Remontada", CAT_COMBATE, "Conseguir 5 victorias tras remontada", 5, REWARD_TITULO, "Espíritu de Remontada", AchievementContext::comebackWins),
        new AchievementDefinition("Fénix del Tablero", CAT_COMBATE, "Conseguir 15 victorias tras remontada", 15, REWARD_TITULO, "Fénix del Tablero", AchievementContext::comebackWins),

        new AchievementDefinition("Estudioso de Reglas", CAT_COMBATE, "Jugar 50 cartas de Entrenador", 50, REWARD_FOTO_PERFIL, "avatar_rules_student", AchievementContext::trainerCardsPlayed),
        new AchievementDefinition("Maestro Táctico", CAT_COMBATE, "Jugar 200 cartas de Entrenador", 200, REWARD_TITULO, "Maestro Táctico", AchievementContext::trainerCardsPlayed),
        new AchievementDefinition("Gran Sabio", CAT_COMBATE, "Jugar 500 cartas de Entrenador", 500, REWARD_TITULO, "Gran Sabio", AchievementContext::trainerCardsPlayed),

        new AchievementDefinition("Piro-Novato", CAT_ELEMENTAL, "Unir 50 energías de Fuego", 50, REWARD_MEDALLA, "medal_fire_50", ctx -> ctx.elemental().fireAttached()),
        new AchievementDefinition("Piro-Maestro", CAT_ELEMENTAL, "Unir 200 energías de Fuego", 200, REWARD_MEDALLA, "medal_fire_200", ctx -> ctx.elemental().fireAttached()),
        new AchievementDefinition("Llama de Kanto", CAT_ELEMENTAL, "Unir 500 energías de Fuego", 500, REWARD_FOTO_PERFIL, "avatar_fire_kanto", ctx -> ctx.elemental().fireAttached()),

        new AchievementDefinition("Hidro-Novato", CAT_ELEMENTAL, "Unir 50 energías de Agua", 50, REWARD_MEDALLA, "medal_water_50", ctx -> ctx.elemental().waterAttached()),
        new AchievementDefinition("Maestro del Surf", CAT_ELEMENTAL, "Unir 200 energías de Agua", 200, REWARD_MEDALLA, "medal_water_200", ctx -> ctx.elemental().waterAttached()),
        new AchievementDefinition("Tsunami Viviente", CAT_ELEMENTAL, "Unir 500 energías de Agua", 500, REWARD_FOTO_PERFIL, "avatar_water_kanto", ctx -> ctx.elemental().waterAttached()),

        new AchievementDefinition("Brote Verde", CAT_ELEMENTAL, "Unir 50 energías de Planta", 50, REWARD_MEDALLA, "medal_grass_50", ctx -> ctx.elemental().grassAttached()),
        new AchievementDefinition("Guardián de la Selva", CAT_ELEMENTAL, "Unir 200 energías de Planta", 200, REWARD_MEDALLA, "medal_grass_200", ctx -> ctx.elemental().grassAttached()),
        new AchievementDefinition("Espíritu del Bosque", CAT_ELEMENTAL, "Unir 500 energías de Planta", 500, REWARD_FOTO_PERFIL, "avatar_grass_kanto", ctx -> ctx.elemental().grassAttached()),

        new AchievementDefinition("Chispa Inicial", CAT_ELEMENTAL, "Unir 50 energías de Rayo", 50, REWARD_MEDALLA, "medal_lightning_50", ctx -> ctx.elemental().lightningAttached()),
        new AchievementDefinition("Voltaje Máximo", CAT_ELEMENTAL, "Unir 200 energías de Rayo", 200, REWARD_MEDALLA, "medal_lightning_200", ctx -> ctx.elemental().lightningAttached()),
        new AchievementDefinition("Tormenta Perpetua", CAT_ELEMENTAL, "Unir 500 energías de Rayo", 500, REWARD_FOTO_PERFIL, "avatar_lightning_kanto", ctx -> ctx.elemental().lightningAttached()),

        new AchievementDefinition("Sensitivo", CAT_ELEMENTAL, "Unir 50 energías Psíquicas", 50, REWARD_MEDALLA, "medal_psychic_50", ctx -> ctx.elemental().psychicAttached()),
        new AchievementDefinition("Mente Mística", CAT_ELEMENTAL, "Unir 200 energías Psíquicas", 200, REWARD_MEDALLA, "medal_psychic_200", ctx -> ctx.elemental().psychicAttached()),
        new AchievementDefinition("Poder Cósmico", CAT_ELEMENTAL, "Unir 500 energías Psíquicas", 500, REWARD_FOTO_PERFIL, "avatar_psychic_kanto", ctx -> ctx.elemental().psychicAttached()),

        new AchievementDefinition("Cinturón Blanco", CAT_ELEMENTAL, "Unir 50 energías de Lucha", 50, REWARD_FOTO_PERFIL, "avatar_belt_white", ctx -> ctx.elemental().fightingAttached()),
        new AchievementDefinition("Cinturón Negro", CAT_ELEMENTAL, "Unir 200 energías de Lucha", 200, REWARD_TITULO, "Cinturón Negro", ctx -> ctx.elemental().fightingAttached()),
        new AchievementDefinition("Fuerza Sísmica", CAT_ELEMENTAL, "Unir 500 energías de Lucha", 500, REWARD_FOTO_PERFIL, "avatar_fighting_kanto", ctx -> ctx.elemental().fightingAttached()),

        new AchievementDefinition("Equilibrio", CAT_ELEMENTAL, "Unir 50 energías Incoloras", 50, REWARD_FOTO_PERFIL, "avatar_neutral_balance", ctx -> ctx.elemental().colorlessAttached()),
        new AchievementDefinition("Estratega Neutral", CAT_ELEMENTAL, "Unir 200 energías Incoloras", 200, REWARD_TITULO, "Estratega Neutral", ctx -> ctx.elemental().colorlessAttached()),
        new AchievementDefinition("Armonía Pura", CAT_ELEMENTAL, "Unir 500 energías Incoloras", 500, REWARD_FOTO_PERFIL, "avatar_colorless_kanto", ctx -> ctx.elemental().colorlessAttached()),

        new AchievementDefinition("Amigo del Ratón", CAT_LEALTAD, "Jugar cartas de Pikachu 15 veces", 15, REWARD_FOTO_PERFIL, "avatar_pikachu_cute", ctx -> ctx.elemental().pikachuPlays()),
        new AchievementDefinition("Compañero Fiel", CAT_LEALTAD, "Jugar cartas de Pikachu 50 veces", 50, REWARD_TITULO, "Compañero Fiel", ctx -> ctx.elemental().pikachuPlays()),

        new AchievementDefinition("Aliento Ígneo", CAT_LEALTAD, "Jugar cartas de Charizard 10 veces", 10, REWARD_FOTO_PERFIL, "avatar_charizard_cute", ctx -> ctx.elemental().charizardPlays()),
        new AchievementDefinition("Llama Ancestral", CAT_LEALTAD, "Jugar cartas de Charizard 30 veces", 30, REWARD_TITULO, "Llama Ancestral", ctx -> ctx.elemental().charizardPlays()),

        new AchievementDefinition("Presión de Agua", CAT_LEALTAD, "Jugar cartas de Blastoise 10 veces", 10, REWARD_FOTO_PERFIL, "avatar_blastoise_cute", ctx -> ctx.elemental().blastoisePlays()),
        new AchievementDefinition("Tsunami de Kanto", CAT_LEALTAD, "Jugar cartas de Blastoise 30 veces", 30, REWARD_TITULO, "Tsunami de Kanto", ctx -> ctx.elemental().blastoisePlays()),

        new AchievementDefinition("Floración Rápida", CAT_LEALTAD, "Jugar cartas de Venusaur 10 veces", 10, REWARD_FOTO_PERFIL, "avatar_venusaur_cute", ctx -> ctx.elemental().venusaurPlays()),
        new AchievementDefinition("Semilla de la Vida", CAT_LEALTAD, "Jugar cartas de Venusaur 30 veces", 30, REWARD_TITULO, "Semilla de la Vida", ctx -> ctx.elemental().venusaurPlays()),

        new AchievementDefinition("Mirada Mental", CAT_LEALTAD, "Jugar cartas de Mewtwo 10 veces", 10, REWARD_FOTO_PERFIL, "avatar_mewtwo_cute", ctx -> ctx.elemental().mewtwoPlays()),
        new AchievementDefinition("Fuerza Psíquica", CAT_LEALTAD, "Jugar cartas de Mewtwo 30 veces", 30, REWARD_TITULO, "Fuerza Psíquica", ctx -> ctx.elemental().mewtwoPlays()),

        // Evaluated last on purpose: progress reads titles.size(), so it must see every title
        // unlocked earlier in this same pass (see checkAndUnlockTitles).
        new AchievementDefinition("Multifacético", "TITULOS", "Desbloquear 10 títulos", 10, REWARD_FOTO_PERFIL, "avatar_multifaceted", ctx -> ctx.unlockedTitles().size()),
        new AchievementDefinition("Celebridad de Kanto", "TITULOS", "Desbloquear 20 títulos", 20, REWARD_FOTO_PERFIL, "avatar_celebrity", ctx -> ctx.unlockedTitles().size())
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

    @SuppressWarnings("PMD.ExcessiveParameterList")
    // Standard Spring constructor injection of every collaborator this domain service needs
    // (profile, showcase, matches, cards, honors, decks, profanity filter, stats, inventory).
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

    private record MatchAggregates(int matchesPlayed, int matchesWon, int matchesLost, double winRate,
            int winStreak, int completedMatchesPlayed) {
    }

    private record CardNameAndType(String name, String pokemonType) {
    }

    @Override
    @Transactional
    public UserProfileResponseDTO getProfile(final String username) {
        final Optional<UserEntity> userOpt = userRepository.findFirstByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        final UserEntity user = userOpt.get();

        final List<ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto> matches = matchRepository.findMatchesByUsername(username);
        final MatchAggregates aggregates = computeMatchAggregates(matches, username);
        final UserProfileResponseDTO.Statistics stats = buildStatistics(user, aggregates);

        final Map<HonorType, Integer> honors = honorService.getHonors(username);
        final int totalHonors = honors.values().stream().mapToInt(Integer::intValue).sum();

        final int uniqueCardsCount = deckRepository.countUniqueCardsByUserId(user.getId());
        checkAndUnlockTitles(user, aggregates.completedMatchesPlayed(), aggregates.matchesWon(), totalHonors, uniqueCardsCount);
        userRepository.save(user);

        final List<UserProfileResponseDTO.ShowcaseSlot> showcaseSlots = buildShowcaseSlots(username);

        final int currentLevel = intOrDefault(user.getLevel(), 1);
        final int currentXp = intOrDefault(user.getXp(), 0);
        final int xpToNext = getXpNeededForNextLevel(currentLevel);

        final UserProfileResponseDTO.ShowcasedDeck showcasedDeckDto = buildShowcasedDeckDto(user);
        final UserProfileResponseDTO.AdvancedStatsDTO advancedStatsDTO = buildAdvancedStats(user.getId());

        final Map<String, Integer> pInv = mapOrEmpty(user.getPacksInventory());
        final int totalPacks = intOrDefault(user.getPacks(), 0);
        final List<UserProfileResponseDTO.CollectedCardDTO> packCollection = buildPackCollection(user.getId());

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
                .mmr(intOrDefault(user.getMmr(), 1000))
                .pokecoins(intOrDefault(user.getPokecoins(), 0))
                .battlePoints(intOrDefault(user.getBattlePoints(), 0))
                .packs(totalPacks)
                .packsInventory(pInv)
                .statistics(stats)
                .honors(honors.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), java.util.Map.Entry::getValue)))
                .unlockedTitles(listOrEmpty(user.getUnlockedTitles()))
                .unlockedAvatars(listOrEmpty(user.getUnlockedAvatars()))
                .showcase(showcaseSlots)
                .packCollection(packCollection)
                .showcasedDeck(showcasedDeckDto)
                .advancedStats(advancedStatsDTO)
                .build();
    }

    private static int intOrDefault(final Integer value, final int fallback) {
        return value != null ? value : fallback;
    }

    private static List<String> listOrEmpty(final Set<String> source) {
        return source != null ? new ArrayList<>(source) : new ArrayList<>();
    }

    private static Map<String, Integer> mapOrEmpty(final Map<String, Integer> source) {
        return source != null ? new java.util.HashMap<>(source) : new java.util.HashMap<>();
    }

    private MatchAggregates computeMatchAggregates(
            final List<ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto> matches, final String username) {
        final int matchesPlayed = matches.size();
        int matchesWon = 0;
        int completedMatchesPlayed = 0;
        for (final ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto m : matches) {
            if (m.winnerUsername() != null && m.winnerUsername().equalsIgnoreCase(username)) {
                matchesWon++;
            }
            if (isCompletedMatch(m)) {
                completedMatchesPlayed++;
            }
        }
        final int matchesLost = matchesPlayed - matchesWon;
        double winRate = matchesPlayed > 0 ? (matchesWon * 100.0) / matchesPlayed : 0.0;
        winRate = Math.round(winRate * 100.0) / 100.0; // Redondear a dos decimales
        final int winStreak = computeWinStreak(matches, username);
        return new MatchAggregates(matchesPlayed, matchesWon, matchesLost, winRate, winStreak, completedMatchesPlayed);
    }

    // Calcular racha de victorias actual
    private int computeWinStreak(final List<ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto> matches, final String username) {
        int winStreak = 0;
        for (final ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto m : matches) {
            if (!isCompletedMatch(m) || m.winnerUsername() == null) {
                continue;
            }
            if (!m.winnerUsername().equalsIgnoreCase(username)) {
                break; // Se cortó la racha
            }
            winStreak++;
        }
        return winStreak;
    }

    private UserProfileResponseDTO.Statistics buildStatistics(final UserEntity user, final MatchAggregates aggregates) {
        return UserProfileResponseDTO.Statistics.builder()
                .matchesPlayed(aggregates.matchesPlayed())
                .matchesWon(aggregates.matchesWon())
                .matchesLost(aggregates.matchesLost())
                .winRate(aggregates.winRate())
                .perfectWins(user.getPerfectWins() != null ? user.getPerfectWins() : 0)
                .comebackWins(user.getComebackWins() != null ? user.getComebackWins() : 0)
                .totalKos(user.getTotalKos() != null ? user.getTotalKos() : 0)
                .trainerCardsPlayed(user.getTrainerCardsPlayed() != null ? user.getTrainerCardsPlayed() : 0)
                .totalDamageDealt(user.getTotalDamageDealt() != null ? user.getTotalDamageDealt() : 0)
                .winStreak(aggregates.winStreak())
                .build();
    }

    private List<UserProfileResponseDTO.ShowcaseSlot> buildShowcaseSlots(final String username) {
        final List<UserShowcaseEntity> showcaseEntities = userShowcaseRepository.findByUserUsernameOrderBySlotPositionAsc(username);
        final List<UserProfileResponseDTO.ShowcaseSlot> showcaseSlots = new ArrayList<>();
        for (final UserShowcaseEntity entity : showcaseEntities) {
            final String cardName = cardRepository.findById(entity.getCardId())
                    .map(CardEntity::getName)
                    .orElse("Carta Desconocida");
            showcaseSlots.add(UserProfileResponseDTO.ShowcaseSlot.builder()
                    .slotPosition(entity.getSlotPosition())
                    .cardId(entity.getCardId())
                    .cardName(cardName)
                    .build());
        }
        return showcaseSlots;
    }

    private UserProfileResponseDTO.ShowcasedDeck buildShowcasedDeckDto(final UserEntity user) {
        if (user.getShowcasedDeck() == null) {
            return null;
        }
        final List<UserProfileResponseDTO.ShowcasedDeckCard> cardsList = new ArrayList<>();
        if (user.getShowcasedDeck().getCards() != null) {
            for (final DeckCardEntity dce : user.getShowcasedDeck().getCards()) {
                cardsList.add(UserProfileResponseDTO.ShowcasedDeckCard.builder()
                        .cardId(dce.getCard().getId())
                        .cardName(dce.getCard().getName())
                        .quantity(dce.getQuantity())
                        .build());
            }
        }
        return UserProfileResponseDTO.ShowcasedDeck.builder()
                .id(user.getShowcasedDeck().getId())
                .name(user.getShowcasedDeck().getName())
                .cards(cardsList)
                .build();
    }

    private UserProfileResponseDTO.AdvancedStatsDTO buildAdvancedStats(final Long userId) {
        final List<UserProfileResponseDTO.CardStatDTO> cardStatDTOs = buildCardStatDTOs(userId);
        final List<UserProfileResponseDTO.EnergyStatDTO> energyStatDTOs = buildEnergyStatDTOs(userId);

        return UserProfileResponseDTO.AdvancedStatsDTO.builder()
                .pokemonStats(cardStatDTOs)
                .energyStats(energyStatDTOs)
                .totalDamageDealt(cardStatDTOs.stream().mapToInt(UserProfileResponseDTO.CardStatDTO::getDamageDealt).sum())
                .totalDamageReceived(cardStatDTOs.stream().mapToInt(UserProfileResponseDTO.CardStatDTO::getDamageReceived).sum())
                .totalKOsMade(cardStatDTOs.stream().mapToInt(UserProfileResponseDTO.CardStatDTO::getKosMade).sum())
                .totalKOsSuffered(cardStatDTOs.stream().mapToInt(UserProfileResponseDTO.CardStatDTO::getKosSuffered).sum())
                .build();
    }

    private List<UserProfileResponseDTO.CardStatDTO> buildCardStatDTOs(final Long userId) {
        final List<UserCardStatEntity> cardStatEntities = userCardStatRepository.findByUserId(userId);
        final List<UserProfileResponseDTO.CardStatDTO> cardStatDTOs = new ArrayList<>();
        for (final UserCardStatEntity statEntity : cardStatEntities) {
            final CardNameAndType nameAndType = resolveCardNameAndType(statEntity.getCardId());
            cardStatDTOs.add(UserProfileResponseDTO.CardStatDTO.builder()
                    .cardId(statEntity.getCardId())
                    .cardName(nameAndType.name())
                    .pokemonType(nameAndType.pokemonType())
                    .timesPlayed(statEntity.getTimesPlayed())
                    .damageDealt(statEntity.getDamageDealt())
                    .damageReceived(statEntity.getDamageReceived())
                    .kosMade(statEntity.getKosMade())
                    .kosSuffered(statEntity.getKosSuffered())
                    .build());
        }
        return cardStatDTOs;
    }

    private CardNameAndType resolveCardNameAndType(final String cardId) {
        final Optional<CardEntity> cardEntityOpt = cardRepository.findById(cardId);
        if (cardEntityOpt.isEmpty()) {
            return new CardNameAndType("Carta Desconocida", "COLORLESS");
        }
        final CardEntity cardEntity = cardEntityOpt.get();
        String pokemonType = "COLORLESS";
        try {
            final Card domainCard = cardMapper.map(cardEntity);
            if (domainCard instanceof PokemonCard pc) {
                pokemonType = pc.getPokemonType().name();
            }
        } catch (IllegalArgumentException e) {
            log.warn("Could not map card {} to a domain Card for pokemonType lookup: {}", cardEntity.getId(), e.getMessage());
        }
        return new CardNameAndType(cardEntity.getName(), pokemonType);
    }

    private List<UserProfileResponseDTO.EnergyStatDTO> buildEnergyStatDTOs(final Long userId) {
        final List<UserEnergyStatEntity> energyStatEntities = userEnergyStatRepository.findByUserId(userId);
        final List<UserProfileResponseDTO.EnergyStatDTO> energyStatDTOs = new ArrayList<>();
        for (final UserEnergyStatEntity energyEntity : energyStatEntities) {
            energyStatDTOs.add(UserProfileResponseDTO.EnergyStatDTO.builder()
                    .energyType(energyEntity.getEnergyType())
                    .count(energyEntity.getTimesPlayed())
                    .build());
        }
        return energyStatDTOs;
    }

    private List<UserProfileResponseDTO.CollectedCardDTO> buildPackCollection(final Long userId) {
        final List<UserShowcaseInventoryEntity> userInventory = userShowcaseInventoryRepository.findByUserId(userId);
        final List<UserProfileResponseDTO.CollectedCardDTO> packCollection = new ArrayList<>();
        for (final UserShowcaseInventoryEntity invEntity : userInventory) {
            final Optional<CardEntity> cardEntityOpt = cardRepository.findById(invEntity.getCardId());
            final String cardName = cardEntityOpt.map(CardEntity::getName).orElse("Carta Desconocida");
            final String rarity = cardEntityOpt.map(ce -> determineRarity(ce.getSubtype())).orElse(RARITY_COMUN);
            packCollection.add(UserProfileResponseDTO.CollectedCardDTO.builder()
                    .cardId(invEntity.getCardId())
                    .cardName(cardName)
                    .isFoil(Boolean.TRUE.equals(invEntity.getIsFoil()))
                    .rarity(rarity)
                    .build());
        }
        return packCollection;
    }

    private String determineRarity(final String rawSubtype) {
        final String subtype = rawSubtype != null ? rawSubtype.toUpperCase(Locale.ROOT) : "";
        if (isLegendarySubtype(subtype)) {
            return RARITY_LEGENDARIA;
        }
        if (subtype.contains("STAGE 2")) {
            return RARITY_EPICA;
        }
        if (subtype.contains("STAGE 1")) {
            return RARITY_RARA;
        }
        return RARITY_COMUN;
    }

    private boolean isLegendarySubtype(final String subtype) {
        return subtype.contains("EX") || subtype.contains("GX") || subtype.contains(" V") || "V".equals(subtype)
                || subtype.contains("MEGA") || subtype.contains("LEGEND");
    }

    private boolean isCompletedMatch(final ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto m) {
        return m.status() != null
                && (MATCH_STATUS_FINISHED.equalsIgnoreCase(m.status()) || MATCH_STATUS_COMPLETED.equalsIgnoreCase(m.status()));
    }

    @Override
    public void updateProfile(final String username, final UpdateProfileRequestDTO request) {
        if (username == null || request == null) {
            return;
        }
        final Optional<UserEntity> userOpt = userRepository.findFirstByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }
        final UserEntity user = userOpt.get();
        applyAvatarIconUpdate(user, request);
        applyDescriptionUpdate(user, request);
        applyActiveTitleUpdate(user, request);
        applySelectedMedalsUpdate(user, request);
        userRepository.save(user);
    }

    private void applyAvatarIconUpdate(final UserEntity user, final UpdateProfileRequestDTO request) {
        if (request.getAvatarIcon() != null) {
            user.setAvatarIcon(request.getAvatarIcon());
        }
    }

    private void applyDescriptionUpdate(final UserEntity user, final UpdateProfileRequestDTO request) {
        if (request.getDescription() == null) {
            return;
        }
        if (request.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("La descripción no puede superar los " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
        final List<String> profanities = profanityFilterService.getProfaneWords(request.getDescription());
        if (!profanities.isEmpty()) {
            throw new IllegalArgumentException("La descripción contiene palabras no permitidas: " + String.join(", ", profanities));
        }
        user.setDescription(request.getDescription());
    }

    private void applyActiveTitleUpdate(final UserEntity user, final UpdateProfileRequestDTO request) {
        if (request.getActiveTitle() == null) {
            return;
        }
        if (request.getActiveTitle().trim().isEmpty()) {
            user.setActiveTitle(null);
        } else if (user.getUnlockedTitles().contains(request.getActiveTitle())) {
            user.setActiveTitle(request.getActiveTitle());
        }
    }

    private void applySelectedMedalsUpdate(final UserEntity user, final UpdateProfileRequestDTO request) {
        if (request.getSelectedMedals() != null) {
            user.setSelectedMedals(request.getSelectedMedals());
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
            applyShowcaseSlot(user, slot);
        }
    }

    private void applyShowcaseSlot(final UserEntity user, final UpdateShowcaseRequestDTO.ShowcaseSlot slot) {
        if (slot.getSlotPosition() == null || slot.getSlotPosition() < MIN_SHOWCASE_SLOT || slot.getSlotPosition() > MAX_SHOWCASE_SLOT) {
            return;
        }

        final Optional<UserShowcaseEntity> existingOpt = userShowcaseRepository.findByUserAndSlotPosition(user, slot.getSlotPosition());
        if (slot.getCardId() == null || slot.getCardId().trim().isEmpty()) {
            // Si la carta viene vacía, elimina ese slot de la vitrina
            existingOpt.ifPresent(userShowcaseRepository::delete);
            return;
        }

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

    @Override
    public void awardXpAndCheckAchievements(final Long userId, final boolean won, final boolean isPerfectWin,
            final boolean isComebackWin, final int kos) {
        if (userId == null) {
            return;
        }
        final Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return;
        }
        final UserEntity user = userOpt.get();

        applyMatchResultStats(user, isPerfectWin, isComebackWin, kos);
        applyXpAndLevelUp(user, won);

        final List<ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto> matches =
                matchRepository.findMatchesByUsername(user.getUsername());
        final MatchAggregates aggregates = computeMatchAggregates(matches, user.getUsername());
        final Map<HonorType, Integer> honors = honorService.getHonors(user.getUsername());
        final int totalHonors = honors.values().stream().mapToInt(Integer::intValue).sum();
        final int uniqueCardsCount = deckRepository.countUniqueCardsByUserId(userId);

        checkAndUnlockTitles(user, aggregates.completedMatchesPlayed(), aggregates.matchesWon(), totalHonors, uniqueCardsCount);

        userRepository.save(user);
    }

    // Increment statistics
    private void applyMatchResultStats(final UserEntity user, final boolean isPerfectWin, final boolean isComebackWin, final int kos) {
        if (isPerfectWin) {
            user.setPerfectWins((user.getPerfectWins() != null ? user.getPerfectWins() : 0) + 1);
        }
        if (isComebackWin) {
            user.setComebackWins((user.getComebackWins() != null ? user.getComebackWins() : 0) + 1);
        }
        user.setTotalKos((user.getTotalKos() != null ? user.getTotalKos() : 0) + kos);
    }

    private void applyXpAndLevelUp(final UserEntity user, final boolean won) {
        final int xpGained = won ? XP_GAIN_WIN : XP_GAIN_LOSS;
        final int coinsGained = won ? COINS_GAIN_WIN : COINS_GAIN_LOSS;
        user.setPokecoins((user.getPokecoins() != null ? user.getPokecoins() : 0) + coinsGained);

        int currentXp = (user.getXp() != null ? user.getXp() : 0) + xpGained;
        int currentLevel = user.getLevel() != null ? user.getLevel() : 1;

        int needed = getXpNeededForNextLevel(currentLevel);
        while (currentXp >= needed) {
            currentXp -= needed;
            currentLevel++;
            needed = getXpNeededForNextLevel(currentLevel);
        }

        user.setXp(currentXp);
        user.setLevel(currentLevel);
    }

    private int getXpNeededForNextLevel(final int currentLevel) {
        if (currentLevel <= LEVEL_TIER_1_MAX) {
            return XP_NEEDED_TIER_1;
        } else if (currentLevel <= LEVEL_TIER_2_MAX) {
            return XP_NEEDED_TIER_2;
        } else if (currentLevel <= LEVEL_TIER_3_MAX) {
            return XP_NEEDED_TIER_3;
        } else {
            return XP_NEEDED_TIER_4;
        }
    }

    private record LoyaltyPlays(int pikachuPlays, int charizardPlays, int blastoisePlays, int venusaurPlays, int mewtwoPlays) {
    }

    private record EnergyAttachments(int fireAttached, int waterAttached, int grassAttached, int lightningAttached,
            int psychicAttached, int fightingAttached, int colorlessAttached) {
    }

    private ElementalStats computeElementalStats(final Long userId) {
        final List<UserCardStatEntity> cardStats = userCardStatRepository.findByUserId(userId);
        final List<UserEnergyStatEntity> energyStats = userEnergyStatRepository.findByUserId(userId);

        final int versatilityCount = (int) cardStats.stream()
                .filter(stat -> stat.getTimesPlayed() > 0)
                .count();

        final LoyaltyPlays loyalty = computeLoyaltyPlays(cardStats);
        final EnergyAttachments energy = computeEnergyAttachments(energyStats);

        return new ElementalStats(versatilityCount,
                loyalty.pikachuPlays(), loyalty.charizardPlays(), loyalty.blastoisePlays(),
                loyalty.venusaurPlays(), loyalty.mewtwoPlays(),
                energy.fireAttached(), energy.waterAttached(), energy.grassAttached(), energy.lightningAttached(),
                energy.psychicAttached(), energy.fightingAttached(), energy.colorlessAttached());
    }

    private LoyaltyPlays computeLoyaltyPlays(final List<UserCardStatEntity> cardStats) {
        final Map<String, String> cardNamesMap = cardRepository.findAll().stream()
                .collect(Collectors.toMap(CardEntity::getId, CardEntity::getName, (a, b) -> a));

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
        return new LoyaltyPlays(pikachuPlays, charizardPlays, blastoisePlays, venusaurPlays, mewtwoPlays);
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    // Flat one-statement-per-case dispatch over the 7 energy types; no branching logic per case.
    private EnergyAttachments computeEnergyAttachments(final List<UserEnergyStatEntity> energyStats) {
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
        return new EnergyAttachments(fireAttached, waterAttached, grassAttached, lightningAttached,
                psychicAttached, fightingAttached, colorlessAttached);
    }

    private AchievementContext buildAchievementContext(final UserEntity user, final int matchesWon, final int losses,
            final int completedMatches, final int uniqueCardsCount, final int totalHonors,
            final ElementalStats elemental, final Set<String> unlockedTitles) {
        final int currentLevel = intOrDefault(user.getLevel(), 1);
        final int mmr = intOrDefault(user.getMmr(), 1000);
        final int pokecoins = intOrDefault(user.getPokecoins(), 0);
        final int battlePoints = intOrDefault(user.getBattlePoints(), 0);
        final int totalDamageDealt = intOrDefault(user.getTotalDamageDealt(), 0);
        final int totalKos = intOrDefault(user.getTotalKos(), 0);
        final int perfectWins = intOrDefault(user.getPerfectWins(), 0);
        final int comebackWins = intOrDefault(user.getComebackWins(), 0);
        final int trainerCardsPlayed = intOrDefault(user.getTrainerCardsPlayed(), 0);
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
        final MatchAggregates aggregates = computeMatchAggregates(matches, username);

        final Map<HonorType, Integer> honors = honorService.getHonors(username);
        final int totalHonors = honors.values().stream().mapToInt(Integer::intValue).sum();
        final int uniqueCardsCount = deckRepository.countUniqueCardsByUserId(user.getId());

        final Set<String> unlocked = user.getUnlockedTitles() != null ? user.getUnlockedTitles() : new HashSet<>();

        final ElementalStats elemental = computeElementalStats(user.getId());
        final int completedMatches = aggregates.completedMatchesPlayed();
        final AchievementContext ctx = buildAchievementContext(user, aggregates.matchesWon(), completedMatches - aggregates.matchesWon(),
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
