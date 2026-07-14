package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.CampaignNodeDTO;
import ar.edu.utn.frc.tup.piii.dtos.CampaignProgressResponseDTO;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.mapper.CardMapper;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.deck.DeckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final MatchCreationService matchCreationService;
    private final CardResolutionService cardResolutionService;
    private final DeckService deckService;

    private static final int DECK_SIZE = 60;
    private static final String ENERGY_GRASS = "xy1-132";
    private static final String ENERGY_LIGHTNING = "xy1-135";
    private static final String ENERGY_FIRE = "xy1-131";
    private static final String PROFESSORS_LETTER = "xy1-123";

    // Configuración estática de los gimnasios
    public static final List<CampaignNodeInfo> NODES = List.of(
        new CampaignNodeInfo(1, "Gimnasio Plateada - Brock", "Bot-Brock", ENERGY_GRASS, Map.of("xy1-12", 4, "xy1-13", 4, PROFESSORS_LETTER, 4, "xy1-125", 4, ENERGY_GRASS, 44), 50, 100), // Mazo Planta/Roca simple
        new CampaignNodeInfo(2, "Gimnasio Celeste - Misty", "Bot-Misty", ENERGY_GRASS, Map.of("xy1-31", 4, "xy1-33", 4, "xy1-35", 4, PROFESSORS_LETTER, 4, ENERGY_GRASS, 44), 100, 150), // Mazo Agua
        new CampaignNodeInfo(3, "Gimnasio Carmín - Lt. Surge", "Bot-LtSurge", ENERGY_LIGHTNING, Map.of("xy1-42", 4, "xy1-43", 4, PROFESSORS_LETTER, 4, "xy1-125", 4, ENERGY_LIGHTNING, 44), 150, 200), // Mazo Rayo
        new CampaignNodeInfo(4, "Gimnasio Azuliza - Erika", "Bot-Erika", ENERGY_GRASS, Map.of("xy1-12", 4, "xy1-14", 2, PROFESSORS_LETTER, 4, "xy1-128", 4, ENERGY_GRASS, 46), 200, 250), // Mazo Planta
        new CampaignNodeInfo(5, "Gimnasio Fucsia - Koga", "Bot-Koga", ENERGY_FIRE, Map.of("xy1-22", 4, "xy1-20", 4, "xy1-121", 4, PROFESSORS_LETTER, 4, ENERGY_FIRE, 44), 250, 300), // Mazo Fuego/Toxina
        new CampaignNodeInfo(6, "Gimnasio Azafrán - Sabrina", "Bot-Sabrina", ENERGY_LIGHTNING, Map.of("xy1-42", 4, PROFESSORS_LETTER, 4, "xy1-127", 4, ENERGY_LIGHTNING, 48), 300, 350), // Mazo Rayo/Psíquico
        new CampaignNodeInfo(7, "Gimnasio Canela - Blaine", "Bot-Blaine", ENERGY_FIRE, Map.of("xy1-20", 4, "xy1-22", 2, PROFESSORS_LETTER, 4, "xy1-127", 4, ENERGY_FIRE, 46), 350, 400), // Mazo Fuego
        new CampaignNodeInfo(8, "Gimnasio Verde - Giovanni", "Bot-Giovanni", ENERGY_FIRE, Map.of("xy1-20", 4, "xy1-31", 2, "xy1-42", 4, PROFESSORS_LETTER, 4, ENERGY_FIRE, 46), 500, 500) // Mazo Multitipo
    );

    public record CampaignNodeInfo(
        int id,
        String name,
        String botId,
        String fallbackEnergyId,
        Map<String, Integer> cardQuantities,
        int rewardCoins,
        int rewardXp
    ) {}

    @Transactional(readOnly = true)
    public CampaignProgressResponseDTO getCampaignProgress(String username) {
        UserEntity user = userRepository.findFirstByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + username));

        Set<Integer> clearedNodes = user.getClearedStoryNodes();
        List<CampaignNodeDTO> nodesList = new ArrayList<>();

        for (CampaignNodeInfo node : NODES) {
            String status = "LOCKED";
            if (clearedNodes.contains(node.id())) {
                status = "CLEARED";
            } else if (node.id() == 1 || clearedNodes.contains(node.id() - 1)) {
                status = "UNLOCKED";
            }

            nodesList.add(CampaignNodeDTO.builder()
                .id(node.id())
                .name(node.name())
                .botName(node.botId())
                .status(status)
                .rewardCoins(node.rewardCoins())
                .rewardXp(node.rewardXp())
                .build());
        }

        return CampaignProgressResponseDTO.builder()
            .clearedNodesCount(clearedNodes.size())
            .totalNodesCount(NODES.size())
            .nodes(nodesList)
            .build();
    }

    @Transactional
    public String iniciarDesafioPvE(String username, int nodeId, Long deckId) {
        UserEntity user = userRepository.findFirstByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + username));

        CampaignNodeInfo nodeInfo = NODES.stream()
            .filter(n -> n.id() == nodeId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Nodo de campaña inválido: " + nodeId));

        // Validar progreso
        Set<Integer> clearedNodes = user.getClearedStoryNodes();
        boolean isUnlocked = (nodeId == 1 || clearedNodes.contains(nodeId - 1));
        if (!isUnlocked) {
            throw new IllegalArgumentException("Este nodo de la campaña se encuentra bloqueado.");
        }

        // Verificar que el mazo pertenezca al jugador antes de usarlo (lanza si no coincide el dueño)
        deckService.getById(deckId, username);

        // Resolver mazo del jugador
        List<Card> deckA = cardResolutionService.resolveCards(deckId);
        if (deckA.isEmpty()) {
            throw new IllegalArgumentException("El mazo seleccionado está vacío o no es válido.");
        }

        // Generar mazo del bot del líder de gimnasio
        List<Card> deckB = buildBotDeck(nodeInfo);

        // Crear la partida PvE asíncrona
        return matchCreationService.createMatch(username, nodeInfo.botId(), deckA, deckB, false);
    }

    @Transactional
    public void completeNode(String username, int nodeId, String matchId) {
        final CampaignNodeInfo nodeInfo = NODES.stream()
            .filter(n -> n.id() == nodeId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Nodo de campaña no encontrado: " + nodeId));

        userRepository.findFirstByUsername(username).ifPresent(user -> {
            Set<Integer> clearedNodes = user.getClearedStoryNodes();
            if (!clearedNodes.contains(nodeId)) {
                clearedNodes.add(nodeId);
                user.setPokecoins(user.getPokecoins() + nodeInfo.rewardCoins());
                user.setXp(user.getXp() + nodeInfo.rewardXp());
                userRepository.save(user);
                log.info("[CAMPAÑA] AUDITORÍA - Recompensas acreditadas. Usuario: {}, Gimnasio: {} (ID: {}), Partida: {}, Pokécoins: +{}, XP: +{}", 
                    username, nodeInfo.name(), nodeId, matchId, nodeInfo.rewardCoins(), nodeInfo.rewardXp());
            } else {
                log.info("[CAMPAÑA] AUDITORÍA - Recompensa ignorada (ya completado anteriormente). Usuario: {}, Gimnasio: {} (ID: {}), Partida: {}", 
                    username, nodeInfo.name(), nodeId, matchId);
            }
        });
    }

    private List<Card> buildBotDeck(CampaignNodeInfo node) {
        List<Card> deck = new ArrayList<>();
        
        // Cargar cartas temáticas configuradas
        for (Map.Entry<String, Integer> entry : node.cardQuantities().entrySet()) {
            addCardsToDeck(deck, entry.getKey(), entry.getValue());
        }

        // Rellenar defensivamente con energía básica si faltan cartas para completar 60
        if (deck.size() < DECK_SIZE) {
            int missing = DECK_SIZE - deck.size();
            addCardsToDeck(deck, node.fallbackEnergyId(), missing);
        }

        // Si por alguna razón sigue sin completar (ej: fallan las cartas en BD), rellenamos con lo que sea que haya
        if (deck.size() < DECK_SIZE) {
            int missing = DECK_SIZE - deck.size();
            addCardsToDeck(deck, ENERGY_FIRE, missing); // Relleno genérico de fuego
        }

        return deck;
    }

    private void addCardsToDeck(List<Card> deck, String cardId, int quantity) {
        Optional<CardEntity> entity = cardRepository.findById(cardId);
        if (entity.isPresent()) {
            for (int i = 0; i < quantity; i++) {
                deck.add(cardMapper.map(entity.get()));
            }
        }
    }
}
