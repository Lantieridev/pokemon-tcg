package ar.edu.utn.frc.tup.piii.engine.manager;

/**
 * Immutable result of {@link SetupManager#execute}.
 *
 * @param firstPlayerIndex  0 or 1 — the player who moves first (determined by coin flip)
 * @param mulliganCountP0   number of Mulligans declared by player 0
 * @param mulliganCountP1   number of Mulligans declared by player 1
 */
public record SetupResult(int firstPlayerIndex, int mulliganCountP0, int mulliganCountP1) {
}
