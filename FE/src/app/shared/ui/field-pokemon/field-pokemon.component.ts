import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EnergyCascadeComponent } from '../energy-cascade/energy-cascade.component';
import { DamageTokensComponent } from '../damage-tokens/damage-tokens.component';
import { CARDS } from '../../data/cards.mock';

@Component({
  selector: 'app-field-pokemon',
  standalone: true,
  imports: [CommonModule, EnergyCascadeComponent, DamageTokensComponent],
  template: `
    <div
      class="relative bench-slot"
      [style.width.px]="width"
      [style.height.px]="width * 1.4"
      [style.cursor]="'pointer'"
    >
      @if (glow && card) {
        <div class="glow" [ngClass]="card.type"></div>
      }

      @if (card) {
        <img
          [src]="card.img"
          [alt]="card.name"
          class="card-img"
          [style.transform]="'rotate(' + rot + 'deg)'"
          style="width: 100%; height: 100%; transition: transform .35s cubic-bezier(.2,.9,.25,1.05); z-index: 2; position: relative;"
          draggable="false"
        />
      }

      <app-energy-cascade [energies]="energies" [direction]="direction" [cardW]="width"></app-energy-cascade>
      <app-damage-tokens [damage]="damage" [status]="status"></app-damage-tokens>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FieldPokemonComponent {
  @Input({ required: true }) cardId!: string;
  @Input() energies: string[] = [];
  @Input() damage: number = 0;
  @Input() status: string = 'none';
  @Input() width: number = 140;
  @Input() glow: boolean = false;
  @Input() direction: 'up' | 'down' = 'down';

  get card() {
    return CARDS[this.cardId];
  }

  get rot(): number {
    if (this.direction === 'up') return 180;
    if (this.status === 'asleep') return 90;
    if (this.status === 'confused') return 180;
    return 0;
  }
}

