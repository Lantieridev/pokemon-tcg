package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.ActionType;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BotDecisionService {

    private final MatchSessionRegistry registry;
    private final MatchService matchService;

    public BotDecisionService(MatchSessionRegistry registry, @Lazy MatchService matchService) {
        this.registry = registry;
        this.matchService = matchService;
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

        int botIndex = session.indexOf("Bot-001");
        if (session.getTurnManager().activePlayerIndex() != botIndex) {
            return; // Not bot's turn
        }

        PlayerRuntime botRuntime = session.getPlayerRuntime(botIndex);

        // 1. Try to place basic pokemon if bench is not full
        if (botRuntime.getBench().size() < 5) {
            Optional<Card> basicOpt = botRuntime.getHand().getCards().stream()
                    .filter(c -> c instanceof PokemonCard && c.isBasicPokemon())
                    .findFirst();
            if (basicOpt.isPresent()) {
                sendAction(matchId, new ActionRequestDTO(ActionType.PLACE_BASIC_POKEMON, basicOpt.get().getCardId(), null, null, null, null));
                return;
            }
        }

        // 2. Try to attach energy to active (we just attempt it, if invalid because already attached, it'll fail, but we catch it)
        Optional<Card> energyOpt = botRuntime.getHand().getCards().stream()
                .filter(c -> c instanceof EnergyCard)
                .findFirst();
        if (energyOpt.isPresent()) {
            // Find what type of energy it is. We can just use the cardId and null for energyType (the constructor handles it)
            sendAction(matchId, new ActionRequestDTO(ActionType.ATTACH_ENERGY, energyOpt.get().getCardId(), null, null, null, null));
            return;
        }

        // 3. Try to attack (always attack index 0 for basic bot)
        if (botRuntime.getActivePokemon() != null && !botRuntime.getActivePokemon().getAttacks().isEmpty()) {
            sendAction(matchId, new ActionRequestDTO(ActionType.DECLARE_ATTACK, null, null, null, null, 0));
            return;
        }

        // 4. End Turn if nothing else
        sendAction(matchId, new ActionRequestDTO(ActionType.END_TURN, null, null, null, null, null));
    }

    private void sendAction(String matchId, ActionRequestDTO action) {
        try {
            matchService.processAction(matchId, "Bot-001", action);
        } catch (Exception e) {
            System.err.println("[Bot] Failed action " + action.type() + ": " + e.getMessage());
            // If action fails, force end turn to prevent infinite loop of failing actions
            if (action.type() != ActionType.END_TURN) {
                try {
                    matchService.processAction(matchId, "Bot-001", new ActionRequestDTO(ActionType.END_TURN, null, null, null, null, null));
                } catch (Exception ignored) {}
            }
        }
    }
}
