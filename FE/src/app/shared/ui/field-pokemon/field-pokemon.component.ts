import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  inject,
} from '@angular/core';
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
      [class.hit-shake]="isShaking"
    >
      @if (glow && card) {
        <div class="glow" [ngClass]="card.type"></div>
      }

      @if (card) {
        <!-- White flash overlay on hit -->
        @if (isFlashing) {
          <div class="hit-flash"></div>
        }

        <img
          [src]="card.img"
          [alt]="card.name"
          class="card-img"
          [style.transform]="'rotate(' + rot + 'deg)'"
          style="width: 100%; height: 100%; transition: transform .35s cubic-bezier(.2,.9,.25,1.05); z-index: 2; position: relative;"
          draggable="false"
        />
      }

      <!-- Tool badge -->
      @if (attachedToolCardId) {
        <div
          class="tool-badge"
          (click)="onToolClick($event)"
          title="Herramienta equipada"
        >🔧</div>
      }

      <!-- Floating Damage Number -->
      @if (showDamageNumber) {
        <div
          class="damage-float"
          [class.damage-heal]="lastDamageAmount < 0"
        >
          {{ lastDamageAmount > 0 ? '-' : '+' }}{{ Math.abs(lastDamageAmount) }}
        </div>
      }

      <!-- HP HUD overlay with animated bar -->
      @if (maxHp > 0 && card) {
        <div
          style="
            position: absolute;
            top: 4px;
            right: -8px;
            z-index: 10;
            background: rgba(0,0,0,.7);
            backdrop-filter: blur(8px);
            border: 1px solid rgba(255,255,255,.15);
            border-radius: 8px;
            padding: 3px 6px;
            font-family: 'Russo One', sans-serif;
            font-size: 9px;
            color: #fff;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 2px;
          "
        >
          <span>{{ displayedHp }}/{{ maxHp }}</span>
          <div
            style="
              width: 36px;
              height: 4px;
              border-radius: 2px;
              background: rgba(255,255,255,.15);
              overflow: hidden;
            "
          >
            <div
              [style.width.%]="displayedHpPercent"
              [style.background]="displayedHpBarColor"
              style="height: 100%; border-radius: 2px; transition: none;"
            ></div>
          </div>
        </div>
      }

      <app-energy-cascade [energies]="energies" [direction]="direction" [cardW]="width"></app-energy-cascade>
      <app-damage-tokens [status]="status"></app-damage-tokens>
    </div>
  `,
  styles: [`
    :host {
      display: inline-block;
      transform-style: preserve-3d;
      transition: transform 0.25s cubic-bezier(0.2, 0.9, 0.3, 1.05);
    }
    .bench-slot {
      transform-style: preserve-3d;
    }
    .tool-badge {
      position: absolute;
      top: -4px;
      right: -4px;
      z-index: 15;
      width: 22px;
      height: 22px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.75);
      border: 1px solid rgba(255, 203, 5, 0.5);
      border-radius: 50%;
      font-size: 11px;
      cursor: pointer;
      transition: transform 0.15s ease;
      line-height: 1;
    }
    .tool-badge:hover {
      transform: scale(1.25);
    }

    /* ── Hit Shake Animation ─────────────────────────────────── */
    .hit-shake {
      animation: hitShake 0.4s ease-out;
    }
    @keyframes hitShake {
      0%   { transform: translateX(0); }
      15%  { transform: translateX(-8px); }
      30%  { transform: translateX(7px); }
      45%  { transform: translateX(-5px); }
      60%  { transform: translateX(4px); }
      75%  { transform: translateX(-2px); }
      90%  { transform: translateX(1px); }
      100% { transform: translateX(0); }
    }

    /* ── White Flash Overlay ──────────────────────────────────── */
    .hit-flash {
      position: absolute;
      inset: 0;
      z-index: 25;
      border-radius: 6px;
      background: rgba(255, 255, 255, 0.75);
      animation: flashPulse 0.3s ease-out forwards;
      pointer-events: none;
    }
    @keyframes flashPulse {
      0%   { opacity: 1; }
      50%  { opacity: 0.4; }
      100% { opacity: 0; }
    }

    /* ── Floating Damage Number ───────────────────────────────── */
    .damage-float {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      z-index: 30;
      font-family: 'Russo One', 'Impact', sans-serif;
      font-size: 28px;
      font-weight: 900;
      color: #ff3b3b;
      text-shadow:
        0 0 8px rgba(255, 30, 30, 0.8),
        0 2px 4px rgba(0, 0, 0, 0.8),
        0 0 20px rgba(255, 30, 30, 0.4);
      animation: damageFloat 1.4s cubic-bezier(0.22, 0.61, 0.36, 1) forwards;
      pointer-events: none;
      white-space: nowrap;
      letter-spacing: 1px;
    }
    .damage-float.damage-heal {
      color: #5ad27a;
      text-shadow:
        0 0 8px rgba(90, 210, 122, 0.8),
        0 2px 4px rgba(0, 0, 0, 0.8),
        0 0 20px rgba(90, 210, 122, 0.4);
    }
    @keyframes damageFloat {
      0% {
        opacity: 0;
        transform: translate(-50%, -50%) scale(0.5);
      }
      12% {
        opacity: 1;
        transform: translate(-50%, -60%) scale(1.3);
      }
      25% {
        transform: translate(-50%, -65%) scale(1);
      }
      70% {
        opacity: 1;
        transform: translate(-50%, -120%) scale(1);
      }
      100% {
        opacity: 0;
        transform: translate(-50%, -160%) scale(0.8);
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FieldPokemonComponent implements OnChanges, OnDestroy {
  private tcgService = inject(PokemonTcgService);
  private cdr = inject(ChangeDetectorRef);

  @Input({ required: true }) cardId!: string;
  @Input() energies: string[] = [];
  @Input() damage: number = 0;
  @Input() status: string = 'none';
  @Input() width: number = 140;
  @Input() glow: boolean = false;
  @Input() direction: 'up' | 'down' = 'down';
  @Input() maxHp: number = 0;
  @Input() attachedToolCardId: string | null = null;
  @Input() damageEvent: number | null = null;  // Damage amount from DamageEvent

  @Output() toolClick = new EventEmitter<string>();

  // ── Animated HP state ─────────────────────────────────────────────────────
  displayedHp: number = 0;
  displayedHpPercent: number = 100;
  displayedHpBarColor: string = '#5ad27a';

  // ── Visual feedback state ──────────────────────────────────────────────────
  isShaking: boolean = false;
  isFlashing: boolean = false;
  showDamageNumber: boolean = false;
  lastDamageAmount: number = 0;

  Math = Math;

  private hpAnimFrame: number | null = null;
  private shakeTimer: any = null;
  private flashTimer: any = null;
  private damageNumberTimer: any = null;
  private animDelayTimer: any = null;
  private initialized = false;

  onToolClick(event: MouseEvent): void {
    event.stopPropagation();
    if (this.attachedToolCardId) {
      this.toolClick.emit(this.attachedToolCardId);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // 1. If cardId changes, we MUST cancel all animations and snap instantly
    const cardIdChanged = changes['cardId'] && !changes['cardId'].isFirstChange();
    if (cardIdChanged) {
      this.cancelAllAnimations();
      this.displayedHp = this.currentHp;
      this.displayedHpPercent = this.hpPercent;
      this.displayedHpBarColor = this.computeBarColor(this.hpPercent);
      this.initialized = true;
    }

    // 2. Initialize displayed HP on first load
    if (changes['cardId']) {
      this.initialized = false;
    }

    // Initialize displayed HP on first load
    if (!this.initialized && this.maxHp > 0) {
      this.displayedHp = this.currentHp;
      this.displayedHpPercent = this.hpPercent;
      this.displayedHpBarColor = this.computeBarColor(this.hpPercent);
      this.initialized = true;
    }

    // 3. Handle incoming damage event (takes precedence for animation)
    if (changes['damageEvent'] && this.damageEvent !== null && this.damageEvent !== 0) {
      this.triggerDamageVisuals(this.damageEvent);
    } 
    // 4. If damage or maxHp changed without an active damageEvent, determine if we snap
    else if (changes['damage'] || changes['maxHp']) {
      // If we are NOT animating a damage event, or if the HP was reset/healed (e.g. new Pokemon promoted), snap immediately
      const isHealedOrReset = this.currentHp > this.displayedHp;
      if (!this.showDamageNumber || isHealedOrReset) {
        this.displayedHp = this.currentHp;
        this.displayedHpPercent = this.hpPercent;
        this.displayedHpBarColor = this.computeBarColor(this.hpPercent);
      }
    }
  }

  ngOnDestroy(): void {
    this.cancelAllAnimations();
  }

  private cancelAllAnimations(): void {
    if (this.hpAnimFrame !== null) {
      cancelAnimationFrame(this.hpAnimFrame);
      this.hpAnimFrame = null;
    }
    clearTimeout(this.damageNumberTimer);
    clearTimeout(this.animDelayTimer);
    clearTimeout(this.shakeTimer);
    clearTimeout(this.flashTimer);
    this.showDamageNumber = false;
    this.isShaking = false;
    this.isFlashing = false;
  }

  private triggerDamageVisuals(amount: number): void {
    this.lastDamageAmount = amount;

    // 1. Show floating damage number
    this.showDamageNumber = true;
    clearTimeout(this.damageNumberTimer);
    this.damageNumberTimer = setTimeout(() => {
      this.showDamageNumber = false;
      this.cdr.markForCheck();
    }, 1500);

    // 2. Trigger shake + flash (only for damage, not healing)
    if (amount > 0) {
      this.isShaking = true;
      this.isFlashing = true;
      clearTimeout(this.shakeTimer);
      clearTimeout(this.flashTimer);
      this.shakeTimer = setTimeout(() => {
        this.isShaking = false;
        this.cdr.markForCheck();
      }, 450);
      this.flashTimer = setTimeout(() => {
        this.isFlashing = false;
        this.cdr.markForCheck();
      }, 350);
    }

    // 3. Animate HP bar after a brief delay (damage number shows first)
    const targetHp = this.currentHp;
    const targetPercent = this.hpPercent;
    const startHp = this.displayedHp;
    const hpDiff = Math.abs(startHp - targetHp);
    // Animation duration: 300ms minimum, scales with HP loss (up to 1200ms for huge hits)
    const duration = Math.min(1200, Math.max(300, hpDiff * 8));
    const delay = 400; // Wait for damage number pop-in

    clearTimeout(this.animDelayTimer);
    this.animDelayTimer = setTimeout(() => {
      this.animateHpBar(startHp, targetHp, targetPercent, duration);
    }, delay);

    this.cdr.markForCheck();
  }

  private animateHpBar(
    startHp: number,
    targetHp: number,
    _targetPercent: number,
    duration: number
  ): void {
    if (this.hpAnimFrame !== null) {
      cancelAnimationFrame(this.hpAnimFrame);
    }

    const startTime = performance.now();

    const tick = (now: number) => {
      const elapsed = now - startTime;
      const progress = Math.min(1, elapsed / duration);
      // Ease-out cubic
      const eased = 1 - Math.pow(1 - progress, 3);

      const currentHp = Math.round(startHp + (targetHp - startHp) * eased);
      this.displayedHp = Math.max(0, currentHp);
      this.displayedHpPercent = this.maxHp > 0
        ? Math.max(0, Math.min(100, (this.displayedHp / this.maxHp) * 100))
        : 0;
      this.displayedHpBarColor = this.computeBarColor(this.displayedHpPercent);
      this.cdr.markForCheck();

      if (progress < 1) {
        this.hpAnimFrame = requestAnimationFrame(tick);
      } else {
        this.hpAnimFrame = null;
        // Snap to final values
        this.displayedHp = Math.max(0, targetHp);
        this.displayedHpPercent = this.maxHp > 0
          ? Math.max(0, Math.min(100, (this.displayedHp / this.maxHp) * 100))
          : 0;
        this.displayedHpBarColor = this.computeBarColor(this.displayedHpPercent);
        this.cdr.markForCheck();
      }
    };

    this.hpAnimFrame = requestAnimationFrame(tick);
  }

  private computeBarColor(pct: number): string {
    if (pct < 20) return '#ee1515';
    if (pct <= 50) return '#ffcb05';
    return '#5ad27a';
  }

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
    if (this.status === 'asleep') return -90;
    if (this.status === 'paralyzed') return 90;
    if (this.status === 'confused') return 180;
    return 0;
  }

  get currentHp(): number {
    return Math.max(0, this.maxHp - this.damage);
  }

  get hpPercent(): number {
    if (this.maxHp <= 0) return 0;
    return Math.max(0, Math.min(100, (this.currentHp / this.maxHp) * 100));
  }

  get hpBarColor(): string {
    return this.computeBarColor(this.hpPercent);
  }
}

