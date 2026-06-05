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
  from: 'me' | 'opp';
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
  readonly pendingSelection = computed(() => {
    const sel = this.store.pendingSelection();
    return sel && this.isMyTurn() ? sel : null;
  });
  readonly isFinished = computed(() => {
    const phase = this.store.phase();
    // Only show game over when state is loaded AND phase is explicitly FINISHED
    return this.store.isLoaded() && phase === 'FINISHED';
  });
  readonly gameResult = computed(() => {
    if (!this.isFinished()) return null;
    // If the game ended and it's "my turn" (activePlayerIndex===0), I won
    // This is a simplification — the backend should send explicit winner info
    return this.store.isMyTurn() ? 'VICTORIA' : 'DERROTA';
  });

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

  readonly quickChat = QUICK_CHAT;
  Math = Math;

  // ── Intro Animations ──────────────────────────────────────────────────────
  readonly animationStage = signal<'idle' | 'coin-flip' | 'dealing-hand' | 'dealing-prizes' | 'complete'>('idle');
  readonly coinFlipResult = signal<'heads' | 'tails' | null>(null);
  readonly animatedHandCount = signal(0);
  readonly animatedOppHandCount = signal(0);
  readonly animatedPrizeCount = signal(0);
  readonly animatedOppPrizeCount = signal(0);

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
          from: msg.sender === username ? 'me' : 'opp',
          text: msg.message,
          t: new Date(msg.timestamp).toTimeString().slice(0, 5)
        }));
        this.chat.set(mapped as ChatEntry[]);
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
          from: msg.sender === username ? 'me' : 'opp',
          text: msg.message,
          t: new Date().toTimeString().slice(0, 5)
        };
        this.chat.update(c => [...c, mapped]);
      });
      this.wsSub.add(chatSub);
    } catch (err) {
      this.connectionError.set('No se pudo conectar. ¿Estás autenticado?');
      this.isConnecting.set(false);
    }
  }

  // ── Acciones del jugador ──────────────────────────────────────────────────

  attack(attackIndex: number): void {
    if (!this.canAttack()) return;
    this.sendAction({ type: 'DECLARE_ATTACK', attackIndex });
    this.closeMenu();
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

  endTurn(): void {
    if (!this.canEndTurn()) return;
    this.sendAction({ type: 'END_TURN' });
  }

  useAbility(cardId: string): void {
    if (!this.isMyTurn()) return;
    this.sendAction({ type: 'USE_ABILITY', cardId });
    this.closeMenu();
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

  playCard(card: PokemonTcgCard, index: number): void {
    if (!this.isMyTurn()) return;
    
    // Si es Pokémon Básico, bajar a la banca directamente
    if (card.supertype === 'Pokémon' && card.subtypes.includes('Basic')) {
      this.sendAction({ type: 'PLACE_BASIC_POKEMON', cardId: card.id });
      this.selectedHandIndex.set(null);
      this.selectedHandCard.set(null);
      return;
    }
    
    // Si es Energía o Evolución, requiere seleccionar un objetivo
    if (card.supertype === 'Energy' || card.subtypes.includes('Stage 1') || card.subtypes.includes('Stage 2')) {
      if (this.selectedHandIndex() === index) {
        this.selectedHandIndex.set(null);
        this.selectedHandCard.set(null);
      } else {
        this.selectedHandIndex.set(index);
        this.selectedHandCard.set(card);
      }
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
    } else if (card.supertype === 'Pokémon' && (card.subtypes.includes('Stage 1') || card.subtypes.includes('Stage 2'))) {
      this.sendAction({
        type: 'EVOLVE',
        cardId: card.id,
        targetIndex: targetType === 'active' ? null : targetIndex
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
                 : card.subtypes.includes('Pokémon Tool') ? 'TOOL' : 'ITEM';
      
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

    if (card.supertype === 'Energy') {
      const energyType = this.getEnergyType(card);
      this.sendAction({
        type: 'ATTACH_ENERGY',
        cardId: card.id,
        energyType: energyType as PokemonType,
        targetIndex: targetType === 'active' ? null : targetIndex
      });
    } else if (card.supertype === 'Pokémon' && (card.subtypes.includes('Stage 1') || card.subtypes.includes('Stage 2'))) {
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

  openCardMenu(e: MouseEvent, pokemon: any): void {
    e.stopPropagation();
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    this.menu.set({
      x: rect.right,
      y: rect.top + rect.height / 2,
      pokemon,
    });
  }

  closeMenu(): void {
    this.menu.set(null);
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
}
