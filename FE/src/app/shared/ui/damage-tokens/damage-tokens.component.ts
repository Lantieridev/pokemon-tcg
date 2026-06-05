import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-damage-tokens',
  standalone: true,
  template: `
    @if (status && status !== 'none') {
      <div class="absolute inset-0 pointer-events-none" style="z-index: 5">
        
        @if (status !== 'asleep' && status !== 'confused') {
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
  @Input() status: string = 'none';

  statusLabel: Record<string, string> = {
    none: '', asleep: 'Dormido', confused: 'Confundido',
    burned: 'Quemado', poisoned: 'Envenenado', paralyzed: 'Paralizado'
  };
}
