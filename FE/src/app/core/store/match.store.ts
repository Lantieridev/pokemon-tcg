import { computed, Injectable, signal } from '@angular/core';
import {
  GameStateResponseDTO,
  BattlePokemonDTO,
  SpecialCondition,
  TurnPhase,
} from '../models/game-state.models';

export type GameStateDTO = GameStateResponseDTO;

/** Evento de daño detectado por diff de estado */
export interface DamageEvent {
  target: string;   // 'my-active' | 'opp-active' | 'my-bench-0' … | 'opp-bench-0' …
  amount: number;   // positivo = daño recibido, negativo = curación
  timestamp: number;
}

/** Versión normalizada para la UI del BattleComponent */
export interface UIPokemon {
  cardId: string;
  name: string;
  energies: string[];       // PokemonType[] convertidos a lowercase para energy-pip
  damage: number;           // damageCounters * 10
  maxHp: number;
  isEx: boolean;
  retreatCost: number;
  hasToolAttached: boolean;
  attachedToolCardId: string | null;
  attacks: { name: string; baseDamage: number; energyCost: string[] }[];
  statusConditions: SpecialCondition[];
  // Compatibilidad con FieldPokemonComponent (status único legacy)
  status: string;
}

/** Estado UI normalizado del jugador */
export interface UIPlayerState {
  name: string;
  tag: string;
  avatar: string;
  active: UIPokemon | null;
  bench: UIPokemon[];
  prizes: boolean[];        // true = disponible, false = tomada
  deckCount: number;
  hand: string[];           // cardIds (solo para el jugador propio)
  handCount: number;
  discardCount: number;
  discard: string[];
}

@Injectable({ providedIn: 'root' })
export class MatchStore {
  private state = signal<GameStateResponseDTO | null>(null);
  private timerInterval: any;
  private readonly timeLeft = signal(60);

  private updateQueue: GameStateResponseDTO[] = [];
  private isProcessingQueue = false;
  private queueTimeout: any = null;

  // ── Damage events (detectados por diff de estado) ────────────────────────
  readonly lastDamageEvents = signal<DamageEvent[]>([]);

  // ── Selectors básicos ────────────────────────────────────────────────────

  readonly isLoaded = computed(() => this.state() !== null);
  readonly matchId = computed(() => this.state()?.matchId);
  readonly phase = computed(() => this.state()?.currentPhase ?? null);
  readonly version = computed(() => this.state()?.version ?? 0);
  readonly pendingSelection = computed(() => this.state()?.pendingSelectionRequest ?? null);
  readonly activeStadiumCardId = computed(() => this.state()?.activeStadiumCardId ?? null);
  readonly winnerId = computed(() => this.state()?.winnerId ?? null);
  readonly victoryReason = computed(() => this.state()?.victoryReason ?? null);
  readonly mvpCardId = computed(() => this.state()?.mvpCardId ?? null);
  readonly mvpCardDamage = computed(() => this.state()?.mvpCardDamage ?? null);
  readonly mmrChange = computed(() => this.state()?.mmrChange ?? null);

  readonly isMyTurn = computed(() => {
    const s = this.state();
    if (!s) return false;
    // activePlayerIndex 0 = self (siempre, ya que el backend envía perspectiva del jugador)
    return s.activePlayerIndex === 0;
  });

  // ── Estado del propio jugador ─────────────────────────────────────────────

  readonly me = computed<UIPlayerState | null>(() => {
    const s = this.state();
    if (!s) return null;
    const self = s.self;
    return {
      name: self.playerId,
      tag: 'LIGA ORO',
      avatar: self.playerId.substring(0, 2).toUpperCase(),
      active: self.active ? this.mapPokemon(self.active) : null,
      bench: self.bench.map((p) => this.mapPokemon(p)),
      prizes: this.buildPrizeArray(self.prizeCount),
      deckCount: self.deckSize,
      hand: self.hand ?? [],
      handCount: (self.hand ?? []).length,
      discardCount: 0,
      discard: [],
    };
  });

