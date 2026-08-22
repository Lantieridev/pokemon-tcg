package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO;
import ar.edu.utn.frc.tup.piii.engine.model.DeckStatus;
import ar.edu.utn.frc.tup.piii.persistence.entity.DeckEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.DeckRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.deck.DeckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ExampleDeckSeeder implements CommandLineRunner {

    private static final int MIN_DUPLICATES_TO_CLEAN_UP = 1;

    private final UserRepository userRepository;
    private final DeckService deckService;
    private final DeckRepository deckRepository;

    public ExampleDeckSeeder(UserRepository userRepository, DeckService deckService, DeckRepository deckRepository) {
        this.userRepository = userRepository;
        this.deckService = deckService;
        this.deckRepository = deckRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        final List<UserEntity> users = userRepository.findAll();
        for (final UserEntity user : users) {
            final List<DeckEntity> existing = new ArrayList<>(deckRepository.findByUserId(user.getId()));
            cleanUpDuplicateDecks(existing);
            seedMissingDecks(user, existing);
        }
    }

    // Clean up any duplicate decks with the same name for this user
    private void cleanUpDuplicateDecks(final List<DeckEntity> existing) {
        final Map<String, List<DeckEntity>> groupedDecks = existing.stream()
                .collect(Collectors.groupingBy(DeckEntity::getName));
        for (final Map.Entry<String, List<DeckEntity>> entry : groupedDecks.entrySet()) {
            final List<DeckEntity> decksWithName = entry.getValue();
            if (decksWithName.size() > MIN_DUPLICATES_TO_CLEAN_UP) {
                for (int i = 1; i < decksWithName.size(); i++) {
                    deckRepository.delete(decksWithName.get(i));
                }
                existing.removeAll(decksWithName.subList(1, decksWithName.size()));
            }
        }
    }

    // PMD AvoidCatchingGenericException: deckService.create() can reject any of the four
    // starter decks for unrelated reasons (validation, card lookup, duplicate name races)
    // and this is best-effort startup seeding - one user's failure must log and move on
    // to the next user, not abort the whole seeding run.
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void seedMissingDecks(final UserEntity user, final List<DeckEntity> existing) {
        try {
            createIfMissing(existing, "Mazo Agua Dev", user, this::buildWaterDeck);
            createIfMissing(existing, "Mazo Fuego Dev", user, this::buildFireDeck);
            createIfMissing(existing, "Mazo Charizard EX Flashfire", user, this::buildCharizardDeck);
            createIfMissing(existing, "Mazo Fuego & Planta Flashfire", user, this::buildFireGrassDeck);
            log.info(">>> SEEDED DECKS FOR USER: {} <<<", user.getUsername());
        } catch (RuntimeException e) {
            log.warn("Failed to seed decks for user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    private void createIfMissing(final List<DeckEntity> existing, final String deckName, final UserEntity user,
            final Supplier<DeckRequestDTO> deckSupplier) {
        final boolean has = existing.stream().anyMatch(d -> deckName.equals(d.getName()));
        if (!has) {
            deckService.create(deckSupplier.get(), user.getUsername());
        }
    }

    private DeckRequestDTO buildWaterDeck() {
        return new DeckRequestDTO(
                "Mazo Agua Dev",
                DeckStatus.VALID,
                List.of(
                        new DeckCardRequestDTO("xy1-31", 4), // Shellder
                        new DeckCardRequestDTO("xy1-33", 4), // Staryu
                        new DeckCardRequestDTO("xy1-35", 4), // Lapras
                        new DeckCardRequestDTO("xy1-29", 2), // Blastoise EX
                        new DeckCardRequestDTO("xy1-123", 4), // Professor's Letter
                        new DeckCardRequestDTO("xy1-125", 4), // Roller Skates
                        new DeckCardRequestDTO("xy1-127", 4), // Shauna
                        new DeckCardRequestDTO("xy1-128", 4), // Super Potion
                        new DeckCardRequestDTO("xy1-121", 4), // Muscle Band
                        new DeckCardRequestDTO("xy1-124", 4), // Red Card
                        new DeckCardRequestDTO("xy1-132", 22)
                )
        );
    }

    private DeckRequestDTO buildFireDeck() {
        return new DeckRequestDTO(
                "Mazo Fuego Dev",
                DeckStatus.VALID,
                List.of(
                        new DeckCardRequestDTO("xy1-4", 4), // Charmander
                        new DeckCardRequestDTO("xy1-20", 4), // Slugma
                        new DeckCardRequestDTO("xy1-22", 4), // Pansear
                        new DeckCardRequestDTO("xy1-11", 2), // Charizard EX
                        new DeckCardRequestDTO("xy1-123", 4), // Professor's Letter
                        new DeckCardRequestDTO("xy1-125", 4), // Roller Skates
                        new DeckCardRequestDTO("xy1-127", 4), // Shauna
                        new DeckCardRequestDTO("xy1-128", 4), // Super Potion
                        new DeckCardRequestDTO("xy1-121", 4), // Muscle Band
                        new DeckCardRequestDTO("xy1-124", 4), // Red Card
                        new DeckCardRequestDTO("xy1-133", 22)
                )
        );
    }

    private DeckRequestDTO buildCharizardDeck() {
        return new DeckRequestDTO(
                "Mazo Charizard EX Flashfire",
                DeckStatus.VALID,
                List.of(
                        new DeckCardRequestDTO("xy2-11", 2), // Charizard-EX (Stoke)
                        new DeckCardRequestDTO("xy2-12", 2), // Charizard-EX (Combustion Blast)
                        new DeckCardRequestDTO("xy2-18", 4), // Litleo
                        new DeckCardRequestDTO("xy2-20", 4), // Pyroar
                        new DeckCardRequestDTO("xy1-24", 4), // Fennekin
                        new DeckCardRequestDTO("xy1-25", 2), // Braixen
                        new DeckCardRequestDTO("xy1-26", 2), // Delphox
                        new DeckCardRequestDTO("xy2-88", 3), // Blacksmith
                        new DeckCardRequestDTO("xy2-89", 3), // Fiery Torch
                        new DeckCardRequestDTO("xy2-90", 2), // Lysandre
                        new DeckCardRequestDTO("xy2-92", 2), // Pal Pad
                        new DeckCardRequestDTO("xy2-95", 2), // Protection Cube
                        new DeckCardRequestDTO("xy2-96", 2), // Sacred Ash
                        new DeckCardRequestDTO("xy2-99", 4), // Ultra Ball
                        new DeckCardRequestDTO("xy1-127", 4), // Shauna
                        new DeckCardRequestDTO("xy1-133", 18) // Fire Energy
                )
        );
    }

    private DeckRequestDTO buildFireGrassDeck() {
        return new DeckRequestDTO(
                "Mazo Fuego & Planta Flashfire",
                DeckStatus.VALID,
                List.of(
                        new DeckCardRequestDTO("xy2-5", 4), // Seedot
                        new DeckCardRequestDTO("xy2-6", 4), // Nuzleaf
                        new DeckCardRequestDTO("xy2-7", 3), // Shiftry
                        new DeckCardRequestDTO("xy2-22", 3), // Feebas
                        new DeckCardRequestDTO("xy2-23", 3), // Milotic
                        new DeckCardRequestDTO("xy2-18", 3), // Litleo
                        new DeckCardRequestDTO("xy2-20", 2), // Pyroar
                        new DeckCardRequestDTO("xy2-91", 3), // Magnetic Storm
                        new DeckCardRequestDTO("xy2-94", 3), // Pokémon Fan Club
                        new DeckCardRequestDTO("xy2-90", 2), // Lysandre
                        new DeckCardRequestDTO("xy2-99", 4), // Ultra Ball
                        new DeckCardRequestDTO("xy2-96", 2), // Sacred Ash
                        new DeckCardRequestDTO("xy1-125", 4), // Roller Skates
                        new DeckCardRequestDTO("xy1-132", 10), // Grass Energy
                        new DeckCardRequestDTO("xy1-133", 10) // Fire Energy
                )
        );
    }
}
