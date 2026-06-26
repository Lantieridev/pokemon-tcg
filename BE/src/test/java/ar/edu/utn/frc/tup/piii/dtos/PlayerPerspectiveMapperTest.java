package ar.edu.utn.frc.tup.piii.dtos;

import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.services.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PlayerPerspectiveMapper — war-fog security and state mapping.
 */
class PlayerPerspectiveMapperTest {

    private PlayerPerspectiveMapper mapper;
    private MatchSession session;

    private static final List<String> PLAYER_0_HAND = List.of("card-p0-1", "card-p0-2", "card-p0-3", "card-p0-4", "card-p0-5");
    private static final List<String> PLAYER_1_HAND = List.of("card-p1-A", "card-p1-B", "card-p1-C", "card-p1-D", "card-p1-E");

    @BeforeEach
    void setUp() {
        mapper = new PlayerPerspectiveMapper();

        final FakeBattlePokemonState active0 = new FakeBattlePokemonState(
                100, PokemonType.FIRE, PokemonType.WATER, null, false);
        final FakeBattlePokemonState active1 = new FakeBattlePokemonState(
                120, PokemonType.WATER, PokemonType.LIGHTNING, PokemonType.FIRE, true);

        final PlayerState player0 = new PlayerState(
                active0, List.of(), PLAYER_0_HAND, 45, 6, Map.of());
        final PlayerState player1 = new PlayerState(
                active1, List.of(), PLAYER_1_HAND, 40, 4, Map.of());

        final MatchBoard board = new MatchBoard(List.of(player0, player1));
        session = new MatchSession("match-001", List.of("playerA", "playerB"), board);
        session.setup();
        session.start();
    }

    @Test
    void shouldNeverExposeOpponentCardIdsOrNamesInOpponentView() {
        // player 0's perspective
        final GameStateResponseDTO response = mapper.toResponse(session, 0);

        // opponent view must be OpponentView, not PlayerView
        assertThat(response.opponent()).isInstanceOf(GameStateResponseDTO.OpponentView.class);

        // opponent hand size == 5
        assertThat(response.opponent().handSize()).isEqualTo(5);

        // CRITICAL: OpponentView must NOT have any field named hand/cards/cardId/cardNames
        final boolean hasLeakingField = Arrays.stream(GameStateResponseDTO.OpponentView.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equals("hand")
                        || f.getName().equals("cards")
                        || f.getName().contains("cardId")
                        || f.getName().contains("cardName"));
        assertThat(hasLeakingField)
                .as("OpponentView must NOT expose hand, cards, cardIds, or cardNames fields (war-fog violation)")
                .isFalse();

        // self must have full hand
        assertThat(response.self()).isInstanceOf(GameStateResponseDTO.PlayerView.class);
        assertThat(response.self().hand()).isNotNull().isNotEmpty();
    }

    @Test
    void shouldReturnFullHandInSelfView() {
        final GameStateResponseDTO response = mapper.toResponse(session, 0);

        assertThat(response.self().hand()).hasSize(5);
        assertThat(response.self().hand()).containsExactlyElementsOf(PLAYER_0_HAND);
    }

    @Test
    void shouldMapActiveAndBenchCorrectly() {
        final GameStateResponseDTO response = mapper.toResponse(session, 0);

        final BattlePokemonDTO selfActive = response.self().active();
        assertThat(selfActive).isNotNull();
        assertThat(selfActive.pokemonType()).isEqualTo(PokemonType.FIRE);
        assertThat(selfActive.maxHp()).isEqualTo(100);
        assertThat(selfActive.weaknessType()).isEqualTo(PokemonType.WATER);
        assertThat(selfActive.resistanceType()).isNull();
        assertThat(selfActive.isEx()).isFalse();

        final BattlePokemonDTO opponentActive = response.opponent().active();
        assertThat(opponentActive).isNotNull();
        assertThat(opponentActive.pokemonType()).isEqualTo(PokemonType.WATER);
        assertThat(opponentActive.maxHp()).isEqualTo(120);
        assertThat(opponentActive.isEx()).isTrue();
    }

