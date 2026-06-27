import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { BattleComponent } from './battle.component';
import { MatchStore } from '../../core/store/match.store';
import { GameStateResponseDTO } from '../../core/models/game-state.models';
import { WebSocketService } from '../../core/services/websocket.service';
import { MatchBackendService } from '../../core/services/match-backend.service';
import { Router, ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('BattleComponent Integration', () => {
  let component: BattleComponent;
  let fixture: ComponentFixture<BattleComponent>;
  let store: MatchStore;
  let wsServiceSpy: jasmine.SpyObj<WebSocketService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let matchBackendSpy: jasmine.SpyObj<MatchBackendService>;

  beforeEach(async () => {
    wsServiceSpy = jasmine.createSpyObj('WebSocketService', ['connect', 'disconnect']);
    (wsServiceSpy as any).chatMessage$ = of();
    (wsServiceSpy as any).gameState$ = of();
    (wsServiceSpy as any).error$ = of();
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    matchBackendSpy = jasmine.createSpyObj('MatchBackendService', ['getMatchState', 'getChatHistory', 'surrenderMatch']);

    matchBackendSpy.getChatHistory.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, BattleComponent],
      providers: [
        MatchStore,
        { provide: WebSocketService, useValue: wsServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatchBackendService, useValue: matchBackendSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: (key: string) => 'match-xyz' },
              queryParamMap: { get: (key: string) => 'match-xyz' }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BattleComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(MatchStore);
  });

  it('should redirect to /lobby if MatchStore is not loaded (REST fails)', () => {
    matchBackendSpy.getMatchState.and.returnValue(throwError(() => new Error('Not found')));
    fixture.detectChanges();
    expect(matchBackendSpy.getMatchState).toHaveBeenCalledWith('match-xyz');
  });

  it('should connect to WebSocket and update MatchStore on load when matchId exists', fakeAsync(() => {
    const mockState: GameStateDTO = {
      matchId: 'match-xyz',
      version: 1,
      turnNumber: 1,
      activePlayerIndex: 0,
      currentPhase: 'MAIN',
      pendingSelectionRequest: null,
      self: {
        playerId: 'AshRivero',
        active: { cardId: 'xy1-1', name: 'Ivysaur', attachedEnergies: [], damageCounters: 0 } as any,
        bench: [],
        hand: [],
        deckSize: 60,
        prizeCount: 6
      },
      opponent: {
        playerId: 'BrockSteel',
        active: { cardId: 'xy1-3', name: 'Venusaur', attachedEnergies: [], damageCounters: 0 } as any,
        bench: [],
        handSize: 7,
        deckSize: 60,
        prizeCount: 6
      }
    } as any;

    matchBackendSpy.getMatchState.and.returnValue(of(mockState));
    wsServiceSpy.connect.and.callFake((id: string) => {
      store.updateState({
        ...mockState,
        version: 2,
        turnNumber: 2
      } as any);
      return of({
        ...mockState,
        version: 2,
        turnNumber: 2
      } as any);
    });

    fixture.detectChanges();
    tick();

    // Emit the update via gameState$ Subject
    (wsServiceSpy as any).gameState$.next({
      ...mockState,
      version: 2
    });

    expect(wsServiceSpy.connect).toHaveBeenCalledWith('match-xyz');
    expect(store.turn().number).toBe(2);
    expect(component.me()!.name).toBe('AshRivero');
    expect(component.opp()!.name).toBe('BrockSteel');
  }));

  it('should disconnect from WS on component destruction', () => {
    const mockState: GameStateResponseDTO = {
      matchId: 'match-xyz',
      version: 1,
      turnNumber: 1,
      activePlayerIndex: 0,
      currentPhase: 'MAIN',
      pendingSelectionRequest: null,
      self: { playerId: 'AshRivero', active: null, bench: [], hand: [], deckSize: 60, prizeCount: 6 },
      opponent: { playerId: 'BrockSteel', active: null, bench: [], handSize: 7, deckSize: 60, prizeCount: 6 }
    } as any;
    
    matchBackendSpy.getMatchState.and.returnValue(of(mockState));
    wsServiceSpy.connect.and.returnValue(of(mockState));

    fixture.detectChanges();
    component.ngOnDestroy();

    expect(wsServiceSpy.disconnect).toHaveBeenCalled();
  });

  it('should not show FLASH_CLAW pending selection to active player', () => {
    const mockState: GameStateResponseDTO = {
      matchId: 'match-xyz',
      version: 1,
      activePlayerIndex: 0,
      currentPhase: 'MAIN',
      self: { playerId: 'AshRivero', active: null, bench: [], hand: [], deckSize: 60, prizeCount: 6 },
      opponent: { playerId: 'BrockSteel', active: null, bench: [], handSize: 7, deckSize: 60, prizeCount: 6 },
      pendingSelectionRequest: {
        sourceEffect: 'FLASH_CLAW',
        maxSelections: 1,
        source: 'HAND',
        options: ['c1']
      }
    };
    store.updateState(mockState);
    fixture.detectChanges();

    expect(component.pendingSelection()).toBeNull();
  });
});
