import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
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
    return CARDS['e_' + id] || CARDS[id];
  }
}
