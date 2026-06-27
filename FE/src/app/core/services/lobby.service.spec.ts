import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { LobbyService, LobbyStatus, LobbyResponse } from './lobby.service';
import * as stompjs from '@stomp/stompjs';
import SockJS from 'sockjs-client';

describe('LobbyService', () => {
  let service: LobbyService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;
  let authServiceStub: any;
  let stompClientInstance: any = null;
  let isConnectedVal = false;
  let subscribeSpy: jasmine.Spy;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    authServiceStub = {
      token: 'valid-jwt-token',
      username: 'AshRivero'
    };

    isConnectedVal = false;
    stompClientInstance = null;

    // Spy on prototype methods
    spyOn(stompjs.Client.prototype, 'activate').and.callFake(function(this: any) {
      stompClientInstance = this;
    });
    spyOn(stompjs.Client.prototype, 'deactivate');
    subscribeSpy = spyOn(stompjs.Client.prototype, 'subscribe');
    spyOnProperty(stompjs.Client.prototype, 'connected', 'get').and.callFake(() => isConnectedVal);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        LobbyService,
        { provide: Router, useValue: routerSpy },
        { provide: AuthService, useValue: authServiceStub }
      ]
    });

    service = TestBed.inject(LobbyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load decks successfully', async () => {
    const mockDecks = [
      { id: 101, name: 'Fuego', status: 'VALID' as const, createdAt: '', totalCards: 60 },
      { id: 102, name: 'Agua', status: 'VALID' as const, createdAt: '', totalCards: 60 }
    ];

    const promise = service.loadDecks();
    const req = httpMock.expectOne('http://localhost:8081/api/decks');
    expect(req.request.method).toBe('GET');
    req.flush(mockDecks);

    await promise;

    expect(service.decks()).toEqual(mockDecks);
    expect(service.selectedDeckId()).toBe(101);
  });

  it('should set error on joinPublicQueue if no deck is selected', async () => {
    service.selectedDeckId.set(null);
    await service.joinPublicQueue();
    expect(service.lobbyError()).toBe('Seleccioná un mazo antes de buscar partida.');
    expect(service.queueStatus()).toBe('idle');
  });

  it('should join public queue and handle MATCH_READY immediately', async () => {
    service.selectedDeckId.set(101);
    const mockResp: LobbyResponse = {
      status: 'MATCH_READY',
      matchId: 'match-xyz',
      opponentId: 'GaryOak'
    };

    const promise = service.joinPublicQueue(false);
    const req = httpMock.expectOne('http://localhost:8081/api/lobby/queue');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ deckId: 101, isRanked: false });
    req.flush(mockResp);

    await promise;

    expect(service.pendingMatchId()).toBe('match-xyz');
    expect(service.opponentId()).toBe('GaryOak');
    expect(service.queueStatus()).toBe('found');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/battle', 'match-xyz']);
  });

  it('should join public queue and remain WAITING', async () => {
    service.selectedDeckId.set(101);
    const mockResp: LobbyResponse = {
      status: 'WAITING'
    };

    const promise = service.joinPublicQueue(true);
    const req = httpMock.expectOne('http://localhost:8081/api/lobby/queue');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ deckId: 101, isRanked: true });
    req.flush(mockResp);

    await promise;

    expect(service.pendingMatchId()).toBeNull();
    expect(service.queueStatus()).toBe('waiting');
  });

  it('should handle joinPublicQueue error', async () => {
    service.selectedDeckId.set(101);
    const promise = service.joinPublicQueue(false);
    const req = httpMock.expectOne('http://localhost:8081/api/lobby/queue');
    req.error(new ErrorEvent('Network error'));

    await promise;

    expect(service.queueStatus()).toBe('idle');
    expect(service.lobbyError()).toBe('Error al unirse a la cola. Intentá de nuevo.');
  });

  it('should leave public queue successfully', async () => {
    service.queueStatus.set('waiting');
    const promise = service.leavePublicQueue();
    const req = httpMock.expectOne('http://localhost:8081/api/lobby/queue');
    expect(req.request.method).toBe('DELETE');
    req.flush({});

    await promise;
    expect(service.queueStatus()).toBe('idle');
  });

  it('should create private room successfully', async () => {
    service.selectedDeckId.set(101);
    const mockResp: LobbyResponse = {
      status: 'WAITING',
      roomCode: 'ROOM12'
    };

    const promise = service.createPrivateRoom();
    const req = httpMock.expectOne('http://localhost:8081/api/lobby/room');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ deckId: 101 });
    req.flush(mockResp);

    await promise;

    expect(service.roomCode()).toBe('ROOM12');
    expect(service.roomStatus()).toBe('waiting');
  });

  it('should join private room successfully', async () => {
    service.selectedDeckId.set(101);
    const mockResp: LobbyResponse = {
      status: 'MATCH_READY',
      matchId: 'match-111',
      opponentId: 'Misty'
    };

    const promise = service.joinPrivateRoom('ROOM12');
    const req = httpMock.expectOne('http://localhost:8081/api/lobby/room/ROOM12/join');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ deckId: 101 });
    req.flush(mockResp);

    await promise;

    expect(service.pendingMatchId()).toBe('match-111');
    expect(service.opponentId()).toBe('Misty');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/battle', 'match-111']);
  });

  it('should handle joinPrivateRoom error', async () => {
    service.selectedDeckId.set(101);
    const promise = service.joinPrivateRoom('ROOM12');
    const req = httpMock.expectOne('http://localhost:8081/api/lobby/room/ROOM12/join');
    req.flush({ message: 'Sala llena' }, { status: 400, statusText: 'Bad Request' });

    await promise;

    expect(service.lobbyError()).toBe('Sala llena');
  });

  it('should connect WS and subscribe to private lobby channel', () => {
    service.connectLobbyWebSocket();

    expect(stompjs.Client.prototype.activate).toHaveBeenCalled();
    expect(stompClientInstance).toBeTruthy();

    // Trigger connect callback
    isConnectedVal = true;
    stompClientInstance.onConnect();

    expect(subscribeSpy).toHaveBeenCalledWith(
      '/topic/lobby/AshRivero',
      jasmine.any(Function)
    );

    // Test triggering WS message
    const msgCallback = subscribeSpy.calls.mostRecent().args[1];
    const wsMessage = {
      body: JSON.stringify({
        status: 'MATCH_READY',
        matchId: 'match-ws',
        opponentId: 'Brock'
      })
    };

    msgCallback(wsMessage);

    expect(service.pendingMatchId()).toBe('match-ws');
    expect(service.opponentId()).toBe('Brock');
    expect(service.queueStatus()).toBe('found');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/battle', 'match-ws']);
  });

  it('should disconnect WS on reset', () => {
    service.connectLobbyWebSocket();
    service.reset();
    expect(stompjs.Client.prototype.deactivate).toHaveBeenCalled();
    expect(service.queueStatus()).toBe('idle');
    expect(service.roomCode()).toBeNull();
  });
});
