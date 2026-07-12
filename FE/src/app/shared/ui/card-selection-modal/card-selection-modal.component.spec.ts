import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CardSelectionModalComponent } from './card-selection-modal.component';
import { PokemonTcgService } from '../../../core/services/pokemon-tcg.service';

describe('CardSelectionModalComponent', () => {
  let fixture: ComponentFixture<CardSelectionModalComponent>;
  let component: CardSelectionModalComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardSelectionModalComponent, HttpClientTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(CardSelectionModalComponent);
    component = fixture.componentInstance;
  });

  describe('default single/multi selection mode', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('cardIds', ['pikachu', 'charizard', 'blastoise']);
      fixture.componentRef.setInput('maxSelections', 2);
      fixture.detectChanges();
    });

    it('toggles a card into the selection', () => {
      component.toggleCard(0);
      expect(component.isSelected(0)).toBeTrue();
    });

    it('toggles a selected card back out', () => {
      component.toggleCard(0);
      component.toggleCard(0);
      expect(component.isSelected(0)).toBeFalse();
    });

    it('refuses to select beyond maxSelections', () => {
      component.toggleCard(0);
      component.toggleCard(1);
      component.toggleCard(2);
      expect(component.isSelected(2)).toBeFalse();
      expect(component.selectedIndices().size).toBe(2);
    });

    it('emits the selected card ids on confirm and resets the selection', () => {
      component.toggleCard(0);
      component.toggleCard(2);

      const emitted: string[][] = [];
      component.confirm.subscribe((ids: string[]) => emitted.push(ids));
      component.onConfirm();

      expect(emitted[0]).toEqual(jasmine.arrayWithExactContents(['pikachu', 'blastoise']));
      expect(component.selectedIndices().size).toBe(0);
    });

    it('emits cancel and resets all internal state', () => {
      component.toggleCard(0);
      let cancelled = false;
      component.cancel.subscribe(() => (cancelled = true));

      component.onCancel();

      expect(cancelled).toBeTrue();
      expect(component.selectedIndices().size).toBe(0);
    });

    it('disables the confirm button while nothing is selected', () => {
      const confirmBtn: HTMLButtonElement = fixture.nativeElement.querySelector('.btn-confirm');
      expect(confirmBtn.disabled).toBeTrue();
    });

    it('enables the confirm button once a card is selected', () => {
      component.toggleCard(0);
      fixture.detectChanges();
      const confirmBtn: HTMLButtonElement = fixture.nativeElement.querySelector('.btn-confirm');
      expect(confirmBtn.disabled).toBeFalse();
    });
  });

  describe('CURSED_DROP mode (counter distribution)', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('cardIds', ['pikachu', 'charizard']);
      fixture.componentRef.setInput('maxSelections', 3);
      fixture.componentRef.setInput('sourceEffect', 'CURSED_DROP');
      fixture.detectChanges();
    });

    it('starts every card at 0 counters', () => {
      expect(component.getCounter('pikachu')).toBe(0);
    });

    it('increments a counter up to the max total across all cards', () => {
      component.incrementCounter('pikachu');
      component.incrementCounter('pikachu');
      component.incrementCounter('charizard');
      expect(component.getCounter('pikachu')).toBe(2);
      expect(component.getCounter('charizard')).toBe(1);
      expect(component.totalDistributed()).toBe(3);
    });

    it('refuses to increment once the total hits maxSelections', () => {
      component.incrementCounter('pikachu');
      component.incrementCounter('pikachu');
      component.incrementCounter('pikachu');
      component.incrementCounter('charizard');
      expect(component.totalDistributed()).toBe(3);
      expect(component.getCounter('charizard')).toBe(0);
    });

    it('decrements a counter and removes the entry once it hits 0', () => {
      component.incrementCounter('pikachu');
      component.decrementCounter('pikachu');
      expect(component.getCounter('pikachu')).toBe(0);
      expect(component.totalDistributed()).toBe(0);
    });

    it('refuses to decrement below 0', () => {
      component.decrementCounter('pikachu');
      expect(component.getCounter('pikachu')).toBe(0);
    });

    it('shows the distribution progress in the subtitle', () => {
      component.incrementCounter('pikachu');
      fixture.detectChanges();
      expect(component.displaySubtitle).toContain('Asignados: 1 / 3');
    });

    it('emits one entry per distributed counter on confirm, and resets counters', () => {
      component.incrementCounter('pikachu');
      component.incrementCounter('pikachu');
      component.incrementCounter('charizard');

      const emitted: string[][] = [];
      component.confirm.subscribe((ids: string[]) => emitted.push(ids));
      component.onConfirm();

      expect(emitted[0]).toEqual(jasmine.arrayWithExactContents(['pikachu', 'pikachu', 'charizard']));
      expect(component.totalDistributed()).toBe(0);
    });

    it('disables confirm until at least one counter has been distributed', () => {
      const confirmBtn: HTMLButtonElement = fixture.nativeElement.querySelector('.btn-confirm');
      expect(confirmBtn.disabled).toBeTrue();

      component.incrementCounter('pikachu');
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.btn-confirm').disabled).toBeFalse();
    });
  });

  describe('EAR_INFLUENCE mode (paired moves)', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('cardIds', ['pikachu', 'charizard', 'blastoise']);
      fixture.componentRef.setInput('sourceEffect', 'EAR_INFLUENCE');
      fixture.detectChanges();
    });

    it('selects a source on the first click', () => {
      component.handleEarInfluenceClick('pikachu');
      expect(component.selectedSourceId()).toBe('pikachu');
      expect(component.moves().length).toBe(0);
    });

    it('deselects the source when clicking it again', () => {
      component.handleEarInfluenceClick('pikachu');
      component.handleEarInfluenceClick('pikachu');
      expect(component.selectedSourceId()).toBeNull();
    });

    it('records a move when a different card is clicked as target, and clears the source', () => {
      component.handleEarInfluenceClick('pikachu');
      component.handleEarInfluenceClick('charizard');

      expect(component.moves()).toEqual([{ sourceId: 'pikachu', targetId: 'charizard' }]);
      expect(component.selectedSourceId()).toBeNull();
    });

    it('allows recording several moves in sequence', () => {
      component.handleEarInfluenceClick('pikachu');
      component.handleEarInfluenceClick('charizard');
      component.handleEarInfluenceClick('blastoise');
      component.handleEarInfluenceClick('pikachu');

      expect(component.moves().length).toBe(2);
    });

    it('deletes a specific move by index', () => {
      component.handleEarInfluenceClick('pikachu');
      component.handleEarInfluenceClick('charizard');
      component.handleEarInfluenceClick('blastoise');
      component.handleEarInfluenceClick('pikachu');

      component.deleteMove(0);
      expect(component.moves().length).toBe(1);
      expect(component.moves()[0].sourceId).toBe('blastoise');
    });

    it('resets all moves and the pending source', () => {
      component.handleEarInfluenceClick('pikachu');
      component.handleEarInfluenceClick('charizard');
      component.handleEarInfluenceClick('blastoise');

      component.resetMoves();
      expect(component.moves().length).toBe(0);
      expect(component.selectedSourceId()).toBeNull();
    });

    it('emits source/target pairs flattened on confirm, and resets moves', () => {
      component.handleEarInfluenceClick('pikachu');
      component.handleEarInfluenceClick('charizard');

      const emitted: string[][] = [];
      component.confirm.subscribe((ids: string[]) => emitted.push(ids));
      component.onConfirm();

      expect(emitted[0]).toEqual(['pikachu', 'charizard']);
      expect(component.moves().length).toBe(0);
    });

    it('disables confirm until at least one move has been made', () => {
      const confirmBtn: HTMLButtonElement = fixture.nativeElement.querySelector('.btn-confirm');
      expect(confirmBtn.disabled).toBeTrue();

      component.handleEarInfluenceClick('pikachu');
      component.handleEarInfluenceClick('charizard');
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.btn-confirm').disabled).toBeFalse();
    });
  });

  describe('display title/subtitle per source effect', () => {
    it('shows the Cursed Drop title', () => {
      fixture.componentRef.setInput('sourceEffect', 'CURSED_DROP');
      expect(component.displayTitle).toContain('Cursed Drop');
    });

    it('shows the Ear Influence title', () => {
      fixture.componentRef.setInput('sourceEffect', 'EAR_INFLUENCE');
      expect(component.displayTitle).toContain('Ear Influence');
    });

    it('shows the Flash Claw title and English subtitle', () => {
      fixture.componentRef.setInput('sourceEffect', 'FLASH_CLAW');
      expect(component.displayTitle).toContain('Flash Claw');
      expect(component.displaySubtitle).toContain('discard');
    });

    it('shows the Push Down title', () => {
      fixture.componentRef.setInput('sourceEffect', 'PUSH_DOWN');
      expect(component.displayTitle).toContain('Push Down');
    });

    it('falls back to the plain title/selection-count subtitle for no special effect', () => {
      fixture.componentRef.setInput('title', 'Elegí una carta');
      fixture.componentRef.setInput('maxSelections', 1);
      expect(component.displayTitle).toBe('Elegí una carta');
      expect(component.displaySubtitle).toContain('Seleccionadas: 0 / 1');
    });
  });

  describe('cleanCardId', () => {
    it('strips a namespaced prefix before the colon', () => {
      expect(component.cleanCardId('deck:pikachu')).toBe('pikachu');
    });

    it('returns the id unchanged when there is no colon', () => {
      expect(component.cleanCardId('pikachu')).toBe('pikachu');
    });
  });

  describe('getCardName', () => {
    let tcgService: PokemonTcgService;

    beforeEach(() => {
      tcgService = TestBed.inject(PokemonTcgService);
    });

    it('resolves the real name from the live registry when available', () => {
      tcgService.cards.set([{ id: 'pikachu', name: 'Pikachu (Live)' } as any]);
      expect(component.getCardName('pikachu')).toBe('Pikachu (Live)');
    });

    it('falls back to the cleaned id when the card is not in the live registry', () => {
      expect(component.getCardName('deck:unknown-card')).toBe('unknown-card');
    });
  });

  describe('getCardImageUrl', () => {
    let tcgService: PokemonTcgService;

    beforeEach(() => {
      tcgService = TestBed.inject(PokemonTcgService);
    });

    it('prefers the live registry image when available', () => {
      tcgService.cards.set([
        { id: 'pikachu', images: { small: 'https://example.com/pika-small.png' } } as any,
      ]);
      expect(component.getCardImageUrl('pikachu')).toBe('https://example.com/pika-small.png');
    });

    it('parses a "set-number" id format when not in the live registry', () => {
      expect(component.getCardImageUrl('xy4-12')).toBe('https://images.pokemontcg.io/xy4/12.png');
    });

    it('falls back to the default image for a totally unrecognized id', () => {
      expect(component.getCardImageUrl('totallyunrecognized')).toBe('https://images.pokemontcg.io/xy1/130.png');
    });
  });

  describe('template interaction', () => {
    it('closes the modal when clicking the overlay', () => {
      fixture.componentRef.setInput('cardIds', ['pikachu']);
      fixture.detectChanges();

      let cancelled = false;
      component.cancel.subscribe(() => (cancelled = true));
      fixture.nativeElement.querySelector('.overlay').click();

      expect(cancelled).toBeTrue();
    });

    it('does not close the modal when clicking inside the panel', () => {
      fixture.componentRef.setInput('cardIds', ['pikachu']);
      fixture.detectChanges();

      let cancelled = false;
      component.cancel.subscribe(() => (cancelled = true));
      fixture.nativeElement.querySelector('.modal-panel').click();

      expect(cancelled).toBeFalse();
    });
  });
});
