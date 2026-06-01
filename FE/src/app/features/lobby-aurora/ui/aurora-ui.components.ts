import { Component, Input, Output, EventEmitter, signal, HostListener, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'aurora-icon',
  standalone: true,
  imports: [CommonModule],
  template: `
    <svg [attr.width]="s" [attr.height]="s" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
      @switch (n) {
        @case ('home') { <path d="M3 10.5 12 3l9 7.5M5 9.5V21h14V9.5" /> }
        @case ('decks') { <rect x="3" y="3" width="13" height="18" rx="2" /><path d="M19 6.5q2 .6 1.4 2.8l-2.4 9" /> }
        @case ('shop') { <path d="M4 8h16l-1 12H5L4 8Zm4 0a4 4 0 0 1 8 0" /> }
        @case ('rank') { <path d="M5 21V10M12 21V4M19 21v-7" /> }
        @case ('profile') { <circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /> }
        @case ('sword') { <path d="M14 3h7v7M21 3l-9 9M3 21l5-1 4-4-4-4-4 4-1 5Z" /> }
        @case ('arrow') { <path d="M5 12h14M13 6l6 6-6 6" /> }
        @case ('fire') { <path d="M12 2c1 3 2.5 3.5 3.5 4.5A5 5 0 0 1 17 10a5 5 0 1 1-10 0c0-1.5.5-2.5 1.5-3.5C9.5 5.5 11 5 12 2z" /><path d="M12 10a2 2 0 1 0 0 4 2 2 0 0 0 0-4z" /> }
      }
    </svg>
  `
})
export class IconComponent {
  @Input() n: string = 'home';
  @Input() s: number = 22;
}

@Component({
  selector: 'aurora-logo',
  standalone: true,
  template: `
    <div style="display: flex; align-items: center; gap: 10px;">
      <div style="width: 26px; height: 26px; border-radius: 8px; position: relative; background: linear-gradient(140deg, var(--accent), var(--accent-dk)); display: flex; align-items: center; justify-content: center;">
        <div style="width: 11px; height: 11px; border-radius: 50%; border: 2.5px solid var(--on-accent); position: relative;">
          <span style="position: absolute; left: -5px; right: -5px; top: 50%; height: 2.5px; background: var(--on-accent); transform: translateY(-50%);"></span>
        </div>
      </div>
      <div style="font-family: var(--display); font-weight: 700; font-size: 15px; letter-spacing: .04em; color: var(--txt);">
        POKÉMON<span style="color: var(--mut); font-weight: 500;"> TCG</span>
      </div>
    </div>
  `
})
export class LogoComponent {}

@Component({
  selector: 'aurora-trainer-chip',
  standalone: true,
  template: `
    <div class="chip">
      <div class="avatar ring">{{ initial }}</div>
      <div style="line-height: 1.15;">
        <div style="font-weight: 800; font-size: 13.5px; letter-spacing: .01em;">{{ name }}</div>
        <div class="num" style="font-size: 10.5px; color: var(--mut); letter-spacing: .04em;">{{ mmr }} MMR</div>
      </div>
    </div>
  `
})
export class TrainerChipComponent {
  @Input() name: string = 'NOVA';
  @Input() mmr: string = '1842';
  @Input() initial: string = 'N';
}

@Component({
  selector: 'aurora-stat',
  standalone: true,
  template: `
    <div>
      <div class="num" [style.color]="accent ? 'var(--accent)' : 'var(--txt)'" style="font-size: 22px; font-weight: 700; letter-spacing: .01em;">{{ v }}</div>
      <div style="font-size: 10.5px; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; color: var(--mut); margin-top: 4px;">{{ k }}</div>
    </div>
  `
})
export class StatComponent {
  @Input() v!: string;
  @Input() k!: string;
  @Input() accent: boolean = false;
}