  // ── Estado del oponente (Niebla de guerra) ────────────────────────────────

  readonly opp = computed<UIPlayerState | null>(() => {
    const s = this.state();
    if (!s) return null;
    const opp = s.opponent;
    return {
      name: opp.playerId,
      tag: 'LIGA PLATA',
      avatar: opp.playerId.substring(0, 2).toUpperCase(),
      active: opp.active ? this.mapPokemon(opp.active) : null,
      bench: opp.bench.map((p) => this.mapPokemon(p)),
      prizes: this.buildPrizeArray(opp.prizeCount),
      deckCount: opp.deckSize,
      hand: [],         // Niebla de guerra: NUNCA exponer la mano del oponente
      handCount: opp.handSize,
      discardCount: 0,
      discard: [],
    };
  });

  readonly turn = computed(() => ({
    number: this.state()?.turnNumber ?? 1,
    owner: this.isMyTurn() ? 'me' : 'opp',
    timer: this.timeLeft(),
  }));

  // ── Condiciones especiales del activo ─────────────────────────────────────

  readonly myActiveConditions = computed<SpecialCondition[]>(
    () => this.me()?.active?.statusConditions ?? []
  );

  readonly oppActiveConditions = computed<SpecialCondition[]>(
    () => this.opp()?.active?.statusConditions ?? []
  );

  // ── Habilitación de botones según fase del turno ──────────────────────────

  readonly canAttack = computed(() => {
    const phase = this.phase();
    return this.isMyTurn() && (phase === 'ATTACK' || phase === 'MAIN');
  });

  readonly canRetreat = computed(
    () => this.isMyTurn() && this.phase() === 'MAIN'
  );

  readonly canEndTurn = computed(
    () => this.isMyTurn() && this.phase() === 'MAIN'
  );

  readonly canPromoteActive = computed(
    () => this.pendingSelection() !== null
  );

  // ── Mutaciones ────────────────────────────────────────────────────────────

  updateState(newState: GameStateResponseDTO): void {
    this.updateQueue.push(newState);
    if (!this.isProcessingQueue) {
      this.processQueue();
    }
  }

  private processQueue(): void {
    if (this.updateQueue.length === 0) {
      this.isProcessingQueue = false;
      return;
    }

    this.isProcessingQueue = true;
    const nextState = this.updateQueue[0];
    const oldState = this.state();

    const hasCoinFlips = !!(nextState.lastCoinFlips && nextState.lastCoinFlips.length > 0);
    const coinFlipDuration = (nextState.lastCoinFlips && nextState.lastCoinFlips.length > 0)
      ? (nextState.lastCoinFlips.length * 2050 + 1800)
      : 0;

    const events = this.computeDamageEvents(oldState, nextState);
    const hasDamage = events.length > 0;

    if (!hasCoinFlips && !hasDamage) {
      // Sync update if no animations needed
      this.applyFinalState(nextState);
      this.updateQueue.shift();
      this.queueTimeout = setTimeout(() => {
        this.queueTimeout = null;
        this.processQueue();
      }, 0);
      return;
    }

    if (hasCoinFlips) {
      this.queueTimeout = setTimeout(() => {
        this.queueTimeout = null;
        this.applyGhostState(oldState, nextState, events, hasDamage, () => {
          this.updateQueue.shift();
          this.processQueue();
        });
      }, coinFlipDuration);
    } else {
      this.applyGhostState(oldState, nextState, events, hasDamage, () => {
        this.updateQueue.shift();
        this.processQueue();
      });
    }
  }

