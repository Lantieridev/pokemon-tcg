import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EnergyCascadeComponent } from '../energy-cascade/energy-cascade.component';
import { DamageTokensComponent } from '../damage-tokens/damage-tokens.component';
import { PokemonTcgService } from '../../../core/services/pokemon-tcg.service';
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
  private tcgService = inject(PokemonTcgService);

  @Input({ required: true }) cardId!: string;
  @Input() energies: string[] = [];
  @Input() damage: number = 0;
  @Input() status: string = 'none';
  @Input() width: number = 140;
  @Input() glow: boolean = false;
  @Input() direction: 'up' | 'down' = 'down';

  get card() {
    const allCards = this.tcgService.cards();
    const found = allCards.find(c => c.id === this.cardId);
    if (found) {
      return {
        id: found.id,
        name: found.name,
        type: found.types?.[0]?.toLowerCase() ?? 'colorless',
        img: found.images?.small ?? found.images?.large ?? ''
      };
    }

    // Try mock fallback
    const mock = CARDS['e_' + this.cardId.toLowerCase()] || CARDS[this.cardId.toLowerCase()] || CARDS[this.cardId];
    if (mock) {
      return {
        id: this.cardId,
        name: mock.name,
        type: mock.type,
        img: mock.img
      };
    }

    // Parse format (e.g. xy1-108)
    const parts = this.cardId.split('-');
    if (parts.length === 2) {
      return {
        id: this.cardId,
        name: 'Pokémon',
        type: 'colorless',
        img: `https://images.pokemontcg.io/${parts[0]}/${parts[1]}.png`
      };
    }

    return {
      id: this.cardId,
      name: 'Pokémon',
      type: 'colorless',
      img: 'https://images.pokemontcg.io/xy1/130.png'
    };
  }

  get rot(): number {
    if (this.status === 'asleep') return 90;
    if (this.status === 'confused') return 180;
    return 0;
  }
}

