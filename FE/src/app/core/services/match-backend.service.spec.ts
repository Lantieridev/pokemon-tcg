import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MatchBackendService } from './match-backend.service';
import { AuthService } from './auth.service';
import { GameStateResponseDTO, DeckResponseDTO, DeckSummaryDTO } from '../models/game-state.models';

describe('MatchBackendService', () => {
  let service: MatchBackendService;
  let httpMock: HttpTestingController;
  let authServiceStub: Partial<AuthService>;

  beforeEach(() => {
    authServiceStub = {
      username: 'AshRivero',
      userId: 123
    };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        MatchBackendService,
        { provide: AuthService, useValue: authServiceStub }
      ]
    });

    service = TestBed.inject(MatchBackendService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should call getMatchState with custom X-Player-Id header', () => {
    const mockState = { matchId: 'match-1', turnNumber: 1 } as any as GameStateResponseDTO;

    service.getMatchState('match-1').subscribe((state) => {
      expect(state).toEqual(mockState);
    });

    const req = httpMock.expectOne('http://localhost:8081/api/matches/match-1/state');
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get('X-Player-Id')).toBe('AshRivero');
    req.flush(mockState);
  });

  it('should call getChatHistory', () => {
    const mockChat = [{ sender: 'Ash', message: 'Hi' }];

    service.getChatHistory('match-1').subscribe((chat) => {
      expect(chat).toEqual(mockChat);
    });

    const req = httpMock.expectOne('http://localhost:8081/api/matches/match-1/chat');
    expect(req.request.method).toBe('GET');
    req.flush(mockChat);
  });

  it('should call createMatch', () => {
    service.createMatch('playerA', 'playerB', 10, 20).subscribe((res) => {
      expect(res.matchId).toBe('match-999');
    });

    const req = httpMock.expectOne('http://localhost:8081/api/matches');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      playerAId: 'playerA',
      playerBId: 'playerB',
      deckAId: 10,
      deckBId: 20
    });
    req.flush({ matchId: 'match-999' });
  });

  it('should call getDecks', () => {
    const mockDecks: DeckSummaryDTO[] = [];
    service.getDecks().subscribe((decks) => {
      expect(decks).toEqual(mockDecks);
    });

    const req = httpMock.expectOne('http://localhost:8081/api/decks/user/123');
    expect(req.request.method).toBe('GET');
    req.flush(mockDecks);
  });

  it('should call getTemplates', () => {
    const mockTemplates: DeckSummaryDTO[] = [];
    service.getTemplates().subscribe((templates) => {
      expect(templates).toEqual(mockTemplates);
    });

    const req = httpMock.expectOne('http://localhost:8081/api/decks/templates');
    expect(req.request.method).toBe('GET');
    req.flush(mockTemplates);
  });

  it('should call getDeck', () => {
    const mockDeck = { id: 10, name: 'Deck' } as any as DeckResponseDTO;
    service.getDeck(10).subscribe((deck) => {
      expect(deck).toEqual(mockDeck);
    });

    const req = httpMock.expectOne('http://localhost:8081/api/decks/10');
    expect(req.request.method).toBe('GET');
    req.flush(mockDeck);
  });

  it('should call createBotMatch', () => {
    service.createBotMatch('AshRivero', 10).subscribe((res) => {
      expect(res.matchId).toBe('match-bot');
    });

    const req = httpMock.expectOne('http://localhost:8081/api/matches/bot');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      playerAId: 'AshRivero',
      deckAId: 10,
      playerBId: 'Bot-001',
      deckBId: 10
    });
    req.flush({ matchId: 'match-bot' });
  });

  it('should call surrenderMatch with custom X-Player-Id header', () => {
    service.surrenderMatch('match-1').subscribe();

    const req = httpMock.expectOne('http://localhost:8081/api/matches/match-1/surrender');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('X-Player-Id')).toBe('AshRivero');
    req.flush({});
  });
});
