package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.pipeline.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.pipeline.AttackPipeline;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a pending card-selection request (Evosoda, Ultra Ball, Rock Rush, etc.) against
 * live match state, one handler per {@link TrainerEffectId}.
 *
 * <p>Extracted out of {@link GameFacade} because each trainer card's selection-resolution
 * logic is an independent unit of behavior with no coupling to the others — this mirrors the
 * Strategy pattern already used for reactive/passive abilities in
 * {@code engine.pipeline.abilities}.</p>
 */
public final class SelectionEffectResolver {

    /**
     * Everything a {@link SelectionHandler} may need to resolve one pending selection.
     */
    private record SelectionContext(
            ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction action, MatchSession session, PlayerRuntime runtime,
            ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest request, List<String> selectedIds) {
    }

    /**
     * Resolves one {@link TrainerEffectId}'s pending card-selection outcome against the live
     * match state. Returns {@code true} once the selection is fully resolved and the main phase
     * can resume, or {@code false} if the handler transitioned to another pending selection
     * request instead (e.g. Ultra Ball's hand-discard step).
     */
    @FunctionalInterface
    private interface SelectionHandler {
        boolean handle(SelectionContext ctx);
    }

    private final AttackPipeline attackPipeline;
    private final Map<TrainerEffectId, SelectionHandler> handlers;

    public SelectionEffectResolver(final AttackPipeline attackPipeline) {
        this.attackPipeline = attackPipeline;
        this.handlers = buildHandlers();
    }

    private Map<TrainerEffectId, SelectionHandler> buildHandlers() {
        final Map<TrainerEffectId, SelectionHandler> map = new EnumMap<>(TrainerEffectId.class);
        map.put(TrainerEffectId.EVOSODA, this::selectEvosoda);
        map.put(TrainerEffectId.BOUNCE, this::selectBounce);
        map.put(TrainerEffectId.GREAT_BALL, this::selectGreatBall);
        map.put(TrainerEffectId.PROFESSORS_LETTER, this::selectProfessorsLetter);
        map.put(TrainerEffectId.MAX_REVIVE, this::selectMaxRevive);
        map.put(TrainerEffectId.SACRED_ASH, this::selectSacredAsh);
        map.put(TrainerEffectId.POKEMON_FAN_CLUB, this::selectPokemonFanClub);
        map.put(TrainerEffectId.QUIVER_DANCE, this::selectQuiverDance);
        map.put(TrainerEffectId.FIERY_TORCH, this::selectFieryTorch);
        map.put(TrainerEffectId.TRICK_SHOVEL, this::selectTrickShovel);
        map.put(TrainerEffectId.PAL_PAD, this::selectPalPad);
        map.put(TrainerEffectId.BLACKSMITH, this::selectBlacksmith);
        map.put(TrainerEffectId.ULTRA_BALL, this::selectUltraBall);
        map.put(TrainerEffectId.CLAIRVOYANT_EYE, this::selectClairvoyantEye);
        map.put(TrainerEffectId.FLASH_CLAW, this::selectFlashClaw);
        map.put(TrainerEffectId.ROCK_RUSH, this::selectRockRush);
        map.put(TrainerEffectId.BRILLIANT_SEARCH, this::selectBrilliantSearch);
        map.put(TrainerEffectId.BURIED_TREASURE_HUNT, this::selectBuriedTreasureHunt);
        map.put(TrainerEffectId.DUAL_BULLET, this::selectDualBullet);
        map.put(TrainerEffectId.PAIN_PELLETS, this::selectPainPellets);
        map.put(TrainerEffectId.BENCH_DAMAGE_ONE, this::selectBenchDamageOne);
        map.put(TrainerEffectId.CURSED_DROP, this::selectCursedDrop);
        map.put(TrainerEffectId.EAR_INFLUENCE, this::selectEarInfluence);
        map.put(TrainerEffectId.FANG_SNIPE, this::selectFangSnipe);
        map.put(TrainerEffectId.RESCUE, this::selectRescue);
        map.put(TrainerEffectId.REVIVAL, this::selectRevival);
        map.put(TrainerEffectId.PUSH_DOWN, this::selectPushDown);
        map.put(TrainerEffectId.PARABOLIC_CHARGE, this::selectParabolicCharge);
        return map;
    }

