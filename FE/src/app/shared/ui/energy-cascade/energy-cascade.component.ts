import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { PokemonTcgService } from '../../../core/services/pokemon-tcg.service';
import { CARDS } from '../../data/cards.mock';

@Component({
  selector: 'app-energy-cascade',
  standalone: true,
  template: `
    @if (energies.length) {
      <div class="absolute left-1/2" [style]="getStyles()">
        @for (e of energies; track $index) {
          <img
            [src]="getCard(e).img"
            class="card-img absolute"
            [style.width]="'100%'"
            [style.left]="'0'"
            [style.top]="getTopOffset($index)"
            [style.z-index]="-($index + 1)"
            style="filter: drop-shadow(0 3px 6px rgba(0,0,0,.65))"
          />
        }
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EnergyCascadeComponent {
  private tcgService = inject(PokemonTcgService);

  @Input() energies: string[] = [];
  @Input() direction: 'up' | 'down' = 'down';
  @Input() cardW: number = 140;

  // Container anchors to the top of the host card always.
  // For 'up': energies go upward (negative top = sticking out above the card top border).
  // For 'down': energies go downward (positive top starting from bottom edge).
  getStyles() {
    return {
      top: '0',
      transform: 'translateX(-50%)',
      width: (this.cardW * 0.95) + 'px',
      'pointer-events': 'none',
      'z-index': '-1'
    };
  }

  getTopOffset(i: number): string {
    const step = Math.max(22, this.cardW * 0.18);
    const cardH = this.cardW * 1.4;
    if (this.direction === 'up') {
      // Stick out from the TOP border: negative values push above the card
      return '-' + ((i + 1) * step) + 'px';
    } else {
      // Stick out from the BOTTOM border: start at bottom of card, go downward
      return (cardH + i * step) + 'px';
    }
  }

  getCard(id: string) {
    if (!id) {
      return { img: 'https://images.pokemontcg.io/xy1/130.png' };
    }
    
    // Check in live card registry loaded from pokemontcg.io API
    const found = this.tcgService.cards().find(c => c.id === id);
    if (found) {
      return {
        id: found.id,
        name: found.name,
        img: found.images?.small ?? found.images?.large ?? ''
      };
    }

    // Direct mapping for standard energy types to XY1 set energy card images
    const energyMap: Record<string, { name: string, img: string }> = {
      grass:     { name: 'Grass Energy',     img: 'https://images.pokemontcg.io/xy1/132.png' },
      fire:      { name: 'Fire Energy',      img: 'https://images.pokemontcg.io/xy1/133.png' },
      water:     { name: 'Water Energy',     img: 'https://images.pokemontcg.io/xy1/134.png' },
      lightning: { name: 'Lightning Energy', img: 'https://images.pokemontcg.io/xy1/135.png' },
      psychic:   { name: 'Psychic Energy',   img: 'https://images.pokemontcg.io/xy1/136.png' },
      fighting:  { name: 'Fighting Energy',  img: 'https://images.pokemontcg.io/xy1/137.png' },
      darkness:  { name: 'Darkness Energy',  img: 'https://images.pokemontcg.io/xy1/138.png' },
      metal:     { name: 'Metal Energy',     img: 'https://images.pokemontcg.io/xy1/139.png' },
      fairy:     { name: 'Fairy Energy',     img: 'https://images.pokemontcg.io/xy1/140.png' },
      colorless: { name: 'Double Colorless', img: 'https://images.pokemontcg.io/xy1/130.png' }
    };

    const typeLower = id.toLowerCase().replace('e_', '');
    if (energyMap[typeLower]) {
      return {
        id,
        name: energyMap[typeLower].name,
        img: energyMap[typeLower].img
      };
    }

    // Try mock fallback
    const mock = CARDS['e_' + id.toLowerCase()] || CARDS[id.toLowerCase()] || CARDS[id];
    if (mock) return mock;

    // Parse format (e.g. xy1-108)
    const parts = id.split('-');
    if (parts.length === 2) {
      return {
        id,
        name: 'Energy',
        img: `https://images.pokemontcg.io/${parts[0]}/${parts[1]}.png`
      };
    }

    // Ultimate fallback
    return {
      id,
      name: 'Energy',
      img: 'https://images.pokemontcg.io/xy1/130.png'
    };
  }
}
