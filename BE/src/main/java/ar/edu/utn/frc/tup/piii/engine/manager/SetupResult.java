package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.model.Card;
import java.util.List;

/**
 * Immutable result of {@link SetupManager#execute}.
 *
 * @param firstPlayerIndex  0 or 1 — the player who moves first (determined by coin flip)
 * @param mulligansP0       list of hands revealed as mulligans by player 0
 * @param mulligansP1       list of hands revealed as mulligans by player 1
 */
public record SetupResult(int firstPlayerIndex, List<List<Card>> mulligansP0, List<List<Card>> mulligansP1) {
}
