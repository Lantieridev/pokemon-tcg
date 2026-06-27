import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EnergyTypeComponent, IconComponent } from '../ui/aurora-ui.components';
import { Router } from '@angular/router';

@Component({
  selector: 'aurora-deck-rail',
  standalone: true,
  imports: [CommonModule, EnergyTypeComponent, IconComponent],
  template: `
    <div class="deck-dock fu" style="animation-delay: .3s;">
      <div style="flex: 0 0 auto;">
        <div class="eyebrow" style="font-size: 10.5px;">Mazo activo</div>
        <div style="display: flex; align-items: center; gap: 12px; margin-top: 7px;">
          <div [style.font-family]="display" style="font-style: italic; font-size: 24px; line-height: 1; white-space: nowrap; flex: 0 0 auto;">{{ deckName || 'Cargando mazo...' }}</div>
          <span style="display: flex; gap: 5px; flex: 0 0 auto;">
            @for (type of energyTypes; track type) {
              <aurora-energy-type [type]="type" [size]="20"></aurora-energy-type>
            }
          </span>
        </div>
      </div>

      <div style="flex: 1; display: flex; justify-content: center;">
        <div class="dock-cards">
          @for (c of slots; track $index) {
            <div class="dock-card" [style.--r]="getR($index) + 'deg'" [style.--ty]="getTy($index) + 'px'" [style.z-index]="$index" [title]="c?.name || ''">
              @if (c?.img) {
                <img [src]="c.img" [alt]="c.name" (error)="c.img = null" width="62" height="86" />
              } @else {
                <div class="dock-ph"></div>
              }
            </div>
          }
        </div>
      </div>

      <div style="flex: 0 0 auto; display: flex; align-items: center; gap: 14px;">
        <span class="dock-count">{{ totalCards }} / 60 cartas</span>
        <button (click)="onEdit()" class="ghost-btn sm"><aurora-icon n="decks" [s]="16"></aurora-icon> Editar mazo</button>
      </div>
    </div>
  `
})
export class DeckRailComponent {
  @Input() deck: any[] = [];
  @Input() display: string = "'Instrument Serif',serif";
  @Input() deckName: string = '';
  @Input() totalCards: number = 0;
  @Input() energyTypes: string[] = [];
  @Input() deckId: number | null = null;

  private router = inject(Router);

  get slots() {
    return this.deck.length ? this.deck.slice(0, 6) : Array.from({ length: 6 });
  }

  getR(i: number) {
    const mid = (this.slots.length - 1) / 2;
    return (i - mid) * 6;
  }

  getTy(i: number) {
    const mid = (this.slots.length - 1) / 2;
    return Math.pow(i - mid, 2) * 1.8;
  }

  onEdit() {
    if (this.deckId !== null) {
      this.router.navigate(['/deck'], { queryParams: { edit: this.deckId } });
    } else {
      this.router.navigate(['/deck']);
    }
  }
}
