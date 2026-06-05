import { computed, Injectable, signal } from '@angular/core';
import {
  GameStateResponseDTO,
  BattlePokemonDTO,
  SpecialCondition,
  TurnPhase,
} from '../models/game-state.models';

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


  // ── Selectors básicos ────────────────────────────────────────────────────

  readonly isLoaded = computed(() => this.state() !== null);
  readonly matchId = computed(() => this.state()?.matchId);
  readonly phase = computed(() => this.state()?.currentPhase ?? null);
  readonly version = computed(() => this.state()?.version ?? 0);
  readonly pendingSelection = computed(() => this.state()?.pendingSelectionRequest ?? null);

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
    () => this.state()?.self?.active?.statusConditions ?? []
  );

  readonly oppActiveConditions = computed<SpecialCondition[]>(
    () => this.state()?.opponent?.active?.statusConditions ?? []
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
    const oldVersion = this.state()?.version;
    this.state.set(newState);
    if (newState.version !== oldVersion) {
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
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  // ── Helpers privados ──────────────────────────────────────────────────────

  private mapPokemon(dto: BattlePokemonDTO): UIPokemon {
    const conditions = dto.statusConditions ?? [];
    return {
      cardId: dto.cardId,
      name: dto.name,
      energies: (dto.attachedEnergies ?? []).map((e) => e.toLowerCase()),
      damage: dto.damageCounters * 10,
      maxHp: dto.maxHp,
      isEx: dto.isEx,
      retreatCost: dto.retreatCost,
      hasToolAttached: dto.hasToolAttached,
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
