import { Component, Input, signal, ElementRef, ViewChild } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-sig-card',
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

@Component({
  selector: 'app-holo-card',
  standalone: true,
  imports: [CommonModule, SigCardComponent, NgOptimizedImage],
  template: `
    <div class="holo"
         [class.holo--float]="idleFloat"
         [class.holo--ex]="holoTier === 'ex'"
         [class.holo--fullart]="holoTier === 'fullart'"
         [class.holo--sir]="holoTier === 'sir'"
         #holoContainer
         [style.width.px]="w" [style.--base-rot]="baseRot + 'deg'"
         (pointermove)="onMove($event)" (pointerleave)="reset()">
      <div class="holo__rot" #holoRot>
        @if (card?.img) {
          <img class="holo__img" [ngSrc]="card.img" [alt]="card.name || 'card'" [width]="w" [height]="w * 1.4" priority draggable="false" />
        } @else {
          <app-sig-card [w]="w" [type]="card?.type || fallbackType" [name]="card?.name || 'CHARIZARD'"></app-sig-card>
        }
        <div class="holo__shine"></div>
        <div class="holo__glitter"></div>
        <div class="holo__glare"></div>
        <div class="holo__glare2"></div>
      </div>
    </div>
  `
})
export class HoloCardComponent {
  @Input() card: any;
  @Input() w: number = 250;
  @Input() baseRot: number = 0;
  @Input() fallbackType: string = 'fire';
  @Input() idleFloat: boolean = true;

  @ViewChild('holoContainer') containerRef!: ElementRef;
  @ViewChild('holoRot') rotRef!: ElementRef;

  rafId: any;

  onMove(e: PointerEvent) {
    const el = this.rotRef?.nativeElement;
    if (!el || !this.containerRef?.nativeElement) return;
    const r = this.containerRef.nativeElement.getBoundingClientRect();
    const px = (e.clientX - r.left) / r.width;
    const py = (e.clientY - r.top) / r.height;

    cancelAnimationFrame(this.rafId);
    this.rafId = requestAnimationFrame(() => {
      // Basic tilt vars
      el.style.setProperty('--mx', (px * 100).toFixed(1) + '%');
      el.style.setProperty('--my', (py * 100).toFixed(1) + '%');
      el.style.setProperty('--rx', ((py - 0.5) * -20).toFixed(2) + 'deg');
      el.style.setProperty('--ry', ((px - 0.5) * 20).toFixed(2) + 'deg');
      el.style.setProperty('--o', '1');
      el.style.setProperty('--bx', (20 + px * 60).toFixed(1) + '%');
      el.style.setProperty('--by', (20 + py * 60).toFixed(1) + '%');

      // Advanced pointer vars (matching pokemon-cards-151 repo)
      el.style.setProperty('--pointer-x', (px * 100).toFixed(1) + '%');
      el.style.setProperty('--pointer-y', (py * 100).toFixed(1) + '%');
      el.style.setProperty('--pointer-from-left', (px).toFixed(4));
      el.style.setProperty('--pointer-from-top', (py).toFixed(4));

      // Pythagorean distance from center, clamped 0..1
      const dist = Math.sqrt(Math.pow((py * 100) - 50, 2) + Math.pow((px * 100) - 50, 2)) / 50;
      el.style.setProperty('--pointer-from-center', Math.min(1, Math.max(0, dist)).toFixed(4));

      el.style.setProperty('--card-opacity', '1');
      el.style.setProperty('--background-x', (50 + (px - 0.5) * 100).toFixed(1) + '%');
      el.style.setProperty('--background-y', (50 + (py - 0.5) * 100).toFixed(1) + '%');

      // Rotation vars used by some effects (--rotate-x is rotateY in their naming)
      el.style.setProperty('--rotate-x', ((px - 0.5) * 20).toFixed(2) + 'deg');
      el.style.setProperty('--rotate-y', ((py - 0.5) * -20).toFixed(2) + 'deg');
    });
  }

