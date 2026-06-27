import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Deck } from './deck';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { DeckStore } from '../../core/store/deck.store';
import { DeckApiService } from './deck-api.service';
import { signal } from '@angular/core';

describe('Deck Component', () => {
  let component: Deck;
  let fixture: ComponentFixture<Deck>;
  let tcgServiceSpy: any;
  let deckStoreSpy: any;
  let deckApiSpy: any;

  beforeEach(async () => {
    tcgServiceSpy = jasmine.createSpyObj('PokemonTcgService', ['loadCards']);
    tcgServiceSpy.cards = signal([]);
    tcgServiceSpy.isLoading = signal(false);
    tcgServiceSpy.loadError = signal(null);

    deckStoreSpy = jasmine.createSpyObj('DeckStore', ['addCard', 'removeCard', 'clearDeck', 'removeAllCopies']);
    deckStoreSpy.deckGrouped = signal([]);
    deckStoreSpy.totalCount = signal(0);
    deckStoreSpy.validation = signal(null);
    deckStoreSpy.isValid = signal(false);
    deckStoreSpy.cardCountById = signal({});
    deckStoreSpy.deckName = signal('');
    deckStoreSpy.deckCards = signal([]);

    deckApiSpy = jasmine.createSpyObj('DeckApiService', ['saveDeck']);

    await TestBed.configureTestingModule({
      imports: [Deck],
      providers: [
        { provide: PokemonTcgService, useValue: tcgServiceSpy },
        { provide: DeckStore, useValue: deckStoreSpy },
        { provide: DeckApiService, useValue: deckApiSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Deck);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });
});
