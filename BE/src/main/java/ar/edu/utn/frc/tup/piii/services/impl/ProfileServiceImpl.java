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

    private final UserRepository userRepository;
    private final UserShowcaseRepository userShowcaseRepository;
    private final MatchRepository matchRepository;
    private final CardRepository cardRepository;
    private final HonorService honorService;
    private final DeckRepository deckRepository;
    private final ProfanityFilterService profanityFilterService;

    public ProfileServiceImpl(final UserRepository userRepository,
                              final UserShowcaseRepository userShowcaseRepository,
                              final MatchRepository matchRepository,
                              final CardRepository cardRepository,
                              final HonorService honorService,
                              final DeckRepository deckRepository,
                              final ProfanityFilterService profanityFilterService) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.userShowcaseRepository = Objects.requireNonNull(userShowcaseRepository, "userShowcaseRepository must not be null");
        this.matchRepository = Objects.requireNonNull(matchRepository, "matchRepository must not be null");
        this.cardRepository = Objects.requireNonNull(cardRepository, "cardRepository must not be null");
        this.honorService = Objects.requireNonNull(honorService, "honorService must not be null");
        this.deckRepository = Objects.requireNonNull(deckRepository, "deckRepository must not be null");
        this.profanityFilterService = Objects.requireNonNull(profanityFilterService, "profanityFilterService must not be null");
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
        final List<MatchEntity> matches = matchRepository.findMatchesByUsername(username);
        int matchesPlayed = matches.size();
        int matchesWon = 0;
        for (final MatchEntity m : matches) {
            if (m.getWinner() != null && m.getWinner().getUsername().equalsIgnoreCase(username)) {
                matchesWon++;
            }
        }
        int matchesLost = matchesPlayed - matchesWon;
        double winRate = matchesPlayed > 0 ? (matchesWon * 100.0) / matchesPlayed : 0.0;
        // Redondear a dos decimales
        winRate = Math.round(winRate * 100.0) / 100.0;

        final UserProfileResponseDTO.Statistics stats = UserProfileResponseDTO.Statistics.builder()
                .matchesPlayed(matchesPlayed)
                .matchesWon(matchesWon)
                .matchesLost(matchesLost)
                .winRate(winRate)
                .build();

        // 2. Honores recibidos
        final Map<HonorType, Integer> honors = honorService.getHonors(username);
        final int totalHonors = honors.values().stream().mapToInt(Integer::intValue).sum();

        // 2.1. Calcular partidas terminadas totalmente
        int completedMatchesPlayed = 0;
        for (final MatchEntity m : matches) {
            if (m.getStatus() != null && (m.getStatus().equalsIgnoreCase("FINISHED") || m.getStatus().equalsIgnoreCase("COMPLETED"))) {
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
        final int xpToNext = getXpNeededForNextLevel(user.getLevel());

        UserProfileResponseDTO.ShowcasedDeck showcasedDeckDto = null;
        if (user.getShowcasedDeck() != null) {
            showcasedDeckDto = UserProfileResponseDTO.ShowcasedDeck.builder()
                    .id(user.getShowcasedDeck().getId())
                    .name(user.getShowcasedDeck().getName())
                    .build();
        }

        return UserProfileResponseDTO.builder()
                .username(user.getUsername())
                .createdAt(user.getCreatedAt())
                .avatarIcon(user.getAvatarIcon())
                .description(user.getDescription())
                .activeTitle(user.getActiveTitle())
                .level(user.getLevel())
                .xp(user.getXp())
                .xpToNextLevel(xpToNext)
                .statistics(stats)
                .honors(honors)
                .unlockedTitles(user.getUnlockedTitles())
                .showcase(showcaseSlots)
                .showcasedDeck(showcasedDeckDto)
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
                // Solo equipar si el título está desbloqueado
                if (user.getUnlockedTitles().contains(request.getActiveTitle())) {
                    user.setActiveTitle(request.getActiveTitle());
                }
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
                if (!deckRepository.existsByUserIdAndCardId(user.getId(), slot.getCardId())) {
                    throw new IllegalArgumentException("La carta no pertenece a la colección del usuario: " + slot.getCardId());
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
    public void awardXpAndCheckAchievements(final Long userId, final boolean won) {
        if (userId == null) {
            return;
        }
        final Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return;
        }
        final UserEntity user = userOpt.get();

        // 1. Asignar XP
        final int xpGained = won ? 50 : 25;
        user.setXp(user.getXp() + xpGained);

        // 2. Subida de Nivel
        int needed = getXpNeededForNextLevel(user.getLevel());
        while (user.getXp() >= needed) {
            user.setXp(user.getXp() - needed);
            user.setLevel(user.getLevel() + 1);
            needed = getXpNeededForNextLevel(user.getLevel());
        }

        // 3. Chequear y Desbloquear Títulos
        final List<MatchEntity> matches = matchRepository.findMatchesByUsername(user.getUsername());
        int matchesWon = 0;
        int completedMatchesPlayed = 0;
        for (final MatchEntity m : matches) {
            if (m.getWinner() != null && m.getWinner().getId().equals(userId)) {
                matchesWon++;
            }
            if (m.getStatus() != null && (m.getStatus().equalsIgnoreCase("FINISHED") || m.getStatus().equalsIgnoreCase("COMPLETED"))) {
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

        boolean changed = false;

        // Títulos por Defecto
        if (titles.add("Novato")) {
            changed = true;
        }
        if (titles.add("Entrenador")) {
            changed = true;
        }

        // Títulos por Nivel
        if (user.getLevel() >= 5) {
            if (titles.add("Estratega en Crecimiento")) {
                changed = true;
            }
        }
        if (user.getLevel() >= 10) {
            if (titles.add("Maestro de Cartas")) {
                changed = true;
            }
        }

        // Títulos por Victorias
        if (matchesWon >= 5) {
            if (titles.add("Ganador Prometedor")) {
                changed = true;
            }
        }
        if (matchesWon >= 20) {
            if (titles.add("Ganador Implacable")) {
                changed = true;
            }
        }
        if (matchesWon >= 50) {
            if (titles.add("Campeón del Tablero")) {
                changed = true;
            }
        }
        if (matchesWon >= 100) {
            if (titles.add("Leyenda del Tablero")) {
                changed = true;
            }
        }

        // Títulos por Partidas Jugadas (Terminadas totalmente)
        if (matchesPlayed >= 10) {
            if (titles.add("Combatiente")) {
                changed = true;
            }
        }
        if (matchesPlayed >= 25) {
            if (titles.add("Combatiente Tenaz")) {
                changed = true;
            }
        }
        if (matchesPlayed >= 50) {
            if (titles.add("Veterano de Batallas")) {
                changed = true;
            }
        }
        if (matchesPlayed >= 100) {
            if (titles.add("Leyenda de Batallas")) {
                changed = true;
            }
        }

        // Títulos por Colección (Cartas distintas en sus mazos)
        if (uniqueCardsCount >= 30) {
            if (titles.add("Coleccionista Novato")) {
                changed = true;
            }
        }
        if (uniqueCardsCount >= 50) {
            if (titles.add("Coleccionista Experto")) {
                changed = true;
            }
        }
        if (uniqueCardsCount >= 100) {
            if (titles.add("Coleccionista de Élite")) {
                changed = true;
            }
        }
        if (uniqueCardsCount >= 150) {
            if (titles.add("Maestro Coleccionista")) {
                changed = true;
            }
        }

        // Títulos por Honores
        if (totalHonors >= 5) {
            if (titles.add("Compañero Amigable")) {
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
}
