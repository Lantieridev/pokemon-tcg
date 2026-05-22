package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.SetupStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SetupManagerTest {

    private static final int STANDARD_SIZE = 60;
    private static final int INITIAL_HAND = 7;
    private static final int PRIZE_COUNT  = 6;

    private static final CoinFlipper HEADS = () -> true;
    private static final CoinFlipper TAILS = () -> false;

    // -------------------------------------------------------------------------
    // Helpers — card factories
    // -------------------------------------------------------------------------

    private PokemonCard basic(final String id) {
        return new PokemonCard.Builder(id, "Basic-" + id, 60, PokemonType.FIRE)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
    }

    private PokemonCard nonBasic(final String id) {
        return new PokemonCard.Builder(id, "Stage1-" + id, 80, PokemonType.FIRE)
                .evolutionStage(EvolutionStage.STAGE_1)
                .evolvesFrom("Some")
                .build();
    }

    /**
     * Builds a 60-card deck where the first {@code leadingNonBasics} cards are non-basics
     * and the rest are basics. With a no-op shuffler this gives deterministic mulligan behaviour:
     * each batch of 7 non-basics forces exactly one additional mulligan iteration.
     */
    private Deck deckWithLeadingNonBasics(final int leadingNonBasics) {
        final List<Card> cards = new ArrayList<>();
        for (int i = 0; i < leadingNonBasics; i++) {
            cards.add(nonBasic("nb-" + i));
        }
        for (int i = 0; i < STANDARD_SIZE - leadingNonBasics; i++) {
            cards.add(basic("b-" + i));
        }
        return new Deck(cards);
    }

    /** Deck where all 60 cards are basics — never needs a mulligan. */
    private Deck allBasicsDeck() {
        return deckWithLeadingNonBasics(0);
    }

    // -------------------------------------------------------------------------
    // Helpers — strategy factories
    // -------------------------------------------------------------------------

    /** Always picks the first basic as active, benches nothing, accepts any bonus. */
    private SetupStrategy simpleStrategy() {
        return new SetupStrategy() {
            @Override
            public String chooseActiveCardId(final List<Card> hand) {
                return hand.stream()
                        .filter(Card::isBasicPokemon)
                        .findFirst()
                        .orElseThrow()
                        .getCardId();
            }

            @Override
            public List<String> chooseBenchCardIds(final List<Card> hand) {
                return List.of();
            }

            @Override
            public boolean acceptBonusDraws(final int opponentMulliganCount) {
                return true;
            }
        };
    }

    /** Same as simpleStrategy but always declines the bonus draw. */
    private SetupStrategy declinesBonusStrategy() {
        return new SetupStrategy() {
            @Override
            public String chooseActiveCardId(final List<Card> hand) {
                return hand.stream()
                        .filter(Card::isBasicPokemon)
                        .findFirst()
                        .orElseThrow()
                        .getCardId();
            }

            @Override
            public List<String> chooseBenchCardIds(final List<Card> hand) {
                return List.of();
            }

            @Override
            public boolean acceptBonusDraws(final int opponentMulliganCount) {
                return false;
            }
        };
    }

    /**
     * Strategy that benches the first {@code count} basics it finds (after active is removed).
     */
    private SetupStrategy benchStrategy(final int count) {
        return new SetupStrategy() {
            @Override
            public String chooseActiveCardId(final List<Card> hand) {
                return hand.stream().filter(Card::isBasicPokemon).findFirst().orElseThrow().getCardId();
            }

            @Override
            public List<String> chooseBenchCardIds(final List<Card> hand) {
                return hand.stream()
                        .filter(Card::isBasicPokemon)
                        .limit(count)
                        .map(Card::getCardId)
                        .toList();
            }

            @Override
            public boolean acceptBonusDraws(final int opponentMulliganCount) {
                return true;
            }
        };
    }

    // -------------------------------------------------------------------------
    // Helper — slot builder
    // -------------------------------------------------------------------------

    private PlayerSetupSlot slot(final Deck deck) {
        return new PlayerSetupSlot(deck, new Hand(), new Bench());
    }

    // -------------------------------------------------------------------------
    // No-mulligan scenarios
    // -------------------------------------------------------------------------

    @Test
    void noMulligan_bothMulliganCountsAreZero() {
        final SetupManager mgr = new SetupManager(HEADS, d -> { }, 6);

        final SetupResult result = mgr.execute(
                slot(allBasicsDeck()), simpleStrategy(),
                slot(allBasicsDeck()), simpleStrategy());

        assertEquals(0, result.mulliganCountP0());
        assertEquals(0, result.mulliganCountP1());
    }

    @Test
    void noMulligan_eachPlayerHasSixPrizeCards() {
        final PlayerSetupSlot s0 = slot(allBasicsDeck());
        final PlayerSetupSlot s1 = slot(allBasicsDeck());

        new SetupManager(HEADS, d -> { }, 6).execute(s0, simpleStrategy(), s1, simpleStrategy());

        assertEquals(PRIZE_COUNT, s0.getPrizes().size());
        assertEquals(PRIZE_COUNT, s1.getPrizes().size());
    }

    @Test
    void noMulligan_deckShrinksByHandPlusPrizes() {
        final PlayerSetupSlot s0 = slot(allBasicsDeck());
        final PlayerSetupSlot s1 = slot(allBasicsDeck());

        new SetupManager(HEADS, d -> { }, 6).execute(s0, simpleStrategy(), s1, simpleStrategy());

        // 60 - 7 (initial hand) - 6 (prizes) - 1 (active removed from hand, not deck)
        // Active is removed from hand so deck stays at 60-7-6 = 47
        assertEquals(STANDARD_SIZE - INITIAL_HAND - PRIZE_COUNT, s0.getDeck().size());
    }

    @Test
    void noMulligan_activeIsPlacedInSlot() {
        final PlayerSetupSlot s0 = slot(allBasicsDeck());

        new SetupManager(HEADS, d -> { }, 6).execute(s0, simpleStrategy(), slot(allBasicsDeck()), simpleStrategy());

        assertNotNull(s0.getActivePokemon());
    }

    @Test
    void noMulligan_activeIsRemovedFromHand() {
        final PlayerSetupSlot s0 = slot(allBasicsDeck());

        new SetupManager(HEADS, d -> { }, 6).execute(s0, simpleStrategy(), slot(allBasicsDeck()), simpleStrategy());

        // Started with 7, active removed → 6 remain (no bench, no bonus)
        assertEquals(INITIAL_HAND - 1, s0.getHand().size());
    }

    @Test
    void noMulligan_benchPokemonRemovedFromHand() {
        final PlayerSetupSlot s0 = slot(allBasicsDeck());

        new SetupManager(HEADS, d -> { }, 6).execute(s0, benchStrategy(2), slot(allBasicsDeck()), simpleStrategy());

        // 7 drawn - 1 active - 2 benched = 4 remaining in hand
        assertEquals(INITIAL_HAND - 1 - 2, s0.getHand().size());
        assertEquals(2, s0.getBench().size());
    }

    @Test
    void noMulligan_firstPlayerIsZeroWhenHeads() {
        final SetupResult result = new SetupManager(HEADS, d -> { }, 6)
                .execute(slot(allBasicsDeck()), simpleStrategy(), slot(allBasicsDeck()), simpleStrategy());

        assertEquals(0, result.firstPlayerIndex());
    }

    @Test
    void noMulligan_firstPlayerIsOneWhenTails() {
        final SetupResult result = new SetupManager(TAILS, d -> { }, 6)
                .execute(slot(allBasicsDeck()), simpleStrategy(), slot(allBasicsDeck()), simpleStrategy());

        assertEquals(1, result.firstPlayerIndex());
    }

    // -------------------------------------------------------------------------
    // Player 0 mulligans
    // -------------------------------------------------------------------------

    @Test
    void player0Mulligan_once_mulliganCountIsOne() {
        // P0 deck: first 7 non-basics → mulligan once, then draws basics
        final SetupResult result = new SetupManager(HEADS, d -> { }, 6)
                .execute(slot(deckWithLeadingNonBasics(7)), simpleStrategy(),
                         slot(allBasicsDeck()),              simpleStrategy());

        assertEquals(1, result.mulliganCountP0());
        assertEquals(0, result.mulliganCountP1());
    }

    @Test
    void player0Mulligan_once_player1DrawsOneBonusCard() {
        final PlayerSetupSlot s1 = slot(allBasicsDeck());

        new SetupManager(HEADS, d -> { }, 6)
                .execute(slot(deckWithLeadingNonBasics(7)), simpleStrategy(),
                         s1, simpleStrategy());

        // P1 drew 7 - 1 active + 1 bonus = 7 cards remain in hand
        assertEquals(INITIAL_HAND - 1 + 1, s1.getHand().size());
    }

    @Test
    void player0Mulligan_once_player1DeclinesBonus_handUnchanged() {
        final PlayerSetupSlot s1 = slot(allBasicsDeck());

        new SetupManager(HEADS, d -> { }, 6)
                .execute(slot(deckWithLeadingNonBasics(7)), simpleStrategy(),
                         s1, declinesBonusStrategy());

        // 7 drawn - 1 active = 6, no bonus
        assertEquals(INITIAL_HAND - 1, s1.getHand().size());
    }

    @Test
    void player0Mulligan_eventuallyGetsBasicInHand() {
        final PlayerSetupSlot s0 = slot(deckWithLeadingNonBasics(7));

        new SetupManager(HEADS, d -> { }, 6)
                .execute(s0, simpleStrategy(), slot(allBasicsDeck()), simpleStrategy());

        assertNotNull(s0.getActivePokemon());
    }

    // -------------------------------------------------------------------------
    // Player 1 mulligans
    // -------------------------------------------------------------------------

    @Test
    void player1Mulligan_once_mulliganCountIsOne() {
        final SetupResult result = new SetupManager(HEADS, d -> { }, 6)
                .execute(slot(allBasicsDeck()),              simpleStrategy(),
                         slot(deckWithLeadingNonBasics(7)), simpleStrategy());

        assertEquals(0, result.mulliganCountP0());
        assertEquals(1, result.mulliganCountP1());
    }

    @Test
    void player1Mulligan_once_player0DrawsOneBonusCard() {
        final PlayerSetupSlot s0 = slot(allBasicsDeck());

        new SetupManager(HEADS, d -> { }, 6)
                .execute(s0, simpleStrategy(),
                         slot(deckWithLeadingNonBasics(7)), simpleStrategy());

        assertEquals(INITIAL_HAND - 1 + 1, s0.getHand().size());
    }

    // -------------------------------------------------------------------------
    // Multiple mulligans by one player
    // -------------------------------------------------------------------------

    @Test
    void player0Mulligan_threeTimes_mulliganCountIsThree() {
        // 21 leading non-basics → 3 batches of 7 without basics
        final SetupResult result = new SetupManager(HEADS, d -> { }, 6)
                .execute(slot(deckWithLeadingNonBasics(21)), simpleStrategy(),
                         slot(allBasicsDeck()),               simpleStrategy());

        assertEquals(3, result.mulliganCountP0());
    }

    @Test
    void player0Mulligan_threeTimes_player1DrawsThreeBonusCards() {
        final PlayerSetupSlot s1 = slot(allBasicsDeck());

        new SetupManager(HEADS, d -> { }, 6)
                .execute(slot(deckWithLeadingNonBasics(21)), simpleStrategy(),
                         s1, simpleStrategy());

        // 7 drawn - 1 active + 3 bonus = 9
        assertEquals(INITIAL_HAND - 1 + 3, s1.getHand().size());
    }

    // -------------------------------------------------------------------------
    // Cross mulligans (both players mulligan)
    // -------------------------------------------------------------------------

    @Test
    void crossMulligan_correctCountsForBothPlayers() {
        // P0 mulligans twice (14 leading non-basics), P1 once (7 leading non-basics)
        final SetupResult result = new SetupManager(HEADS, d -> { }, 6)
                .execute(slot(deckWithLeadingNonBasics(14)), simpleStrategy(),
                         slot(deckWithLeadingNonBasics(7)),  simpleStrategy());

        assertEquals(2, result.mulliganCountP0());
        assertEquals(1, result.mulliganCountP1());
    }

    @Test
    void crossMulligan_player0DrawsNoBonusCardsDueToCanceling() {
        final PlayerSetupSlot s0 = slot(deckWithLeadingNonBasics(14));

        new SetupManager(HEADS, d -> { }, 6)
                .execute(s0, simpleStrategy(),
                         slot(deckWithLeadingNonBasics(7)), simpleStrategy());

        // P0 mulliganed twice, P1 mulliganed once.
        // Net mulligan for P0 against P1 = 0.
        // P0: 7 drawn - 1 active + 0 bonus = 6
        assertEquals(INITIAL_HAND - 1 + 0, s0.getHand().size());
    }

    @Test
    void crossMulligan_player1DrawsNetBonusCards() {
        final PlayerSetupSlot s1 = slot(deckWithLeadingNonBasics(7));

        new SetupManager(HEADS, d -> { }, 6)
                .execute(slot(deckWithLeadingNonBasics(14)), simpleStrategy(),
                         s1, simpleStrategy());

        // P0 mulliganed twice, P1 mulliganed once.
        // Net mulligan for P1 against P0 = 1.
        // P1: 7 drawn - 1 active + 1 bonus (net difference) = 7
        assertEquals(INITIAL_HAND - 1 + 1, s1.getHand().size());
    }

    // -------------------------------------------------------------------------
    // Active Pokémon identity check
    // -------------------------------------------------------------------------

    @Test
    void activePokemonNameMatchesFirstBasicInDeck() {
        // allBasicsDeck first card is "b-0"
        final PlayerSetupSlot s0 = slot(allBasicsDeck());

        new SetupManager(HEADS, d -> { }, 6).execute(s0, simpleStrategy(), slot(allBasicsDeck()), simpleStrategy());

        assertEquals("b-0", s0.getActivePokemon().getCardId());
    }

    // -------------------------------------------------------------------------
    // Null-safety
    // -------------------------------------------------------------------------

    @Test
    void constructorShouldThrowWhenCoinFlipperIsNull() {
        assertThrows(NullPointerException.class, () -> new SetupManager(null));
    }
}
