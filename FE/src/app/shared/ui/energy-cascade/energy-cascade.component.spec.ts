import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { EnergyCascadeComponent } from './energy-cascade.component';
import { PokemonTcgService } from '../../../core/services/pokemon-tcg.service';

describe('EnergyCascadeComponent', () => {
  let fixture: ComponentFixture<EnergyCascadeComponent>;
  let component: EnergyCascadeComponent;
  let tcgService: PokemonTcgService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnergyCascadeComponent, HttpClientTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(EnergyCascadeComponent);
    component = fixture.componentInstance;
    tcgService = TestBed.inject(PokemonTcgService);
    fixture.detectChanges();
  });

  it('renders nothing when there are no energies', () => {
    expect(fixture.nativeElement.querySelector('.card-img')).toBeNull();
  });

  it('renders one image per energy, in order', () => {
    fixture.componentRef.setInput('energies', ['fire', 'water']);
    fixture.detectChanges();
    const imgs = fixture.nativeElement.querySelectorAll('.card-img');
    expect(imgs.length).toBe(2);
    expect(imgs[0].src).toContain('133.png');
    expect(imgs[1].src).toContain('134.png');
  });

  describe('getCard resolution order', () => {
    it('prefers a live card from the PokemonTcgService registry when present', () => {
      tcgService.cards.set([
        { id: 'custom-1', name: 'Custom Card', images: { small: 'https://example.com/small.png', large: 'https://example.com/large.png' } } as any,
      ]);

      const card = component.getCard('custom-1');
      expect(card.img).toBe('https://example.com/small.png');
    });

    it('falls back to the large image when a live card has no small image', () => {
      tcgService.cards.set([
        { id: 'custom-2', name: 'Custom Card 2', images: { large: 'https://example.com/large-only.png' } } as any,
      ]);

      const card = component.getCard('custom-2');
      expect(card.img).toBe('https://example.com/large-only.png');
    });

    it('falls back to the standard energy type map when not in the live registry', () => {
      const card = component.getCard('grass');
      expect(card.name).toBe('Grass Energy');
      expect(card.img).toContain('132.png');
    });

    it('strips an "e_" prefix before matching the energy type map', () => {
      const card = component.getCard('e_fighting');
      expect(card.name).toBe('Fighting Energy');
    });

    it('falls back to the mock CARDS registry when the id is a Pokemon, not an energy type', () => {
      // 'pikachu' is not in the standard energy-type map, so this specifically
      // exercises the CARDS[id.toLowerCase()] mock-fallback branch.
      const card = component.getCard('pikachu');
      expect(card.name).toBe('Pikachu');
      expect(card.img).toContain('base1/58');
    });

    it('parses a "set-number" id format when nothing else matches', () => {
      const card = component.getCard('xy2-55');
      expect(card.img).toBe('https://images.pokemontcg.io/xy2/55.png');
      expect(card.name).toBe('Energy');
    });

    it('returns the ultimate fallback image for a completely unrecognized id', () => {
      const card = component.getCard('totally-unknown-id-format-here');
      expect(card.img).toBe('https://images.pokemontcg.io/xy1/130.png');
    });

    it('returns the default colorless image for an empty id', () => {
      const card = component.getCard('');
      expect(card.img).toBe('https://images.pokemontcg.io/xy1/130.png');
    });
  });

  describe('getTopOffset', () => {
    it('stacks upward with negative offsets when direction is "up"', () => {
      fixture.componentRef.setInput('direction', 'up');
      fixture.componentRef.setInput('cardW', 100);
      fixture.detectChanges();

      expect(component.getTopOffset(0)).toBe('-22px');
      expect(component.getTopOffset(1)).toBe('-44px');
    });

    it('stacks downward from below the card when direction is "down"', () => {
      fixture.componentRef.setInput('direction', 'down');
      fixture.componentRef.setInput('cardW', 100);
      fixture.detectChanges();

      // cardH = 100 * 1.4 = 140; step = max(22, 100*0.18) = 22
      expect(component.getTopOffset(0)).toBe('140px');
      expect(component.getTopOffset(1)).toBe('162px');
    });

    it('uses a proportionally larger step for wider cards', () => {
      fixture.componentRef.setInput('direction', 'up');
      fixture.componentRef.setInput('cardW', 200);
      fixture.detectChanges();

      // step = max(22, 200*0.18) = 36
      expect(component.getTopOffset(0)).toBe('-36px');
    });
  });

  describe('getStyles', () => {
    it('derives the container width from cardW', () => {
      fixture.componentRef.setInput('cardW', 100);
      fixture.detectChanges();
      expect(component.getStyles().width).toBe('95px');
    });
  });
});