@Component({
  selector: 'aurora-rank-crest',
  standalone: true,
  template: `
    <div style="position: relative; flex: 0 0 auto;" [style.width.px]="size" [style.height.px]="size">
      <svg [attr.width]="size" [attr.height]="size" viewBox="0 0 100 100" style="position: absolute; inset: 0; overflow: visible;">
        <circle cx="50" cy="50" r="49" fill="none" stroke="var(--accent)" stroke-opacity=".55" stroke-width="1.4" stroke-dasharray="3 6" style="animation: spin 20s linear infinite; transform-origin: 50px 50px;" />
      </svg>
      <svg [attr.width]="size" [attr.height]="size" viewBox="0 0 100 100" style="position: relative; filter: drop-shadow(0 4px 9px rgba(0,0,0,.45));">
        <defs>
          <linearGradient [id]="'t' + id" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffe680" /><stop offset="1" stop-color="#e0a417" /></linearGradient>
          <linearGradient [id]="'b' + id" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffffff" /><stop offset="1" stop-color="#c7d0dc" /></linearGradient>
          <clipPath [id]="'c' + id"><circle cx="50" cy="50" r="42" /></clipPath>
        </defs>
        <g [attr.clip-path]="'url(#c' + id + ')'">
          <rect x="6" y="6" width="88" height="44" [attr.fill]="'url(#t' + id + ')'" />
          <rect x="6" y="50" width="88" height="44" [attr.fill]="'url(#b' + id + ')'" />
          <path d="M31 14 L40 31 M69 14 L60 31" stroke="#222" stroke-width="6" stroke-linecap="round" />
          <circle cx="40" cy="26" r="9" fill="none" stroke="#fff" stroke-opacity=".4" stroke-width="3" />
        </g>
        <circle cx="50" cy="50" r="42" fill="none" stroke="#161616" stroke-width="4" />
        <rect x="8" y="46" width="84" height="8" fill="#161616" />
        <circle cx="50" cy="50" r="13" fill="#161616" />
        <circle cx="50" cy="50" r="8.5" fill="#fff" />
        <circle cx="50" cy="50" r="8.5" fill="none" stroke="#aab2bf" stroke-width="1.4" />
      </svg>
      <div [style]="'position: absolute; right: -' + (size * 0.08) + 'px; bottom: -' + (size * 0.08) + 'px; min-width: ' + (size * 0.42) + 'px; height: ' + (size * 0.36) + 'px; padding: 0 4px; border-radius: ' + (size * 0.12) + 'px; background: var(--accent); color: var(--on-accent); font-family: Space Grotesk, sans-serif; font-weight: 700; font-size: ' + (size * 0.26) + 'px; line-height: 1; display: flex; align-items: center; justify-content: center; border: 2px solid var(--bg); box-shadow: 0 2px 5px rgba(0,0,0,.45);'">{{ tier }}</div>
    </div>
  `
})
export class RankCrestComponent {
  @Input() size: number = 56;
  @Input() tier: string = 'III';
  id = Math.random().toString(36).substr(2, 9);
}

@Component({
  selector: 'aurora-ball-icon',
  standalone: true,
  template: `
    <span [style.width.px]="size" [style.height.px]="size" style="border-radius: 50%; display: inline-block; flex: 0 0 auto; position: relative; background: linear-gradient(#ee1515 0 50%, #f4f4f4 50% 100%); box-shadow: inset 0 0 0 1.5px rgba(0,0,0,.6);">
      <span style="position: absolute; top: 50%; left: 0; right: 0; height: 2px; background: #161616; transform: translateY(-50%);"></span>
      <span [style.width.px]="size * 0.34" [style.height.px]="size * 0.34" style="position: absolute; top: 50%; left: 50%; border-radius: 50%; background: #fff; border: 1.5px solid #161616; transform: translate(-50%,-50%);"></span>
    </span>
  `
})
export class BallIconComponent {
  @Input() size: number = 16;
}

