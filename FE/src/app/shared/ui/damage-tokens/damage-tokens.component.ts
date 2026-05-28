import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-damage-tokens',
  standalone: true,
  template: `
    @if (damage > 0 || (status && status !== 'none')) {
      <div class="absolute inset-0 pointer-events-none" style="z-index: 5">
        
        @if (fifties > 0) {
          <div class="dmg-token x10"
               style="left:8%; top:42%; width:38px; height:38px; font-size:13px; background:radial-gradient(circle at 35% 30%, #fff 0%, #ffe5a8 18%, #e3a13a 50%, #6b4310 100%)">
            {{ fifties * 50 }}
          </div>
        }

        @for (item of remainderArray; track $index) {
          <div class="dmg-token"
               [style.right]="(4 + ($index % 2) * 16) + '%'"
               [style.top]="(48 + getFloor($index / 2) * 18) + '%'">
            10
          </div>
        }

        @if (damage >= 10) {
          <div class="absolute font-chunky font-bold text-white text-[12px] px-[9px] py-[2px] rounded-lg"
               style="bottom: 4%; left: 50%; transform: translateX(-50%); background: rgba(0,0,0,.85); border: 2px solid #fff; text-shadow: 0 1px 2px rgba(0,0,0,.7); box-shadow: 0 4px 0 #1a1010;">
            {{ damage }}
          </div>
        }

        @if (status && status !== 'none' && status !== 'asleep' && status !== 'confused') {
          <span class="status-token {{ status }}"
                style="left: 50%; top: -10px; transform: translateX(-50%)">
            {{ statusLabel[status] }}
          </span>
        }

        @if (status === 'asleep' || status === 'confused') {
          <span class="status-token {{ status }}"
                style="right: -12px; top: -10px">
            {{ statusLabel[status] }}
          </span>
        }
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DamageTokensComponent {
  @Input() damage: number = 0;
  @Input() status: string = 'none';

  statusLabel: Record<string, string> = {
    none: '', asleep: 'Dormido', confused: 'Confundido',
    burned: 'Quemado', poisoned: 'Envenenado', paralyzed: 'Paralizado'
  };

  get fifties() { return Math.floor(Math.floor(this.damage / 10) / 5); }
  get remainderArray() { return new Array(Math.min(Math.floor(this.damage / 10) - this.fifties * 5, 4)); }
  getFloor(val: number) { return Math.floor(val); }
}
