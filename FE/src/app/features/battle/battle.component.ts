import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
  ElementRef,
  AfterViewChecked,
  inject,
  signal,
  computed,
  effect,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { FieldPokemonComponent } from '../../shared/ui/field-pokemon/field-pokemon.component';
import { EnergyPipComponent } from '../../shared/ui/energy-pip/energy-pip.component';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { CardSelectionModalComponent } from '../../shared/ui/card-selection-modal/card-selection-modal.component';
import { SparksComponent, AmbientComponent, BallIconComponent } from '../lobby-aurora/ui/aurora-ui.components';

import { WebSocketService } from '../../core/services/websocket.service';
import { MatchStore } from '../../core/store/match.store';
import { MatchBackendService } from '../../core/services/match-backend.service';
import { AuthService } from '../../core/services/auth.service';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { ActionRequestDTO, SpecialCondition, PokemonTcgCard, PokemonType } from '../../core/models/game-state.models';
import { CARDS } from '../../shared/data/cards.mock';
import { LOCAL_CARDS_DB } from '../../shared/data/cards-local-db';
import { ToastService } from '../../core/services/toast.service';

// ── Chat & Log (UI local, sin lógica de negocio) ─────────────────────────────

const QUICK_CHAT = [
  '¡Buena suerte!', '¡Buena jugada!', '¡Gracias!',
  'Estuvo cerca…', 'Hmm…', '¡GG!',
];

interface LogEntry {
  t: number;
  who: string;
  kind: string;
  txt: string;
  mine: boolean;
}

interface ChatEntry {
  from: 'me' | 'opp' | 'system';
  text: string;
  t: string;
}

