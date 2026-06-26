import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  inject,
  signal,
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
          <h2 class="modal-title">{{ title }}</h2>
          <span class="source-badge">{{ sourceLabel }}</span>
        </div>

        <!-- Subtitle -->
        <p class="modal-subtitle">
          Seleccionadas: {{ selectedIndices().size }} / {{ maxSelections }}
        </p>

        <!-- Card grid -->
        <div class="card-grid">
          @for (id of cardIds; track $index) {
            <div
              class="card-slot"
              [class.selected]="isSelected($index)"
              [class.disabled]="!isSelected($index) && selectedIndices().size >= maxSelections"
              (click)="toggleCard($index)"
            >
              <img
                [src]="getCardImageUrl(id)"
                [alt]="id"
                class="card-img"
                draggable="false"
              />
            </div>
          }
        </div>

        <!-- Footer -->
        <div class="modal-footer">
          <button class="btn-cancel" (click)="onCancel()">Cancelar</button>
          <button
            class="btn-confirm"
            [disabled]="selectedIndices().size === 0 && maxSelections > 0"
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
      max-height: 80vh;
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
      color: rgba(255, 255, 255, .55);
      margin: 4px 0 16px;
    }

    /* ── Card grid ─────────────────────────────────────── */
    .card-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
      gap: 16px;
      overflow-y: auto;
      max-height: 50vh;
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

    .card-slot:hover {
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

  /* ── Outputs ───────────────────────────────────────── */
  @Output() confirm = new EventEmitter<string[]>();
  @Output() cancel = new EventEmitter<void>();

  /* ── Internal state ────────────────────────────────── */
  selectedIndices = signal<Set<number>>(new Set());

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

  onConfirm(): void {
    const selectedIds = Array.from(this.selectedIndices()).map(idx => this.cardIds[idx]);
    this.confirm.emit(selectedIds);
    this.selectedIndices.set(new Set());
  }

  onCancel(): void {
    this.cancel.emit();
    this.selectedIndices.set(new Set());
  }

  getCardImageUrl(cardId: string): string {
    // 1. Try the TCG service cache
    const allCards = this.tcgService.cards();
    const found = allCards.find(c => c.id === cardId);
    if (found) {
      return found.images?.small ?? found.images?.large ?? '';
    }

    // 2. Parse ID format  e.g. "xy1-108" → https://images.pokemontcg.io/xy1/108.png
    const parts = cardId.split('-');
    if (parts.length === 2) {
      return `https://images.pokemontcg.io/${parts[0]}/${parts[1]}.png`;
    }

    // 3. Fallback
    return 'https://images.pokemontcg.io/xy1/130.png';
  }
}
