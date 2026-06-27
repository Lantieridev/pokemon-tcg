import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { PokemonTcgService } from '../../../core/services/pokemon-tcg.service';

@Component({
  selector: 'app-card-selection-modal',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- Full-screen overlay -->
    <div class="overlay" (click)="onCancel()">
      <!-- Modal panel — stop click propagation so clicking inside doesn't close -->
      <div class="modal-panel" (click)="$event.stopPropagation()">

        <!-- Header -->
        <div class="modal-header">
          <h2 class="modal-title">{{ displayTitle }}</h2>
          <span class="source-badge">{{ sourceLabel }}</span>
        </div>

        <!-- Subtitle -->
        <p class="modal-subtitle">
          {{ displaySubtitle }}
        </p>

        <!-- Card grid -->
        <div class="card-grid">
          @for (id of cardIds; track $index) {
            <!-- 1. CURSED_DROP -->
            @if (sourceEffect === 'CURSED_DROP') {
              <div class="card-slot counter-slot" [class.has-counters]="getCounter(id) > 0">
                <img
                  [src]="getCardImageUrl(id)"
                  [alt]="cleanCardId(id)"
                  class="card-img"
                  draggable="false"
                />
                <div class="counter-controls">
                  <button
                    class="counter-btn"
                    [disabled]="getCounter(id) <= 0"
                    (click)="decrementCounter(id)"
                  >-</button>
                  <span class="counter-value">{{ getCounter(id) }}</span>
                  <button
                    class="counter-btn"
                    [disabled]="totalDistributed() >= maxSelections"
                    (click)="incrementCounter(id)"
                  >+</button>
                </div>
              </div>
            }
            <!-- 2. EAR_INFLUENCE -->
            @else if (sourceEffect === 'EAR_INFLUENCE') {
              <div
                class="card-slot pair-slot"
                [class.active-source]="selectedSourceId() === id"
                (click)="handleEarInfluenceClick(id)"
              >
                <img
                  [src]="getCardImageUrl(id)"
                  [alt]="cleanCardId(id)"
                  class="card-img"
                  draggable="false"
                />
                @if (selectedSourceId() === id) {
                  <div class="source-label-overlay">Origen</div>
                }
              </div>
            }
            <!-- 3. DEFAULT SELECTION -->
            @else {
              <div
                class="card-slot"
                [class.selected]="isSelected($index)"
                [class.disabled]="!isSelected($index) && selectedIndices().size >= maxSelections"
                (click)="toggleCard($index)"
              >
                <img
                  [src]="getCardImageUrl(id)"
                  [alt]="cleanCardId(id)"
                  class="card-img"
                  draggable="false"
                />
              </div>
            }
          }
        </div>

        <!-- Ear Influence Moves List -->
        @if (sourceEffect === 'EAR_INFLUENCE' && moves().length > 0) {
          <div class="moves-container">
            <div class="moves-header">
              <span class="moves-title">Movimientos a realizar:</span>
              <button class="btn-reset-moves" (click)="resetMoves()">Reiniciar</button>
            </div>
            <div class="moves-list">
              @for (m of moves(); track $index) {
                <div class="move-item">
                  <span class="move-index">#{{ $index + 1 }}</span>
                  <span class="move-text">{{ getCardName(m.sourceId) }} ➔ {{ getCardName(m.targetId) }}</span>
                  <button class="btn-delete-move" (click)="deleteMove($index)">✕</button>
                </div>
              }
            </div>
          </div>
        }

        <!-- Footer -->
        <div class="modal-footer">
          <button class="btn-cancel" (click)="onCancel()">Cancelar</button>
          <button
            class="btn-confirm"
            [disabled]="
              sourceEffect === 'CURSED_DROP' ? totalDistributed() === 0 :
              sourceEffect === 'EAR_INFLUENCE' ? moves().length === 0 :
              selectedIndices().size === 0 && maxSelections > 0
            "
            (click)="onConfirm()"
          >
            Confirmar
          </button>
        </div>

      </div>
    </div>
  `,
  styles: [`
    /* ── Overlay ───────────────────────────────────────── */
    .overlay {
      position: fixed;
      inset: 0;
      z-index: 55;
      background: rgba(0, 0, 0, .85);
      backdrop-filter: blur(8px);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    /* ── Panel ─────────────────────────────────────────── */
    .modal-panel {
      background: linear-gradient(180deg, rgba(28, 42, 68, .95), rgba(13, 21, 33, .98));
      border: 2px solid rgba(255, 203, 5, .3);
      border-radius: 24px;
      padding: 24px;
      box-shadow: 0 24px 80px rgba(0, 0, 0, .8);
      max-width: 720px;
      width: 92%;
      max-height: 85vh;
      display: flex;
      flex-direction: column;
      animation: modalIn .25s ease-out;
    }

    @keyframes modalIn {
      from { opacity: 0; transform: translateY(24px) scale(.96); }
      to   { opacity: 1; transform: translateY(0) scale(1); }
    }

    /* ── Header ────────────────────────────────────────── */
    .modal-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 4px;
    }

    .modal-title {
      font-family: 'Russo One', sans-serif;
      font-size: 1.25rem;
      color: #ffcb05;
      margin: 0;
      text-shadow: 0 0 12px rgba(255, 203, 5, .35);
    }

    .source-badge {
      font-family: 'Russo One', sans-serif;
      font-size: .65rem;
      letter-spacing: 1px;
      text-transform: uppercase;
      color: #0d1521;
      background: rgba(255, 203, 5, .85);
      padding: 3px 10px;
      border-radius: 6px;
    }

    /* ── Subtitle ──────────────────────────────────────── */
    .modal-subtitle {
      font-size: .85rem;
      color: rgba(255, 255, 255, .65);
      margin: 4px 0 16px;
      line-height: 1.4;
    }

    /* ── Card grid ─────────────────────────────────────── */
    .card-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
      gap: 16px;
      overflow-y: auto;
      max-height: 42vh;
      flex: 1;
      min-height: 0;
      padding: 10px;
      scrollbar-width: thin;
      scrollbar-color: rgba(255, 203, 5, 0.4) transparent;
    }

    .card-grid::-webkit-scrollbar {
      width: 6px;
    }

    .card-grid::-webkit-scrollbar-track {
      background: transparent;
    }

    .card-grid::-webkit-scrollbar-thumb {
      background: rgba(255, 203, 5, 0.3);
      border-radius: 4px;
    }

    .card-grid::-webkit-scrollbar-thumb:hover {
      background: rgba(255, 203, 5, 0.6);
    }

    .card-slot {
      border-radius: 12px;
      overflow: hidden;
      cursor: pointer;
      border: 3px solid transparent;
      transition: border .2s, box-shadow .2s, opacity .2s, transform .15s;
      background: rgba(0, 0, 0, 0.2);
    }

    .card-slot:hover:not(.counter-slot) {
      transform: scale(1.05);
    }

    .card-slot.selected {
      border: 3px solid #ffcb05;
      box-shadow: 0 0 16px rgba(255, 203, 5, 0.6);
      transform: scale(1.02);
    }

    .card-slot.disabled {
      opacity: 0.5;
      cursor: not-allowed;
      pointer-events: none;
    }

    .card-img {
      width: 100%;
      height: auto;
      display: block;
      border-radius: 8px;
    }

    /* ── Counter Controls (Cursed Drop) ────────────────── */
    .counter-slot {
      position: relative;
      display: flex;
      flex-direction: column;
    }
    .counter-slot.has-counters {
      border: 3px solid #ffcb05;
      box-shadow: 0 0 16px rgba(255, 203, 5, 0.4);
    }
    .counter-controls {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: rgba(13, 21, 33, 0.85);
      border-top: 1px solid rgba(255, 255, 255, 0.1);
      padding: 6px 8px;
      border-radius: 0 0 8px 8px;
      z-index: 2;
    }
    .counter-btn {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      border: none;
      background: #ffcb05;
      color: #0d1521;
      font-weight: bold;
      font-size: 1rem;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: filter 0.15s, opacity 0.15s;
    }
    .counter-btn:disabled {
      background: rgba(255, 255, 255, 0.15);
      color: rgba(255, 255, 255, 0.3);
      cursor: not-allowed;
    }
    .counter-value {
      font-family: 'Russo One', sans-serif;
      color: #ffcb05;
      font-size: 0.95rem;
    }

    /* ── Pair Slots (Ear Influence) ────────────────────── */
    .pair-slot {
      position: relative;
    }
    .pair-slot.active-source {
      border: 3px solid #ffcb05;
      box-shadow: 0 0 16px rgba(255, 203, 5, 0.6);
      transform: scale(1.03);
    }
    .source-label-overlay {
      position: absolute;
      inset: 0;
      background: rgba(255, 203, 5, 0.25);
      backdrop-filter: blur(2px);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-family: 'Russo One', sans-serif;
      font-size: 1rem;
      text-shadow: 0 2px 4px rgba(0,0,0,0.8);
      border-radius: 8px;
    }

    /* ── Moves Container (Ear Influence) ────────────────── */
    .moves-container {
      margin-top: 16px;
      background: rgba(0, 0, 0, 0.25);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 16px;
      padding: 12px 16px;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .moves-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .moves-title {
      font-family: 'Russo One', sans-serif;
      font-size: 0.85rem;
      color: #ffcb05;
    }
    .btn-reset-moves {
      background: transparent;
      border: 1px solid rgba(244, 67, 54, 0.4);
      color: #ef5350;
      padding: 3px 8px;
      border-radius: 6px;
      font-size: 0.75rem;
      cursor: pointer;
      transition: background 0.15s;
    }
    .btn-reset-moves:hover {
      background: rgba(244, 67, 54, 0.1);
    }
    .moves-list {
      max-height: 100px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 6px;
      scrollbar-width: thin;
    }
    .move-item {
      display: flex;
      align-items: center;
      gap: 10px;
      background: rgba(255, 255, 255, 0.03);
      padding: 6px 12px;
      border-radius: 8px;
      font-size: 0.8rem;
      color: rgba(255, 255, 255, 0.85);
    }
    .move-index {
      color: #ffcb05;
      font-family: 'Russo One', sans-serif;
    }
    .move-text {
      flex: 1;
    }
    .btn-delete-move {
      background: transparent;
      border: none;
      color: rgba(255,255,255,0.4);
      cursor: pointer;
      font-size: 0.85rem;
      transition: color 0.15s;
    }
    .btn-delete-move:hover {
      color: #ef5350;
    }

    /* ── Footer ────────────────────────────────────────── */
    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 16px;
    }

    .btn-cancel {
      font-family: 'Russo One', sans-serif;
      font-size: .85rem;
      background: transparent;
      color: rgba(255, 255, 255, .7);
      border: 1px solid rgba(255, 255, 255, .2);
      border-radius: 12px;
      padding: 10px 24px;
      cursor: pointer;
      transition: background .2s, color .2s;
    }

    .btn-cancel:hover {
      background: rgba(255, 255, 255, .08);
      color: #fff;
    }

    .btn-confirm {
      font-family: 'Russo One', sans-serif;
      font-size: .85rem;
      background: #ffcb05;
      color: #1a1010;
      border: none;
      border-radius: 12px;
      padding: 10px 24px;
      cursor: pointer;
      transition: filter .2s, transform .15s;
    }

    .btn-confirm:hover:not(:disabled) {
      filter: brightness(1.1);
      transform: translateY(-1px);
    }

    .btn-confirm:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  `],
})
export class CardSelectionModalComponent {
  private tcgService = inject(PokemonTcgService);

  /* ── Inputs ────────────────────────────────────────── */
  @Input() title: string = 'Selecciona cartas';
  @Input() cardIds: string[] = [];
  @Input() maxSelections: number = 1;
  @Input() sourceLabel: string = 'MAZO';
  @Input() sourceEffect: string = '';

  /* ── Outputs ───────────────────────────────────────── */
  @Output() confirm = new EventEmitter<string[]>();
  @Output() cancel = new EventEmitter<void>();

  /* ── Internal state ────────────────────────────────── */
  selectedIndices = signal<Set<number>>(new Set());

  // State for CURSED_DROP
  distributedCounters = signal<Record<string, number>>({});

  // State for EAR_INFLUENCE
  selectedSourceId = signal<string | null>(null);
  moves = signal<Array<{ sourceId: string; targetId: string }>>([]);

  /* ── Computeds ────────────────────────────────────── */

  totalDistributed = computed(() => {
    return Object.values(this.distributedCounters()).reduce((a, b) => a + b, 0);
  });

  get displayTitle(): string {
    if (this.sourceEffect === 'CURSED_DROP') {
      return 'Cursed Drop — Distribuye contadores';
    }
    if (this.sourceEffect === 'EAR_INFLUENCE') {
      return 'Ear Influence — Mueve contadores';
    }
    if (this.sourceEffect === 'FLASH_CLAW') {
      return 'Flash Claw — Descarta una carta';
    }
    if (this.sourceEffect === 'PUSH_DOWN') {
      return 'Push Down — Promueve un Pokémon';
    }
    return this.title;
  }

  get displaySubtitle(): string {
    if (this.sourceEffect === 'CURSED_DROP') {
      return `Distribuye hasta ${this.maxSelections} contadores de daño en cualquier combinación. Asignados: ${this.totalDistributed()} / ${this.maxSelections}`;
    }
    if (this.sourceEffect === 'EAR_INFLUENCE') {
      return `Selecciona un Pokémon con daño (Origen), luego selecciona otro (Destino). Puedes realizar varios movimientos.`;
    }
    if (this.sourceEffect === 'FLASH_CLAW') {
      return 'Choose a card to discard';
    }
    if (this.sourceEffect === 'PUSH_DOWN' || this.sourceEffect === 'PECK_OFF' || this.sourceEffect === 'DISCARD_OPPONENT_TOOL') {
      return "Choose a Tool Card from your opponent's Active Pokemon to discard";
    }
    return `Seleccionadas: ${this.selectedIndices().size} / ${this.maxSelections}`;
  }

  /* ── Methods ───────────────────────────────────────── */

  toggleCard(index: number): void {
    this.selectedIndices.update(prev => {
      const next = new Set(prev);
      if (next.has(index)) {
        next.delete(index);
      } else if (next.size < this.maxSelections) {
        next.add(index);
      }
      return next;
    });
  }

  isSelected(index: number): boolean {
    return this.selectedIndices().has(index);
  }

  // --- Cursed Drop Helpers ---
  getCounter(cardId: string): number {
    return this.distributedCounters()[cardId] || 0;
  }

  incrementCounter(cardId: string): void {
    if (this.totalDistributed() < this.maxSelections) {
      this.distributedCounters.update(prev => {
        const next = { ...prev };
        next[cardId] = (next[cardId] || 0) + 1;
        return next;
      });
    }
  }

  decrementCounter(cardId: string): void {
    if (this.getCounter(cardId) > 0) {
      this.distributedCounters.update(prev => {
        const next = { ...prev };
        next[cardId] = next[cardId] - 1;
        if (next[cardId] <= 0) {
          delete next[cardId];
        }
        return next;
      });
    }
  }

  // --- Ear Influence Helpers ---
  handleEarInfluenceClick(cardId: string): void {
    const src = this.selectedSourceId();
    if (!src) {
      this.selectedSourceId.set(cardId);
    } else if (src === cardId) {
      this.selectedSourceId.set(null);
    } else {
      this.moves.update(prev => [...prev, { sourceId: src, targetId: cardId }]);
      this.selectedSourceId.set(null);
    }
  }

  deleteMove(idx: number): void {
    this.moves.update(prev => prev.filter((_, i) => i !== idx));
  }

  resetMoves(): void {
    this.moves.set([]);
    this.selectedSourceId.set(null);
  }

  cleanCardId(cardId: string): string {
    if (cardId && cardId.includes(':')) {
      return cardId.split(':')[1];
    }
    return cardId;
  }

  getCardName(cardId: string): string {
    const cleanId = this.cleanCardId(cardId);
    const found = this.tcgService.cards().find(c => c.id === cleanId);
    return found ? found.name : cleanId;
  }

  // --- Confirmation / Cancellation ---

  onConfirm(): void {
    if (this.sourceEffect === 'CURSED_DROP') {
      const selectedIds: string[] = [];
      Object.entries(this.distributedCounters()).forEach(([cardId, count]) => {
        for (let i = 0; i < count; i++) {
          selectedIds.push(cardId);
        }
      });
      this.confirm.emit(selectedIds);
      this.distributedCounters.set({});
    } else if (this.sourceEffect === 'EAR_INFLUENCE') {
      const selectedIds: string[] = [];
      this.moves().forEach(m => {
        selectedIds.push(m.sourceId);
        selectedIds.push(m.targetId);
      });
      this.confirm.emit(selectedIds);
      this.resetMoves();
    } else {
      const selectedIds = Array.from(this.selectedIndices()).map(idx => this.cardIds[idx]);
      this.confirm.emit(selectedIds);
      this.selectedIndices.set(new Set());
    }
  }

  onCancel(): void {
    this.cancel.emit();
    this.selectedIndices.set(new Set());
    this.distributedCounters.set({});
    this.resetMoves();
  }

  getCardImageUrl(cardId: string): string {
    const cleanId = this.cleanCardId(cardId);
    // 1. Try the TCG service cache
    const allCards = this.tcgService.cards();
    const found = allCards.find(c => c.id === cleanId);
    if (found) {
      return found.images?.small ?? found.images?.large ?? '';
    }

    // 2. Parse ID format  e.g. "xy1-108" → https://images.pokemontcg.io/xy1/108.png
    const parts = cleanId.split('-');
    if (parts.length === 2) {
      return `https://images.pokemontcg.io/${parts[0]}/${parts[1]}.png`;
    }

    // 3. Fallback
    return 'https://images.pokemontcg.io/xy1/130.png';
  }
}
