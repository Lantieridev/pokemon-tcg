package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.ActionType;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BotDecisionService {

    private final MatchSessionRegistry registry;
    private final MatchService matchService;
    private final GameFacade facade;

    public BotDecisionService(MatchSessionRegistry registry, @Lazy MatchService matchService, GameFacade facade) {
        this.registry = registry;
        this.matchService = matchService;
        this.facade = facade;
    }

    @Async
    public void evaluateAndPlay(String matchId) {
        try {
            // Artificial delay to make bot moves visible
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Optional<MatchSession> sessionOpt = registry.find(matchId);
        if (sessionOpt.isEmpty()) {
            return;
        }
        MatchSession session = sessionOpt.get();

        if (session.getWinnerId() != null) {
            return; // Game over
        }

        int botIndex = -1;
        for (int i = 0; i < session.getPlayerIds().size(); i++) {
            if (session.getPlayerIds().get(i).startsWith("Bot-")) {
                botIndex = i;
                break;
            }
        }

        if (botIndex == -1) {
            return; // No bot in this session
        }

        String botId = session.getPlayerIds().get(botIndex);

        // 1. Mandatory Promotion (XY1 Rulebook §2)
        if (session.isAwaitingPromotion()) {
            if (session.getPromotingPlayerIndex() == botIndex) {
                ActionRequestDTO promotion = tryPromoteActive(session, botIndex).orElse(null);
                if (promotion != null) {
                    sendAction(matchId, botId, promotion);
                }
            }
            return; // Wait until promotion is resolved
        }

        // 2. Pending Selection Request (e.g. from Trainers like Professor's Letter)
        if (session.getPendingSelectionRequest() != null) {
            if (session.getTurnManager().activePlayerIndex() == botIndex) {
                ActionRequestDTO selection = trySelectCards(session, botIndex).orElse(
                    new ActionRequestDTO(ActionType.SELECT_CARDS, null, null, null, null, null, null, java.util.Collections.emptyList(), null, java.util.Collections.emptyList())
                );
                sendAction(matchId, botId, selection);
            }
            return; // Wait until selection is resolved
        }

        if (session.getTurnManager().activePlayerIndex() != botIndex) {
            return; // Not bot's turn
        }

        // Action Priority Sequence
        Optional<ActionRequestDTO> nextAction = tryEvolve(session, botIndex)
                .or(() -> tryPlayTrainer(session, botIndex))
                .or(() -> tryPlaceBench(session, botIndex))
                .or(() -> tryAttachEnergy(session, botIndex))
                .or(() -> tryRetreat(session, botIndex))
                .or(() -> tryAttack(session, botIndex));

        if (nextAction.isPresent()) {
            sendAction(matchId, botId, nextAction.get());
        } else {
            sendAction(matchId, botId, new ActionRequestDTO(ActionType.END_TURN, null, null, null, null, null));
        }
    }

    private Optional<ActionRequestDTO> tryPromoteActive(MatchSession session, int botIndex) {
        PlayerRuntime botRuntime = session.getPlayerRuntime(botIndex);
        List<BattlePokemonState> bench = botRuntime.getBench().getAll();
        
        int bestIndex = 0;
        int maxHp = -1;
        
        for (int i = 0; i < bench.size(); i++) {
            int hp = bench.get(i).getMaxHp() - (bench.get(i).getDamageCounters() * 10);
            if (hp > maxHp) {
                maxHp = hp;
                bestIndex = i;
            }
        }
        
        if (!bench.isEmpty()) {
            return Optional.of(new ActionRequestDTO(ActionType.PROMOTE_ACTIVE, null, null, bestIndex, null, null));
        }
        return Optional.empty();
    }

    private Optional<ActionRequestDTO> trySelectCards(MatchSession session, int botIndex) {
        PlayerRuntime botRuntime = session.getPlayerRuntime(botIndex);
        PendingSelectionRequest request = session.getPendingSelectionRequest();
        
        if (request == null) return Optional.empty();

        List<String> selectedIds = new ArrayList<>();
        
        if (request.sourceEffect() == TrainerEffectId.PROFESSORS_LETTER) {
            selectedIds = botRuntime.getDeck().getCards().stream()
                .filter(c -> c instanceof EnergyCard ec && ec.isBasic())
                .limit(request.maxSelections())
                .map(Card::getCardId)
                .toList();
        } else if (request.sourceEffect() == TrainerEffectId.EVOSODA) {
            selectedIds = botRuntime.getDeck().getCards().stream()
                .filter(c -> c instanceof PokemonCard pc && pc.getEvolvesFrom() != null && pc.getEvolvesFrom().equals(request.target().getName()))
                .limit(1)
                .map(Card::getCardId)
                .toList();
        } else if (request.sourceEffect() == TrainerEffectId.GREAT_BALL) {
            selectedIds = botRuntime.getDeck().getCards().stream()
                .limit(7)
                .filter(c -> c instanceof PokemonCard)
                .limit(1)
                .map(Card::getCardId)
                .toList();
        } else if (request.sourceEffect() == TrainerEffectId.MAX_REVIVE) {
            selectedIds = botRuntime.getDiscardPile().getCards().stream()
                .filter(c -> c instanceof PokemonCard pc && pc.getEvolutionStage() == EvolutionStage.BASIC)
                .limit(1)
                .map(Card::getCardId)
                .toList();
        }

        return Optional.of(new ActionRequestDTO(ActionType.SELECT_CARDS, null, null, null, null, null, null, java.util.Collections.emptyList(), null, selectedIds));
    }

    private Optional<ActionRequestDTO> tryEvolve(MatchSession session, int botIndex) {
        PlayerRuntime botRuntime = session.getPlayerRuntime(botIndex);
        List<ActionRequestDTO> candidates = new ArrayList<>();
        
        for (Card card : botRuntime.getHand().getCards()) {
            if (card instanceof PokemonCard pc && (pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.STAGE_1 || pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.STAGE_2)) {
                // Try to evolve Active
                if (botRuntime.getActivePokemon() != null) {
                    candidates.add(new ActionRequestDTO(ActionType.EVOLVE, card.getCardId(), null, -1, null, null));
                }
                // Try to evolve Bench
                for (int i = 0; i < botRuntime.getBench().size(); i++) {
                    candidates.add(new ActionRequestDTO(ActionType.EVOLVE, card.getCardId(), null, i, null, null));
                }
            }
        }
        return findValidAction(session, botIndex, candidates);
    }

    private Optional<ActionRequestDTO> tryPlayTrainer(MatchSession session, int botIndex) {
        PlayerRuntime botRuntime = session.getPlayerRuntime(botIndex);
        List<ActionRequestDTO> candidates = new ArrayList<>();
        
        for (Card card : botRuntime.getHand().getCards()) {
            if (card instanceof TrainerCard tc) {
                // Try without target
                candidates.add(new ActionRequestDTO(ActionType.PLAY_TRAINER, card.getCardId(), null, null, tc.getTrainerType(), null));
                
                // Try targeting active
                if (botRuntime.getActivePokemon() != null) {
                    candidates.add(new ActionRequestDTO(ActionType.PLAY_TRAINER, card.getCardId(), null, -1, tc.getTrainerType(), null));
                }
                
                // Try targeting bench
                for (int i = 0; i < botRuntime.getBench().size(); i++) {
                    candidates.add(new ActionRequestDTO(ActionType.PLAY_TRAINER, card.getCardId(), null, i, tc.getTrainerType(), null));
                }
            }
        }
        return findValidAction(session, botIndex, candidates);
    }

    private Optional<ActionRequestDTO> tryPlaceBench(MatchSession session, int botIndex) {
        PlayerRuntime botRuntime = session.getPlayerRuntime(botIndex);
        if (botRuntime.getBench().size() < 5) {
            List<ActionRequestDTO> candidates = new ArrayList<>();
            for (Card card : botRuntime.getHand().getCards()) {
                if (card instanceof PokemonCard pc && pc.isBasicPokemon()) {
                    candidates.add(new ActionRequestDTO(ActionType.PLACE_BASIC_POKEMON, card.getCardId(), null, null, null, null));
                }
            }
            return findValidAction(session, botIndex, candidates);
        }
        return Optional.empty();
    }

    private Optional<ActionRequestDTO> tryAttachEnergy(MatchSession session, int botIndex) {
        PlayerRuntime botRuntime = session.getPlayerRuntime(botIndex);
        List<ActionRequestDTO> candidates = new ArrayList<>();
        
        for (Card card : botRuntime.getHand().getCards()) {
            if (card instanceof EnergyCard ec) {
                // Prioritize Active
                if (botRuntime.getActivePokemon() != null) {
                    candidates.add(new ActionRequestDTO(ActionType.ATTACH_ENERGY, card.getCardId(), null, -1, null, null, ec.getEnergyType(), java.util.Collections.emptyList(), null, java.util.Collections.emptyList()));
                }
                // Then Bench
                for (int i = 0; i < botRuntime.getBench().size(); i++) {
                    candidates.add(new ActionRequestDTO(ActionType.ATTACH_ENERGY, card.getCardId(), null, i, null, null, ec.getEnergyType(), java.util.Collections.emptyList(), null, java.util.Collections.emptyList()));
                }
            }
        }
        return findValidAction(session, botIndex, candidates);
    }

    private Optional<ActionRequestDTO> tryRetreat(MatchSession session, int botIndex) {
        PlayerRuntime botRuntime = session.getPlayerRuntime(botIndex);
        BattlePokemonState active = botRuntime.getActivePokemon();
        
        // Only retreat if Active is in danger (HP <= 30) and bench is not empty
        if (active != null && (active.getMaxHp() - (active.getDamageCounters() * 10)) <= 30 && !botRuntime.getBench().getAll().isEmpty()) {
            List<ActionRequestDTO> candidates = new ArrayList<>();
            for (int i = 0; i < botRuntime.getBench().size(); i++) {
                candidates.add(new ActionRequestDTO(ActionType.RETREAT, null, null, i, null, null));
            }
            return findValidAction(session, botIndex, candidates);
        }
        return Optional.empty();
    }

    private Optional<ActionRequestDTO> tryAttack(MatchSession session, int botIndex) {
        PlayerRuntime botRuntime = session.getPlayerRuntime(botIndex);
        BattlePokemonState active = botRuntime.getActivePokemon();
        
        if (active != null) {
            List<ActionRequestDTO> candidates = new ArrayList<>();
            List<Attack> attacks = active.getAttacks();
            
            // Prioritize highest damage attack (simulate by adding in reverse order of index, assuming later attacks are stronger)
            for (int i = attacks.size() - 1; i >= 0; i--) {
                candidates.add(new ActionRequestDTO(ActionType.DECLARE_ATTACK, null, null, null, null, i));
            }
            return findValidAction(session, botIndex, candidates);
        }
        return Optional.empty();
    }

    private Optional<ActionRequestDTO> findValidAction(MatchSession session, int botIndex, List<ActionRequestDTO> candidates) {
        for (ActionRequestDTO dto : candidates) {
            try {
                Action engineAction = facade.toEngineAction(session, botIndex, dto);
                ValidationResult result = session.getRuleValidator().validate(engineAction, botIndex);
                if (result instanceof ValidationResult.Valid) {
                    return Optional.of(dto);
                }
            } catch (Exception ignored) {
                // Exception from toEngineAction if missing targets, etc.
            }
        }
        return Optional.empty();
    }

    private void sendAction(String matchId, String botId, ActionRequestDTO action) {
        try {
            matchService.processAction(matchId, botId, action);
        } catch (Exception e) {
            System.err.println("[Bot] Failed action " + action.type() + ": " + e.getMessage());
            if (action.type() != ActionType.END_TURN) {
                try {
                    matchService.processAction(matchId, botId, new ActionRequestDTO(ActionType.END_TURN, null, null, null, null, null));
                } catch (Exception ignored) {}
            }
        }
    }
}
