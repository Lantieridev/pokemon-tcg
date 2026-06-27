import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Deck } from './deck';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { DeckStore } from '../../core/store/deck.store';
import { DeckApiService } from './deck-api.service';
import { PokemonTcgCard } from '../../core/models/game-state.models';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';

describe('Deck Component', () => {
  let component: Deck;
  let fixture: ComponentFixture<Deck>;
  let tcgServiceSpy: jasmine.SpyObj<PokemonTcgService>;
  let deckApiSpy: jasmine.SpyObj<DeckApiService>;
  let deckStore: DeckStore;

  const mockApiCards: PokemonTcgCard[] = [
    {
      id: 'xy1-1',
      name: 'Ivysaur',
      supertype: 'Pok├®mon',
      subtypes: ['Basic'],
      types: ['Grass'],
      hp: '90',
      images: { small: 'ivy.png', large: 'ivy_large.png' },
      set: { id: 'xy1' }
    },
    {
      id: 'xy1-2',
      name: 'Fire Energy',
      supertype: 'Energy',
      subtypes: ['Basic Energy'],
      types: ['Fire'],
      images: { small: 'fire.png', large: 'fire_large.png' },
      set: { id: 'xy1' }
    }
  ];

  beforeEach(async () => {
    tcgServiceSpy = jasmine.createSpyObj('PokemonTcgService', ['loadCards'], {
      cards: signal<PokemonTcgCard[]>(mockApiCards),
      isLoading: signal(false),
      loadError: signal<string | null>(null)
    });

    deckApiSpy = jasmine.createSpyObj('DeckApiService', ['saveDeck']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, Deck],
      providers: [
        { provide: PokemonTcgService, useValue: tcgServiceSpy },
        { provide: DeckApiService, useValue: deckApiSpy },
        DeckStore
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Deck);
    component = fixture.componentInstance;
    deckStore = TestBed.inject(DeckStore);
    
    // Reset store state
    deckStore.clearDeck();
    deckStore.deckName.set('');
    
    fixture.detectChanges();
  });

  it('should call loadCards on init', () => {
    expect(tcgServiceSpy.loadCards).toHaveBeenCalled();
    expect(component.filteredCards().length).toBe(2);
  });

  it('should add cards to the deck list via DeckStore', () => {
    component.addCard(mockApiCards[0]);
    expect(component.totalCount()).toBe(1);
    expect(component.deckGrouped().length).toBe(1);
    expect(component.deckGrouped()[0].card.id).toBe('xy1-1');
  });

  it('should remove card from the deck list via DeckStore', () => {
    component.addCard(mockApiCards[0]);
    expect(component.totalCount()).toBe(1);

    component.removeCard('xy1-1');
    expect(component.totalCount()).toBe(0);
  });

  it('should remove all copies of a card via DeckStore', () => {
    component.addCard(mockApiCards[0]);
    component.addCard(mockApiCards[0]);
    expect(component.totalCount()).toBe(2);

    component.removeAll('xy1-1');
    expect(component.totalCount()).toBe(0);
  });

  it('should clear deck when confirmed', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.addCard(mockApiCards[0]);
    expect(component.totalCount()).toBe(1);

    component.clearDeck();
    expect(component.totalCount()).toBe(0);
  });

  it('should not clear deck if cancel is clicked', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    component.addCard(mockApiCards[0]);
    expect(component.totalCount()).toBe(1);

    component.clearDeck();
    expect(component.totalCount()).toBe(1);
  });

  it('should restrict adding non-energy cards to max 4 copies', () => {
    for (let i = 0; i < 6; i++) {
      if (component.canAdd(mockApiCards[0])) {
        component.addCard(mockApiCards[0]);
      }
    }
    expect(component.totalCount()).toBe(4);
  });

  it('should allow adding energy cards past 4 copies', () => {
    for (let i = 0; i < 6; i++) {
      if (component.canAdd(mockApiCards[1])) {
        component.addCard(mockApiCards[1]);
      }
    }
    expect(component.totalCount()).toBe(6);
  });

  it('should return correct type color and image paths', () => {
    expect(component.getTypeColor(['Fire'])).toBe('#ff7a3d');
    expect(component.getTypeColor(['Grass'])).toBe('#5ad27a');
    expect(component.getTypeColor(['Unknown'])).toBe('#9aa9c7');
    expect(component.getTypeColor(undefined)).toBe('#9aa9c7');

    expect(component.getCardImage(mockApiCards[0])).toBe('ivy.png');
  });

  it('should save deck successfully', () => {
    deckStore.deckName.set('My Deck');
    const validDeck = [mockApiCards[0], ...Array(59).fill(mockApiCards[1])];
    deckStore.loadDeck(validDeck, 'My Deck');

    expect(component.isValid()).toBeTrue();

    deckApiSpy.saveDeck.and.returnValue(of({ id: 123, name: 'My Deck', status: 'VALID', createdAt: '', cards: [] }));

    component.saveDeck();

    expect(component.saveLoading()).toBeFalse();
    expect(component.saveSuccess()).toBeTrue();
    expect(deckApiSpy.saveDeck).toHaveBeenCalledWith('My Deck', 'VALID');
  });

  it('should handle error when saving deck fails', () => {
    deckStore.deckName.set('My Deck');
    const validDeck = [mockApiCards[0], ...Array(59).fill(mockApiCards[1])];
    deckStore.loadDeck(validDeck, 'My Deck');

    expect(component.isValid()).toBeTrue();

    const mockError = { error: { message: 'El mazo no es v├ílido' } };
    deckApiSpy.saveDeck.and.returnValue(throwError(() => mockError));

    component.saveDeck();

    expect(component.saveLoading()).toBeFalse();
    expect(component.saveSuccess()).toBeFalse();
    expect(component.saveError()).toBe('El mazo no es v├ílido');
  });
});
