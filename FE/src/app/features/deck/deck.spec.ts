import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Deck } from './deck';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { AuthService } from '../../core/services/auth.service';
import { of } from 'rxjs';

describe('Deck Component', () => {
  let component: Deck;
  let fixture: ComponentFixture<Deck>;
  let tcgServiceSpy: jasmine.SpyObj<PokemonTcgService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let httpMock: HttpTestingController;

  const mockApiCards = [
    { id: 'xy1-1', name: 'Ivysaur', supertype: 'Pokémon', types: ['Grass'], hp: '90', images: { small: 'ivy.png' } },
    { id: 'xy1-2', name: 'Fire Energy', supertype: 'Energy', images: { small: 'fire.png' } }
  ];

  beforeEach(async () => {
    tcgServiceSpy = jasmine.createSpyObj('PokemonTcgService', ['getCards']);
    tcgServiceSpy.getCards.and.returnValue(of(mockApiCards));

    authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      userId: 1
    });

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, Deck],
      providers: [
        { provide: PokemonTcgService, useValue: tcgServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Deck);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load cards on init', () => {
    expect(component.allCards.length).toBe(2);
    expect(component.allCards[0].id).toBe('xy1-1');
    expect(component.allCards[0].energy).toBeFalse();
    expect(component.allCards[1].id).toBe('xy1-2');
    expect(component.allCards[1].energy).toBeTrue();
  });

  it('should add cards to the deck list', () => {
    component.addCard('xy1-1');
    expect(component.deck().length).toBe(1);
    expect(component.deck()[0]).toBe('xy1-1');
  });

  it('should restrict adding non-energy cards to max 4 copies', () => {
    for (let i = 0; i < 6; i++) {
      component.addCard('xy1-1');
    }
    expect(component.deck().filter(id => id === 'xy1-1').length).toBe(4);
  });

  it('should allow adding energy cards past 4 copies', () => {
    for (let i = 0; i < 6; i++) {
      component.addCard('xy1-2');
    }
    expect(component.deck().filter(id => id === 'xy1-2').length).toBe(6);
  });

  it('should save draft to localStorage', () => {
    component.deckName.set('Water Deck');
    component.deck.set(['xy1-1']);
    
    spyOn(window, 'alert');
    component.saveDraft();

    expect(localStorage.getItem('deck_draft_name')).toBe('Water Deck');
    expect(localStorage.getItem('deck_draft_cards')).toBe(JSON.stringify(['xy1-1']));
    expect(window.alert).toHaveBeenCalledWith('Borrador guardado localmente.');
  });

  it('should post deck to backend if exact total is 60', () => {
    // Fill the deck to 60
    const fullDeck = Array(60).fill('xy1-2');
    component.deck.set(fullDeck);

    spyOn(window, 'alert');
    component.saveDeck();

    const req = httpMock.expectOne('http://localhost:8081/api/decks');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.userId).toBe(1);
    expect(req.request.body.cards.length).toBe(1);
    expect(req.request.body.cards[0]).toEqual({ cardId: 'xy1-2', quantity: 60 });
    
    req.flush({ id: 123 });

    expect(window.alert).toHaveBeenCalledWith('Mazo guardado correctamente en el servidor.');
    expect(localStorage.getItem('active_deck_id')).toBe('123');
  });
});