  reset() {
    const el = this.rotRef?.nativeElement;
    if (!el) return;
    cancelAnimationFrame(this.rafId);
    el.style.setProperty('--rx', '0deg');
    el.style.setProperty('--ry', '0deg');
    el.style.setProperty('--o', '0');
    el.style.setProperty('--card-opacity', '0');
    el.style.setProperty('--pointer-from-center', '0');
    el.style.setProperty('--pointer-from-left', '0.5');
    el.style.setProperty('--pointer-from-top', '0.5');
  }

  /** Classify the card into one of 4 tiers based on rarity/subtypes */
  get holoTier(): 'basic' | 'ex' | 'fullart' | 'sir' {
    if (!this.card) return 'basic';
    const rarity = (this.card.rarity || '').toLowerCase();
    const subtypes: string[] = (this.card.subtypes || []).map((s: string) => s.toLowerCase());
    const name = (this.card.name || '').toLowerCase();

    // Tier 4 — Special Illustration Rare / Hyper Rare (most premium sparkle)
    if (rarity.includes('special illustration rare') || rarity.includes('hyper rare') || rarity.includes('secret rare')) {
      return 'sir';
    }
    // Tier 3 — Ultra Rare Full Art / Illustration Rare (metallic foil)
    if (rarity.includes('ultra rare') || rarity.includes('illustration rare')) {
      return 'fullart';
    }
    // Tier 2 — EX / V / VMAX / Double Rare (enhanced rainbow + glitter)
    if (subtypes.includes('ex') || subtypes.includes('v') || subtypes.includes('vmax') || subtypes.includes('vstar') || rarity.includes('double rare') || rarity.includes('rare holo v') || name.endsWith(' ex') || name.endsWith(' v') || name.endsWith(' vmax') || name.endsWith(' vstar')) {
      return 'ex';
    }
    return 'basic';
  }
}

@Component({
  selector: 'app-float-card',
  standalone: true,
  imports: [CommonModule, SigCardComponent, NgOptimizedImage],
  template: `
    <div style="position: relative; z-index: 1;" 
         [style.width.px]="w" 
         [style.z-index]="z" 
         [style.transform]="'rotate(' + rot + 'deg)'" 
         [style.animation]="'floaty ' + (5.5 + z * 0.4) + 's ease-in-out infinite'" 
         [style.animation-delay]="delay" 
         [style.opacity]="op"
         style="filter: drop-shadow(0 34px 46px rgba(0,0,0,.6));">
      @if (show()) {
        <div style="position: relative; overflow: hidden; background: rgba(255,255,255,.04); box-shadow: inset 0 0 0 1px rgba(255,255,255,.16); transition: opacity .5s;"
             [style.width.px]="w" 
             [style.border-radius.px]="w * 0.055"
             [style.opacity]="loaded() ? 1 : 0">
          <img [ngSrc]="card.img" [alt]="card.name || 'card'" [width]="w" [height]="w * 1.4" style="width: 100%; display: block; height: auto;" priority (load)="loaded.set(true)" (error)="err.set(true)" />
          <div style="position: absolute; inset: 0; pointer-events: none; mix-blend-mode: screen; background: linear-gradient(125deg,transparent 38%,rgba(255,255,255,.22) 50%,transparent 62%); background-size: 250% 250%; animation: holo 6s ease-in-out infinite;"></div>
        </div>
      } @else {
        <app-sig-card [w]="w" [type]="card?.type || 'fire'" [name]="card?.name || 'CHARIZARD'"></app-sig-card>
      }
    </div>
  `
})
export class FloatCardComponent {
  @Input() card: any;
  @Input() w: number = 200;
  @Input() rot: number = 0;
  @Input() delay: string = '0s';
  @Input() z: number = 1;
  @Input() op: number = 1;

  loaded = signal(false);
  err = signal(false);

  get show() {
    return () => this.card?.img && !this.err();
  }
}
