import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PokemonTcgService } from './pokemon-tcg.service';

describe('PokemonTcgService', () => {
  let service: PokemonTcgService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PokemonTcgService]
    });
    service = TestBed.inject(PokemonTcgService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch cards from API and cache them in localStorage', () => {
    const dummyCards = [{ id: 'xy1-1', name: 'Ivysaur', supertype: 'Pokémon' }];

    service.getCards().subscribe(cards => {
      expect(cards.length).toBe(1);
      expect(cards).toEqual(dummyCards);
      expect(localStorage.getItem('pokemon_cards_xy1')).toBe(JSON.stringify(dummyCards));
    });

    const req = httpMock.expectOne('https://api.pokemontcg.io/v2/cards?q=set.id:xy1&pageSize=250&page=1');
    expect(req.request.method).toBe('GET');
    req.flush({ data: dummyCards });
  });

  it('should return cards from localStorage cache if present', () => {
    const cachedCards = [{ id: 'xy1-2', name: 'Venusaur', supertype: 'Pokémon' }];
    localStorage.setItem('pokemon_cards_xy1', JSON.stringify(cachedCards));

    service.getCards().subscribe(cards => {
      expect(cards.length).toBe(1);
      expect(cards).toEqual(cachedCards);
    });

    httpMock.expectNone('https://api.pokemontcg.io/v2/cards?q=set.id:xy1&pageSize=250&page=1');
  });
});
