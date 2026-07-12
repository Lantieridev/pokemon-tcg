import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { SimpleChange } from '@angular/core';
import { FieldPokemonComponent } from './field-pokemon.component';
import { PokemonTcgService } from '../../../core/services/pokemon-tcg.service';

describe('FieldPokemonComponent', () => {
  let fixture: ComponentFixture<FieldPokemonComponent>;
  let component: FieldPokemonComponent;
  let tcgService: PokemonTcgService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FieldPokemonComponent, HttpClientTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(FieldPokemonComponent);
    component = fixture.componentInstance;
    tcgService = TestBed.inject(PokemonTcgService);
    fixture.componentRef.setInput('cardId', 'pikachu');
  });

  describe('card resolution', () => {
    it('prefers a live card from the PokemonTcgService registry when present', () => {
      tcgService.cards.set([
        { id: 'pikachu', name: 'Pikachu Live', types: ['Lightning'], images: { small: 'https://example.com/pika.png' } } as any,
      ]);
      fixture.detectChanges();
      expect(component.card.name).toBe('Pikachu Live');
      expect(component.card.type).toBe('lightning');
      expect(component.card.img).toBe('https://example.com/pika.png');
    });

    it('falls back to the mock CARDS registry when not in the live registry', () => {
      fixture.detectChanges();
      expect(component.card.name).toBe('Pikachu');
      expect(component.card.type).toBe('lightning');
    });

    it('parses a "set-number" id format when nothing else matches', () => {
      fixture.componentRef.setInput('cardId', 'xy3-77');
      fixture.detectChanges();
      expect(component.card.img).toBe('https://images.pokemontcg.io/xy3/77.png');
      expect(component.card.name).toBe('Pokémon');
    });

    it('returns the ultimate fallback for a completely unrecognized id', () => {
      fixture.componentRef.setInput('cardId', 'nonexistent-card-id-here');
      fixture.detectChanges();
      // 5 hyphen-split parts, doesn't match the set-number format either
      expect(component.card.img).toBe('https://images.pokemontcg.io/xy1/130.png');
    });
  });

  describe('rot (rotation by status)', () => {
    it('is 0 for the default "none" status', () => {
      fixture.detectChanges();
      expect(component.rot).toBe(0);
    });

    it('is -90 for asleep', () => {
      fixture.componentRef.setInput('status', 'asleep');
      fixture.detectChanges();
      expect(component.rot).toBe(-90);
    });

    it('is 90 for paralyzed', () => {
      fixture.componentRef.setInput('status', 'paralyzed');
      fixture.detectChanges();
      expect(component.rot).toBe(90);
    });

    it('is 180 for confused', () => {
      fixture.componentRef.setInput('status', 'confused');
      fixture.detectChanges();
      expect(component.rot).toBe(180);
    });
  });

  describe('HP calculations', () => {
    it('computes currentHp as maxHp minus damage, floored at 0', () => {
      fixture.componentRef.setInput('maxHp', 100);
      fixture.componentRef.setInput('damage', 30);
      fixture.detectChanges();
      expect(component.currentHp).toBe(70);
    });

    it('never lets currentHp go negative when damage exceeds maxHp', () => {
      fixture.componentRef.setInput('maxHp', 50);
      fixture.componentRef.setInput('damage', 999);
      fixture.detectChanges();
      expect(component.currentHp).toBe(0);
    });

    it('computes hpPercent from currentHp/maxHp', () => {
      fixture.componentRef.setInput('maxHp', 200);
      fixture.componentRef.setInput('damage', 50);
      fixture.detectChanges();
      expect(component.hpPercent).toBe(75);
    });

    it('reports 0% hp when maxHp is not set', () => {
      fixture.detectChanges();
      expect(component.hpPercent).toBe(0);
    });

    it('uses the red bar color when hp is critically low', () => {
      fixture.componentRef.setInput('maxHp', 100);
      fixture.componentRef.setInput('damage', 90);
      fixture.detectChanges();
      expect(component.hpBarColor).toBe('#ee1515');
    });

    it('uses the yellow bar color at medium hp', () => {
      fixture.componentRef.setInput('maxHp', 100);
      fixture.componentRef.setInput('damage', 60);
      fixture.detectChanges();
      expect(component.hpBarColor).toBe('#ffcb05');
    });

    it('uses the green bar color at healthy hp', () => {
      fixture.componentRef.setInput('maxHp', 100);
      fixture.componentRef.setInput('damage', 10);
      fixture.detectChanges();
      expect(component.hpBarColor).toBe('#5ad27a');
    });
  });

  describe('ngOnChanges - displayed HP initialization', () => {
    it('initializes displayedHp on first change when maxHp is set', () => {
      fixture.componentRef.setInput('maxHp', 100);
      fixture.componentRef.setInput('damage', 20);
      component.ngOnChanges({
        cardId: new SimpleChange(undefined, 'pikachu', true),
        maxHp: new SimpleChange(undefined, 100, true),
      });
      expect(component.displayedHp).toBe(80);
      expect(component.displayedHpPercent).toBe(80);
    });

    it('snaps displayedHp instantly when the cardId changes (new Pokemon in slot)', () => {
      fixture.componentRef.setInput('maxHp', 100);
      fixture.componentRef.setInput('damage', 0);
      component.ngOnChanges({
        maxHp: new SimpleChange(undefined, 100, true),
        cardId: new SimpleChange(undefined, 'pikachu', true),
      });
      expect(component.displayedHp).toBe(100);

      fixture.componentRef.setInput('cardId', 'raichu');
      fixture.componentRef.setInput('damage', 40);
      component.ngOnChanges({
        cardId: new SimpleChange('pikachu', 'raichu', false),
      });
      expect(component.displayedHp).toBe(60);
      expect(component.isShaking).toBeFalse();
      expect(component.showDamageNumber).toBeFalse();
    });
  });

  describe('damage visual feedback', () => {
    beforeEach(() => {
      jasmine.clock().install();
      fixture.componentRef.setInput('maxHp', 100);
      fixture.componentRef.setInput('damage', 0);
      component.ngOnChanges({
        maxHp: new SimpleChange(undefined, 100, true),
        cardId: new SimpleChange(undefined, 'pikachu', true),
      });
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('shows the shake, flash, and damage number immediately on a positive damage event', () => {
      fixture.componentRef.setInput('damage', 25);
      fixture.componentRef.setInput('damageEvent', 25);
      component.ngOnChanges({
        damageEvent: new SimpleChange(null, 25, false),
      });

      expect(component.showDamageNumber).toBeTrue();
      expect(component.isShaking).toBeTrue();
      expect(component.isFlashing).toBeTrue();
      expect(component.lastDamageAmount).toBe(25);
    });

    it('does not shake or flash on a healing (negative) damage event', () => {
      fixture.componentRef.setInput('damage', -20);
      fixture.componentRef.setInput('damageEvent', -20);
      component.ngOnChanges({
        damageEvent: new SimpleChange(null, -20, false),
      });

      expect(component.showDamageNumber).toBeTrue();
      expect(component.isShaking).toBeFalse();
      expect(component.isFlashing).toBeFalse();
    });

    it('clears the shake and flash flags after their timers elapse', () => {
      fixture.componentRef.setInput('damage', 25);
      fixture.componentRef.setInput('damageEvent', 25);
      component.ngOnChanges({
        damageEvent: new SimpleChange(null, 25, false),
      });

      jasmine.clock().tick(500);
      expect(component.isShaking).toBeFalse();
      expect(component.isFlashing).toBeFalse();
    });

    it('clears the floating damage number after its timer elapses', () => {
      fixture.componentRef.setInput('damage', 25);
      fixture.componentRef.setInput('damageEvent', 25);
      component.ngOnChanges({
        damageEvent: new SimpleChange(null, 25, false),
      });

      jasmine.clock().tick(1600);
      expect(component.showDamageNumber).toBeFalse();
    });

    it('ignores a damageEvent of exactly 0 (no-op sentinel, not real healing)', () => {
      fixture.componentRef.setInput('damageEvent', 0);
      component.ngOnChanges({
        damageEvent: new SimpleChange(null, 0, false),
      });
      expect(component.showDamageNumber).toBeFalse();
    });
  });

  describe('onToolClick', () => {
    it('emits the attached tool card id and stops propagation', () => {
      fixture.componentRef.setInput('attachedToolCardId', 'tool-42');
      fixture.detectChanges();

      const emitted: string[] = [];
      component.toolClick.subscribe((id: string) => emitted.push(id));

      const event = new MouseEvent('click');
      spyOn(event, 'stopPropagation');
      component.onToolClick(event);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(emitted).toEqual(['tool-42']);
    });

    it('does not emit when there is no attached tool', () => {
      fixture.detectChanges();
      const emitted: string[] = [];
      component.toolClick.subscribe((id: string) => emitted.push(id));

      component.onToolClick(new MouseEvent('click'));

      expect(emitted).toEqual([]);
    });
  });

  describe('ngOnDestroy', () => {
    it('cancels in-flight animations and resets visual feedback flags', () => {
      fixture.componentRef.setInput('maxHp', 100);
      fixture.componentRef.setInput('damage', 25);
      fixture.componentRef.setInput('damageEvent', 25);
      component.ngOnChanges({
        maxHp: new SimpleChange(undefined, 100, true),
        cardId: new SimpleChange(undefined, 'pikachu', true),
        damageEvent: new SimpleChange(null, 25, false),
      });
      expect(component.isShaking).toBeTrue();

      component.ngOnDestroy();

      expect(component.isShaking).toBeFalse();
      expect(component.isFlashing).toBeFalse();
      expect(component.showDamageNumber).toBeFalse();
    });
  });

  describe('template rendering', () => {
    it('renders the card image and HP HUD when a card and maxHp are set', () => {
      fixture.componentRef.setInput('maxHp', 100);
      fixture.componentRef.setInput('damage', 10);
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.card-img')).not.toBeNull();
      expect(fixture.nativeElement.textContent).toContain('90/100');
    });

    it('does not render the HP HUD when maxHp is 0', () => {
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('/0');
    });

    it('renders the tool badge when a tool is attached', () => {
      fixture.componentRef.setInput('attachedToolCardId', 'tool-1');
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.tool-badge')).not.toBeNull();
    });
  });
});
