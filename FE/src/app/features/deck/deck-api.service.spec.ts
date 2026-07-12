import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { DeckApiService } from './deck-api.service';
import { AuthService } from '../../core/services/auth.service';
import { DeckStore } from '../../core/store/deck.store';
import { PokemonTcgCard } from '../../core/models/game-state.models';
import { signal } from '@angular/core';

describe('DeckApiService', () => {
  let service: DeckApiService;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let deckStoreSpy: jasmine.SpyObj<DeckStore>;

  const mockCard: PokemonTcgCard = {
    id: 'xy1-1',
    name: 'Ivysaur',
    supertype: 'Pokémon',
    subtypes: ['Basic'],
    images: { small: 'ivy.png', large: 'ivy_large.png' },
    set: { id: 'xy1' }
  };

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      userId: 123
    });

    deckStoreSpy = jasmine.createSpyObj('DeckStore', [], {
      deckGrouped: signal([{ card: mockCard, count: 2 }])
    });

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        DeckApiService,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: DeckStore, useValue: deckStoreSpy }
      ]
    });

    service = TestBed.inject(DeckApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should post new deck on saveDeck', () => {
    service.saveDeck('Test Deck', 'VALID').subscribe((res) => {
      expect(res.id).toBe(1);
      expect(res.name).toBe('Test Deck');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/decks`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      userId: 123,
      name: 'Test Deck',
      status: 'VALID',
      cards: [{ cardId: 'xy1-1', quantity: 2 }]
    });

    req.flush({ id: 1, name: 'Test Deck', status: 'VALID', createdAt: '', cards: [] });
  });

  it('should throw error on saveDeck if no authenticated user', () => {
    Object.defineProperty(authServiceSpy, 'userId', { get: () => undefined });
    expect(() => service.saveDeck('Test Deck', 'VALID')).toThrowError('No hay usuario autenticado para guardar el mazo.');
  });

  it('should put deck on updateDeck', () => {
    service.updateDeck(1, 'Updated Deck', 'DRAFT').subscribe((res) => {
      expect(res.name).toBe('Updated Deck');
      expect(res.status).toBe('DRAFT');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/decks/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.name).toBe('Updated Deck');
    expect(req.request.body.status).toBe('DRAFT');

    req.flush({ id: 1, name: 'Updated Deck', status: 'DRAFT', createdAt: '', cards: [] });
  });

  it('should delete deck on deleteDeck', () => {
    service.deleteDeck(1).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/decks/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should get decks by user id', () => {
    service.getDecksByUserId(123).subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/decks/user/123`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, name: 'User Deck' }]);
  });

  it('should call autocomplete deck', () => {
    service.autocompleteDeck().subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/decks/assistant/autocomplete`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual([{ cardId: 'xy1-1', quantity: 2 }]);
    req.flush([{ cardId: 'xy1-2', quantity: 1 }]);
  });

  it('should get deck by id', () => {
    service.getDeckById(1).subscribe((res) => {
      expect(res.id).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/decks/1`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 1 });
  });

  it('should clone template', () => {
    service.cloneTemplate(123, 9).subscribe((res) => {
      expect(res.id).toBe(10);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/decks/users/123/clone/9`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 10 });
  });

  it('should generate wizard deck', () => {
    service.generateWizardDeck('Fire/Water').subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/decks/assistant/wizard`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ theme: 'Fire/Water' });
    req.flush([{ cardId: 'xy1-2', quantity: 60 }]);
  });
});