    @Test
    void shouldMapAllBattlePokemonFieldsIncludingNameAndCardId() {
        final FakeBattlePokemonState pokemon =
                new FakeBattlePokemonState(70, PokemonType.FIRE, PokemonType.WATER, null, false);
        pokemon.setName("Charmander");
        pokemon.setCardId("xy1-46");
        pokemon.setRetreatCost(1);
        pokemon.addAttachedEnergy(PokemonType.FIRE);
        pokemon.addAttachedEnergy(PokemonType.FIRE);
        pokemon.attachTool(new ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.Builder("tool", "tool", ar.edu.utn.frc.tup.piii.engine.model.TrainerType.POKEMON_TOOL).build());

        final ar.edu.utn.frc.tup.piii.engine.session.PlayerState ps =
                new ar.edu.utn.frc.tup.piii.engine.session.PlayerState(
                        pokemon, java.util.List.of(), java.util.List.of(), 45, 6, java.util.Map.of());
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerState ps1 =
                new ar.edu.utn.frc.tup.piii.engine.session.PlayerState(
                        new FakeBattlePokemonState(100, PokemonType.WATER, null, null, false),
                        java.util.List.of(), java.util.List.of(), 45, 6, java.util.Map.of());

        final ar.edu.utn.frc.tup.piii.engine.session.MatchBoard b =
                new ar.edu.utn.frc.tup.piii.engine.session.MatchBoard(java.util.List.of(ps, ps1));
        final ar.edu.utn.frc.tup.piii.engine.session.MatchSession s =
                new ar.edu.utn.frc.tup.piii.engine.session.MatchSession(
                        "m-test", java.util.List.of("playerX", "playerY"), b);
        s.setup();
        s.start();

        final GameStateResponseDTO response = mapper.toResponse(s, 0);

        final BattlePokemonDTO dto = response.self().active();
        assertThat(dto.name()).isEqualTo("Charmander");
        assertThat(dto.cardId()).isEqualTo("xy1-46");
        assertThat(dto.retreatCost()).isEqualTo(1);
        assertThat(dto.attachedEnergies()).hasSize(2);
        assertThat(dto.hasToolAttached()).isTrue();
        assertThat(dto.attachedToolCardId()).isEqualTo("tool");
    }

    @Test
    void shouldMapActiveStadiumCardId() {
        final ar.edu.utn.frc.tup.piii.engine.model.TrainerCard stadium =
                new ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.Builder("stadium-card-id", "Rough Seas", ar.edu.utn.frc.tup.piii.engine.model.TrainerType.STADIUM).build();
        session.getBoard().replaceStadium(stadium);

        final GameStateResponseDTO response = mapper.toResponse(session, 0);
        assertThat(response.activeStadiumCardId()).isEqualTo("stadium-card-id");
    }

    @Test
    void shouldMapAttacksFromActivePokemonToBattlePokemonDto() {
        final FakeBattlePokemonState pokemon =
                new FakeBattlePokemonState(70, PokemonType.FIRE, null, null, false);
        pokemon.setName("Charmander");
        pokemon.setCardId("xy1-46");
        pokemon.addAttack(new Attack("Ember", 20, List.of(PokemonType.FIRE)));

        final ar.edu.utn.frc.tup.piii.engine.session.PlayerState ps =
                new ar.edu.utn.frc.tup.piii.engine.session.PlayerState(
                        pokemon, List.of(), List.of(), 45, 6, Map.of());
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerState ps1 =
                new ar.edu.utn.frc.tup.piii.engine.session.PlayerState(
                        new FakeBattlePokemonState(100, PokemonType.WATER, null, null, false),
                        List.of(), List.of(), 45, 6, Map.of());

        final ar.edu.utn.frc.tup.piii.engine.session.MatchBoard b =
                new ar.edu.utn.frc.tup.piii.engine.session.MatchBoard(List.of(ps, ps1));
        final ar.edu.utn.frc.tup.piii.engine.session.MatchSession s =
                new ar.edu.utn.frc.tup.piii.engine.session.MatchSession(
                        "m-attacks", List.of("playerX", "playerY"), b);
        s.setup();
        s.start();

        final GameStateResponseDTO response = mapper.toResponse(s, 0);

        final BattlePokemonDTO dto = response.self().active();
        assertThat(dto.attacks()).isNotNull();
        assertThat(dto.attacks()).hasSize(1);
        assertThat(dto.attacks().get(0).name()).isEqualTo("Ember");
        assertThat(dto.attacks().get(0).baseDamage()).isEqualTo(20);
    }

