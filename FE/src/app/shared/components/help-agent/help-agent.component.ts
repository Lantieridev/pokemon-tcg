import { Component, inject, computed, signal, HostListener, effect, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TutorialService } from '../../../core/services/tutorial.service';

@Component({
  selector: 'app-help-agent',
  standalone: true,
  imports: [CommonModule],
  encapsulation: ViewEncapsulation.None,
  styles: [`
    .tutorial-overlay {
      position: fixed;
      inset: 0;
      z-index: 15000;
      pointer-events: auto;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      align-items: center;
      padding: 48px;
    }

    .tutorial-mask-dark {
      position: absolute;
      inset: 0;
      background: rgba(4, 2, 10, 0.72);
      z-index: 15001;
      pointer-events: none;
      transition: opacity 0.3s ease;
    }

    .tutorial-spotlight {
      position: absolute;
      border-radius: 16px;
      box-shadow: 0 0 0 9999px rgba(4, 2, 10, 0.72);
      transition: all 0.28s cubic-bezier(0.25, 0.8, 0.25, 1);
      pointer-events: none;
      z-index: 15001;
      border: 2px dashed rgba(251, 191, 36, 0.6);
      box-sizing: border-box;
    }

    .tutorial-container {
      position: relative;
      z-index: 15002;
      display: flex;
      align-items: flex-end;
      width: 100%;
      max-width: 1060px;
      gap: 36px;
      margin-bottom: 16px;
    }

    .pikachu-wrapper {
      flex: 0 0 200px;
      height: 240px;
      display: flex;
      align-items: flex-end;
      justify-content: center;
      position: relative;
    }

    .pikachu-img {
      width: 100%;
      height: auto;
      max-height: 240px;
      object-fit: contain;
      filter: drop-shadow(0 12px 24px rgba(0, 0, 0, 0.6));
      animation: pikachuFloat 4s ease-in-out infinite;
      transition: all 0.3s ease-in-out;
    }

    @keyframes pikachuFloat {
      0%, 100% { transform: translateY(0) rotate(0deg); }
      50% { transform: translateY(-10px) rotate(1deg); }
    }

    .speech-bubble {
      flex: 1;
      background: rgba(15, 12, 30, 0.76);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 24px;
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      padding: 24px 30px;
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
      display: flex;
      flex-direction: column;
      gap: 12px;
      position: relative;
      animation: bubbleSlideIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
    }

    @keyframes bubbleSlideIn {
      from { opacity: 0; transform: translateY(20px) scale(0.96); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }

    .speech-bubble::before {
      content: '';
      position: absolute;
      left: -10px;
      bottom: 36px;
      width: 20px;
      height: 20px;
      background: rgba(15, 12, 30, 0.76);
      border-left: 1px solid rgba(255, 255, 255, 0.1);
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
      transform: rotate(45deg);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
    }

    .bubble-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .bubble-title {
      font-family: var(--display, 'Space Grotesk'), sans-serif;
      font-weight: 800;
      font-size: 13.5px;
      color: var(--accent2, #fbbf24);
      letter-spacing: 0.12em;
      text-transform: uppercase;
      text-shadow: 0 0 10px rgba(251, 191, 36, 0.3);
    }

    .bubble-counter {
      font-family: 'Space Mono', monospace;
      font-size: 11px;
      color: var(--mut, #7a8090);
      font-weight: 700;
      background: rgba(255, 255, 255, 0.05);
      padding: 2px 8px;
      border-radius: 20px;
      border: 1px solid rgba(255, 255, 255, 0.05);
    }

    .bubble-text {
      font-size: 14.5px;
      line-height: 1.6;
      color: var(--txt, #e8eaf6);
      margin: 0;
      font-weight: 500;
    }

    .bubble-footer {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      margin-top: 4px;
    }

    .bubble-next-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      font-family: var(--display, 'Space Grotesk'), sans-serif;
      font-size: 10.5px;
      color: var(--accent2, #fbbf24);
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      user-select: none;
      cursor: pointer;
      background: rgba(251, 191, 36, 0.08);
      border: 1px solid rgba(251, 191, 36, 0.2);
      padding: 6px 14px;
      border-radius: 30px;
      transition: all 0.2s ease;
    }

    .bubble-next-btn:hover {
      background: rgba(251, 191, 36, 0.15);
      border-color: var(--accent2, #fbbf24);
      box-shadow: 0 0 12px rgba(251, 191, 36, 0.35);
      transform: translateX(2px);
    }

    .bubble-next-btn svg {
      transition: transform 0.2s ease;
    }

    .bubble-next-btn:hover svg {
      transform: translateX(3px);
    }
  `],
  template: `
    @if (tutorialService.activeTutorial()) {
      <div class="tutorial-overlay" (click)="onOverlayClick()">
        
        <!-- Mask / Spotlight Backdrop -->
        @if (spotlightStyle()) {
          <div class="tutorial-spotlight" [ngStyle]="spotlightStyle()"></div>
        } @else {
          <div class="tutorial-mask-dark"></div>
        }

        <!-- Character & Speech Bubble container -->
        <div class="tutorial-container" (click)="$event.stopPropagation()">
          
          <!-- Pikachu pose display -->
          <div class="pikachu-wrapper">
            <img 
              [src]="getPikachuImageUrl()" 
              [alt]="'Profesor Pikachu Pose ' + currentStep()?.pose" 
              class="pikachu-img"
            />
          </div>

          <!-- Dialog bubble -->
          <div class="speech-bubble">
            <div class="bubble-header">
              <span class="bubble-title">⚡ Profesor Pikachu</span>
              <span class="bubble-counter">{{ currentStepIndex() + 1 }} / {{ totalSteps() }}</span>
            </div>

            <p class="bubble-text">
              {{ currentStep()?.text }}
            </p>

            <div class="bubble-footer">
              <div class="bubble-next-btn" (click)="onNextClick()">
                <span>{{ isLastStep() ? 'Entendido' : 'Siguiente' }}</span>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M5 12h14M12 5l7 7-7 7"/>
                </svg>
              </div>
            </div>
          </div>

        </div>

      </div>
    }
  `
})
export class HelpAgentComponent {
  readonly tutorialService = inject(TutorialService);

