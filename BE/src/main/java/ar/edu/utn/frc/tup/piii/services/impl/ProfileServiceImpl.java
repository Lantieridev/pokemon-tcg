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
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserShowcaseRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final String[] ALL_ACHIEVEMENT_TITLES = {
        "Novato", "Entrenador",
        "Estratega en Crecimiento", "Maestro de Cartas", "Gran Mentor", "Líder de Élite", "Leyenda Viviente", "Maestro de Kanto",
        "Ganador Prometedor", "Ganador Implacable", "Campeón del Tablero", "Leyenda del Tablero", "Inmortal del Tablero",
        "Espíritu Resiliente", "Fuerza de Voluntad",
        "Combatiente", "Combatiente Tenaz", "Veterano de Batallas", "Leyenda de Batallas", "Espíritu Inquebrantable",
        "Coleccionista Novato", "Coleccionista Experto", "Coleccionista de Élite", "Maestro Coleccionista", "Curador del Museo",
        "Compañero Amigable", "Entrenador Respetado", "Héroe del Fair Play",
        "Entrenador Destacado", "Líder de Gimnasio", "Alto Mando", "Campeón de la Liga",
        "Estratega Versátil", "Maestro Adaptable",
        "Multifacético", "Celebridad de Kanto",
        "Súper Nerd de las Ventas", "Magnate de Kanto", "Gladiador del Tablero", "Campeón del Coliseo",
        "Poder Eléctrico", "Fuerza Brutal", "Fuerza de la Naturaleza", "Destructor Cósmico",
        "Derribador", "Cazador de KOs", "Ejecutor Implacable", "Verdugo Supremo",
        "Estratega Imbatible", "Intocable", "Inmaculado",
        "Rey del Clímax", "Espíritu de Remontada", "Fénix del Tablero",
        "Estudioso de Reglas", "Maestro Táctico", "Gran Sabio",
        "Piro-Novato", "Piro-Maestro", "Llama de Kanto",
        "Hidro-Novato", "Maestro del Surf", "Tsunami Viviente",
        "Brote Verde", "Guardián de la Selva", "Espíritu del Bosque",
        "Chispa Inicial", "Voltaje Máximo", "Tormenta Perpetua",
        "Sensitivo", "Mente Mística", "Poder Cósmico",
        "Cinturón Blanco", "Cinturón Negro", "Fuerza Sísmica",
        "Equilibrio", "Estratega Neutral", "Armonía Pura",
        "Amigo del Ratón", "Compañero Fiel", "Aliento Ígneo", "Llama Ancestral",
        "Presión de Agua", "Tsunami de Kanto", "Floración Rápida", "Semilla de la Vida",
        "Mirada Mental", "Fuerza Psíquica"
    };

    private final UserRepository userRepository;
    private final UserShowcaseRepository userShowcaseRepository;
    private final MatchRepository matchRepository;
    private final CardRepository cardRepository;
    private final HonorService honorService;
    private final DeckRepository deckRepository;
    private final ProfanityFilterService profanityFilterService;
    private final UserCardStatRepository userCardStatRepository;
    private final UserEnergyStatRepository userEnergyStatRepository;
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
        this.cardMapper = Objects.requireNonNull(cardMapper, "cardMapper must not be null");
    }

    @Override
    @Transactional
    public UserProfileResponseDTO getProfile(final String username) {
        final Optional<UserEntity> userOpt = userRepository.findByUsername(username);
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
            showcasedDeckDto = UserProfileResponseDTO.ShowcasedDeck.builder()
                    .id(user.getShowcasedDeck().getId())
                    .name(user.getShowcasedDeck().getName())
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
                } catch (Exception e) {
                    // Fallback
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
                .statistics(stats)
                .honors(honors)
                .unlockedTitles(user.getUnlockedTitles())
                .showcase(showcaseSlots)
                .showcasedDeck(showcasedDeckDto)
                .advancedStats(advancedStatsDTO)
                .build();
    }

    @Override
    public void updateProfile(final String username, final UpdateProfileRequestDTO request) {
        if (username == null || request == null) {
            return;
        }
        final Optional<UserEntity> userOpt = userRepository.findByUsername(username);
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
        final Optional<UserEntity> userOpt = userRepository.findByUsername(username);
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

        // 1. Asignar XP
        final int xpGained = won ? 50 : 25;
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

    private void checkAndUnlockTitles(final UserEntity user, final int matchesPlayed, final int matchesWon, final int totalHonors, final int uniqueCardsCount) {
        Set<String> titles = user.getUnlockedTitles();
        if (titles == null) {
            titles = new HashSet<>();
        }

        if (user.getUsername() != null && (user.getUsername().equalsIgnoreCase("admin") || user.getUsername().equalsIgnoreCase("model"))) {
            boolean addedAny = false;
            for (final String title : ALL_ACHIEVEMENT_TITLES) {
                if (titles.add(title)) {
                    addedAny = true;
                }
            }
            if (addedAny) {
                user.setUnlockedTitles(titles);
                userRepository.save(user);
            }
            return;
        }

        boolean changed = false;

        final List<UserCardStatEntity> cardStats = userCardStatRepository.findByUserId(user.getId());
        final List<UserEnergyStatEntity> energyStats = userEnergyStatRepository.findByUserId(user.getId());

        // Map cards to names
        final List<CardEntity> allCards = cardRepository.findAll();
        final Map<String, String> cardNamesMap = allCards.stream()
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
            if (name != null) {
                final String nameLower = name.toLowerCase();
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
        }

        int fireAttached = 0;
        int waterAttached = 0;
        int grassAttached = 0;
        int lightningAttached = 0;
        int psychicAttached = 0;
        int fightingAttached = 0;
        int colorlessAttached = 0;

        for (final UserEnergyStatEntity stat : energyStats) {
            if (stat.getEnergyType() != null) {
                switch (stat.getEnergyType().toUpperCase()) {
                    case "FIRE" -> fireAttached += stat.getTimesPlayed();
                    case "WATER" -> waterAttached += stat.getTimesPlayed();
                    case "GRASS" -> grassAttached += stat.getTimesPlayed();
                    case "LIGHTNING" -> lightningAttached += stat.getTimesPlayed();
                    case "PSYCHIC" -> psychicAttached += stat.getTimesPlayed();
                    case "FIGHTING" -> fightingAttached += stat.getTimesPlayed();
                    case "COLORLESS" -> colorlessAttached += stat.getTimesPlayed();
                }
            }
        }

        final int currentLevel = user.getLevel() != null ? user.getLevel() : 1;
        final int mmr = user.getMmr() != null ? user.getMmr() : 1000;
        final int pokecoins = user.getPokecoins() != null ? user.getPokecoins() : 0;
        final int battlePoints = user.getBattlePoints() != null ? user.getBattlePoints() : 0;
        final int totalDamageDealt = user.getTotalDamageDealt() != null ? user.getTotalDamageDealt() : 0;
        final int totalKos = user.getTotalKos() != null ? user.getTotalKos() : 0;
        final int perfectWins = user.getPerfectWins() != null ? user.getPerfectWins() : 0;
        final int comebackWins = user.getComebackWins() != null ? user.getComebackWins() : 0;
        final int trainerCardsPlayed = user.getTrainerCardsPlayed() != null ? user.getTrainerCardsPlayed() : 0;
        final int losses = matchesPlayed - matchesWon;

        // Títulos por Defecto
        if (titles.add("Novato")) changed = true;
        if (titles.add("Entrenador")) changed = true;

        // 1. Nivel
        if (currentLevel >= 5 && titles.add("Estratega en Crecimiento")) changed = true;
        if (currentLevel >= 10 && titles.add("Maestro de Cartas")) changed = true;
        if (currentLevel >= 20 && titles.add("Gran Mentor")) changed = true;
        if (currentLevel >= 30 && titles.add("Líder de Élite")) changed = true;
        if (currentLevel >= 50 && titles.add("Leyenda Viviente")) changed = true;
        if (currentLevel >= 100 && titles.add("Maestro de Kanto")) changed = true;

        // 2. Victorias
        if (matchesWon >= 5 && titles.add("Ganador Prometedor")) changed = true;
        if (matchesWon >= 20 && titles.add("Ganador Implacable")) changed = true;
        if (matchesWon >= 50 && titles.add("Campeón del Tablero")) changed = true;
        if (matchesWon >= 100 && titles.add("Leyenda del Tablero")) changed = true;
        if (matchesWon >= 250 && titles.add("Inmortal del Tablero")) changed = true;

        // 3. Resiliencia
        if (losses >= 10 && titles.add("Espíritu Resiliente")) changed = true;
        if (losses >= 50 && titles.add("Fuerza de Voluntad")) changed = true;

        // 4. Partidas Jugadas
        if (matchesPlayed >= 10 && titles.add("Combatiente")) changed = true;
        if (matchesPlayed >= 25 && titles.add("Combatiente Tenaz")) changed = true;
        if (matchesPlayed >= 50 && titles.add("Veterano de Batallas")) changed = true;
        if (matchesPlayed >= 100 && titles.add("Leyenda de Batallas")) changed = true;
        if (matchesPlayed >= 250 && titles.add("Espíritu Inquebrantable")) changed = true;

        // 5. Colección de Cartas
        if (uniqueCardsCount >= 30 && titles.add("Coleccionista Novato")) changed = true;
        if (uniqueCardsCount >= 50 && titles.add("Coleccionista Experto")) changed = true;
        if (uniqueCardsCount >= 100 && titles.add("Coleccionista de Élite")) changed = true;
        if (uniqueCardsCount >= 150 && titles.add("Maestro Coleccionista")) changed = true;
        if (uniqueCardsCount >= 200 && titles.add("Curador del Museo")) changed = true;

        // 6. Honores
        if (totalHonors >= 5 && titles.add("Compañero Amigable")) changed = true;
        if (totalHonors >= 15 && titles.add("Entrenador Respetado")) changed = true;
        if (totalHonors >= 30 && titles.add("Héroe del Fair Play")) changed = true;

        // 7. Competitivo y MMR
        if (mmr >= 1200 && titles.add("Entrenador Destacado")) changed = true;
        if (mmr >= 1500 && titles.add("Líder de Gimnasio")) changed = true;
        if (mmr >= 1800 && titles.add("Alto Mando")) changed = true;
        if (mmr >= 2000 && titles.add("Campeón de la Liga")) changed = true;

        // 8. Versatilidad
        if (versatilityCount >= 20 && titles.add("Estratega Versátil")) changed = true;
        if (versatilityCount >= 50 && titles.add("Maestro Adaptable")) changed = true;

        // 10. Economía
        if (pokecoins >= 1000 && titles.add("Súper Nerd de las Ventas")) changed = true;
        if (pokecoins >= 5000 && titles.add("Magnate de Kanto")) changed = true;
        if (battlePoints >= 500 && titles.add("Gladiador del Tablero")) changed = true;
        if (battlePoints >= 2000 && titles.add("Campeón del Coliseo")) changed = true;

        // 11. Combate - Daño
        if (totalDamageDealt >= 1000 && titles.add("Poder Eléctrico")) changed = true;
        if (totalDamageDealt >= 5000 && titles.add("Fuerza Brutal")) changed = true;
        if (totalDamageDealt >= 15000 && titles.add("Fuerza de la Naturaleza")) changed = true;
        if (totalDamageDealt >= 50000 && titles.add("Destructor Cósmico")) changed = true;

        // 11. Combate - KOs
        if (totalKos >= 10 && titles.add("Derribador")) changed = true;
        if (totalKos >= 50 && titles.add("Cazador de KOs")) changed = true;
        if (totalKos >= 150 && titles.add("Ejecutor Implacable")) changed = true;
        if (totalKos >= 300 && titles.add("Verdugo Supremo")) changed = true;

        // 11. Combate - Victorias Perfectas
        if (perfectWins >= 1 && titles.add("Estratega Imbatible")) changed = true;
        if (perfectWins >= 5 && titles.add("Intocable")) changed = true;
        if (perfectWins >= 15 && titles.add("Inmaculado")) changed = true;

        // 11. Combate - Victorias de Remontada
        if (comebackWins >= 1 && titles.add("Rey del Clímax")) changed = true;
        if (comebackWins >= 5 && titles.add("Espíritu de Remontada")) changed = true;
        if (comebackWins >= 15 && titles.add("Fénix del Tablero")) changed = true;

        // 11. Combate - Cartas de Entrenador
        if (trainerCardsPlayed >= 50 && titles.add("Estudioso de Reglas")) changed = true;
        if (trainerCardsPlayed >= 200 && titles.add("Maestro Táctico")) changed = true;
        if (trainerCardsPlayed >= 500 && titles.add("Gran Sabio")) changed = true;

        // 12. Elemental - Fuego
        if (fireAttached >= 50 && titles.add("Piro-Novato")) changed = true;
        if (fireAttached >= 200 && titles.add("Piro-Maestro")) changed = true;
        if (fireAttached >= 500 && titles.add("Llama de Kanto")) changed = true;

        // 12. Elemental - Agua
        if (waterAttached >= 50 && titles.add("Hidro-Novato")) changed = true;
        if (waterAttached >= 200 && titles.add("Maestro del Surf")) changed = true;
        if (waterAttached >= 500 && titles.add("Tsunami Viviente")) changed = true;

        // 12. Elemental - Planta
        if (grassAttached >= 50 && titles.add("Brote Verde")) changed = true;
        if (grassAttached >= 200 && titles.add("Guardián de la Selva")) changed = true;
        if (grassAttached >= 500 && titles.add("Espíritu del Bosque")) changed = true;

        // 12. Elemental - Rayo
        if (lightningAttached >= 50 && titles.add("Chispa Inicial")) changed = true;
        if (lightningAttached >= 200 && titles.add("Voltaje Máximo")) changed = true;
        if (lightningAttached >= 500 && titles.add("Tormenta Perpetua")) changed = true;

        // 12. Elemental - Psíquico
        if (psychicAttached >= 50 && titles.add("Sensitivo")) changed = true;
        if (psychicAttached >= 200 && titles.add("Mente Mística")) changed = true;
        if (psychicAttached >= 500 && titles.add("Poder Cósmico")) changed = true;

        // 12. Elemental - Lucha
        if (fightingAttached >= 50 && titles.add("Cinturón Blanco")) changed = true;
        if (fightingAttached >= 200 && titles.add("Cinturón Negro")) changed = true;
        if (fightingAttached >= 500 && titles.add("Fuerza Sísmica")) changed = true;

        // 12. Elemental - Incoloro
        if (colorlessAttached >= 50 && titles.add("Equilibrio")) changed = true;
        if (colorlessAttached >= 200 && titles.add("Estratega Neutral")) changed = true;
        if (colorlessAttached >= 500 && titles.add("Armonía Pura")) changed = true;

        // 13. Lealtad - Pikachu
        if (pikachuPlays >= 15 && titles.add("Amigo del Ratón")) changed = true;
        if (pikachuPlays >= 50 && titles.add("Compañero Fiel")) changed = true;

        // 13. Lealtad - Charizard
        if (charizardPlays >= 10 && titles.add("Aliento Ígneo")) changed = true;
        if (charizardPlays >= 30 && titles.add("Llama Ancestral")) changed = true;

        // 13. Lealtad - Blastoise
        if (blastoisePlays >= 10 && titles.add("Presión de Agua")) changed = true;
        if (blastoisePlays >= 30 && titles.add("Tsunami de Kanto")) changed = true;

        // 13. Lealtad - Venusaur
        if (venusaurPlays >= 10 && titles.add("Floración Rápida")) changed = true;
        if (venusaurPlays >= 30 && titles.add("Semilla de la Vida")) changed = true;

        // 13. Lealtad - Mewtwo
        if (mewtwoPlays >= 10 && titles.add("Mirada Mental")) changed = true;
        if (mewtwoPlays >= 30 && titles.add("Fuerza Psíquica")) changed = true;

        // 9. Colección de Títulos (se chequea al final para incluir los recién desbloqueados)
        if (titles.size() >= 10 && titles.add("Multifacético")) changed = true;
        if (titles.size() >= 20 && titles.add("Celebridad de Kanto")) changed = true;

        if (changed) {
            user.setUnlockedTitles(titles);
        }
    }

    @Override
    public void updateShowcaseDeck(final String username, final Long deckId) {
        if (username == null) {
            return;
        }
        final Optional<UserEntity> userOpt = userRepository.findByUsername(username);
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
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setTotalDamageDealt((user.getTotalDamageDealt() != null ? user.getTotalDamageDealt() : 0) + damage);
            userRepository.save(user);
        });
    }

    @Override
    public void trackTrainerCardPlayed(final String username) {
        if (username == null) {
            return;
        }
        userRepository.findByUsername(username).ifPresent(user -> {
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
        final Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado: " + username);
        }
        final UserEntity user = userOpt.get();

        // 1. Estadísticas necesarias
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
            unlocked = new java.util.HashSet<>();
        }

        final List<UserCardStatEntity> cardStats = userCardStatRepository.findByUserId(user.getId());
        final List<UserEnergyStatEntity> energyStats = userEnergyStatRepository.findByUserId(user.getId());

        // Map cards to names
        final List<CardEntity> allCards = cardRepository.findAll();
        final Map<String, String> cardNamesMap = allCards.stream()
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
            if (name != null) {
                final String nameLower = name.toLowerCase();
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
        }

        int fireAttached = 0;
        int waterAttached = 0;
        int grassAttached = 0;
        int lightningAttached = 0;
        int psychicAttached = 0;
        int fightingAttached = 0;
        int colorlessAttached = 0;

        for (final UserEnergyStatEntity stat : energyStats) {
            if (stat.getEnergyType() != null) {
                switch (stat.getEnergyType().toUpperCase()) {
                    case "FIRE" -> fireAttached += stat.getTimesPlayed();
                    case "WATER" -> waterAttached += stat.getTimesPlayed();
                    case "GRASS" -> grassAttached += stat.getTimesPlayed();
                    case "LIGHTNING" -> lightningAttached += stat.getTimesPlayed();
                    case "PSYCHIC" -> psychicAttached += stat.getTimesPlayed();
                    case "FIGHTING" -> fightingAttached += stat.getTimesPlayed();
                    case "COLORLESS" -> colorlessAttached += stat.getTimesPlayed();
                }
            }
        }

        final int currentLevel = user.getLevel() != null ? user.getLevel() : 1;
        final int mmr = user.getMmr() != null ? user.getMmr() : 1000;
        final int pokecoins = user.getPokecoins() != null ? user.getPokecoins() : 0;
        final int battlePoints = user.getBattlePoints() != null ? user.getBattlePoints() : 0;
        final int totalDamageDealt = user.getTotalDamageDealt() != null ? user.getTotalDamageDealt() : 0;
        final int totalKos = user.getTotalKos() != null ? user.getTotalKos() : 0;
        final int perfectWins = user.getPerfectWins() != null ? user.getPerfectWins() : 0;
        final int comebackWins = user.getComebackWins() != null ? user.getComebackWins() : 0;
        final int trainerCardsPlayed = user.getTrainerCardsPlayed() != null ? user.getTrainerCardsPlayed() : 0;
        final int losses = completedMatches - matchesWon;
        final int titleCount = unlocked.size();

        final List<ar.edu.utn.frc.tup.piii.dtos.UserAchievementProgressDTO> list = new java.util.ArrayList<>();

        // Novato, Entrenador (Defecto)
        list.add(createProgressDTO("Novato", "DEFECTO", unlocked.contains("Novato"), "Título inicial por defecto", 1, 1, "TITULO", "Novato"));
        list.add(createProgressDTO("Entrenador", "DEFECTO", unlocked.contains("Entrenador"), "Título inicial por defecto", 1, 1, "TITULO", "Entrenador"));

        // 1. Nivel y Experiencia (NIVEL)
        list.add(createProgressDTO("Estratega en Crecimiento", "NIVEL", unlocked.contains("Estratega en Crecimiento"), "Alcanzar nivel 5", currentLevel, 5, "TITULO", "Estratega en Crecimiento"));
        list.add(createProgressDTO("Maestro de Cartas", "NIVEL", unlocked.contains("Maestro de Cartas"), "Alcanzar nivel 10", currentLevel, 10, "TITULO", "Maestro de Cartas"));
        list.add(createProgressDTO("Gran Mentor", "NIVEL", unlocked.contains("Gran Mentor"), "Alcanzar nivel 20", currentLevel, 20, "FOTO_PERFIL", "avatar_bulbasaur"));
        list.add(createProgressDTO("Líder de Élite", "NIVEL", unlocked.contains("Líder de Élite"), "Alcanzar nivel 30", currentLevel, 30, "FOTO_PERFIL", "avatar_charizard_3d"));
        list.add(createProgressDTO("Leyenda Viviente", "NIVEL", unlocked.contains("Leyenda Viviente"), "Alcanzar nivel 50", currentLevel, 50, "MEDALLA", "medal_legend"));
        list.add(createProgressDTO("Maestro de Kanto", "NIVEL", unlocked.contains("Maestro de Kanto"), "Alcanzar nivel 100", currentLevel, 100, "FOTO_PERFIL", "avatar_mewtwo_3d"));

        // 2. Victorias (VICTORIAS)
        list.add(createProgressDTO("Ganador Prometedor", "VICTORIAS", unlocked.contains("Ganador Prometedor"), "Ganar 5 partidas", matchesWon, 5, "TITULO", "Ganador Prometedor"));
        list.add(createProgressDTO("Ganador Implacable", "VICTORIAS", unlocked.contains("Ganador Implacable"), "Ganar 20 partidas", matchesWon, 20, "FOTO_PERFIL", "avatar_winner_badge"));
        list.add(createProgressDTO("Campeón del Tablero", "VICTORIAS", unlocked.contains("Campeón del Tablero"), "Ganar 50 partidas", matchesWon, 50, "MEDALLA", "medal_champion"));
        list.add(createProgressDTO("Leyenda del Tablero", "VICTORIAS", unlocked.contains("Leyenda del Tablero"), "Ganar 100 partidas", matchesWon, 100, "MEDALLA", "medal_board_legend"));
        list.add(createProgressDTO("Inmortal del Tablero", "VICTORIAS", unlocked.contains("Inmortal del Tablero"), "Ganar 250 partidas", matchesWon, 250, "FOTO_PERFIL", "avatar_red_champ"));

        // 3. Derrotas y Resiliencia (RESILIENCIA)
        list.add(createProgressDTO("Espíritu Resiliente", "RESILIENCIA", unlocked.contains("Espíritu Resiliente"), "Perder 10 partidas en total", losses, 10, "TITULO", "Espíritu Resiliente"));
        list.add(createProgressDTO("Fuerza de Voluntad", "RESILIENCIA", unlocked.contains("Fuerza de Voluntad"), "Perder 50 partidas en total", losses, 50, "FOTO_PERFIL", "avatar_resilience_mid"));

        // 4. Partidas Jugadas (PARTIDAS_JUGADAS)
        list.add(createProgressDTO("Combatiente", "PARTIDAS_JUGADAS", unlocked.contains("Combatiente"), "Jugar 10 partidas completas", completedMatches, 10, "TITULO", "Combatiente"));
        list.add(createProgressDTO("Combatiente Tenaz", "PARTIDAS_JUGADAS", unlocked.contains("Combatiente Tenaz"), "Jugar 25 partidas completas", completedMatches, 25, "TITULO", "Combatiente Tenaz"));
        list.add(createProgressDTO("Veterano de Batallas", "PARTIDAS_JUGADAS", unlocked.contains("Veterano de Batallas"), "Jugar 50 partidas completas", completedMatches, 50, "MEDALLA", "medal_veteran"));
        list.add(createProgressDTO("Leyenda de Batallas", "PARTIDAS_JUGADAS", unlocked.contains("Leyenda de Batallas"), "Jugar 100 partidas completas", completedMatches, 100, "FOTO_PERFIL", "avatar_lucas_legend"));
        list.add(createProgressDTO("Espíritu Inquebrantable", "PARTIDAS_JUGADAS", unlocked.contains("Espíritu Inquebrantable"), "Jugar 250 partidas completas", completedMatches, 250, "FOTO_PERFIL", "avatar_eevee_3d"));

        // 5. Colección de Cartas (COLECCION)
        list.add(createProgressDTO("Coleccionista Novato", "COLECCION", unlocked.contains("Coleccionista Novato"), "Tener 30 cartas distintas en tus mazos", uniqueCardsCount, 30, "TITULO", "Coleccionista Novato"));
        list.add(createProgressDTO("Coleccionista Experto", "COLECCION", unlocked.contains("Coleccionista Experto"), "Tener 50 cartas distintas en tus mazos", uniqueCardsCount, 50, "TITULO", "Coleccionista Experto"));
        list.add(createProgressDTO("Coleccionista de Élite", "COLECCION", unlocked.contains("Coleccionista de Élite"), "Tener 100 cartas distintas en tus mazos", uniqueCardsCount, 100, "MEDALLA", "medal_collector_elite"));
        list.add(createProgressDTO("Maestro Coleccionista", "COLECCION", unlocked.contains("Maestro Coleccionista"), "Tener 150 cartas distintas en tus mazos", uniqueCardsCount, 150, "MEDALLA", "medal_collector_legend"));
        list.add(createProgressDTO("Curador del Museo", "COLECCION", unlocked.contains("Curador del Museo"), "Tener 200 cartas distintas en tus mazos", uniqueCardsCount, 200, "FOTO_PERFIL", "avatar_collector_legend"));

        // 6. Honores Recibidos (HONORES)
        list.add(createProgressDTO("Compañero Amigable", "HONORES", unlocked.contains("Compañero Amigable"), "Recibir 5 honores de otros jugadores", totalHonors, 5, "TITULO", "Compañero Amigable"));
        list.add(createProgressDTO("Entrenador Respetado", "HONORES", unlocked.contains("Entrenador Respetado"), "Recibir 15 honores de otros jugadores", totalHonors, 15, "TITULO", "Entrenador Respetado"));
        list.add(createProgressDTO("Héroe del Fair Play", "HONORES", unlocked.contains("Héroe del Fair Play"), "Recibir 30 honores de otros jugadores", totalHonors, 30, "MEDALLA", "medal_fair_play_legend"));

        // 7. Competitivo y MMR (COMPETITIVO)
        list.add(createProgressDTO("Entrenador Destacado", "COMPETITIVO", unlocked.contains("Entrenador Destacado"), "Alcanzar 1200 de MMR", mmr, 1200, "TITULO", "Entrenador Destacado"));
        list.add(createProgressDTO("Líder de Gimnasio", "COMPETITIVO", unlocked.contains("Líder de Gimnasio"), "Alcanzar 1500 de MMR", mmr, 1500, "FOTO_PERFIL", "avatar_gym_leader"));
        list.add(createProgressDTO("Alto Mando", "COMPETITIVO", unlocked.contains("Alto Mando"), "Alcanzar 1800 de MMR", mmr, 1800, "TITULO", "Alto Mando"));
        list.add(createProgressDTO("Campeón de la Liga", "COMPETITIVO", unlocked.contains("Campeón de la Liga"), "Alcanzar 2000 de MMR", mmr, 2000, "MEDALLA", "medal_league_champion"));

        // 8. Versatilidad (VERSATILIDAD)
        list.add(createProgressDTO("Estratega Versátil", "VERSATILIDAD", unlocked.contains("Estratega Versátil"), "Jugar 20 cartas diferentes en partidas", versatilityCount, 20, "FOTO_PERFIL", "avatar_versatility_mid"));
        list.add(createProgressDTO("Maestro Adaptable", "VERSATILIDAD", unlocked.contains("Maestro Adaptable"), "Jugar 50 cartas diferentes en partidas", versatilityCount, 50, "FOTO_PERFIL", "avatar_versatility_3d"));

        // 9. Colección de Títulos (TITULOS)
        list.add(createProgressDTO("Multifacético", "TITULOS", unlocked.contains("Multifacético"), "Desbloquear 10 títulos", titleCount, 10, "FOTO_PERFIL", "avatar_multifaceted"));
        list.add(createProgressDTO("Celebridad de Kanto", "TITULOS", unlocked.contains("Celebridad de Kanto"), "Desbloquear 20 títulos", titleCount, 20, "FOTO_PERFIL", "avatar_celebrity"));

        // 10. Economía y Divisas (ECONOMIA)
        list.add(createProgressDTO("Súper Nerd de las Ventas", "ECONOMIA", unlocked.contains("Súper Nerd de las Ventas"), "Acumular 1,000 Pokecoins", pokecoins, 1000, "MEDALLA", "medal_coins_1k"));
        list.add(createProgressDTO("Magnate de Kanto", "ECONOMIA", unlocked.contains("Magnate de Kanto"), "Acumular 5,000 Pokecoins", pokecoins, 5000, "MEDALLA", "medal_magnate_gold"));
        list.add(createProgressDTO("Gladiador del Tablero", "ECONOMIA", unlocked.contains("Gladiador del Tablero"), "Acumular 500 Battle Points", battlePoints, 500, "TITULO", "Gladiador del Tablero"));
        list.add(createProgressDTO("Campeón del Coliseo", "ECONOMIA", unlocked.contains("Campeón del Coliseo"), "Acumular 2,000 Battle Points", battlePoints, 2000, "MEDALLA", "medal_colosseum_legend"));

        // 11. Logros de Combate (COMBATE)
        list.add(createProgressDTO("Poder Eléctrico", "COMBATE", unlocked.contains("Poder Eléctrico"), "Infligir 1,000 puntos de daño en total", totalDamageDealt, 1000, "MEDALLA", "medal_power_1k"));
        list.add(createProgressDTO("Fuerza Brutal", "COMBATE", unlocked.contains("Fuerza Brutal"), "Infligir 5,000 puntos de daño en total", totalDamageDealt, 5000, "TITULO", "Fuerza Brutal"));
        list.add(createProgressDTO("Fuerza de la Naturaleza", "COMBATE", unlocked.contains("Fuerza de la Naturaleza"), "Infligir 15,000 puntos de daño en total", totalDamageDealt, 15000, "FOTO_PERFIL", "avatar_nature_force"));
        list.add(createProgressDTO("Destructor Cósmico", "COMBATE", unlocked.contains("Destructor Cósmico"), "Infligir 50,000 puntos de daño en total", totalDamageDealt, 50000, "TITULO", "Destructor Cósmico"));

        list.add(createProgressDTO("Derribador", "COMBATE", unlocked.contains("Derribador"), "Realizar 10 KOs en total", totalKos, 10, "MEDALLA", "medal_kos_10"));
        list.add(createProgressDTO("Cazador de KOs", "COMBATE", unlocked.contains("Cazador de KOs"), "Realizar 50 KOs en total", totalKos, 50, "TITULO", "Cazador de KOs"));
        list.add(createProgressDTO("Ejecutor Implacable", "COMBATE", unlocked.contains("Ejecutor Implacable"), "Realizar 150 KOs en total", totalKos, 150, "FOTO_PERFIL", "avatar_executor_mid"));
        list.add(createProgressDTO("Verdugo Supremo", "COMBATE", unlocked.contains("Verdugo Supremo"), "Realizar 300 KOs en total", totalKos, 300, "TITULO", "Verdugo Supremo"));

        list.add(createProgressDTO("Estratega Imbatible", "COMBATE", unlocked.contains("Estratega Imbatible"), "Conseguir 1 victoria perfecta (sin sufrir KOs)", perfectWins, 1, "MEDALLA", "medal_perfect_1"));
        list.add(createProgressDTO("Intocable", "COMBATE", unlocked.contains("Intocable"), "Conseguir 5 victorias perfectas", perfectWins, 5, "TITULO", "Intocable"));
        list.add(createProgressDTO("Inmaculado", "COMBATE", unlocked.contains("Inmaculado"), "Conseguir 15 victorias perfectas", perfectWins, 15, "TITULO", "Inmaculado"));

        list.add(createProgressDTO("Rey del Clímax", "COMBATE", unlocked.contains("Rey del Clímax"), "Conseguir 1 victoria tras remontada", comebackWins, 1, "MEDALLA", "medal_comeback_1"));
        list.add(createProgressDTO("Espíritu de Remontada", "COMBATE", unlocked.contains("Espíritu de Remontada"), "Conseguir 5 victorias tras remontada", comebackWins, 5, "TITULO", "Espíritu de Remontada"));
        list.add(createProgressDTO("Fénix del Tablero", "COMBATE", unlocked.contains("Fénix del Tablero"), "Conseguir 15 victorias tras remontada", comebackWins, 15, "TITULO", "Fénix del Tablero"));

        list.add(createProgressDTO("Estudioso de Reglas", "COMBATE", unlocked.contains("Estudioso de Reglas"), "Jugar 50 cartas de Entrenador", trainerCardsPlayed, 50, "FOTO_PERFIL", "avatar_rules_student"));
        list.add(createProgressDTO("Maestro Táctico", "COMBATE", unlocked.contains("Maestro Táctico"), "Jugar 200 cartas de Entrenador", trainerCardsPlayed, 200, "TITULO", "Maestro Táctico"));
        list.add(createProgressDTO("Gran Sabio", "COMBATE", unlocked.contains("Gran Sabio"), "Jugar 500 cartas de Entrenador", trainerCardsPlayed, 500, "TITULO", "Gran Sabio"));

        // 12. Logros Elementales (ELEMENTAL)
        list.add(createProgressDTO("Piro-Novato", "ELEMENTAL", unlocked.contains("Piro-Novato"), "Unir 50 energías de Fuego", fireAttached, 50, "MEDALLA", "medal_fire_50"));
        list.add(createProgressDTO("Piro-Maestro", "ELEMENTAL", unlocked.contains("Piro-Maestro"), "Unir 200 energías de Fuego", fireAttached, 200, "MEDALLA", "medal_fire_200"));
        list.add(createProgressDTO("Llama de Kanto", "ELEMENTAL", unlocked.contains("Llama de Kanto"), "Unir 500 energías de Fuego", fireAttached, 500, "FOTO_PERFIL", "avatar_fire_kanto"));

        list.add(createProgressDTO("Hidro-Novato", "ELEMENTAL", unlocked.contains("Hidro-Novato"), "Unir 50 energías de Agua", waterAttached, 50, "MEDALLA", "medal_water_50"));
        list.add(createProgressDTO("Maestro del Surf", "ELEMENTAL", unlocked.contains("Maestro del Surf"), "Unir 200 energías de Agua", waterAttached, 200, "MEDALLA", "medal_water_200"));
        list.add(createProgressDTO("Tsunami Viviente", "ELEMENTAL", unlocked.contains("Tsunami Viviente"), "Unir 500 energías de Agua", waterAttached, 500, "FOTO_PERFIL", "avatar_water_kanto"));

        list.add(createProgressDTO("Brote Verde", "ELEMENTAL", unlocked.contains("Brote Verde"), "Unir 50 energías de Planta", grassAttached, 50, "MEDALLA", "medal_grass_50"));
        list.add(createProgressDTO("Guardián de la Selva", "ELEMENTAL", unlocked.contains("Guardián de la Selva"), "Unir 200 energías de Planta", grassAttached, 200, "MEDALLA", "medal_grass_200"));
        list.add(createProgressDTO("Espíritu del Bosque", "ELEMENTAL", unlocked.contains("Espíritu del Bosque"), "Unir 500 energías de Planta", grassAttached, 500, "FOTO_PERFIL", "avatar_grass_kanto"));

        list.add(createProgressDTO("Chispa Inicial", "ELEMENTAL", unlocked.contains("Chispa Inicial"), "Unir 50 energías de Rayo", lightningAttached, 50, "MEDALLA", "medal_lightning_50"));
        list.add(createProgressDTO("Voltaje Máximo", "ELEMENTAL", unlocked.contains("Voltaje Máximo"), "Unir 200 energías de Rayo", lightningAttached, 200, "MEDALLA", "medal_lightning_200"));
        list.add(createProgressDTO("Tormenta Perpetua", "ELEMENTAL", unlocked.contains("Tormenta Perpetua"), "Unir 500 energías de Rayo", lightningAttached, 500, "FOTO_PERFIL", "avatar_lightning_kanto"));

        list.add(createProgressDTO("Sensitivo", "ELEMENTAL", unlocked.contains("Sensitivo"), "Unir 50 energías Psíquicas", psychicAttached, 50, "MEDALLA", "medal_psychic_50"));
        list.add(createProgressDTO("Mente Mística", "ELEMENTAL", unlocked.contains("Mente Mística"), "Unir 200 energías Psíquicas", psychicAttached, 200, "MEDALLA", "medal_psychic_200"));
        list.add(createProgressDTO("Poder Cósmico", "ELEMENTAL", unlocked.contains("Poder Cósmico"), "Unir 500 energías Psíquicas", psychicAttached, 500, "FOTO_PERFIL", "avatar_psychic_kanto"));

        list.add(createProgressDTO("Cinturón Blanco", "ELEMENTAL", unlocked.contains("Cinturón Blanco"), "Unir 50 energías de Lucha", fightingAttached, 50, "FOTO_PERFIL", "avatar_belt_white"));
        list.add(createProgressDTO("Cinturón Negro", "ELEMENTAL", unlocked.contains("Cinturón Negro"), "Unir 200 energías de Lucha", fightingAttached, 200, "TITULO", "Cinturón Negro"));
        list.add(createProgressDTO("Fuerza Sísmica", "ELEMENTAL", unlocked.contains("Fuerza Sísmica"), "Unir 500 energías de Lucha", fightingAttached, 500, "FOTO_PERFIL", "avatar_fighting_kanto"));

        list.add(createProgressDTO("Equilibrio", "ELEMENTAL", unlocked.contains("Equilibrio"), "Unir 50 energías Incoloras", colorlessAttached, 50, "FOTO_PERFIL", "avatar_neutral_balance"));
        list.add(createProgressDTO("Estratega Neutral", "ELEMENTAL", unlocked.contains("Estratega Neutral"), "Unir 200 energías Incoloras", colorlessAttached, 200, "TITULO", "Estratega Neutral"));
        list.add(createProgressDTO("Armonía Pura", "ELEMENTAL", unlocked.contains("Armonía Pura"), "Unir 500 energías Incoloras", colorlessAttached, 500, "FOTO_PERFIL", "avatar_colorless_kanto"));

        // 13. Logros de Lealtad (LEALTAD)
        list.add(createProgressDTO("Amigo del Ratón", "LEALTAD", unlocked.contains("Amigo del Ratón"), "Jugar cartas de Pikachu 15 veces", pikachuPlays, 15, "FOTO_PERFIL", "avatar_pikachu_cute"));
        list.add(createProgressDTO("Compañero Fiel", "LEALTAD", unlocked.contains("Compañero Fiel"), "Jugar cartas de Pikachu 50 veces", pikachuPlays, 50, "TITULO", "Compañero Fiel"));

        list.add(createProgressDTO("Aliento Ígneo", "LEALTAD", unlocked.contains("Aliento Ígneo"), "Jugar cartas de Charizard 10 veces", charizardPlays, 10, "FOTO_PERFIL", "avatar_charizard_cute"));
        list.add(createProgressDTO("Llama Ancestral", "LEALTAD", unlocked.contains("Llama Ancestral"), "Jugar cartas de Charizard 30 veces", charizardPlays, 30, "TITULO", "Llama Ancestral"));

        list.add(createProgressDTO("Presión de Agua", "LEALTAD", unlocked.contains("Presión de Agua"), "Jugar cartas de Blastoise 10 veces", blastoisePlays, 10, "FOTO_PERFIL", "avatar_blastoise_cute"));
        list.add(createProgressDTO("Tsunami de Kanto", "LEALTAD", unlocked.contains("Tsunami de Kanto"), "Jugar cartas de Blastoise 30 veces", blastoisePlays, 30, "TITULO", "Tsunami de Kanto"));

        list.add(createProgressDTO("Floración Rápida", "LEALTAD", unlocked.contains("Floración Rápida"), "Jugar cartas de Venusaur 10 veces", venusaurPlays, 10, "FOTO_PERFIL", "avatar_venusaur_cute"));
        list.add(createProgressDTO("Semilla de la Vida", "LEALTAD", unlocked.contains("Semilla de la Vida"), "Jugar cartas de Venusaur 30 veces", venusaurPlays, 30, "TITULO", "Semilla de la Vida"));

        list.add(createProgressDTO("Mirada Mental", "LEALTAD", unlocked.contains("Mirada Mental"), "Jugar cartas de Mewtwo 10 veces", mewtwoPlays, 10, "FOTO_PERFIL", "avatar_mewtwo_cute"));
        list.add(createProgressDTO("Fuerza Psíquica", "LEALTAD", unlocked.contains("Fuerza Psíquica"), "Jugar cartas de Mewtwo 30 veces", mewtwoPlays, 30, "TITULO", "Fuerza Psíquica"));

        return list;
    }

    private ar.edu.utn.frc.tup.piii.dtos.UserAchievementProgressDTO createProgressDTO(
            final String title, final String category, final boolean unlocked, final String req, final int progress, final int target,
            final String rewardType, final String rewardValue) {
        return ar.edu.utn.frc.tup.piii.dtos.UserAchievementProgressDTO.builder()
                .title(title)
                .category(category)
                .unlocked(unlocked)
                .requirement(req)
                .progress(unlocked ? target : Math.min(progress, target))
                .target(target)
                .rewardType(rewardType)
                .rewardValue(rewardValue)
                .build();
    }
}
