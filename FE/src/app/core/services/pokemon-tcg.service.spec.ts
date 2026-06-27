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

    service.loadCards();

    const req = httpMock.expectOne('https://api.pokemontcg.io/v2/cards?q=set.id:xy1&pageSize=250');
    expect(req.request.method).toBe('GET');
    req.flush({ data: dummyCards });

    expect(service.cards().length).toBe(1);
    expect(service.cards()).toEqual(dummyCards as any);
    expect(localStorage.getItem('xy1_cards_cache')).toBe(JSON.stringify(dummyCards));
  });

  it('should return cards from localStorage cache if present', () => {
    const cachedCards = [{ id: 'xy1-2', name: 'Venusaur', supertype: 'Pokémon' }];
    localStorage.setItem('xy1_cards_cache', JSON.stringify(cachedCards));

    service.loadCards();

    expect(service.cards().length).toBe(1);
    expect(service.cards()).toEqual(cachedCards as any);

    httpMock.expectNone('https://api.pokemontcg.io/v2/cards?q=set.id:xy1&pageSize=250');
  });
});
