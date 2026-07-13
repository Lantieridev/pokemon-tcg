import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SigCardComponent, HoloCardComponent, FloatCardComponent } from './holo-card.component';

describe('SigCardComponent', () => {
  let fixture: ComponentFixture<SigCardComponent>;
  let component: SigCardComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [SigCardComponent] }).compileComponents();
    fixture = TestBed.createComponent(SigCardComponent);
    component = fixture.componentInstance;
  });

  it('uses fire tones by default', () => {
    fixture.detectChanges();
    expect(component.tones()).toEqual(['#ff8a4c', '#c0392b', '#3a1410']);
  });

  it('uses water tones for the water type', () => {
    fixture.componentRef.setInput('type', 'water');
    fixture.detectChanges();
    expect(component.tones()).toEqual(['#5aa9e6', '#2a5a9e', '#0c1f38']);
  });

  it('uses lightning tones for the lightning type', () => {
    fixture.componentRef.setInput('type', 'lightning');
    fixture.detectChanges();
    expect(component.tones()).toEqual(['#f5d34c', '#d6a31f', '#423207']);
  });

  it('falls back to fire tones for an unrecognized type', () => {
    fixture.componentRef.setInput('type', 'psychic');
    fixture.detectChanges();
    expect(component.tones()).toEqual(['#ff8a4c', '#c0392b', '#3a1410']);
  });

  it('renders the card name in the template', () => {
    fixture.componentRef.setInput('name', 'PIKACHU');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('PIKACHU');
  });
});

describe('HoloCardComponent', () => {
  let fixture: ComponentFixture<HoloCardComponent>;
  let component: HoloCardComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HoloCardComponent] }).compileComponents();
    fixture = TestBed.createComponent(HoloCardComponent);
    component = fixture.componentInstance;
  });

  describe('holoTier', () => {
    it('is "basic" when there is no card', () => {
      expect(component.holoTier).toBe('basic');
    });

    it('is "basic" for a plain card with no special rarity or subtype', () => {
      component.card = { name: 'Pidgey', rarity: 'Common', subtypes: ['Basic'] };
      expect(component.holoTier).toBe('basic');
    });

    it('is "sir" for Special Illustration Rare', () => {
      component.card = { name: 'Charizard', rarity: 'Special Illustration Rare' };
      expect(component.holoTier).toBe('sir');
    });

    it('is "sir" for Hyper Rare', () => {
      component.card = { name: 'Charizard', rarity: 'Hyper Rare' };
      expect(component.holoTier).toBe('sir');
    });

    it('is "sir" for Secret Rare', () => {
      component.card = { name: 'Charizard', rarity: 'Secret Rare' };
      expect(component.holoTier).toBe('sir');
    });

    it('is "fullart" for Ultra Rare', () => {
      component.card = { name: 'Charizard', rarity: 'Ultra Rare' };
      expect(component.holoTier).toBe('fullart');
    });

    it('is "fullart" for Illustration Rare', () => {
      component.card = { name: 'Charizard', rarity: 'Illustration Rare' };
      expect(component.holoTier).toBe('fullart');
    });

    it('is "ex" when subtypes include EX', () => {
      component.card = { name: 'Charizard', rarity: 'Rare Holo', subtypes: ['EX'] };
      expect(component.holoTier).toBe('ex');
    });

    it('is "ex" when subtypes include VMAX', () => {
      component.card = { name: 'Charizard', rarity: 'Rare Holo', subtypes: ['VMAX'] };
      expect(component.holoTier).toBe('ex');
    });

    it('is "ex" when the name ends in " ex" even without a matching subtype', () => {
      component.card = { name: 'Charizard ex', rarity: 'Rare Holo', subtypes: [] };
      expect(component.holoTier).toBe('ex');
    });

    it('is "ex" for Double Rare', () => {
      component.card = { name: 'Charizard', rarity: 'Double Rare', subtypes: [] };
      expect(component.holoTier).toBe('ex');
    });

    it('prioritizes "sir" over "ex" when both would otherwise match', () => {
      component.card = { name: 'Charizard ex', rarity: 'Secret Rare', subtypes: ['EX'] };
      expect(component.holoTier).toBe('sir');
    });
  });

  describe('template rendering', () => {
    it('renders the card image when card.img is present', () => {
      component.card = { img: 'https://images.pokemontcg.io/xy1/4.png', name: 'Charizard' };
      fixture.detectChanges();
      const img = fixture.nativeElement.querySelector('img.holo__img');
      expect(img).not.toBeNull();
      expect(fixture.nativeElement.querySelector('app-sig-card')).toBeNull();
    });

    it('falls back to the sig-card placeholder when there is no card image', () => {
      component.card = null;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('img.holo__img')).toBeNull();
      expect(fixture.nativeElement.querySelector('app-sig-card')).not.toBeNull();
    });

    it('applies the holo--ex class when the card resolves to the ex tier', () => {
      component.card = { name: 'Charizard', rarity: 'Rare Holo', subtypes: ['EX'] };
      fixture.detectChanges();
      const holo = fixture.nativeElement.querySelector('.holo');
      expect(holo.classList.contains('holo--ex')).toBeTrue();
    });
  });

  describe('pointer interaction', () => {
    it('sets tilt CSS custom properties on the rotation element based on pointer position', () => {
      component.card = { img: 'https://images.pokemontcg.io/xy1/4.png', name: 'Charizard' };
      fixture.detectChanges();

      const container: HTMLElement = component.containerRef.nativeElement;
      spyOn(container, 'getBoundingClientRect').and.returnValue({
        left: 0, top: 0, width: 200, height: 280, right: 200, bottom: 280, x: 0, y: 0, toJSON: () => ({}),
      } as DOMRect);
      spyOn(window, 'requestAnimationFrame').and.callFake((cb: FrameRequestCallback) => {
        cb(0);
        return 0;
      });

      component.onMove({ clientX: 100, clientY: 140 } as PointerEvent);

      const rotEl: HTMLElement = component.rotRef.nativeElement;
      expect(rotEl.style.getPropertyValue('--mx')).toBe('50.0%');
      expect(rotEl.style.getPropertyValue('--my')).toBe('50.0%');
      expect(rotEl.style.getPropertyValue('--o')).toBe('1');
    });

    it('does nothing when the container ref is unavailable (defensive guard)', () => {
      expect(() => component.onMove({ clientX: 0, clientY: 0 } as PointerEvent)).not.toThrow();
    });

    it('resets the CSS custom properties on pointer leave', () => {
      component.card = { img: 'https://images.pokemontcg.io/xy1/4.png', name: 'Charizard' };
      fixture.detectChanges();

      const rotEl: HTMLElement = component.rotRef.nativeElement;
      rotEl.style.setProperty('--o', '1');
      component.reset();
      expect(rotEl.style.getPropertyValue('--o')).toBe('0');
      expect(rotEl.style.getPropertyValue('--rx')).toBe('0deg');
    });
  });
});

