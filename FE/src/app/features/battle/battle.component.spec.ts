import { TestBed, ComponentFixture } from '@angular/core/testing';
import { BattleComponent } from './battle.component';
import { MatchStore, GameStateDTO } from '../../core/store/match.store';
import { WebSocketService } from '../../core/services/websocket.service';
import { Router } from '@angular/router';
import { of } from 'rxjs';

describe('BattleComponent Integration', () => {
  let component: BattleComponent;
  let fixture: ComponentFixture<BattleComponent>;
  let store: MatchStore;
  let wsServiceSpy: jasmine.SpyObj<WebSocketService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    wsServiceSpy = jasmine.createSpyObj('WebSocketService', ['connect', 'disconnect']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [BattleComponent],
      providers: [
        MatchStore,
        { provide: WebSocketService, useValue: wsServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BattleComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(MatchStore);
  });

  it('should redirect to /lobby if MatchStore is not loaded', () => {
    fixture.detectChanges();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lobby']);
  });

  it('should connect to WebSocket and update MatchStore on load when matchId exists', () => {
    const mockState: GameStateDTO = {
      matchId: 'match-xyz',
      version: 1,
      activePlayerIndex: 0,
      currentPhase: 'MAIN',
      self: {
        playerId: 'AshRivero',
        active: { cardId: 'xy1-1', name: 'Ivysaur', attachedEnergies: [], damageCounters: 0 },
        bench: [],
        hand: [],
        deckSize: 60,
        prizeCount: 6
      },
      opponent: {
        playerId: 'BrockSteel',
        active: { cardId: 'xy1-3', name: 'Venusaur', attachedEnergies: [], damageCounters: 0 },
        bench: [],
        handSize: 7,
        deckSize: 60,
        prizeCount: 6
      }
    };

    store.updateState(mockState);
    wsServiceSpy.connect.and.returnValue(of({
      ...mockState,
      version: 2
    }));

    fixture.detectChanges();

    expect(wsServiceSpy.connect).toHaveBeenCalledWith('match-xyz');
    expect(store.turn().number).toBe(2);
    expect(component.me()!.name).toBe('AshRivero');
    expect(component.opp()!.name).toBe('BrockSteel');
    expect(component.log().length).toBe(2); // Initial log + version 2 update log
  });

  it('should disconnect from WS on component destruction', () => {
    const mockState: GameStateDTO = {
      matchId: 'match-xyz',
      version: 1,
      activePlayerIndex: 0,
      currentPhase: 'MAIN',
      self: { playerId: 'AshRivero', active: null, bench: [], hand: [], deckSize: 60, prizeCount: 6 },
      opponent: { playerId: 'BrockSteel', active: null, bench: [], handSize: 7, deckSize: 60, prizeCount: 6 }
    };
    store.updateState(mockState);
    wsServiceSpy.connect.and.returnValue(of(mockState));

    fixture.detectChanges();
    component.ngOnDestroy();

    expect(wsServiceSpy.disconnect).toHaveBeenCalled();
  });
});
