import { TestBed } from '@angular/core/testing';
import { MatchStore, GameStateDTO } from './match.store';

describe('MatchStore', () => {
  let store: MatchStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MatchStore]
    });
    store = TestBed.inject(MatchStore);
  });

  it('should initialize with not loaded state', () => {
    expect(store.isLoaded()).toBeFalse();
    expect(store.matchId()).toBeUndefined();
    expect(store.me()).toBeNull();
    expect(store.opp()).toBeNull();
  });

  it('should update state and correctly calculate computed properties', () => {
    const mockState: any = {
      matchId: 'match-123',
      version: 5,
      turnNumber: 5,
      activePlayerIndex: 0,
      currentPhase: 'MAIN',
      pendingSelectionRequest: null,
      self: {
        playerId: 'AshRivero',
        active: {
          cardId: 'xy1-1',
          name: 'Ivysaur',
          attachedEnergies: ['FIRE'],
          damageCounters: 3,
          statusConditions: ['ASLEEP']
        } as any,
        bench: [],
        hand: ['xy1-2'],
        deckSize: 45,
        prizeCount: 6
      },
      opponent: {
        playerId: 'BrockSteel',
        active: {
          cardId: 'xy1-3',
          name: 'Venusaur',
          attachedEnergies: [],
          damageCounters: 0,
          statusConditions: []
        } as any,
        bench: [],
        handSize: 5,
        deckSize: 40,
        prizeCount: 6
      },
      discardPile: [],
      opponentDiscardPile: [],
      log: []
    };

    store.updateState(mockState);

    expect(store.isLoaded()).toBeTrue();
    expect(store.matchId()).toBe('match-123');
    expect(store.phase()).toBe('MAIN');
    expect(store.isMyTurn()).toBeTrue();
    
    const me = store.me();
    expect(me).toBeTruthy();
    expect(me!.name).toBe('AshRivero');
    expect(me!.active!.name).toBe('Ivysaur');
    expect(me!.active!.damage).toBe(30);
    expect(me!.active!.status).toBe('asleep');
    expect(me!.deckCount).toBe(45);

    const opp = store.opp();
    expect(opp).toBeTruthy();
    expect(opp!.name).toBe('BrockSteel');
    expect(opp!.active!.status).toBe('none');
    expect(opp!.handCount).toBe(5);

    expect(store.turn()).toEqual({ number: 5, owner: 'me', timer: 60 });
  });
});