  private applyGhostState(
    oldState: GameStateResponseDTO | null,
    newState: GameStateResponseDTO,
    events: DamageEvent[],
    hasDamage: boolean,
    onComplete: () => void
  ): void {
    if (hasDamage) {
      this.lastDamageEvents.set(events);
    } else {
      this.lastDamageEvents.set([]);
    }

    const ghostState = this.createGhostState(oldState, newState);

    if (newState.currentPhase === 'FINISHED' && oldState) {
      ghostState.currentPhase = oldState.currentPhase;
    }

    this.state.set(ghostState);

    const damageAnimDuration = hasDamage ? 2000 : 0;
    this.queueTimeout = setTimeout(() => {
      this.queueTimeout = null;
      this.applyFinalState(newState);
      onComplete();
    }, damageAnimDuration);
  }

  private applyFinalState(newState: GameStateResponseDTO): void {
    this.lastDamageEvents.set([]);
    const oldState = this.state();
    const turnChanged = !oldState || 
                        oldState.turnNumber !== newState.turnNumber || 
                        oldState.activePlayerIndex !== newState.activePlayerIndex;

    this.state.set(newState);

    if (turnChanged) {
      this.timeLeft.set(60);
      this.startTimer();
    }
  }

  private startTimer(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
    this.timerInterval = setInterval(() => {
      this.timeLeft.update(t => Math.max(0, t - 1));
    }, 1000);
  }

  reset(): void {
    this.state.set(null);
    this.lastDamageEvents.set([]);
    if (this.timerInterval) clearInterval(this.timerInterval);
    if (this.queueTimeout) {
      clearTimeout(this.queueTimeout);
      this.queueTimeout = null;
    }
    this.updateQueue = [];
    this.isProcessingQueue = false;
  }

  // ── Damage diff engine ────────────────────────────────────────────────────

  private computeDamageEvents(
    oldState: GameStateResponseDTO | null,
    newState: GameStateResponseDTO
  ): DamageEvent[] {
    if (!oldState) return [];
    const now = Date.now();
    const events: DamageEvent[] = [];

    const diffPokemon = (
      oldPoke: BattlePokemonDTO | null | undefined,
      newPoke: BattlePokemonDTO | null | undefined,
      target: string
    ) => {
      if (!oldPoke) return;
      const oldDmg = oldPoke.damageCounters * 10;

      if (!newPoke) {
        // Pokemon was removed / knocked out! Calculate remaining HP as lethal damage
        const diff = oldPoke.maxHp - oldDmg;
        if (diff > 0) {
          events.push({ target, amount: diff, timestamp: now });
        }
        return;
      }

      if (oldPoke.cardId !== newPoke.cardId) return;
      const newDmg = newPoke.damageCounters * 10;
      const diff = newDmg - oldDmg;
      if (diff !== 0) {
        events.push({ target, amount: diff, timestamp: now });
      }
    };

    // Self active
    diffPokemon(oldState.self.active, newState.self.active, 'my-active');
    // Self bench
    const oldSelfBench = oldState.self.bench ?? [];
    const newSelfBench = newState.self.bench ?? [];
    for (let i = 0; i < Math.min(oldSelfBench.length, newSelfBench.length); i++) {
      diffPokemon(oldSelfBench[i], newSelfBench[i], `my-bench-${i}`);
    }

    // Opponent active
    diffPokemon(oldState.opponent.active, newState.opponent.active, 'opp-active');
    // Opponent bench
    const oldOppBench = oldState.opponent.bench ?? [];
    const newOppBench = newState.opponent.bench ?? [];
    for (let i = 0; i < Math.min(oldOppBench.length, newOppBench.length); i++) {
      diffPokemon(oldOppBench[i], newOppBench[i], `opp-bench-${i}`);
    }

    return events;
  }

