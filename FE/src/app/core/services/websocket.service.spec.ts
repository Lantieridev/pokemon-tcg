import { TestBed } from '@angular/core/testing';
import { WebSocketService } from './websocket.service';
import { AuthService } from './auth.service';
import { MatchStore } from '../store/match.store';
import * as stompjs from '@stomp/stompjs';
import { GameStateResponseDTO, ActionRequestDTO } from '../models/game-state.models';

describe('WebSocketService', () => {
  let service: WebSocketService;
  let authServiceStub: any;
  let matchStoreSpy: jasmine.SpyObj<MatchStore>;
  let stompClientInstance: any = null;
  let isConnectedVal = false;
  let subscribeSpy: jasmine.Spy;
  let publishSpy: jasmine.Spy;

  beforeEach(() => {
    authServiceStub = {
      token: 'jwt-token-123',
      username: 'AshRivero'
    };

    matchStoreSpy = jasmine.createSpyObj('MatchStore', ['updateState', 'reset']);

    isConnectedVal = false;
    stompClientInstance = null;

    // Spy on prototype methods
    spyOn(stompjs.Client.prototype, 'activate').and.callFake(function(this: any) {
      stompClientInstance = this;
    });
    spyOn(stompjs.Client.prototype, 'deactivate');
    subscribeSpy = spyOn(stompjs.Client.prototype, 'subscribe');
    publishSpy = spyOn(stompjs.Client.prototype, 'publish');
    spyOnProperty(stompjs.Client.prototype, 'connected', 'get').and.callFake(() => isConnectedVal);

    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        { provide: AuthService, useValue: authServiceStub },
        { provide: MatchStore, useValue: matchStoreSpy }
      ]
    });

    service = TestBed.inject(WebSocketService);
  });

  it('should throw error if connecting when not authenticated', () => {
    authServiceStub.token = undefined;
    expect(() => service.connect('match-123')).toThrowError('Debes estar autenticado para conectarte al tablero.');
  });

  it('should connect and subscribe to topics', () => {
    service.connect('match-123');

    expect(stompjs.Client.prototype.activate).toHaveBeenCalled();
    expect(stompClientInstance).toBeTruthy();

    // Trigger connection
    isConnectedVal = true;
    stompClientInstance.onConnect();

    // Verify subscriptions
    expect(subscribeSpy).toHaveBeenCalledWith(
      '/topic/match/match-123/player/AshRivero',
      jasmine.any(Function)
    );
    expect(subscribeSpy).toHaveBeenCalledWith(
      '/topic/chat/match-123',
      jasmine.any(Function)
    );
    expect(subscribeSpy).toHaveBeenCalledWith(
      '/topic/match/match-123/player/AshRivero/errors',
      jasmine.any(Function)
    );
  });

  it('should handle incoming state message and update store', (done) => {
    service.connect('match-123');
    stompClientInstance.onConnect();

    const stateSub = subscribeSpy.calls.argsFor(0);
    const callback = stateSub[1];

    const mockState = { matchId: 'match-123', turnNumber: 2 } as any as GameStateResponseDTO;

    service.gameState$.subscribe((state) => {
      expect(state).toEqual(mockState);
      expect(matchStoreSpy.updateState).toHaveBeenCalledWith(mockState);
      done();
    });

    callback({ body: JSON.stringify(mockState) });
  });

  it('should handle incoming chat message', (done) => {
    service.connect('match-123');
    stompClientInstance.onConnect();

    const chatSub = subscribeSpy.calls.argsFor(1);
    const callback = chatSub[1];

    const mockChat = { sender: 'Misty', message: 'Hello' };

    service.chatMessage$.subscribe((chat) => {
      expect(chat).toEqual(mockChat);
      done();
    });

    callback({ body: JSON.stringify(mockChat) });
  });

  it('should handle incoming errors', (done) => {
    service.connect('match-123');
    stompClientInstance.onConnect();

    const errorSub = subscribeSpy.calls.argsFor(2);
    const callback = errorSub[1];

    const mockError = { error: 'Movimiento inválido' };

    service.error$.subscribe((err) => {
      expect(err).toBe('Movimiento inválido');
      done();
    });

    callback({ body: JSON.stringify(mockError) });
  });

  it('should sendAction when connected', () => {
    service.connect('match-123');
    isConnectedVal = true;

    const action: ActionRequestDTO = { type: 'END_TURN' };
    service.sendAction('match-123', action);

    expect(publishSpy).toHaveBeenCalledWith({
      destination: '/app/match/match-123/action',
      headers: { playerId: 'AshRivero' },
      body: JSON.stringify(action)
    });
  });

  it('should sendChatMessage when connected', () => {
    service.connect('match-123');
    isConnectedVal = true;

    service.sendChatMessage('match-123', 'Hello world');

    expect(publishSpy).toHaveBeenCalledWith({
      destination: '/app/chat/match-123',
      body: JSON.stringify({ sender: 'AshRivero', message: 'Hello world' })
    });
  });

  it('should deactivate client and reset store on disconnect', () => {
    service.connect('match-123');
    service.disconnect();

    expect(stompjs.Client.prototype.deactivate).toHaveBeenCalled();
    expect(matchStoreSpy.reset).toHaveBeenCalled();
    expect(service.isConnected).toBeFalse();
    expect(service.matchId).toBeNull();
  });
});
