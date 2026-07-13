import { Component, Input, Output, EventEmitter, signal, HostListener, ElementRef, ViewChild, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-glyph-icon',
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
        @case ('pokecoin') { <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM12 7v10M9 9.5a3 3 0 1 0 0 5h2.5" /> }
      }
    </svg>
  `
})
export class IconComponent {
  @Input() n: string = 'home';
  @Input() s: number = 22;
}

@Component({
  selector: 'app-logo',
  standalone: true,
  imports: [RouterModule],
  template: `
    <div routerLink="/lobby" style="display: flex; align-items: center; gap: 10px; cursor: pointer;">
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
export class LogoComponent { }

@Component({
  selector: 'app-trainer-chip',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div style="position: relative;">
      <div class="chip" (click)="toggle()" style="cursor: pointer; user-select: none;"
           [style.outline]="open ? '2px solid var(--accent)' : 'none'"
           [style.outline-offset]="'2px'">
        <div class="avatar ring" style="display: flex; align-items: center; justify-content: center; overflow: hidden; padding: 0; font-size: 16px;">
          @if (isCustomAvatar(avatarIcon)) {
            <img [src]="getAvatarUrl(avatarIcon)" style="width: 100%; height: 100%; object-fit: cover;" />
          } @else if (getAvatarEmoji(avatarIcon)) {
            {{ getAvatarEmoji(avatarIcon) }}
          } @else {
            {{ initial }}
          }
        </div>
        <div style="line-height: 1.15;">
          <div style="font-weight: 800; font-size: 13.5px; letter-spacing: .01em;">{{ name }}</div>
          <div class="num" style="font-size: 10.5px; color: var(--mut); letter-spacing: .04em;">{{ mmr ? mmr + ' MMR' : '...' }}</div>
        </div>
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
             style="margin-left: 4px; color: var(--mut); transition: transform 0.2s;"
             [style.transform]="open ? 'rotate(180deg)' : 'rotate(0deg)'">
          <path d="M6 9l6 6 6-6"/>
        </svg>
      </div>

      @if (open) {
        <!-- Backdrop para cerrar al click afuera -->
        <div style="position: fixed; inset: 0; z-index: 9998;" (click)="open = false"></div>

        <!-- Dropdown menu -->
        <div style="
          position: absolute; right: 0; top: calc(100% + 8px);
          min-width: 180px; background: var(--bg2, #1a1a2e);
          border: 1px solid var(--line, rgba(255,255,255,0.1));
          border-radius: 12px; padding: 8px;
          box-shadow: 0 16px 48px rgba(0,0,0,0.6);
          z-index: 9999; animation: chipDrop 0.15s ease;">
          <div style="padding: 8px 12px 10px; border-bottom: 1px solid var(--line, rgba(255,255,255,0.08)); margin-bottom: 6px; display: flex; align-items: center; gap: 10px;">
            <div class="avatar ring" style="display: flex; align-items: center; justify-content: center; overflow: hidden; padding: 0; font-size: 18px; width: 34px; height: 34px; border-radius: 50%; background: var(--surface);">
              @if (isCustomAvatar(avatarIcon)) {
                <img [src]="getAvatarUrl(avatarIcon)" style="width: 100%; height: 100%; object-fit: cover;" />
              } @else if (getAvatarEmoji(avatarIcon)) {
                {{ getAvatarEmoji(avatarIcon) }}
              } @else {
                {{ initial }}
              }
            </div>
            <div>
              <div style="font-size: 11px; color: var(--mut); margin-bottom: 1px;">Sesión activa</div>
              <div style="font-weight: 700; font-size: 13.5px;">{{ name }}</div>
            </div>
          </div>
          
          <a
            routerLink="/profile"
            (click)="open = false"
            class="profile-item"
            style="
              display: flex; align-items: center; gap: 8px;
              width: 100%; padding: 9px 12px; border: none; border-radius: 8px;
              background: transparent; color: var(--txt);
              font-size: 13px; font-weight: 600; cursor: pointer; text-decoration: none;
              transition: background 0.15s; margin-bottom: 6px;">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            Mi Perfil
          </a>

          <button
            (click)="logout()"
            style="
              width: 100%; padding: 9px 12px; border: none; border-radius: 8px;
              background: rgba(239,68,68,0.12); color: #f87171;
              font-size: 13px; font-weight: 600; cursor: pointer; text-align: left;
              display: flex; align-items: center; gap: 8px;
              transition: background 0.15s;">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>
            </svg>
            Cerrar Sesión
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    @keyframes chipDrop {
      from { opacity: 0; transform: translateY(-6px); }
      to   { opacity: 1; transform: translateY(0); }
    }
    .profile-item:hover {
      background: rgba(255, 255, 255, 0.08) !important;
    }
  `]
})
export class TrainerChipComponent {
  @Input() name: string = 'NOVA';
  @Input() mmr: string = '';
  @Input() initial: string = 'N';
  @Input() avatarIcon: string = '';

  open = false;

  toggle() { this.open = !this.open; }

  getAvatarEmoji(icon: string | undefined): string {
    if (!icon) return '🎒';
    switch (icon.toLowerCase()) {
      case 'ash': return '🧢';
      case 'misty': return '💧';
      case 'brock': return '🪨';
      case 'gary': return '👑';
      case 'serena': return '🎀';
      case 'red': return '⚡';
      default: return '🎒';
    }
  }

  isCustomAvatar(av: string | undefined): boolean {
    if (!av) return false;
    const emojis = ['ash', 'misty', 'brock', 'gary', 'serena', 'red', 'default_trainer'];
    return !emojis.includes(av);
  }

  getAvatarUrl(av: string | undefined): string {
    if (!av) return '';

    if (av === 'Bulbasaur Clásico' || av === 'bulbasaur_classic') return 'assets/store/avatar_bulbasaur.png';
    if (av === 'Charmander Fuego' || av === 'charmander_fire') return 'assets/store/avatar_charmander.png';
    if (av === 'Squirtle Agua' || av === 'squirtle_water') return 'assets/store/avatar_squirtle.png';
    if (av === 'Ash Ketchum' || av === 'ash_avatar') return 'assets/store/avatar_ash.png';
    if (av === 'Misty' || av === 'misty_avatar') return 'assets/store/avatar_misty.png';
    if (av === 'Brock' || av === 'brock_avatar') return 'assets/store/avatar_brock.png';
    if (av === 'Charizard 3D Premium' || av === 'charizard_3d') return 'assets/store/avatar_charizard_3d.png';
    if (av === 'Mewtwo Legendario' || av === 'mewtwo_3d') return 'assets/store/avatar_mewtwo_3d.png';
    if (av === 'Pikachu Chibi' || av === 'pikachu_cute') return 'assets/store/avatar_pikachu_cute.png';
    if (av === 'collector_legend') return 'assets/store/avatar_collector.png';

    const normalizedValue = av.toLowerCase()
      .normalize("NFD").replace(/[\u0300-\u036f]/g, "")
      .replace(/\s+/g, '_')
      .replace(/[^a-z0-9_]/g, '');

    const prefix = normalizedValue.startsWith('avatar_') ? '' : 'avatar_';
    return `assets/achievements/avatars/${prefix}${normalizedValue}.png`;
  }

  logout() {
    this.open = false;
    localStorage.removeItem('jwt');
    localStorage.removeItem('username');
    localStorage.removeItem('userId');
    window.location.href = '/login';
  }
}


@Component({
  selector: 'app-stat',
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
  selector: 'app-rank-crest',
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
  selector: 'app-ball-icon',
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
  selector: 'app-coin-icon',
  standalone: true,
  template: `
    <span [style.width.px]="size" [style.height.px]="size" style="border-radius: 50%; display: inline-block; flex: 0 0 auto; position: relative; background: linear-gradient(135deg, #fff080 0%, #f1c40f 40%, #b9770e 100%); box-shadow: inset 0 0 0 1px #fcf3cf, 0 1px 4px rgba(0,0,0,.6);">
      <span [style.width.px]="size * 0.65" [style.height.px]="size * 0.65" style="position: absolute; top: 50%; left: 50%; border-radius: 50%; border: 1.5px solid rgba(255,255,255,0.6); transform: translate(-50%,-50%); box-shadow: inset 0 0 2px rgba(0,0,0,0.1);"></span>
      <span [style.fontSize.px]="size * 0.45" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: rgba(255,255,255,0.95); font-weight: 800; font-family: var(--display, sans-serif); line-height: 1; padding-top: 1px; text-shadow: 0 1px 1px rgba(0,0,0,0.3);">P</span>
    </span>
  `
})
export class CoinIconComponent {
  @Input() size: number = 16;
}

@Component({
  selector: 'app-battle-cta',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    @if (_searching()) {
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
        <span class="cta__arrow"><app-glyph-icon n="arrow" [s]="22"></app-glyph-icon></span>
      </button>
    }
  `
})
export class BattleCtaComponent implements OnDestroy {
  @Input() title: string = 'BATALLAR';
  @Input() sub: string = 'Clasificatoria · Liga Oro III';

  @Input() set searching(val: boolean) {
    this._searching.set(val);
    if (val) {
      this.startTimer();
    } else {
      this.stopTimer();
    }
  }
  get searching() {
    return this._searching();
  }
  _searching = signal(false);

  @Output() startBattle = new EventEmitter<void>();
  @Output() cancelBattle = new EventEmitter<void>();

  secs = signal(0);
  intervalId: any;

  startSearch() {
    this.startBattle.emit();
  }

  stopSearch() {
    this.cancelBattle.emit();
  }

  startTimer() {
    clearInterval(this.intervalId);
    this.secs.set(0);
    this.intervalId = setInterval(() => {
      this.secs.update(s => s + 1);
    }, 1000);
  }

  stopTimer() {
    clearInterval(this.intervalId);
  }

  ngOnDestroy() {
    this.stopTimer();
  }

  mm() {
    return String(Math.floor(this.secs() / 60)).padStart(2, '0');
  }

  ss() {
    return String(this.secs() % 60).padStart(2, '0');
  }
}

@Component({
  selector: 'app-energy-type',
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
      colorless: ['#dfe2ec', 'fill', 'M12 2.5l2.7 6.4 6.8.6-5.2 4.5 1.6 6.7L12 17.6 6.1 21.2l1.6-6.7L2.5 9.9l6.8-.6z'],
      grass: ['#5ad27a', 'fill', 'M12 22C5 22 2 14 2 10c0-4 4-8 10-8 6 0 8 2 10 4 2 2 2 5 2 9 0 5-6 13-12 13z'],
      psychic: ['#b273d6', 'fill', 'M12 3c-5.5 0-10 4.5-10 10s4.5 10 10 10 10-4.5 10-10S17.5 3 12 3zm0 16c-3.3 0-6-2.7-6-6s2.7-6 6-6 6 2.7 6 6-2.7 6-6 6zm0-10c-2.2 0-4 1.8-4 4s1.8 4 4 4 4-1.8 4-4-1.8-4-4-4z'],
      fighting: ['#d65f3c', 'fill', 'M21 7h-4V3h-4v4h-2V3H7v4H3v4h4v6H3v4h18v-4h-4v-6h4V7z'],
      darkness: ['#5c6e80', 'fill', 'M12 22C6.5 22 2 17.5 2 12s4.5-10 10-10c.8 0 1.5.1 2.2.3-3.6 1.4-6.2 4.9-6.2 9.1s2.6 7.7 6.2 9.1c-.7.2-1.4.3-2.2.3z'],
      metal: ['#9aa9c7', 'fill', 'M12 2L20.7 7v10L12 22 3.3 17V7z'],
      fairy: ['#f28aae', 'fill', 'M12 2l3 6.5 7.2 1-5.2 5 1.2 7.2-6.5-3.4-6.4 3.4 1.2-7.2-5.2-5 7.2-1z'],
      dragon: ['#b0931e', 'fill', 'M5 2c0 8 4 14 7 20 3-6 7-12 7-20-3 2-6 2-7 4-1-2-4-2-7-4z']
    };
    return types[this.type] || types['colorless'];
  }
}

@Component({
  selector: 'app-sparks',
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
  selector: 'app-ambient',
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
export class AmbientComponent { }
