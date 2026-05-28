import { computed, Injectable, signal } from '@angular/core';

export interface GameStateDTO {
  matchId: string;
  version: number;
  activePlayerIndex: number;
  currentPhase: string;
  self: {
    playerId: string;
    active: any;
    bench: any[];
    hand: string[];
    deckSize: number;
    prizeCount: number;
  };
  opponent: {
    playerId: string;
    active: any;
    bench: any[];
    handSize: number;
    deckSize: number;
    prizeCount: number;
  };
}

const INITIAL_STATE: GameStateDTO | null = null;

@Injectable({ providedIn: 'root' })
export class MatchStore {
  private state = signal<GameStateDTO | null>(INITIAL_STATE);

  readonly isLoaded = computed(() => this.state() !== null);
  readonly matchId = computed(() => this.state()?.matchId);
  readonly phase = computed(() => this.state()?.currentPhase);
  readonly isMyTurn = computed(() => {
    const s = this.state();
    if (!s) return false;
    return s.activePlayerIndex === (s.self.playerId === 'AshRivero' ? 0 : 1); // Simple logic or we can use IDs
  });

  readonly me = computed(() => {
    const s = this.state();
    if (!s) return null;
    return {
      name: s.self.playerId, tag: 'LIGA ORO', avatar: s.self.playerId.substring(0,2).toUpperCase(),
      active: this.mapPokemon(s.self.active),
      bench: s.self.bench.map(p => this.mapPokemon(p)).filter(p => p !== null),
      prizes: Array(6).fill(false).map((_, i) => i < s.self.prizeCount),
      deckCount: s.self.deckSize,
      discard: [],
      hand: s.self.hand || [],
      handCount: (s.self.hand || []).length,
      discardCount: 0
    };
  });

  readonly opp = computed(() => {
    const s = this.state();
    if (!s) return null;
    return {
      name: s.opponent.playerId, tag: 'LIGA PLATA', avatar: s.opponent.playerId.substring(0,2).toUpperCase(),
      active: this.mapPokemon(s.opponent.active),
      bench: s.opponent.bench.map(p => this.mapPokemon(p)).filter(p => p !== null),
      prizes: Array(6).fill(false).map((_, i) => i < s.opponent.prizeCount),
      deckCount: s.opponent.deckSize,
      handCount: s.opponent.handSize,
      discardCount: 0,
      discard: [],
      hand: []
    };
  });

  readonly turn = computed(() => ({ number: this.state()?.version || 0, owner: this.isMyTurn() ? 'me' : 'opp', timer: 60 }));

  updateState(newState: GameStateDTO) {
    this.state.set(newState);
  }

  private mapPokemon(dto: any) {
    if (!dto) return null;
    let status = 'none';
    if (dto.statusConditions && dto.statusConditions.length > 0) {
      const cond = dto.statusConditions[0].toLowerCase();
      if (cond === 'dormido') status = 'asleep';
      else if (cond === 'confundido') status = 'confused';
      else if (cond === 'quemado') status = 'burned';
      else if (cond === 'envenenado') status = 'poisoned';
      else if (cond === 'paralizado') status = 'paralyzed';
    }
    return {
      card: dto.cardId || 'unknown',
      cardId: dto.cardId || 'unknown',
      name: dto.name || 'Unknown',
      energies: dto.attachedEnergies?.map((e: string) => e.toLowerCase()) || [],
      damage: (dto.damageCounters || 0) * 10,
      status: status
    };
  }
}