    @Test
    void shouldProduceMirrorViewsForBothPlayers() {
        final GameStateResponseDTO view0 = mapper.toResponse(session, 0);
        final GameStateResponseDTO view1 = mapper.toResponse(session, 1);

        // for player 0: self = playerA, opponent = playerB
        assertThat(view0.self().playerId()).isEqualTo("playerA");
        assertThat(view0.opponent().playerId()).isEqualTo("playerB");

        // for player 1: self = playerB, opponent = playerA
        assertThat(view1.self().playerId()).isEqualTo("playerB");
        assertThat(view1.opponent().playerId()).isEqualTo("playerA");

        // each sees their own full hand
        assertThat(view0.self().hand()).hasSize(5);
        assertThat(view1.self().hand()).hasSize(5);

        // opponent hand size matches actual opponent hand
        assertThat(view0.opponent().handSize()).isEqualTo(5);
        assertThat(view1.opponent().handSize()).isEqualTo(5);
    }

    @Test
    void shouldMapPhaseToFinishedWhenSessionIsFinished() {
        session.finish();
        final GameStateResponseDTO response = mapper.toResponse(session, 0);
        assertThat(response.currentPhase()).isEqualTo("FINISHED");
    }

    @Test
    void shouldMapVictoryDetailsAndMVPCardWhenFinished() {
        final List<Card> dummyCards = List.of(new EnergyCard("dummy", "Energy", PokemonType.FIRE, true));
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime pr0 = 
                new ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime(
                        new Deck(dummyCards), new Hand(), new Bench(), new DiscardPile(),
                        new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(new ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper()),
                        null, List.of());
        pr0.getStatisticsTracker().addDamageDealt("xy1-46", 50);

        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime pr1 = 
                new ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime(
                        new Deck(dummyCards), new Hand(), new Bench(), new DiscardPile(),
                        new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(new ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper()),
                        null, List.of());
        pr1.getStatisticsTracker().addDamageDealt("xy1-1", 120);

        final MatchSession customSession = new MatchSession(
                "custom-001", List.of("playerA", "playerB"), session.getBoard(), List.of(pr0, pr1));
        
        customSession.setup();
        customSession.start();
        
        GameStateResponseDTO activeResponse = mapper.toResponse(customSession, 0);
        assertThat(activeResponse.winnerId()).isNull();
        assertThat(activeResponse.victoryReason()).isNull();
        assertThat(activeResponse.mvpCardId()).isNull();
        assertThat(activeResponse.mvpCardDamage()).isNull();

        customSession.setWinnerId("playerB");
        customSession.setVictoryReason("PRIZE_CARDS");
        customSession.finish();

        GameStateResponseDTO responseA = mapper.toResponse(customSession, 0);
        assertThat(responseA.winnerId()).isEqualTo("playerB");
        assertThat(responseA.victoryReason()).isEqualTo("PRIZE_CARDS");
        assertThat(responseA.mvpCardId()).isEqualTo("xy1-46");
        assertThat(responseA.mvpCardDamage()).isEqualTo(50);

        GameStateResponseDTO responseB = mapper.toResponse(customSession, 1);
        assertThat(responseB.winnerId()).isEqualTo("playerB");
        assertThat(responseB.victoryReason()).isEqualTo("PRIZE_CARDS");
        assertThat(responseB.mvpCardId()).isEqualTo("xy1-1");
        assertThat(responseB.mvpCardDamage()).isEqualTo(120);
    }

