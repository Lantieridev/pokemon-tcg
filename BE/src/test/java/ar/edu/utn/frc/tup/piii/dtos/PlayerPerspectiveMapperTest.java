package ar.edu.utn.frc.tup.piii.dtos;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
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
}
