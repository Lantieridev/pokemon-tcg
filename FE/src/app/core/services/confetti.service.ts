import { Injectable, NgZone } from '@angular/core';
import confetti from 'canvas-confetti';

/** Thin wrapper around canvas-confetti so the burst runs outside Angular's
 *  change detection (it's a self-contained rAF loop with nothing to bind to)
 *  and every call site shares the same brand-accent palette. */
@Injectable({ providedIn: 'root' })
export class ConfettiService {
  constructor(private zone: NgZone) {}

  /** A gym cleared, a match won — a warm, moderate burst. */
  celebrate() {
    this.zone.runOutsideAngular(() => {
      confetti({
        particleCount: 120,
        spread: 75,
        origin: { y: 0.6 },
        colors: ['#ff2e3e', '#ffce32', '#ffffff'],
      });
    });
  }

  /** A legendary/rare pull — bigger, from both sides, gold-leaning. */
  legendaryPull() {
    this.zone.runOutsideAngular(() => {
      const end = Date.now() + 700;
      const colors = ['#ffce32', '#ff2e3e', '#fff6df'];
      (function frame() {
        confetti({ particleCount: 4, angle: 60, spread: 60, origin: { x: 0 }, colors });
        confetti({ particleCount: 4, angle: 120, spread: 60, origin: { x: 1 }, colors });
        if (Date.now() < end) requestAnimationFrame(frame);
      })();
    });
  }
}