    /**
     * Resolves the pending selection tied to {@code request.sourceEffect()}.
     *
     * @return {@code true} if the caller should clear the pending request and resume the main
     *         phase, {@code false} if this call transitioned to a new pending selection instead
     */
    public boolean resolve(final ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction action, final MatchSession session,
            final PlayerRuntime runtime, final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest request,
            final List<String> selectedIds) {
        final SelectionHandler handler = handlers.get(request.sourceEffect());
        return handler == null || handler.handle(new SelectionContext(action, session, runtime, request, selectedIds));
    }

    // --- selection handlers (one per TrainerEffectId with a pending-selection resolution) ---


    private boolean selectEvosoda(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime runtime = ctx.runtime();
        final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest request = ctx.request();
        if (!selectedIds.isEmpty()) {
            final BattlePokemonState target = resolveLivePokemon(session, request.target());
            final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(selectedIds.get(0)), 1);
            if (!found.isEmpty()) {
                Card selectedCard = found.get(0);
                if (selectedCard instanceof PokemonCard pc && pc.getEvolvesFrom() != null && pc.getEvolvesFrom().equals(target.getName())) {
                    target.evolveInto(pc);
                    if (pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.MEGA) {
                        session.setMegaEvolvedThisTurn(true);
                    }
                } else {
                    throw new IllegalArgumentException("Selected card is not a valid evolution for the target");
                }
            }
        }
        runtime.getDeck().shuffle();
        return true;
    }

    private boolean selectBounce(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            final String selectedCardId = selectedIds.get(0);
            int benchIndex = -1;
            if (selectedCardId.startsWith("bench_")) {
                benchIndex = Integer.parseInt(selectedCardId.split(":")[0].substring(6));
            } else {
                for (int i = 0; i < runtime.getBench().getAll().size(); i++) {
                    if (runtime.getBench().getAll().get(i).getCardId().equals(selectedCardId)) {
                        benchIndex = i;
                        break;
                    }
                }
            }
            if (benchIndex != -1 && benchIndex < runtime.getBench().getAll().size()) {
                final BattlePokemonState newActive = runtime.getBench().promote(benchIndex);
                final BattlePokemonState oldActive = runtime.getActivePokemon();
                runtime.setActivePokemon(newActive);
                runtime.getBench().place(oldActive);
                runtime.getStatusEffectManager().clearAll();
                runtime.recordPokemonEntered(oldActive);
            }
        }
        return true;
    }

    private boolean selectGreatBall(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            final List<Card> top7 = runtime.getDeck().drawMultiple(Math.min(7, runtime.getDeck().size()));
            Card selected = null;
            for (Card c : top7) {
                if (c.getCardId().equals(selectedIds.get(0))) {
                    selected = c;
                }
            }
            if (selected != null) {
                if (!(selected instanceof PokemonCard)) {
                    throw new IllegalArgumentException("Great Ball can only select a Pokemon card");
                }
                top7.remove(selected);
                runtime.getHand().addCard(selected);
            }
            runtime.getDeck().addCards(top7);
            runtime.getDeck().shuffle();
        } else {
            runtime.getDeck().shuffle();
        }
        return true;
    }

    private boolean selectProfessorsLetter(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            for (String id : selectedIds) {
                final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(id), 1);
                if (!found.isEmpty()) {
                    if (!(found.get(0) instanceof EnergyCard ec) || !ec.isBasic()) {
                        throw new IllegalArgumentException("Professor's Letter can only select basic energy cards");
                    }
                    runtime.getHand().addCard(found.get(0));
                }
            }
        }
        runtime.getDeck().shuffle();
        return true;
    }

    private boolean selectMaxRevive(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            Card found = null;
            for (Card c : runtime.getDiscardPile().getCards()) {
                if (c.getCardId().equals(selectedIds.get(0))) {
                    found = c;
                    break;
                }
            }
            if (found != null) {
                if (!(found instanceof PokemonCard pc) || pc.getEvolutionStage() != ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC) {
                    throw new IllegalArgumentException("Max Revive can only select a Basic Pokemon card");
                }
                runtime.getDiscardPile().remove(found);
                runtime.getDeck().addToTop(found);
            }
        }
        return true;
    }

    private boolean selectSacredAsh(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            final List<Card> toReturn = new ArrayList<>();
            for (String id : selectedIds) {
                final List<Card> found = runtime.getDiscardPile().getCards().stream()
                        .filter(c -> c.getCardId().equals(id))
                        .toList();
                if (!found.isEmpty()) {
                    final Card card = found.get(0);
                    if (!(card instanceof PokemonCard)) {
                        throw new IllegalArgumentException("Sacred Ash can only select Pokemon cards");
                    }
                    runtime.getDiscardPile().remove(card);
                    toReturn.add(card);
                }
            }
            if (!toReturn.isEmpty()) {
                runtime.getDeck().addCards(toReturn);
                runtime.getDeck().shuffle();
            }
        }
        return true;
    }

    private boolean selectPokemonFanClub(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            for (String id : selectedIds) {
                final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(id), 1);
                if (!found.isEmpty()) {
                    final Card card = found.get(0);
                    if (!(card instanceof PokemonCard pc && pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC)) {
                        throw new IllegalArgumentException("Pokemon Fan Club can only select Basic Pokemon cards");
                    }
                    runtime.getHand().addCard(card);
                }
            }
        }
        runtime.getDeck().shuffle();
        return true;
    }

    private boolean selectQuiverDance(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        boolean attached = false;
        if (!selectedIds.isEmpty()) {
            final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(selectedIds.get(0)), 1);
            if (!found.isEmpty()) {
                final Card card = found.get(0);
                if (!(card instanceof EnergyCard ec && ec.isBasic())) {
                    throw new IllegalArgumentException("Quiver Dance can only select a Basic Energy card");
                }
                if (runtime.getActivePokemon() != null) {
                    runtime.getActivePokemon().attachEnergy((EnergyCard) card);
                    attached = true;
                }
            }
        }
        runtime.getDeck().shuffle();
        if (attached && runtime.getActivePokemon() != null) {
            runtime.getActivePokemon().heal(40);
        }
        return true;
    }

    private boolean selectFieryTorch(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            final String cardId = selectedIds.get(0);
            final Card card = runtime.getHand().removeCard(cardId);
            if (card == null || !(card instanceof EnergyCard ec && ec.getEnergyType() == PokemonType.FIRE)) {
                throw new IllegalArgumentException("Fiery Torch requires discarding a Fire Energy card from hand");
            }
            runtime.getDiscardPile().add(card);
            final int drawCount = Math.min(2, runtime.getDeck().size());
            if (drawCount > 0) {
                runtime.getHand().addCards(runtime.getDeck().drawMultiple(drawCount));
            }
        }
        return true;
    }

    private boolean selectTrickShovel(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime runtime = ctx.runtime();
        final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest request = ctx.request();
        final BattlePokemonState target = resolveLivePokemon(session, request.target());
        final int targetPlayerIndex = (target != null && target.equals(runtime.getActivePokemon()))
                ? session.getActivePlayerIndex() : (1 - session.getActivePlayerIndex());
        final PlayerRuntime targetRuntime = session.getPlayerRuntime(targetPlayerIndex);
        if (!selectedIds.isEmpty()) {
            final List<Card> topCardList = targetRuntime.getDeck().drawMultiple(1);
            if (!topCardList.isEmpty()) {
                targetRuntime.getDiscardPile().add(topCardList.get(0));
            }
        }
        return true;
    }

    private boolean selectPalPad(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            final List<Card> toReturn = new ArrayList<>();
            for (String id : selectedIds) {
                final List<Card> found = runtime.getDiscardPile().getCards().stream()
                        .filter(c -> c.getCardId().equals(id))
                        .toList();
                if (!found.isEmpty()) {
                    final Card card = found.get(0);
                    if (!(card instanceof TrainerCard tc && tc.getTrainerType() == TrainerType.SUPPORTER)) {
                        throw new IllegalArgumentException("Pal Pad can only select Supporter cards");
                    }
                    runtime.getDiscardPile().remove(card);
                    toReturn.add(card);
                }
            }
            if (!toReturn.isEmpty()) {
                runtime.getDeck().addCards(toReturn);
                runtime.getDeck().shuffle();
            }
        }
        return true;
    }

    private boolean selectBlacksmith(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime runtime = ctx.runtime();
        final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest request = ctx.request();
        if (!selectedIds.isEmpty()) {
            final BattlePokemonState target = resolveLivePokemon(session, request.target());
            if (target == null) {
                throw new IllegalStateException("Blacksmith requires a target Pokemon");
            }
            for (String id : selectedIds) {
                final List<Card> found = runtime.getDiscardPile().getCards().stream()
                        .filter(c -> c.getCardId().equals(id))
                        .toList();
                if (!found.isEmpty()) {
                    final Card card = found.get(0);
                    if (!(card instanceof EnergyCard ec && ec.getEnergyType() == PokemonType.FIRE)) {
                        throw new IllegalArgumentException("Blacksmith can only select Fire Energy cards");
                    }
                    runtime.getDiscardPile().remove(card);
                    target.attachEnergy((EnergyCard) card);
                }
            }
        }
        return true;
    }

    private boolean selectUltraBall(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime runtime = ctx.runtime();
        final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest request = ctx.request();
        if (request.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND) {
            if (selectedIds.size() != 2) {
                throw new IllegalArgumentException("Ultra Ball requires discarding exactly 2 cards");
            }
            for (String id : selectedIds) {
                final Card discarded = runtime.getHand().removeCard(id);
                if (discarded != null) {
                    runtime.getDiscardPile().add(discarded);
                }
            }
            // Transition to deck search
            session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(TrainerEffectId.ULTRA_BALL, null, 1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK));
            return false; // Do NOT resume the main phase yet
        } else if (request.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK) {
            if (!selectedIds.isEmpty()) {
                final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(selectedIds.get(0)), 1);
                if (!found.isEmpty()) {
                    final Card card = found.get(0);
                    if (!(card instanceof PokemonCard)) {
                        throw new IllegalArgumentException("Ultra Ball can only select a Pokemon card");
                    }
                    runtime.getHand().addCard(card);
                }
            }
            runtime.getDeck().shuffle();
        }
        return true;
    }

    private boolean selectClairvoyantEye(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            final List<Card> topCards = runtime.getDeck().drawMultiple(selectedIds.size());
            final List<Card> reordered = new ArrayList<>();
            for (String id : selectedIds) {
                Card foundCard = null;
                for (Card c : topCards) {
                    if (c.getCardId().equals(id)) {
                        foundCard = c;
                        break;
                    }
                }
                if (foundCard != null) {
                    topCards.remove(foundCard);
                    reordered.add(foundCard);
                }
            }
            reordered.addAll(topCards);
            for (int i = reordered.size() - 1; i >= 0; i--) {
                runtime.getDeck().addToTop(reordered.get(i));
            }
        }
        return true;
    }

    private boolean selectFlashClaw(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime opponentRuntime = session.getPlayerRuntime(1 - session.getTurnManager().activePlayerIndex());
        if (!selectedIds.isEmpty()) {
            final String cardId = selectedIds.get(0);
            final Card discarded = opponentRuntime.getHand().removeCard(cardId);
            if (discarded != null) {
                opponentRuntime.getDiscardPile().add(discarded);
            }
        }
        return true;
    }

    private boolean selectRockRush(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            for (String id : selectedIds) {
                final Card discarded = runtime.getHand().removeCard(id);
                if (discarded != null) {
                    runtime.getDiscardPile().add(discarded);
                }
            }
        }
        session.setPendingSelectionRequest(null);

        final int defenderIndex = 1 - session.getActivePlayerIndex();
        final PlayerRuntime defender = session.getPlayerRuntime(defenderIndex);

        final Attack attack = runtime.getActivePokemon().getAttacks().stream()
                .filter(a -> "Rock Rush".equalsIgnoreCase(a.name()))
                .findFirst()
                .orElseGet(() -> runtime.getActivePokemon().getAttacks().get(0));

        final AttackContext attackCtx = new AttackContext.Builder(
                runtime.getActivePokemon(),
                defender.getActivePokemon(),
                attack,
                runtime.getStatusEffectManager(),
                defender.getStatusEffectManager(),
                session.getKnockoutHandler(),
                session.getCoinFlipper()::flip
        )
        .attackerRuntime(runtime)
        .defenderRuntime(defender)
        .defenderBench(defender.getBench().getAll())
        .effectText(attack.effectText())
        .stadiumProvider(session.getBoard())
        .attackerStats(runtime.getStatisticsTracker())
        .defenderStats(defender.getStatisticsTracker())
        .matchSession(session)
        .build();

        attackCtx.setRockRushResolved(true);
        attackCtx.setRockRushDiscardCount(selectedIds.size());
        attackPipeline.execute(attackCtx);
        return true;
    }

    private boolean selectBrilliantSearch(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            for (String id : selectedIds) {
                final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(id), 1);
                if (!found.isEmpty()) {
                    runtime.getHand().addCard(found.get(0));
                }
            }
        }
        runtime.getDeck().shuffle();
        return true;
    }

    private boolean selectBuriedTreasureHunt(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        final List<Card> topCards = runtime.getDeck().drawMultiple(Math.min(4, runtime.getDeck().size()));
        for (Card card : topCards) {
            if (selectedIds.contains(card.getCardId())) {
                runtime.getHand().addCard(card);
            } else {
                runtime.getDiscardPile().add(card);
            }
        }
        return true;
    }

    private boolean selectDualBullet(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime runtime = ctx.runtime();
        final PlayerRuntime opponent = session.getPlayerRuntime(1 - session.getActivePlayerIndex());
        final BattlePokemonState attacker = runtime.getActivePokemon();
        final List<BattlePokemonState> targets = new ArrayList<>();
        if (opponent.getActivePokemon() != null && selectedIds.contains(opponent.getActivePokemon().getCardId())) {
            targets.add(opponent.getActivePokemon());
        }
        for (BattlePokemonState benched : opponent.getBench().getAll()) {
            if (selectedIds.contains(benched.getCardId())) {
                targets.add(benched);
            }
        }
        for (BattlePokemonState target : targets) {
            if (target == opponent.getActivePokemon()) {
                final ar.edu.utn.frc.tup.piii.engine.model.DamageContext dmgCtx = new ar.edu.utn.frc.tup.piii.engine.model.DamageContext(
                        attacker,
                        target,
                        new Attack("Dual Bullet", 50, List.of(), ""),
                        List.of(),
                        List.of()
                );
                final ar.edu.utn.frc.tup.piii.engine.model.DamageResult dmgResult = new DamageCalculator().calculate(dmgCtx, false, false);
                target.addDamageCounters(dmgResult.damageCountersToPlace());
            } else {
                target.addDamageCounters(5);
            }
            if (target.getDamageCounters() * 10 >= target.getMaxHp()) {
                session.getKnockoutHandler().onKnockout(target, target.isEx() ? 2 : 1);
            }
        }
        return true;
    }

    private boolean selectPainPellets(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime runtime = ctx.runtime();
        final PlayerRuntime opponent = session.getPlayerRuntime(1 - session.getActivePlayerIndex());
        final BattlePokemonState attacker = runtime.getActivePokemon();
        final List<BattlePokemonState> targets = new ArrayList<>();
        if (opponent.getActivePokemon() != null && selectedIds.contains(opponent.getActivePokemon().getCardId())) {
            targets.add(opponent.getActivePokemon());
        }
        for (BattlePokemonState benched : opponent.getBench().getAll()) {
            if (selectedIds.contains(benched.getCardId())) {
                targets.add(benched);
            }
        }
        if (!targets.isEmpty() && attacker != null) {
            final BattlePokemonState target = targets.get(0);
            final int countersToPlace = attacker.getDamageCounters();
            target.addDamageCounters(countersToPlace);
            if (target.getDamageCounters() * 10 >= target.getMaxHp()) {
                session.getKnockoutHandler().onKnockout(target, target.isEx() ? 2 : 1);
            }
        }
        return true;
    }

    private boolean selectBenchDamageOne(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime runtime = ctx.runtime();
        final PlayerRuntime opponent = session.getPlayerRuntime(1 - session.getActivePlayerIndex());
        int damageAmount = 20; // Default fallback
        if (runtime.getActivePokemon() != null) {
            for (ar.edu.utn.frc.tup.piii.engine.model.Attack attack : runtime.getActivePokemon().getAttacks()) {
                String eff = attack.effectText();
                if (eff != null && eff.startsWith("bench_damage_one:")) {
                    try {
                        damageAmount = Integer.parseInt(eff.substring("bench_damage_one:".length()));
                    } catch (NumberFormatException ignored) {
                        // keep default fallback
                    }
                }
            }
        }
        final int counters = damageAmount / 10;
        for (BattlePokemonState benched : opponent.getBench().getAll()) {
            if (selectedIds.contains(benched.getCardId())) {
                benched.addDamageCounters(counters);
                if (benched.getDamageCounters() * 10 >= benched.getMaxHp()) {
                    session.getKnockoutHandler().onKnockout(benched, benched.isEx() ? 2 : 1);
                }
            }
        }
        return true;
    }

    private boolean selectCursedDrop(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime opponent = session.getPlayerRuntime(1 - session.getActivePlayerIndex());
        for (String id : selectedIds) {
            final BattlePokemonState target = resolveSlotTarget(opponent, id);
            if (target != null) {
                target.addDamageCounters(1);
                if (target.getDamageCounters() * 10 >= target.getMaxHp()) {
                    session.getKnockoutHandler().onKnockout(target, target.isEx() ? 2 : 1);
                }
            }
        }
        return true;
    }

    private boolean selectEarInfluence(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime opponent = session.getPlayerRuntime(1 - session.getActivePlayerIndex());
        for (int i = 0; i < selectedIds.size(); i += 2) {
            if (i + 1 >= selectedIds.size()) {
                break;
            }
            String srcId = selectedIds.get(i);
            String destId = selectedIds.get(i + 1);

            BattlePokemonState source = resolveSlotTarget(opponent, srcId);
            BattlePokemonState dest = resolveSlotTarget(opponent, destId);

            if (source != null && dest != null && source != dest && source.getDamageCounters() > 0) {
                source.addDamageCounters(-1);
                dest.addDamageCounters(1);
                if (dest.getDamageCounters() * 10 >= dest.getMaxHp()) {
                    session.getKnockoutHandler().onKnockout(dest, dest.isEx() ? 2 : 1);
                }
            }
        }
        return true;
    }

    private boolean selectFangSnipe(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime opponent = session.getPlayerRuntime(1 - session.getActivePlayerIndex());
        if (!selectedIds.isEmpty()) {
            Card found = null;
            for (Card c : opponent.getHand().getCards()) {
                if (c.getCardId().equals(selectedIds.get(0))) {
                    found = c;
                    break;
                }
            }
            if (found instanceof TrainerCard) {
                opponent.getHand().removeCard(found.getCardId());
                opponent.getDiscardPile().add(found);
            } else if (found != null) {
                throw new IllegalArgumentException("Can only discard a Trainer card");
            }
        }
        return true;
    }

    private boolean selectRescue(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            for (String id : selectedIds) {
                Card found = null;
                for (Card c : runtime.getDiscardPile().getCards()) {
                    if (c.getCardId().equals(id)) {
                        found = c;
                        break;
                    }
                }
                if (found instanceof PokemonCard pc) {
                    runtime.getDiscardPile().remove(pc);
                    runtime.getDeck().addCards(java.util.List.of(pc));
                } else if (found != null) {
                    throw new IllegalArgumentException("Rescue can only select Pokemon cards from discard pile");
                }
            }
            runtime.getDeck().shuffle();
        }
        return true;
    }

    private boolean selectRevival(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime opponent = session.getPlayerRuntime(1 - session.getActivePlayerIndex());
        if (!selectedIds.isEmpty() && opponent.getBench().getAll().size() < 5) {
            Card found = null;
            for (Card c : opponent.getDiscardPile().getCards()) {
                if (c.getCardId().equals(selectedIds.get(0))) {
                    found = c;
                    break;
                }
            }
            if (found instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc && pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC) {
                opponent.getDiscardPile().remove(pc);
                ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon state = new ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon(pc);
                state.setOwner(opponent);
                opponent.getBench().place(state);
            } else if (found != null) {
                throw new IllegalArgumentException("Can only place basic Pokemon cards from opponent's discard pile");
            }
        }
        return true;
    }

    private boolean selectPushDown(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final MatchSession session = ctx.session();
        final PlayerRuntime opponentRuntime = session.getPlayerRuntime(1 - session.getTurnManager().activePlayerIndex());
        if (!selectedIds.isEmpty()) {
            final String cardId = selectedIds.get(0);
            int index = -1;
            final java.util.List<BattlePokemonState> benched = opponentRuntime.getBench().getAll();
            for (int i = 0; i < benched.size(); i++) {
                if (benched.get(i).getCardId().equals(cardId)) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                final BattlePokemonState oldActive = opponentRuntime.getActivePokemon();
                final BattlePokemonState newActive = opponentRuntime.getBench().promote(index);
                opponentRuntime.setActivePokemon(newActive);
                opponentRuntime.getBench().place(oldActive);
                opponentRuntime.getStatusEffectManager().clearAll();
                opponentRuntime.recordPokemonEntered(oldActive);
            }
        }
        return true;
    }

    private boolean selectParabolicCharge(final SelectionContext ctx) {
        final List<String> selectedIds = ctx.selectedIds();
        final PlayerRuntime runtime = ctx.runtime();
        if (!selectedIds.isEmpty()) {
            for (String id : selectedIds) {
                final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(id), 1);
                if (!found.isEmpty()) {
                    if (!(found.get(0) instanceof EnergyCard ec)) {
                        throw new IllegalArgumentException("Parabolic Charge can only select energy cards");
                    }
                    runtime.getHand().addCard(ec);
                }
            }
        }
        runtime.getDeck().shuffle();
        return true;
    }


    // --- helpers ---

    private BattlePokemonState resolveLivePokemon(final MatchSession session, final BattlePokemonState target) {
        if (target == null) {
            return null;
        }
        for (int pIdx = 0; pIdx < 2; pIdx++) {
            final var player = session.getPlayerRuntime(pIdx);
            if (player == null) continue;
            final var active = player.getActivePokemon();
            if (active != null && active.equals(target)) {
                return active;
            }
            final var benched = player.getBench().getAll();
            for (var b : benched) {
                if (b != null && b.equals(target)) {
                    return b;
                }
            }
        }
        return target;
    }

    private BattlePokemonState resolveSlotTarget(final PlayerRuntime player, final String slotId) {
        if (slotId == null) {
            return null;
        }
        if (slotId.startsWith("active:")) {
            return player.getActivePokemon();
        } else if (slotId.startsWith("bench_")) {
            final int index = Integer.parseInt(slotId.split(":")[0].substring(6));
            if (index >= 0 && index < player.getBench().getAll().size()) {
                return player.getBench().getAll().get(index);
            }
        } else {
            if (player.getActivePokemon() != null && player.getActivePokemon().getCardId().equals(slotId)) {
                return player.getActivePokemon();
            }
            for (var benched : player.getBench().getAll()) {
                if (benched != null && benched.getCardId().equals(slotId)) {
                    return benched;
                }
            }
        }
        return null;
    }
}
