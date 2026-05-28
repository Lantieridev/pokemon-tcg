export interface PokemonCard {
  id: string;
  name: string;
  img: string;
  hp: number;
  type: string;
}

export interface CardEntity {
  card: string; // cardId
  energies: string[];
  damage: number;
  status?: 'none' | 'asleep' | 'confused' | 'burned' | 'poisoned' | 'paralyzed';
}

export interface PlayerState {
  name: string;
  tag: string;
  avatar: string;
  active: CardEntity | null;
  bench: CardEntity[];
  prizes: boolean[];
  deckCount: number;
  discard: string[];
  hand: string[];
  handCount: number;
  discardCount: number;
}

export interface MatchState {
  me: PlayerState;
  opp: PlayerState;
  turn: {
    number: number;
    owner: 'me' | 'opp';
    timer: number;
  };
}
