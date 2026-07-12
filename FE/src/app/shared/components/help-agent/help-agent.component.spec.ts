import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { HelpAgentComponent } from './help-agent.component';
import { TutorialService } from '../../../core/services/tutorial.service';

describe('HelpAgentComponent', () => {
  let fixture: ComponentFixture<HelpAgentComponent>;
  let component: HelpAgentComponent;
  let tutorialService: TutorialService;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [HelpAgentComponent, HttpClientTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(HelpAgentComponent);
    component = fixture.componentInstance;
    tutorialService = TestBed.inject(TutorialService);
  });

  afterEach(() => localStorage.clear());

  it('renders nothing when there is no active tutorial', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.tutorial-overlay')).toBeNull();
  });

  it('renders the overlay with the first step once a tutorial is triggered', () => {
    tutorialService.triggerTutorial('lobby');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.tutorial-overlay')).not.toBeNull();
    expect(component.currentStepIndex()).toBe(0);
    expect(component.totalSteps()).toBe(4);
    expect(fixture.nativeElement.querySelector('.bubble-counter').textContent).toContain('1 / 4');
  });

  it('is not the last step at index 0 of a 4-step tutorial', () => {
    tutorialService.triggerTutorial('lobby');
    fixture.detectChanges();
    expect(component.isLastStep()).toBeFalse();
  });

  it('is the last step once advanced to the final index', () => {
    tutorialService.triggerTutorial('lobby');
    tutorialService.nextStep();
    tutorialService.nextStep();
    tutorialService.nextStep();
    fixture.detectChanges();

    expect(component.currentStepIndex()).toBe(3);
    expect(component.isLastStep()).toBeTrue();
  });

  it('shows "Siguiente" before the last step and "Entendido" on the last step', () => {
    tutorialService.triggerTutorial('lobby');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.bubble-next-btn span').textContent).toBe('Siguiente');

    tutorialService.nextStep();
    tutorialService.nextStep();
    tutorialService.nextStep();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.bubble-next-btn span').textContent).toBe('Entendido');
  });

  describe('spotlightStyle', () => {
    it('is null for a step with no targetSelector (first lobby step)', () => {
      tutorialService.triggerTutorial('lobby');
      fixture.detectChanges();
      expect(component.spotlightStyle()).toBeNull();
      expect(fixture.nativeElement.querySelector('.tutorial-mask-dark')).not.toBeNull();
    });

    it('is null when the target selector does not match any element on the page', () => {
      tutorialService.triggerTutorial('lobby');
      tutorialService.nextStep(); // step 2 targets #btn-battle, which doesn't exist in this test's DOM
      fixture.detectChanges();
      expect(component.spotlightStyle()).toBeNull();
    });

    it('computes a padded bounding box when the target element exists', () => {
      const target = document.createElement('div');
      target.id = 'btn-battle';
      Object.defineProperty(target, 'getBoundingClientRect', {
        value: () => ({ left: 100, top: 50, width: 200, height: 40, right: 300, bottom: 90, x: 100, y: 50, toJSON: () => ({}) }),
      });
      document.body.appendChild(target);

      try {
        tutorialService.triggerTutorial('lobby');
        tutorialService.nextStep(); // step 2 targets #btn-battle
        fixture.detectChanges();

        const style = component.spotlightStyle();
        expect(style).toEqual({
          display: 'block',
          left: '88px',
          top: '38px',
          width: '224px',
          height: '64px',
        });
      } finally {
        document.body.removeChild(target);
      }
    });
  });

  describe('getPikachuImageUrl', () => {
    it('resolves the image path for the current step\'s pose', () => {
      tutorialService.triggerTutorial('lobby');
      fixture.detectChanges();
      expect(component.getPikachuImageUrl()).toBe('assets/pikachu_pose_1.png');

      tutorialService.nextStep();
      fixture.detectChanges();
      expect(component.getPikachuImageUrl()).toBe('assets/pikachu_pose_2.png');
    });

    it('defaults to pose 1 when there is no current step', () => {
      expect(component.getPikachuImageUrl()).toBe('assets/pikachu_pose_1.png');
    });
  });

  describe('interactions', () => {
    it('advances the tutorial on overlay click', () => {
      tutorialService.triggerTutorial('lobby');
      fixture.detectChanges();

      component.onOverlayClick();
      expect(tutorialService.currentStepIndex()).toBe(1);
    });

    it('advances the tutorial on next-button click', () => {
      tutorialService.triggerTutorial('lobby');
      fixture.detectChanges();

      component.onNextClick();
      expect(tutorialService.currentStepIndex()).toBe(1);
    });

    it('closes the tutorial once "Entendido" is clicked on the last step', () => {
      tutorialService.triggerTutorial('lobby');
      tutorialService.nextStep();
      tutorialService.nextStep();
      tutorialService.nextStep();
      fixture.detectChanges();

      component.onNextClick();

      expect(tutorialService.activeTutorial()).toBeNull();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.tutorial-overlay')).toBeNull();
    });
  });
});
