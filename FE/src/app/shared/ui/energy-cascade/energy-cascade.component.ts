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
            [style.top]="direction === 'down' ? getOffset($index) : 'auto'"
            [style.bottom]="direction === 'up' ? getOffset($index) : 'auto'"
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

  getStyles() {
    return {
      top: this.direction === 'down' ? '72%' : 'auto',
      bottom: this.direction === 'up' ? '72%' : 'auto',
      transform: 'translateX(-50%)',
      width: (this.cardW * 0.85) + 'px',
      'pointer-events': 'none',
      'z-index': '-1'
    };
  }

  getOffset(i: number): string {
    const step = Math.min(20, this.cardW * 0.16);
    return ((i + 1) * step) + 'px';
  }

  getCard(id: string) {
    return CARDS['e_' + id] || CARDS[id];
  }
}