@Component({
  selector: 'aurora-battle-cta',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    @if (searching()) {
      <div class="cta cta--searching">
        <div class="pokespin"></div>
        <div style="text-align: left; flex: 1;">
          <div style="font-family: var(--display); font-weight: 700; font-size: 18px; letter-spacing: .01em; white-space: nowrap;">Buscando rival…</div>
          <div style="font-size: 11px; color: var(--mut); font-weight: 600; letter-spacing: .04em; margin-top: 2px; white-space: nowrap;">Emparejando por MMR cercano</div>
        </div>
        <div class="cta-time">{{ mm() }}:{{ ss() }}</div>
        <button class="cancel-link" (click)="stopSearch()">Cancelar</button>
      </div>
    } @else {
      <button class="cta" (click)="startSearch()">
        <span class="cta-beacon"></span>
        <div style="text-align: left;">
          <div class="cta__title">{{ title }}</div>
          <div class="cta__sub">{{ sub }}</div>
        </div>
        <span class="cta__arrow"><aurora-icon n="arrow" [s]="22"></aurora-icon></span>
      </button>
    }
  `
})
export class BattleCtaComponent {
  @Input() title: string = 'BATALLAR';
  @Input() sub: string = 'Clasificatoria · Liga Oro III';
  @Output() startBattle = new EventEmitter<void>();
  
  searching = signal(false);
  secs = signal(0);
  intervalId: any;

  startSearch() {
    this.searching.set(true);
    this.secs.set(0);
    this.intervalId = setInterval(() => {
      this.secs.update(s => s + 1);
    }, 1000);
    this.startBattle.emit();
  }

  stopSearch() {
    this.searching.set(false);
    clearInterval(this.intervalId);
  }

  mm() {
    return String(Math.floor(this.secs() / 60)).padStart(2, '0');
  }

  ss() {
    return String(this.secs() % 60).padStart(2, '0');
  }
}

@Component({
  selector: 'aurora-energy-type',
  standalone: true,
  template: `
    <span class="etype" [style.width.px]="size" [style.height.px]="size" [style.--c]="c">
      <svg viewBox="0 0 24 24" [attr.fill]="mode === 'fill' ? 'currentColor' : 'none'" [attr.stroke]="mode === 'stroke' ? 'currentColor' : 'none'" stroke-width="2.2" stroke-linecap="round">
        <path [attr.d]="glyphD"></path>
      </svg>
    </span>
  `
})
export class EnergyTypeComponent {
  @Input() type: string = 'fire';
  @Input() size: number = 22;

  get c() { return this.getGlyph()[0]; }
  get mode() { return this.getGlyph()[1]; }
  get glyphD() { return this.getGlyph()[2]; }

  getGlyph() {
    const types: Record<string, string[]> = {
      fire: ['#ff7a3c', 'fill', 'M13 2c.6 3.6-3 4.6-3 8a3 3 0 0 0 6 0c0-1-.6-2-1.2-2.8C16.2 9 17 11 17 13.2A5 5 0 0 1 7 13C7 8.4 11 6 13 2z'],
      water: ['#4d9fe0', 'fill', 'M12 3c3.3 4.9 6 7.9 6 11.3A6 6 0 0 1 6 14.3C6 10.9 8.7 7.9 12 3z'],
      lightning: ['#f5c542', 'fill', 'M13 2 5 13.2h5l-1.2 8.8 9.2-12.4h-6L13 2z'],
      colorless: ['#dfe2ec', 'fill', 'M12 2.5l2.7 6.4 6.8.6-5.2 4.5 1.6 6.7L12 17.6 6.1 21.2l1.6-6.7L2.5 9.9l6.8-.6z']
    };
    return types[this.type] || types['fire'];
  }
}

@Component({
  selector: 'aurora-sparks',
  standalone: true,
  template: `
    <div class="sparks">
      @for (item of items; track item) {
        <span class="spark" [style]="'left: ' + item.left + '%; top: ' + item.top + '%; --s: ' + item.s + '; --d: ' + item.d + 's; --sc: ' + color + '; animation-delay: ' + item.delay + 's;'"></span>
      }
    </div>
  `
})
export class SparksComponent {
  @Input() n: number = 16;
  @Input() color: string = 'var(--accent2)';
  @Input() area: any;
  items: any[] = [];

  ngOnInit() {
    this.items = Array.from({ length: this.n }, () => ({
      left: (this.area ? this.area.x : 0) + Math.random() * (this.area ? this.area.w : 100),
      top: (this.area ? this.area.y : 0) + Math.random() * (this.area ? this.area.h : 100),
      s: 0.45 + Math.random() * 1.1,
      d: 3.5 + Math.random() * 5,
      delay: -Math.random() * 8,
    }));
  }
}

@Component({
  selector: 'aurora-ambient',
  standalone: true,
  template: `
    <div class="energy-ring" style="width: 400px; height: 400px; right: 180px; top: 60px;"></div>
    <svg class="poke-motif" width="560" height="560" viewBox="0 0 100 100" fill="none" stroke="currentColor" stroke-width="1.4" style="right: -120px; top: 90px;">
      <circle cx="50" cy="50" r="46" />
      <circle cx="50" cy="50" r="13" />
      <circle cx="50" cy="50" r="20" stroke-dasharray="2 4" opacity="0.7" />
      <path d="M4 50h33M63 50h33" />
      <circle cx="50" cy="50" r="6" fill="currentColor" stroke="none" opacity="0.5" />
    </svg>
  `
})
export class AmbientComponent {}

@Component({
  selector: 'aurora-sig-card',
  standalone: true,
  template: `
    <div [style]="'width: ' + w + 'px; aspect-ratio: 5/7; border-radius: ' + (w * 0.06) + 'px; position: relative; flex: 0 0 auto; transform: rotate(' + rot + 'deg); background: linear-gradient(160deg, ' + tones()[0] + ', ' + tones()[1] + ' 55%, ' + tones()[2] + '); box-shadow: 0 30px 60px -22px rgba(0,0,0,.8), inset 0 0 0 1px rgba(255,255,255,.18);'">
      <div [style]="'position: absolute; inset: ' + (w * 0.045) + 'px; border-radius: ' + (w * 0.04) + 'px; border: 1px solid rgba(255,255,255,.22); overflow: hidden;'">
        <div style="position: absolute; inset: 0; background: linear-gradient(125deg,transparent 35%,rgba(255,255,255,.28) 50%,transparent 62%); background-size: 250% 250%; animation: holo 6s ease-in-out infinite;"></div>
        <div [style]="'position: absolute; left: 8%; right: 8%; top: 14%; height: 52%; border-radius: ' + (w * 0.03) + 'px; border: 1px solid rgba(255,255,255,.25); background: radial-gradient(60% 60% at 50% 40%, rgba(255,255,255,.4), transparent 70%), repeating-linear-gradient(45deg, rgba(255,255,255,.06) 0 8px, transparent 8px 16px);'"></div>
        <div [style]="'position: absolute; left: 8%; right: 8%; top: 7%; font-family: Space Grotesk,sans-serif; font-weight: 700; font-size: ' + (w * 0.072) + 'px; color: #fff; text-shadow: 0 1px 4px rgba(0,0,0,.5); display: flex; justify-content: space-between;'">
          <span>{{ name }}</span><span class="num" [style]="'font-size: ' + (w * 0.05) + 'px; opacity: .9;'">HP 180</span>
        </div>
        <div [style]="'position: absolute; left: 8%; right: 8%; bottom: 7%; height: ' + (w * 0.14) + 'px; border-radius: ' + (w * 0.02) + 'px; background: rgba(0,0,0,.28); display: flex; align-items: center; padding: 0 8%; font-family: Space Mono,monospace; font-size: ' + (w * 0.042) + 'px; color: rgba(255,255,255,.8); letter-spacing: .06em;'">XY · 011/146</div>
      </div>
    </div>
  `
})
export class SigCardComponent {
  @Input() w: number = 230;
  @Input() type: string = 'fire';
  @Input() name: string = 'CHARIZARD';
  @Input() rot: number = 0;

  tones() {
    const t: Record<string, string[]> = {
      fire: ['#ff8a4c', '#c0392b', '#3a1410'],
      water: ['#5aa9e6', '#2a5a9e', '#0c1f38'],
      lightning: ['#f5d34c', '#d6a31f', '#423207']
    };
    return t[this.type] || t['fire'];
  }
}
