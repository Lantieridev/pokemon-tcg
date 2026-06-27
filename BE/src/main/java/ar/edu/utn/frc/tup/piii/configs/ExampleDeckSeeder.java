package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.persistence.entity.DeckEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.DeckRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.deck.DeckService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ExampleDeckSeeder implements CommandLineRunner {

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
    @Override
    public void run(String... args) throws Exception {
        List<UserEntity> users = userRepository.findAll();
        for (UserEntity user : users) {
            List<DeckEntity> existing = new java.util.ArrayList<>(deckRepository.findByUserId(user.getId()));

            // Clean up any duplicate decks with the same name for this user
            java.util.Map<String, List<DeckEntity>> groupedDecks = existing.stream()
                    .collect(java.util.stream.Collectors.groupingBy(DeckEntity::getName));
            for (java.util.Map.Entry<String, List<DeckEntity>> entry : groupedDecks.entrySet()) {
                List<DeckEntity> decksWithName = entry.getValue();
                if (decksWithName.size() > 1) {
                    for (int i = 1; i < decksWithName.size(); i++) {
                        deckRepository.delete(decksWithName.get(i));
                    }
                    existing.removeAll(decksWithName.subList(1, decksWithName.size()));
                }
            }

            boolean hasWater = existing.stream().anyMatch(d -> d.getName().equals("Mazo Agua Dev"));
            boolean hasFire = existing.stream().anyMatch(d -> d.getName().equals("Mazo Fuego Dev"));
            boolean hasCharizard = existing.stream().anyMatch(d -> d.getName().equals("Mazo Charizard EX Flashfire"));
            boolean hasFireGrass = existing.stream().anyMatch(d -> d.getName().equals("Mazo Fuego & Planta Flashfire"));

            try {
                if (!hasWater) {
                    ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO waterDeck = new ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO(
                            user.getId(),
                            "Mazo Agua Dev",
                            ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                            List.of(
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-31", 4), // Shellder
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-33", 4), // Staryu
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-35", 4), // Lapras
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-29", 2), // Blastoise EX
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-123", 4), // Professor's Letter
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-125", 4), // Roller Skates
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-127", 4), // Shauna
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-128", 4), // Super Potion
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-121", 4), // Muscle Band
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-124", 4), // Red Card
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-132", 22)
                             )
                    );
                    deckService.create(waterDeck);
                }

                if (!hasFire) {
                    ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO fireDeck = new ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO(
                            user.getId(),
                            "Mazo Fuego Dev",
                            ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                            List.of(
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-4", 4), // Charmander
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-20", 4), // Slugma
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-22", 4), // Pansear
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-11", 2), // Charizard EX
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-123", 4), // Professor's Letter
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-125", 4), // Roller Skates
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-127", 4), // Shauna
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-128", 4), // Super Potion
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-121", 4), // Muscle Band
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-124", 4), // Red Card
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-133", 22)
                            )
                    );
                    deckService.create(fireDeck);
                }

                if (!hasCharizard) {
                    ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO charizardDeck = new ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO(
                            user.getId(),
                            "Mazo Charizard EX Flashfire",
                            ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                            List.of(
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-11", 2), // Charizard-EX (Stoke)
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-12", 2), // Charizard-EX (Combustion Blast)
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-18", 4), // Litleo
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-20", 4), // Pyroar
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-24", 4), // Fennekin
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-25", 2), // Braixen
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-26", 2), // Delphox
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-88", 3), // Blacksmith
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-89", 3), // Fiery Torch
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-90", 2), // Lysandre
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-92", 2), // Pal Pad
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-95", 2), // Protection Cube
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-96", 2), // Sacred Ash
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-99", 4), // Ultra Ball
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-127", 4), // Shauna
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-133", 18) // Fire Energy
                            )
                    );
                    deckService.create(charizardDeck);
                }

                if (!hasFireGrass) {
                    ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO fireGrassDeck = new ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO(
                            user.getId(),
                            "Mazo Fuego & Planta Flashfire",
                            ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                            List.of(
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-5", 4), // Seedot
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-6", 4), // Nuzleaf
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-7", 3), // Shiftry
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-22", 3), // Feebas
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-23", 3), // Milotic
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-18", 3), // Litleo
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-20", 2), // Pyroar
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-91", 3), // Magnetic Storm
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-94", 3), // Pokémon Fan Club
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-90", 2), // Lysandre
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-99", 4), // Ultra Ball
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy2-96", 2), // Sacred Ash
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-125", 4), // Roller Skates
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-132", 10), // Grass Energy
                                    new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-133", 10) // Fire Energy
                            )
                    );
                    deckService.create(fireGrassDeck);
                }
                System.out.println(">>> SEEDED DECKS FOR USER: " + user.getUsername() + " <<<");
            } catch (Exception e) {
                System.err.println("Failed to seed decks for user " + user.getUsername() + ": " + e.getMessage());
            }
        }
    }
}
