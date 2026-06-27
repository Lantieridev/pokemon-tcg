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
        Optional<UserEntity> userOpt = userRepository.findFirstByUsername("usuario");
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            
            // Check if decks already exist to avoid duplicates on multiple restarts
            List<DeckEntity> existing = deckRepository.findByUserId(user.getId());
            if (existing.size() >= 2) {
                return;
            }

            // Create Fire Deck
            ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO fireDeck = new ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO(
                    user.getId(),
                    "Mazo Fuego Dev",
                    ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                    List.of(
                            // Pokemons
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-4", 4), // Charmander
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-5", 3), // Charmeleon
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-6", 2), // Charizard
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-20", 3), // Slugma
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-11", 2), // Charizard EX
                            
                            // Trainers
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-123", 4), // Professor's Letter
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-125", 4), // Roller Skates
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-127", 4), // Shauna
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-128", 4), // Super Potion
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-121", 4), // Muscle Band
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-124", 4), // Red Card
                            
                            // Energies
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-131", 22)
                    )
            );
            
            // Create Water Deck
            ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO waterDeck = new ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO(
                    user.getId(),
                    "Mazo Agua Dev",
                    ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                    List.of(
                            // Pokemons
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-31", 4), // Shellder
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-32", 3), // Cloyster
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-33", 3), // Staryu
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-34", 2), // Starmie
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-29", 2), // Blastoise EX
                            
                            // Trainers
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-123", 4), // Professor's Letter
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-125", 4), // Roller Skates
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-127", 4), // Shauna
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-128", 4), // Super Potion
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-121", 4), // Muscle Band
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-124", 4), // Red Card
                            
                            // Energies
                            new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO("xy1-132", 22)
                    )
            );

            try {
                deckService.create(waterDeck);
                deckService.create(fireDeck);
                System.out.println(">>> SEEDED 2 DECKS FOR USER USUARIO <<<");
            } catch (Exception e) {
                System.err.println("Failed to seed decks: " + e.getMessage());
            }
        }
    }
}