describe('FloatCardComponent', () => {
  let fixture: ComponentFixture<FloatCardComponent>;
  let component: FloatCardComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [FloatCardComponent] }).compileComponents();
    fixture = TestBed.createComponent(FloatCardComponent);
    component = fixture.componentInstance;
  });

  it('shows the real image when the card has an image and has not errored', () => {
    component.card = { img: 'https://images.pokemontcg.io/xy1/4.png', name: 'Charizard' };
    fixture.detectChanges();
    expect(component.show()).toBeTrue();
  });

  it('falls back to the sig-card placeholder when the card has no image', () => {
    component.card = { name: 'Charizard' };
    fixture.detectChanges();
    expect(component.show()).toBeFalsy();
    expect(fixture.nativeElement.querySelector('app-sig-card')).not.toBeNull();
  });

  it('falls back to the placeholder once the image errors, even if it had an img url', () => {
    component.card = { img: 'https://images.pokemontcg.io/xy1/4.png', name: 'Charizard' };
    fixture.detectChanges();
    component.err.set(true);
    fixture.detectChanges();
    expect(component.show()).toBeFalsy();
  });

  it('starts with loaded=false and flips to true once the image load event fires', () => {
    component.card = { img: 'https://images.pokemontcg.io/xy1/4.png', name: 'Charizard' };
    fixture.detectChanges();
    expect(component.loaded()).toBeFalse();

    const img: HTMLImageElement = fixture.nativeElement.querySelector('img');
    img.dispatchEvent(new Event('load'));
    expect(component.loaded()).toBeTrue();
  });
});