@Component({
  selector: 'app-battle',
  standalone: true,
  imports: [FieldPokemonComponent, EnergyPipComponent, IconComponent, CardSelectionModalComponent, SparksComponent, AmbientComponent, BallIconComponent],
  templateUrl: './battle.html',
  styleUrl: './battle.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BattleComponent implements OnInit, OnDestroy, AfterViewChecked {
  // ── Inyecciones ────────────────────────────────────────────────────────────
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private wsService = inject(WebSocketService);
  readonly store = inject(MatchStore);
  private matchBackend = inject(MatchBackendService);
  private authService = inject(AuthService);
  readonly tcgService = inject(PokemonTcgService);
  private toastService = inject(ToastService);

  // ── Alias del store para el template ──────────────────────────────────────
  readonly me = this.store.me;
  readonly opp = this.store.opp;
  readonly turn = this.store.turn;
  readonly isMyTurn = this.store.isMyTurn;
  readonly canAttack = this.store.canAttack;
  readonly canRetreat = this.store.canRetreat;
  readonly canEndTurn = this.store.canEndTurn;
  readonly myActiveConditions = this.store.myActiveConditions;
  readonly oppActiveConditions = this.store.oppActiveConditions;
  readonly activeStadiumCardId = this.store.activeStadiumCardId;
  readonly pendingSelection = computed(() => {
    const sel = this.store.pendingSelection();
    if (!sel) return null;
    const isOpponentChoosing = sel.sourceEffect === 'FLASH_CLAW' || sel.sourceEffect === 'PUSH_DOWN';
    if (isOpponentChoosing) {
      return !this.isMyTurn() ? sel : null;
    }
    return this.isMyTurn() ? sel : null;
  });
  readonly isFinished = computed(() => {
    const phase = this.store.phase();
    // Only show game over when state is loaded AND phase is explicitly FINISHED
    return this.store.isLoaded() && phase === 'FINISHED';
  });
  readonly gameResult = computed(() => {
    if (!this.isFinished()) return null;
    const winnerId = this.store.winnerId();
    if (!winnerId) return 'EMPATE';
    return winnerId === this.me()?.name ? 'VICTORIA' : 'DERROTA';
  });

  readonly victoryReasonText = computed(() => {
    if (!this.isFinished()) return '';
    const reason = this.store.victoryReason();
    const isWinner = this.gameResult() === 'VICTORIA';
    
    if (isWinner) {
      switch (reason) {
        case 'PRIZE_CARDS':
          return '¡Ganaste al tomar todas tus cartas de Premio!';
        case 'NO_BENCH_POKEMON':
          return '¡Ganaste porque tu oponente se quedó sin Pokémon en juego!';
        case 'DECK_OUT':
          return '¡Ganaste porque tu oponente se quedó sin cartas en su mazo!';
        case 'ABANDON':
          return '¡Ganaste porque tu oponente abandonó la partida!';
        default:
          return '¡Victoria!';
      }
    } else {
      switch (reason) {
        case 'PRIZE_CARDS':
          return 'Tu oponente tomó todas sus cartas de Premio.';
        case 'NO_BENCH_POKEMON':
          return 'Perdiste al quedarte sin Pokémon en juego.';
        case 'DECK_OUT':
          return 'Perdiste al quedarte sin cartas en tu mazo al comenzar tu turno.';
        case 'ABANDON':
          return 'Abandonaste la partida.';
        default:
          return 'Derrota.';
      }
    }
  });

  readonly mvpCardId = this.store.mvpCardId;
  readonly mvpCardDamage = this.store.mvpCardDamage;
  readonly mmrChange = this.store.mmrChange;

  // ── Estado UI local ────────────────────────────────────────────────────────
  readonly log = signal<LogEntry[]>([]);
  readonly chat = signal<ChatEntry[]>([]);
  readonly menu = signal<any>(null);
  readonly isConnecting = signal(true);
  readonly connectionError = signal<string | null>(null);
  readonly zoomedCardUrl = signal<string | null>(null);
  readonly selectedHandCard = signal<PokemonTcgCard | null>(null);
  readonly selectedHandIndex = signal<number | null>(null);
  readonly draggedCard = signal<PokemonTcgCard | null>(null);
  readonly draggedCardIndex = signal<number | null>(null);
  readonly isRetreating = signal<boolean>(false);
  readonly targetingAbility = signal<{ name: string; sourceIndex: number | null } | null>(null);
  readonly scorchingFangPrompt = signal<{ attackIndex: number } | null>(null);

  readonly quickChat = QUICK_CHAT;
  Math = Math;

  // ── Intro Animations ──────────────────────────────────────────────────────
  readonly animationStage = signal<'idle' | 'coin-flip' | 'dealing-hand' | 'dealing-prizes' | 'complete'>('idle');
  readonly coinFlipResult = signal<'heads' | 'tails' | null>(null);
  readonly animatedHandCount = signal(0);
  readonly animatedOppHandCount = signal(0);
  readonly animatedPrizeCount = signal(0);
  readonly animatedOppPrizeCount = signal(0);

  readonly isAttackCoinFlipping = signal<boolean>(false);
  readonly attackCoinFlips = signal<boolean[]>([]);
  readonly currentAttackFlipIndex = signal<number>(-1);
  readonly currentAttackFlipResult = signal<'heads' | 'tails' | null>(null);
  readonly coinFlipSubText = signal<string>('');
  readonly isSmokescreenFlip = signal<boolean>(false);

  // ── Computed helpers ──────────────────────────────────────────────────────
  readonly myEmptyBench = computed(() =>
    Array(Math.max(0, 5 - (this.me()?.bench.length ?? 0))).fill(0)
  );
  readonly oppEmptyBench = computed(() =>
    Array(Math.max(0, 5 - (this.opp()?.bench.length ?? 0))).fill(0)
  );
  readonly oppHandArray = computed(() =>
    Array(this.opp()?.handCount ?? 0).fill(0)
  );

  readonly oppHandArrayAnimated = computed(() =>
    Array(this.animationStage() === 'complete' ? (this.opp()?.handCount ?? 0) : this.animatedOppHandCount()).fill(0)
  );

  readonly handCount = computed(() => this.me()?.hand.length ?? 0);
  readonly spread = 6;

  readonly handCards = computed<PokemonTcgCard[]>(() => {
    const handIds = this.me()?.hand ?? [];
    const allCards = this.tcgService.cards();
    if (handIds.length > 0) {
      console.log('Hand IDs from store:', handIds);
      console.log('All cards from TCG size:', allCards.length);
    }
    return handIds.map(id => {
      const card = allCards.find(c => c.id === id);
      if (card) return card;
      
      const localCard = (LOCAL_CARDS_DB as any)[id];
      if (localCard) {
        return {
          id: id,
          name: localCard.name,
          supertype: localCard.supertype,
          subtypes: localCard.subtypes || [],
          images: { small: '', large: '' },
          set: { id: 'xy1' }
        } as PokemonTcgCard;
      }
      console.warn(`Card not found in TCG Service: ${id}`);
      return {
        id,
        name: 'Carta',
        supertype: 'Pokémon',
        subtypes: ['Basic'],
        images: { small: '', large: '' },
        set: { id: 'xy1' }
      } as PokemonTcgCard;
    });
  });

  readonly handCardsAnimated = computed(() => {
    const cards = this.handCards();
    if (this.animationStage() === 'complete') {
      return cards;
    }
    return cards.slice(0, this.animatedHandCount());
  });

  readonly myPrizesAnimated = computed(() => {
    const prizes = this.me()?.prizes ?? [];
    if (this.animationStage() === 'complete') {
      return prizes;
    }
    return prizes.slice(0, this.animatedPrizeCount());
  });

  readonly oppPrizesAnimated = computed(() => {
    const prizes = this.opp()?.prizes ?? [];
    if (this.animationStage() === 'complete') {
      return prizes;
    }
    return prizes.slice(0, this.animatedOppPrizeCount());
  });

  readonly myDeckCountAnimated = computed(() => {
    const base = this.me()?.deckCount ?? 60;
    if (this.animationStage() === 'complete') return base;
    const handRemaining = (this.me()?.hand.length ?? 7) - this.animatedHandCount();
    const prizeRemaining = (this.me()?.prizes.length ?? 6) - this.animatedPrizeCount();
    return base + handRemaining + prizeRemaining;
  });

  readonly oppDeckCountAnimated = computed(() => {
    const base = this.opp()?.deckCount ?? 60;
    if (this.animationStage() === 'complete') return base;
    const handRemaining = (this.opp()?.handCount ?? 7) - this.animatedOppHandCount();
    const prizeRemaining = (this.opp()?.prizes.length ?? 6) - this.animatedOppPrizeCount();
    return base + handRemaining + prizeRemaining;
  });

  // ── Condiciones especiales para CSS ───────────────────────────────────────
  readonly myActiveIsAsleep = computed(() =>
    this.myActiveConditions().includes('ASLEEP')
  );
  readonly myActiveIsConfused = computed(() =>
    this.myActiveConditions().includes('CONFUSED')
  );
  readonly myActiveIsParalyzed = computed(() =>
    this.myActiveConditions().includes('PARALYZED')
  );
  readonly myActiveIsBurned = computed(() =>
    this.myActiveConditions().includes('BURNED')
  );
  readonly myActiveIsPoisoned = computed(() =>
    this.myActiveConditions().includes('POISONED')
  );
  readonly oppActiveIsAsleep = computed(() =>
    this.oppActiveConditions().includes('ASLEEP')
  );
  readonly oppActiveIsConfused = computed(() =>
    this.oppActiveConditions().includes('CONFUSED')
  );
  readonly oppActiveIsBurned = computed(() =>
    this.oppActiveConditions().includes('BURNED')
  );
  readonly oppActiveIsPoisoned = computed(() =>
    this.oppActiveConditions().includes('POISONED')
  );

  private matchId!: string;
  private wsSub?: Subscription;
  private lastAutoEndedVersion = -1;

  @ViewChild('scrollRef') scrollRef!: ElementRef;
  @ViewChild('chatRef') chatRef!: ElementRef;

  constructor() {
    effect(() => {
      const turnState = this.turn();
      const myTurn = this.isMyTurn();
      const hasActive = this.me()?.active !== null;
      const currentVersion = turnState.number;
      if (myTurn && hasActive && turnState.timer === 0 && this.canEndTurn()) {
        if (this.lastAutoEndedVersion !== currentVersion) {
          this.lastAutoEndedVersion = currentVersion;
          console.log(`[Battle] Timer reached 0 for version ${currentVersion}. Ending turn automatically.`);
          this.endTurn();
        }
      }
    });

    // Clear selection when it's no longer our turn
    effect(() => {
      if (!this.isMyTurn()) {
        this.selectedHandIndex.set(null);
        this.selectedHandCard.set(null);
      }
    });

    // Clear selection if hand updates and selected card is no longer at that index
    effect(() => {
      const hand = this.me()?.hand;
      const index = this.selectedHandIndex();
      const selectedCard = this.selectedHandCard();
      if (!hand || index === null || selectedCard === null) {
        return;
      }
      if (index >= hand.length || hand[index] !== selectedCard.id) {
        this.selectedHandIndex.set(null);
        this.selectedHandCard.set(null);
      }
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    // Obtener matchId desde params de ruta o query params
    this.matchId =
      this.route.snapshot.paramMap.get('matchId') ??
      this.route.snapshot.queryParamMap.get('matchId') ??
      'dev-match-001';

    this.tcgService.loadCards();
    this.initializeMatch();
  }

  ngAfterViewChecked(): void {
    if (this.scrollRef?.nativeElement)
      this.scrollRef.nativeElement.scrollTop = 0;
    if (this.chatRef?.nativeElement)
      this.chatRef.nativeElement.scrollTop =
        this.chatRef.nativeElement.scrollHeight;
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.wsService.disconnect();
  }

  // ── Inicialización ────────────────────────────────────────────────────────

  private initializeMatch(): void {
    this.isConnecting.set(true);
    this.connectionError.set(null);

    // 1. Cargar estado inicial vía REST
    this.matchBackend.getMatchState(this.matchId).subscribe({
      next: (state) => {
        this.store.updateState(state);
        this.connectWebSocket();
      },
      error: (err) => {
        console.warn('[Battle] Error REST inicial, conectando WS de todas formas:', err);
        // Intentar WebSocket aunque falle el REST (puede que el estado llegue por WS)
        this.connectWebSocket();
      },
    });

    // Cargar historial de chat
    this.matchBackend.getChatHistory(this.matchId).subscribe({
      next: (history) => {
        const username = this.authService.username;
        const mapped = history.map(msg => ({
          from: msg.sender === 'SISTEMA' || msg.sender === 'SYSTEM' ? 'system'
              : msg.sender === username ? 'me' : 'opp',
          text: msg.message,
          t: new Date(msg.timestamp).toTimeString().slice(0, 5)
        }));
        this.chat.set(mapped as ChatEntry[]);

        // Show Toast alerts for system messages (Mulligans)
        mapped.forEach(msg => {
          if (msg.from === 'system') {
            this.toastService.info(msg.text);
          }
        });
      },
      error: (err) => {
        console.warn('[Battle] Error cargando historial de chat:', err);
      }
    });
  }

  private connectWebSocket(): void {
    try {
      this.wsSub = this.wsService.connect(this.matchId).subscribe({
        next: () => {
          this.isConnecting.set(false);
          this.checkAndStartIntro();
        },
        error: (err) => {
          this.connectionError.set('Error de conexión al tablero.');
          this.isConnecting.set(false);
          console.error('[Battle] WS error:', err);
        },
      });
      // Marcar como conectado tras activar (el estado llega por onConnect)
      setTimeout(() => {
        if (this.isConnecting()) {
          this.isConnecting.set(false);
          this.checkAndStartIntro();
        }
      }, 3000);
      
      const chatSub = this.wsService.chatMessage$.subscribe(msg => {
        const username = this.authService.username;
        const mapped: ChatEntry = {
          from: msg.sender === 'SISTEMA' || msg.sender === 'SYSTEM' ? 'system'
              : msg.sender === username ? 'me' : 'opp',
          text: msg.message,
          t: new Date().toTimeString().slice(0, 5)
        };
        this.chat.update(c => [...c, mapped]);
        if (mapped.from === 'system') {
          this.toastService.info(mapped.text);
        }
      });
      const stateSub = this.wsService.gameState$.subscribe(state => {
        if (state.lastCoinFlips && state.lastCoinFlips.length > 0) {
          this.runAttackCoinFlips(state.lastCoinFlips);
        }
      });
      this.wsSub.add(stateSub);
      this.wsSub.add(chatSub);
    } catch (err) {
      this.connectionError.set('No se pudo conectar. ¿Estás autenticado?');
      this.isConnecting.set(false);
    }
  }

  // ── Acciones del jugador ──────────────────────────────────────────────────

  attack(attackIndex: number): void {
    if (!this.canAttack()) return;
    const active = this.me()?.active;
    const atk = active?.attacks?.[attackIndex];
    if (active?.cardId === 'xy2-20' && atk?.name === 'Scorching Fang') {
      const hasFireEnergy = active.energies?.some((e: string) => e.toUpperCase() === 'FIRE');
      const hasThreeOrMoreEnergies = active.energies && active.energies.length >= 3;
      if (hasFireEnergy && hasThreeOrMoreEnergies) {
        this.scorchingFangPrompt.set({ attackIndex });
        this.closeMenu();
        return;
      }
    }
    this.sendAction({ type: 'DECLARE_ATTACK', attackIndex });
    this.closeMenu();
  }

  resolveScorchingFang(attackIndex: number, wantsDiscard: boolean): void {
    this.sendAction({
      type: 'DECLARE_ATTACK',
      attackIndex,
      selectedCardIds: wantsDiscard ? ['discard_fire_energy'] : []
    });
    this.scorchingFangPrompt.set(null);
  }

  retreat(targetIndex: number, energyIndices: number[]): void {
    if (!this.canRetreat()) return;
    this.sendAction({
      type: 'RETREAT',
      targetIndex,
      selectedEnergyIndices: energyIndices,
    });
    this.closeMenu();
  }

  startRetreat(): void {
    if (!this.canRetreat()) return;
    
    const active = this.me()?.active;
    if (active && active.statusConditions) {
      const conds = active.statusConditions.map(c => String(c).toUpperCase());
      if (conds.includes('ASLEEP') || conds.includes('DORMIDO')) {
        this.toastService.error('No puedes retirar a un Pokémon Dormido.');
        this.closeMenu();
        return;
      }
      if (conds.includes('PARALYZED') || conds.includes('PARALIZADO')) {
        this.toastService.error('No puedes retirar a un Pokémon Paralizado.');
        this.closeMenu();
        return;
      }
      if (conds.includes('RETREAT_BLOCKED') || conds.includes('BLOQUEO_RETIRADA')) {
        this.toastService.error('No puedes retirar a este Pokémon porque su retirada está bloqueada por el ataque del oponente.');
        this.closeMenu();
        return;
      }
    }

    this.isRetreating.set(true);
    this.closeMenu();
  }

  cancelRetreat(): void {
    this.isRetreating.set(false);
  }

  onRetreatClick(targetIndex: number): void {
    this.isRetreating.set(false);
    const active = this.me()?.active;
    if (!active) return;
    
    const hasFairyEnergy = active.energies.includes('fairy');
    const isFairyGarden = this.activeStadiumCardId() === 'xy1-117';
    const retreatCost = (isFairyGarden && hasFairyEnergy) ? 0 : active.retreatCost;
    
    const energyIndices: number[] = [];
    for (let i = 0; i < retreatCost; i++) {
      energyIndices.push(i);
    }
    this.retreat(targetIndex, energyIndices);
  }

  endTurn(): void {
    if (!this.canEndTurn()) return;
    this.sendAction({ type: 'END_TURN' });
  }

  useAbility(abilityName: string, sourceIndex: number | null): void {
    if (!this.isMyTurn()) return;
    this.sendAction({
      type: 'USE_ABILITY',
      cardId: abilityName,
      sourceIndex: sourceIndex
    });
    this.closeMenu();
  }

  triggerAbility(abilityName: string, sourceIndex: number | null): void {
    this.closeMenu();
    if (abilityName === 'Water Shuriken' || abilityName === 'Energy Grace' || abilityName === 'Shadow Void') {
      this.targetingAbility.set({ name: abilityName, sourceIndex });
    } else if (abilityName === 'Fairy Transfer') {
      const targetIndex = sourceIndex === -1 ? 0 : -1;
      this.sendAction({
        type: 'USE_ABILITY',
        cardId: abilityName,
        sourceIndex: sourceIndex,
        targetIndex: targetIndex,
        selectedEnergyIndices: [0]
      });
    } else {
      this.useAbility(abilityName, sourceIndex);
    }
  }

  onAbilityTargetClick(targetType: 'active' | 'bench', targetIndex: number | null): void {
    const targeting = this.targetingAbility();
    if (!targeting) return;
    this.targetingAbility.set(null);
    this.sendAction({
      type: 'USE_ABILITY',
      cardId: targeting.name,
      sourceIndex: targeting.sourceIndex,
      targetIndex: targetType === 'active' ? -1 : targetIndex
    });
  }

  isActiveAbility(name: string): boolean {
    const activeAbilities = ['Fairy Transfer', 'Mystical Fire', 'Magnetic Draw', 'Water Shuriken', 'Upside-Down Evolution', 'Stance Change', 'Drive Off', 'Leaf Draw', 'Energy Grace', 'Shadow Void', 'Gooey Regeneration', 'Big Jump'];
    return activeAbilities.includes(name);
  }

  confirmSelection(selectedCardIds: string[]): void {
    this.sendAction({ type: 'SELECT_CARDS', selectedCardIds });
  }

  cancelSelection(): void {
    // Send empty selection to cancel/skip
    this.sendAction({ type: 'SELECT_CARDS', selectedCardIds: [] });
  }

  private sendAction(action: ActionRequestDTO): void {
    this.wsService.sendAction(this.matchId, action);
  }

  isTargetingTrainer(card: PokemonTcgCard): boolean {
    if (card.supertype !== 'Trainer') return false;
    const name = card.name.toLowerCase();
    if (card.subtypes.includes('Pokémon Tool')) return true;
    if (name.includes('evosoda')) return true;
    if (name.includes('potion')) return true;
    if (name.includes('cassius')) return true;
    if (name.includes('lysandre')) return true;
    if (name.includes('blacksmith')) return true;
    if (name.includes('trick shovel')) return true;
    return false;
  }

  isValidTarget(side: 'me' | 'opponent', targetType: 'active' | 'bench', targetIndex: number | null): boolean {
    const card = this.selectedHandCard() || this.draggedCard();
    if (!card) return false;

    // Lysandre targets opponent's bench only
    if (card.supertype === 'Trainer' && card.name.toLowerCase().includes('lysandre')) {
      return side === 'opponent' && targetType === 'bench';
    }

    // Blacksmith targets my own Fire Pokémon (active or benched)
    if (card.supertype === 'Trainer' && card.name.toLowerCase().includes('blacksmith')) {
      if (side !== 'me') return false;
      const targetPk = targetType === 'active' ? this.me()?.active : this.me()?.bench[targetIndex!];
      if (!targetPk) return false;
      return targetPk.pokemonType === 'FIRE';
    }

    // Trick Shovel can target either Active Pokémon to choose the deck to look at
    if (card.supertype === 'Trainer' && card.name.toLowerCase().includes('trick shovel')) {
      return targetType === 'active';
    }

    // For other trainers/tools/energy/evolution: they target my own board
    if (card.supertype === 'Energy' || card.supertype === 'Pokémon' || (card.supertype === 'Trainer' && this.isTargetingTrainer(card))) {
      return side === 'me';
    }

    return false;
  }

  playCard(card: PokemonTcgCard, index: number): void {
    if (!this.isMyTurn()) return;
    
    // Si es Pokémon Básico, bajar a la banca directamente
    if (card.supertype === 'Pokémon' && card.subtypes.includes('Basic')) {
      this.sendAction({ type: 'PLACE_BASIC_POKEMON', cardId: card.id });
      this.selectedHandIndex.set(null);
      this.selectedHandCard.set(null);
      return;
    }
    
    // Si es Energía, Evolución o Entrenador que requiere objetivo, requiere seleccionar un objetivo
    if (
      card.supertype === 'Energy' || 
      (card.supertype === 'Pokémon' && (card.subtypes.includes('Stage 1') || card.subtypes.includes('Stage 2') || card.subtypes.includes('MEGA'))) ||
      this.isTargetingTrainer(card)
    ) {
      if (this.selectedHandIndex() === index) {
        this.selectedHandIndex.set(null);
        this.selectedHandCard.set(null);
      } else {
        this.selectedHandIndex.set(index);
        this.selectedHandCard.set(card);
      }
    } else if (card.supertype === 'Trainer') {
      // Entrenador no-objetivo (Shauna, Profesores, Roller Skates, etc.) - Jugar de inmediato
      const type = card.subtypes.includes('Stadium') ? 'STADIUM'
                 : card.subtypes.includes('Supporter') ? 'SUPPORTER' : 'ITEM';
      this.sendAction({
        type: 'PLAY_TRAINER',
        cardId: card.id,
        trainerType: type,
        targetIndex: null
      });
      this.selectedHandIndex.set(null);
      this.selectedHandCard.set(null);
    }
  }

  selectTarget(targetType: 'active' | 'bench', targetIndex: number | null): void {
    const card = this.selectedHandCard();
    if (!card || !this.isMyTurn()) return;

    if (card.supertype === 'Energy') {
      const energyType = this.getEnergyType(card);
      this.sendAction({
        type: 'ATTACH_ENERGY',
        cardId: card.id,
        energyType: energyType as PokemonType,
        targetIndex: targetType === 'active' ? null : targetIndex
      });
    } else if (card.supertype === 'Pokémon' && (card.subtypes.includes('Stage 1') || card.subtypes.includes('Stage 2') || card.subtypes.includes('MEGA'))) {
      this.sendAction({
        type: 'EVOLVE',
        cardId: card.id,
        targetIndex: targetType === 'active' ? null : targetIndex
      });
    } else if (card.supertype === 'Trainer') {
      const type = card.subtypes.includes('Supporter') ? 'SUPPORTER' 
                 : card.subtypes.includes('Pokémon Tool') ? 'POKEMON_TOOL' : 'ITEM';
      this.sendAction({
        type: 'PLAY_TRAINER',
        cardId: card.id,
        trainerType: type,
        targetIndex: targetType === 'active' ? -1 : targetIndex
      });
    } else if (targetType === 'bench' && card.supertype === 'Pokémon' && card.subtypes.includes('Basic')) {
      this.sendAction({ type: 'PLACE_BASIC_POKEMON', cardId: card.id });
    }

    this.selectedHandIndex.set(null);
    this.selectedHandCard.set(null);
  }

  getEnergyType(card: PokemonTcgCard): string {
    const name = card.name.toUpperCase();
    if (name.includes('FIRE')) return 'FIRE';
    if (name.includes('WATER')) return 'WATER';
    if (name.includes('GRASS')) return 'GRASS';
    if (name.includes('LIGHTNING')) return 'LIGHTNING';
    if (name.includes('PSYCHIC')) return 'PSYCHIC';
    if (name.includes('FIGHTING')) return 'FIGHTING';
    if (name.includes('DARKNESS')) return 'DARKNESS';
    if (name.includes('METAL')) return 'METAL';
    if (name.includes('FAIRY')) return 'FAIRY';
    if (name.includes('DRAGON')) return 'DRAGON';
    return 'COLORLESS';
  }

  getCardImageUrl(cardId: string): string {
    if (!cardId) return 'https://images.pokemontcg.io/xy1/130.png';
    const allCards = this.tcgService.cards();
    const found = allCards.find(c => c.id === cardId);
    if (found) {
      return found.images?.large || found.images?.small || '';
    }
    // Try mock fallback
    const mock = CARDS['e_' + cardId.toLowerCase()] || CARDS[cardId.toLowerCase()] || CARDS[cardId];
    if (mock && mock.img) {
      return mock.img;
    }
    // Parse format (e.g. xy1-108)
    const parts = cardId.split('-');
    if (parts.length === 2) {
      return `https://images.pokemontcg.io/${parts[0]}/${parts[1]}.png`;
    }
    return 'https://images.pokemontcg.io/xy1/130.png';
  }

  getCardImage(card: PokemonTcgCard): string {
    return this.getCardImageUrl(card.id);
  }

  zoomCard(url: string): void {
    if (!url) return;
    this.zoomedCardUrl.set(url);
  }

  closeZoom(): void {
    this.zoomedCardUrl.set(null);
  }

  isSelected(index: number): boolean {
    return this.selectedHandIndex() === index || this.draggedCardIndex() === index;
  }

  // ── Drag & Drop Handlers ──────────────────────────────────────────────────

  onDragStart(e: DragEvent, card: PokemonTcgCard, index: number): void {
    // Only allow drag if it's my turn, OR if we need to promote active (active is null)
    if (!this.isMyTurn() && this.me()?.active) return;
    
    this.draggedCard.set(card);
    this.draggedCardIndex.set(index);
    if (e.dataTransfer) {
      e.dataTransfer.effectAllowed = 'move';
      e.dataTransfer.setData('text/plain', card.id);
      
      // Create a ghost image so it doesn't look like dragging the whole fan container
      const target = ((e.target as HTMLElement).closest('.fan-card') || e.target) as HTMLElement;
      if (target) {
         e.dataTransfer.setDragImage(target, target.offsetWidth / 2, target.offsetHeight / 2);
      }
    }
  }

  onDragOver(e: DragEvent): void {
    e.preventDefault();
    if (e.dataTransfer) {
      e.dataTransfer.dropEffect = 'move';
    }
  }

  onDropField(e: DragEvent): void {
    e.preventDefault();
    e.stopPropagation(); // Avoid triggering multiple field drop event loops
    const card = this.draggedCard();
    if (!card) return;

    if (card.supertype === 'Trainer') {
      const type = card.subtypes.includes('Supporter') ? 'SUPPORTER' 
                 : card.subtypes.includes('Stadium') ? 'STADIUM' 
                 : card.subtypes.includes('Pokémon Tool') ? 'POKEMON_TOOL' : 'ITEM';
      
      this.sendAction({
        type: 'PLAY_TRAINER',
        cardId: card.id,
        trainerType: type
      });
    } else if (card.supertype === 'Pokémon' && card.subtypes.includes('Basic')) {
      this.sendAction({ type: 'PLACE_BASIC_POKEMON', cardId: card.id });
    }
    
    this.draggedCard.set(null);
    this.draggedCardIndex.set(null);
  }

  onDropPokemon(e: DragEvent, targetType: 'active' | 'bench', targetIndex: number | null): void {
    e.preventDefault();
    e.stopPropagation(); // Avoid triggering field drop
    
    const card = this.draggedCard();
    if (!card) return;

    if (card.supertype === 'Trainer' && !card.subtypes.includes('Stadium')) {
      const type = card.subtypes.includes('Supporter') ? 'SUPPORTER' 
                 : card.subtypes.includes('Pokémon Tool') ? 'POKEMON_TOOL' : 'ITEM';
      this.sendAction({
        type: 'PLAY_TRAINER',
        cardId: card.id,
        trainerType: type,
        targetIndex: targetType === 'active' ? -1 : targetIndex
      });
    } else if (card.supertype === 'Energy') {
      const energyType = this.getEnergyType(card);
      this.sendAction({
        type: 'ATTACH_ENERGY',
        cardId: card.id,
        energyType: energyType as PokemonType,
        targetIndex: targetType === 'active' ? null : targetIndex
      });
    } else if (card.supertype === 'Pokémon' && (card.subtypes.includes('Stage 1') || card.subtypes.includes('Stage 2') || card.subtypes.includes('MEGA'))) {
      this.sendAction({
        type: 'EVOLVE',
        cardId: card.id,
        targetIndex: targetType === 'active' ? null : targetIndex
      });
    } else if (targetType === 'bench' && card.supertype === 'Pokémon' && card.subtypes.includes('Basic')) {
       this.sendAction({ type: 'PLACE_BASIC_POKEMON', cardId: card.id });
    }

    this.draggedCard.set(null);
    this.draggedCardIndex.set(null);
  }

  promoteActive(benchIndex: number): void {
    this.sendAction({
      type: 'PROMOTE_ACTIVE',
      sourceIndex: benchIndex
    });
  }

  // ── UI Handlers ───────────────────────────────────────────────────────────

  openCardMenu(e: MouseEvent, pokemon: any, index: number | null = null): void {
    e.stopPropagation();
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    this.menu.set({
      x: rect.right,
      y: rect.top + rect.height / 2,
      pokemon,
      index,
    });
  }

  closeMenu(): void {
    this.menu.set(null);
    this.selectedHandIndex.set(null);
    this.selectedHandCard.set(null);
  }

  sendChat(text: string): void {
    this.wsService.sendChatMessage(this.matchId, text);
  }

  onLeave(): void {
    this.wsService.disconnect();
    this.router.navigate(['/lobby']);
  }

  getKindStyles(kind: string): { color: string; label: string } {
    const map: Record<string, { color: string; label: string }> = {
      attack: { color: 'var(--p-red)', label: 'ATQ' },
      energy: { color: 'var(--p-yellow)', label: 'ENG' },
      discard: { color: '#9aa9c7', label: 'DSC' },
      prize: { color: '#5ad27a', label: 'PRZ' },
      ko: { color: '#fff', label: 'KO!' },
      draw: { color: '#7aa3ff', label: 'DRW' },
      bench: { color: '#c87bff', label: 'BNC' },
    };
    return map[kind] ?? map['attack'];
  }

  /** Helper: convierte SpecialCondition[] a clases CSS */
  getStatusClasses(conditions: SpecialCondition[]): Record<string, boolean> {
    return {
      'status--asleep': conditions.includes('ASLEEP'),
      'status--confused': conditions.includes('CONFUSED'),
      'status--paralyzed': conditions.includes('PARALYZED'),
      'status--burned': conditions.includes('BURNED'),
      'status--poisoned': conditions.includes('POISONED'),
    };
  }

  // ── Intro Sequence Runner ─────────────────────────────────────────────────
  checkAndStartIntro(): void {
    const isNew = this.turn().number === 1 && !this.me()?.active && !this.opp()?.active;
    if (isNew) {
      this.animationStage.set('coin-flip');
      this.runIntroSequence();
    } else {
      this.animationStage.set('complete');
    }
  }

  private runIntroSequence(): void {
    const iStart = this.isMyTurn();
    this.coinFlipResult.set(iStart ? 'heads' : 'tails');

    setTimeout(() => {
      this.animationStage.set('dealing-hand');
      this.dealHands();
    }, 2800);
  }

  private dealHands(): void {
    const totalCards = this.me()?.hand.length ?? 7;
    const oppCards = this.opp()?.handCount ?? 7;
    
    let dealt = 0;
    const interval = setInterval(() => {
      if (dealt < totalCards) {
        this.animatedHandCount.update(n => n + 1);
      }
      if (dealt < oppCards) {
        this.animatedOppHandCount.update(n => n + 1);
      }
      dealt++;
      if (dealt >= Math.max(totalCards, oppCards)) {
        clearInterval(interval);
        setTimeout(() => {
          this.animationStage.set('dealing-prizes');
          this.dealPrizes();
        }, 600);
      }
    }, 180);
  }

  private dealPrizes(): void {
    const totalPrizes = this.me()?.prizes.length ?? 6;
    const oppPrizes = this.opp()?.prizes.length ?? 6;

    let dealt = 0;
    const interval = setInterval(() => {
      if (dealt < totalPrizes) {
        this.animatedPrizeCount.update(n => n + 1);
      }
      if (dealt < oppPrizes) {
        this.animatedOppPrizeCount.update(n => n + 1);
      }
      dealt++;
      if (dealt >= Math.max(totalPrizes, oppPrizes)) {
        clearInterval(interval);
        setTimeout(() => {
          this.animationStage.set('complete');
        }, 800);
      }
    }, 200);
  }

  runAttackCoinFlips(flips: boolean[]): void {
    if (this.isAttackCoinFlipping()) return;
    this.isAttackCoinFlipping.set(true);
    this.attackCoinFlips.set(flips);
    this.animateFlip(0);
  }

  private animateFlip(index: number): void {
    const flips = this.attackCoinFlips();
    if (index >= flips.length) {
      setTimeout(() => {
        this.isAttackCoinFlipping.set(false);
        this.currentAttackFlipIndex.set(-1);
        this.currentAttackFlipResult.set(null);
      }, 1500);
      return;
    }

    const isSmokescreen = this.store.hadPrecisionBaja() && index === 0;
    this.isSmokescreenFlip.set(isSmokescreen);
    this.currentAttackFlipIndex.set(index);
    this.currentAttackFlipResult.set(null);

    if (isSmokescreen) {
      this.coinFlipSubText.set('Chequeo de Humo (Precisión Baja): Cruz falla el ataque');
    } else {
      this.coinFlipSubText.set(
        this.store.hadPrecisionBaja()
          ? `Lanzando moneda para el ataque/entrenador (Moneda #${index})...`
          : `Lanzando moneda para el ataque/entrenador (Moneda #${index + 1})...`
      );
    }

    setTimeout(() => {
      const result = flips[index] ? 'heads' : 'tails';
      this.currentAttackFlipResult.set(result);
      
      if (isSmokescreen) {
        if (result === 'heads') {
          this.coinFlipSubText.set('¡Cara! Evitaste el humo. El ataque procede...');
        } else {
          this.coinFlipSubText.set('¡Cruz! El ataque ha fallado por el humo.');
        }
      } else {
        const flipNum = this.store.hadPrecisionBaja() ? index : index + 1;
        this.coinFlipSubText.set(
          flips[index] 
            ? `¡Cara! (Moneda #${flipNum} exitosa)` 
            : `¡Cruz! (Moneda #${flipNum} fallida)`
        );
      }

      setTimeout(() => {
        if (isSmokescreen && result === 'tails') {
          this.isAttackCoinFlipping.set(false);
          this.currentAttackFlipIndex.set(-1);
          this.currentAttackFlipResult.set(null);
          return;
        }
        this.animateFlip(index + 1);
      }, 2000);
    }, 50);
  }
}