  private resizeTrigger = signal(0);

  readonly currentStepIndex = computed(() => this.tutorialService.currentStepIndex());
  readonly totalSteps = computed(() => this.tutorialService.currentSteps().length);
  readonly currentStep = computed(() => this.tutorialService.currentSteps()[this.currentStepIndex()]);
  readonly isLastStep = computed(() => this.currentStepIndex() === this.totalSteps() - 1);

  constructor() {
    effect(() => {
      const step = this.currentStep();
      if (step) {
        // Recalculate spotlight after a layout tick
        setTimeout(() => {
          this.resizeTrigger.update(v => v + 1);
        }, 50);

        // Backup update in case of transitions
        setTimeout(() => {
          this.resizeTrigger.update(v => v + 1);
        }, 250);
      }
    });
  }

  @HostListener('window:resize')
  onResize() {
    this.resizeTrigger.update(v => v + 1);
  }

  readonly spotlightStyle = computed(() => {
    // Read the signal dependencies
    this.resizeTrigger();
    const step = this.currentStep();
    if (!step || !step.targetSelector) return null;

    const el = document.querySelector(step.targetSelector);
    if (!el) return null;

    const rect = el.getBoundingClientRect();
    const pad = 12; // slightly larger padding for a nicer halo around elements

    return {
      display: 'block',
      left: `${rect.left - pad}px`,
      top: `${rect.top - pad}px`,
      width: `${rect.width + pad * 2}px`,
      height: `${rect.height + pad * 2}px`
    };
  });

  getPikachuImageUrl(): string {
    const step = this.currentStep();
    const pose = step?.pose || 1;
    return `assets/pikachu_professor_pose${pose}.png`;
  }

  onOverlayClick() {
    this.tutorialService.nextStep();
  }

  onNextClick() {
    this.tutorialService.nextStep();
  }
}