    @Test
    void shouldFilterPendingSelectionOptionsBasedOnTrainerEffect() {
        PokemonCard pikachu = new PokemonCard.Builder("xy1-1", "Pikachu", 60, PokemonType.LIGHTNING)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
        PokemonCard raichu = new PokemonCard.Builder("xy1-2", "Raichu", 90, PokemonType.LIGHTNING)
                .evolutionStage(EvolutionStage.STAGE_1)
                .evolvesFrom("Pikachu")
                .build();
        EnergyCard basicLightning = new EnergyCard("xy1-3", "Lightning Energy", PokemonType.LIGHTNING, true);
        EnergyCard specialEnergy = new EnergyCard("xy1-4", "Double Colorless", PokemonType.COLORLESS, false, 2, false);
        TrainerCard itemCard = new TrainerCard.Builder("xy1-5", "Potion", TrainerType.ITEM).build();

        PlayerRuntime pr = new PlayerRuntime(
                new Deck(List.of(pikachu, raichu, basicLightning, specialEnergy, itemCard)),
                new Hand(), new Bench(), new DiscardPile(),
                new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(new ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper()),
                null, List.of()
        );
        MatchSession customSession = new MatchSession(
                "custom-sel", List.of("playerA", "playerB"), session.getBoard(), List.of(pr, pr));
        customSession.setActivePlayerIndex(0);

        customSession.setPendingSelectionRequest(new PendingSelectionRequest(
                TrainerEffectId.PROFESSORS_LETTER, null, 2, SelectionSource.DECK));

        GameStateResponseDTO response = mapper.toResponse(customSession, 0);
        assertThat(response.pendingSelectionRequest()).isNotNull();
        assertThat(response.pendingSelectionRequest().options()).containsExactly("xy1-3");

        InPlayPokemon activePikachu = new InPlayPokemon(pikachu);
        customSession.setPendingSelectionRequest(new PendingSelectionRequest(
                TrainerEffectId.EVOSODA, activePikachu, 1, SelectionSource.DECK));

        response = mapper.toResponse(customSession, 0);
        assertThat(response.pendingSelectionRequest()).isNotNull();
        assertThat(response.pendingSelectionRequest().options()).containsExactly("xy1-2");

        customSession.setPendingSelectionRequest(new PendingSelectionRequest(
                TrainerEffectId.GREAT_BALL, null, 1, SelectionSource.TOP_7_DECK));

        response = mapper.toResponse(customSession, 0);
        assertThat(response.pendingSelectionRequest()).isNotNull();
        assertThat(response.pendingSelectionRequest().options()).containsExactlyInAnyOrder("xy1-1", "xy1-2", "xy1-3", "xy1-4", "xy1-5");
    }

    @Test
    void shouldFilterPendingSelectionOptionsFromDiscardPileForSacredAsh() {
        PokemonCard pikachu = new PokemonCard.Builder("xy1-1", "Pikachu", 60, PokemonType.LIGHTNING)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
        EnergyCard basicLightning = new EnergyCard("xy1-3", "Lightning Energy", PokemonType.LIGHTNING, true);
        TrainerCard itemCard = new TrainerCard.Builder("xy1-5", "Potion", TrainerType.ITEM).build();

        DiscardPile discard = new DiscardPile();
        discard.add(pikachu);
        discard.add(basicLightning);
        discard.add(itemCard);

        PlayerRuntime pr = new PlayerRuntime(
                new Deck(List.of(pikachu)),
                new Hand(), new Bench(), discard,
                new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(new ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper()),
                null, List.of()
        );
        MatchSession customSession = new MatchSession(
                "custom-sel-discard", List.of("playerA", "playerB"), session.getBoard(), List.of(pr, pr));
        customSession.setActivePlayerIndex(0);

        customSession.setPendingSelectionRequest(new PendingSelectionRequest(
                TrainerEffectId.SACRED_ASH, null, 5, SelectionSource.DISCARD_PILE));

        GameStateResponseDTO response = mapper.toResponse(customSession, 0);
        assertThat(response.pendingSelectionRequest()).isNotNull();
        assertThat(response.pendingSelectionRequest().options()).containsExactly("xy1-1");
    }

    @Test
    void shouldMapRetreatBlockedConditionToActiveConditions() {
        PokemonCard pikachu = new PokemonCard.Builder("xy1-1", "Pikachu", 60, PokemonType.LIGHTNING)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
        DiscardPile discard = new DiscardPile();
        ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager sem = new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(new ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper());
        sem.setRetreatBlockedNextTurn(true);

        PlayerRuntime pr = new PlayerRuntime(
                new Deck(List.of(pikachu)),
                new Hand(), new Bench(), discard,
                sem,
                null, List.of()
        );

        MatchSession customSession = new MatchSession(
                "custom-retreat-blocked", List.of("playerA", "playerB"), session.getBoard(), List.of(pr, pr));

        GameStateResponseDTO response = mapper.toResponse(customSession, 0);
        assertThat(response.self().active().statusConditions()).contains("RETREAT_BLOCKED");
    }
}
