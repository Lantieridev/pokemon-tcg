import { Component, Input, signal, ElementRef, ViewChild, HostListener } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { SigCardComponent } from '../ui/aurora-ui.components';

@Component({
  selector: 'aurora-holo-card',
  standalone: true,
  imports: [CommonModule, SigCardComponent, NgOptimizedImage],
  template: `
    <div class="holo" [class.holo--float]="idleFloat" #holoContainer
         [style.width.px]="w" [style.--base-rot]="baseRot + 'deg'"
         (pointermove)="onMove($event)" (pointerleave)="reset()">
      <div class="holo__rot" #holoRot>
        @if (card?.img) {
          <img class="holo__img" [ngSrc]="card.img" [alt]="card.name" width="250" height="350" priority crossorigin="anonymous" draggable="false" />
        } @else {
          <aurora-sig-card [w]="w" [type]="card?.type || fallbackType" [name]="card?.name || 'CHARIZARD'"></aurora-sig-card>
        }
        <div class="holo__foil"></div>
        <div class="holo__glare"></div>
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
      el.style.setProperty('--mx', (px * 100).toFixed(1) + '%');
      el.style.setProperty('--my', (py * 100).toFixed(1) + '%');
      el.style.setProperty('--rx', ((py - 0.5) * -20).toFixed(2) + 'deg');
      el.style.setProperty('--ry', ((px - 0.5) * 20).toFixed(2) + 'deg');
      el.style.setProperty('--o', '1');
      el.style.setProperty('--bx', (20 + px * 60).toFixed(1) + '%');
      el.style.setProperty('--by', (20 + py * 60).toFixed(1) + '%');
    });
  }

  reset() {
    const el = this.rotRef?.nativeElement;
    if (!el) return;
    cancelAnimationFrame(this.rafId);
    el.style.setProperty('--rx', '0deg');
    el.style.setProperty('--ry', '0deg');
    el.style.setProperty('--o', '0');
  }
}

@Component({
  selector: 'aurora-float-card',
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
          <img [ngSrc]="card.img" [alt]="card.name" width="200" height="280" style="width: 100%; display: block; height: auto;" crossorigin="anonymous" (load)="loaded.set(true)" (error)="err.set(true)" />
          <div style="position: absolute; inset: 0; pointer-events: none; mix-blend-mode: screen; background: linear-gradient(125deg,transparent 38%,rgba(255,255,255,.22) 50%,transparent 62%); background-size: 250% 250%; animation: holo 6s ease-in-out infinite;"></div>
        </div>
      } @else {
        <aurora-sig-card [w]="w" [type]="card?.type || 'fire'" [name]="card?.name || 'CHARIZARD'"></aurora-sig-card>
      }
    </div>
  `
})
export class AuroraCardComponent {
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
