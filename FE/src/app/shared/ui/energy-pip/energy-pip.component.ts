import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { TYPE_COLORS } from '../../data/cards.mock';

const TYPE_GLYPH: Record<string, string> = {
  fire:      '🔥',
  water:     '💧',
  grass:     '🍃',
  lightning: '⚡',
  psychic:   '👁',
  fighting:  '✊',
  darkness:  '🌑',
  metal:     '🛡',
  fairy:     '🎀',
  dragon:    '🐉',
  colorless: '✦',
};

@Component({
  selector: 'app-energy-pip',
  standalone: true,
  template: `
    <div [style.width.px]="size" [style.height.px]="size" 
         [style.background]="color"
         class="rounded-full flex items-center justify-center flex-shrink-0 shadow-sm border border-black/20"
         [style.boxShadow]="glow ? '0 0 8px ' + color : 'none'">
      <span [style.fontSize.px]="size * 0.6" class="leading-none select-none text-white drop-shadow-md">
        {{ glyph }}
      </span>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EnergyPipComponent {
  @Input({ required: true }) type!: string;
  @Input() size: number = 20;
  @Input() glow: boolean = false;

  get color(): string {
    const t = this.type?.toLowerCase();
    return TYPE_COLORS[t]?.hex || '#cfd6e4';
  }

  get glyph(): string {
    const t = this.type?.toLowerCase();
    return TYPE_GLYPH[t] || '✦';
  }
}