  private createGhostState(
    oldState: GameStateResponseDTO | null,
    newState: GameStateResponseDTO
  ): GameStateResponseDTO {
    if (!oldState) return newState;

    const ghost = JSON.parse(JSON.stringify(newState)) as GameStateResponseDTO;

    // Ghost active Pokemon
    if (oldState.self.active) {
      if (!newState.self.active || newState.self.active.cardId !== oldState.self.active.cardId) {
        ghost.self.active = {
          ...oldState.self.active,
          damageCounters: oldState.self.active.maxHp / 10
        };
      }
    }
    if (oldState.opponent.active) {
      if (!newState.opponent.active || newState.opponent.active.cardId !== oldState.opponent.active.cardId) {
        ghost.opponent.active = {
          ...oldState.opponent.active,
          damageCounters: oldState.opponent.active.maxHp / 10
        };
      }
    }

    // Ghost bench Pokemon
    ghost.self.bench = this.alignBench(oldState.self.bench ?? [], newState.self.bench ?? []);
    ghost.opponent.bench = this.alignBench(oldState.opponent.bench ?? [], newState.opponent.bench ?? []);

    return ghost;
  }

  private alignBench(
    oldBench: BattlePokemonDTO[],
    newBench: BattlePokemonDTO[]
  ): BattlePokemonDTO[] {
    const aligned: BattlePokemonDTO[] = [];
    const usedNewIndices = new Set<number>();

    for (const oldPoke of oldBench) {
      const newIdx = newBench.findIndex((np, idx) => np.cardId === oldPoke.cardId && !usedNewIndices.has(idx));
      if (newIdx !== -1) {
        aligned.push(newBench[newIdx]);
        usedNewIndices.add(newIdx);
      } else {
        aligned.push({
          ...oldPoke,
          damageCounters: oldPoke.maxHp / 10
        });
      }
    }

    for (let i = 0; i < newBench.length; i++) {
      if (!usedNewIndices.has(i)) {
        aligned.push(newBench[i]);
      }
    }

    return aligned;
  }

  // ── Helpers privados ──────────────────────────────────────────────────────

  private mapPokemon(dto: BattlePokemonDTO): UIPokemon {
    const conditions = (dto.statusConditions ?? []).map((c: any) => {
      const s = String(c).toUpperCase();
      if (s === 'DORMIDO' || s === 'ASLEEP') return 'ASLEEP';
      if (s === 'CONFUNDIDO' || s === 'CONFUSED') return 'CONFUSED';
      if (s === 'PARALIZADO' || s === 'PARALYZED') return 'PARALYZED';
      if (s === 'QUEMADO' || s === 'BURNED') return 'BURNED';
      if (s === 'ENVENENADO' || s === 'POISONED') return 'POISONED';
      return s as SpecialCondition;
    });
    return {
      cardId: dto.cardId,
      name: dto.name,
      energies: (dto.attachedEnergies ?? []).map((e) => e.toLowerCase()),
      damage: dto.damageCounters * 10,
      maxHp: dto.maxHp,
      isEx: dto.isEx,
      retreatCost: dto.retreatCost,
      hasToolAttached: dto.hasToolAttached,
      attachedToolCardId: dto.attachedToolCardId ?? null,
      attacks: (dto.attacks ?? []).map((a) => ({
        name: a.name,
        baseDamage: a.baseDamage,
        energyCost: a.energyCost.map((e) => e.toLowerCase()),
      })),
      statusConditions: conditions,
      // Compatibilidad legacy con FieldPokemonComponent
      status: this.primaryStatus(conditions),
    };
  }

  /**
   * Retorna la condición de estado "primaria" para la rotación CSS.
   * ASLEEP → rotate left | CONFUSED → rotate 180 | PARALYZED → rotate right
   * BURNED/POISONED no afectan la rotación.
   */
  private primaryStatus(conditions: SpecialCondition[]): string {
    if (conditions.includes('ASLEEP')) return 'asleep';
    if (conditions.includes('CONFUSED')) return 'confused';
    if (conditions.includes('PARALYZED')) return 'paralyzed';
    return 'none';
  }

  /** 6 slots de premios: true = aún disponible */
  private buildPrizeArray(remainingPrizes: number): boolean[] {
    return Array(6)
      .fill(false)
      .map((_, i) => i < remainingPrizes);
  }
}
