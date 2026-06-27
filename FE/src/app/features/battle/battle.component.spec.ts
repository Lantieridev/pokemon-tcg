import { TestBed, ComponentFixture } from '@angular/core/testing';
import { signal } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BattleComponent } from './battle.component';
import { MatchStore } from '../../core/store/match.store';
import { GameStateResponseDTO } from '../../core/models/game-state.models';
import { WebSocketService } from '../../core/services/websocket.service';
import { MatchBackendService } from '../../core/services/match-backend.service';
import { AuthService } from '../../core/services/auth.service';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { Router, ActivatedRoute } from '@angular/router';
import { of, Subject } from 'rxjs';

describe('BattleComponent Integration', () => {
  let component: BattleComponent;
  let fixture: ComponentFixture<BattleComponent>;
  let store: MatchStore;
  let wsServiceSpy: jasmine.SpyObj<WebSocketService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let matchBackendSpy: jasmine.SpyObj<MatchBackendService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let tcgServiceSpy: jasmine.SpyObj<PokemonTcgService>;

  beforeEach(async () => {
    wsServiceSpy = jasmine.createSpyObj('WebSocketService', ['connect', 'disconnect']);
    (wsServiceSpy as any).chatMessage$ = new Subject<any>();
    (wsServiceSpy as any).gameState$ = new Subject<any>();
    (wsServiceSpy as any).error$ = new Subject<any>();
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    matchBackendSpy = jasmine.createSpyObj('MatchBackendService', ['getMatchState', 'getChatHistory', 'surrenderMatch']);
    authServiceSpy = jasmine.createSpyObj('AuthService', [], { username: 'AshRivero' });
    tcgServiceSpy = jasmine.createSpyObj('PokemonTcgService', ['loadCards']);
    (tcgServiceSpy as any).cards = signal([]);

    // Set default mock responses
    const defaultMockState: any = {
      matchId: 'match-xyz',
      version: 1,
      turnNumber: 1,
      activePlayerIndex: 0,
      currentPhase: 'MAIN',
      pendingSelectionRequest: null,
      self: { playerId: 'AshRivero', active: null, bench: [], hand: [], deckSize: 60, prizeCount: 6 },
      opponent: { playerId: 'BrockSteel', active: null, bench: [], handSize: 7, deckSize: 60, prizeCount: 6 },
      discardPile: [],
      opponentDiscardPile: [],
      log: []
    };
    matchBackendSpy.getMatchState.and.returnValue(of(defaultMockState));
    matchBackendSpy.getChatHistory.and.returnValue(of([]));
    matchBackendSpy.surrenderMatch.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [BattleComponent, HttpClientTestingModule],
      providers: [
        MatchStore,
        { provide: WebSocketService, useValue: wsServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatchBackendService, useValue: matchBackendSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: PokemonTcgService, useValue: tcgServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => 'match-xyz' } },
            paramMap: of({ get: () => 'match-xyz' })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BattleComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(MatchStore);
  });

  it('should load match state on init', () => {
    fixture.detectChanges();
    expect(matchBackendSpy.getMatchState).toHaveBeenCalledWith('match-xyz');
  });

  it('should connect to WebSocket and update MatchStore on load when matchId exists', () => {
    const mockState: any = {
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
      },
      discardPile: [],
      opponentDiscardPile: [],
      log: []
    };

    matchBackendSpy.getMatchState.and.returnValue(of(mockState));
    store.updateState(mockState);
    wsServiceSpy.connect.and.returnValue(of(undefined as any));

    fixture.detectChanges();

    // Emit the update via gameState$ Subject
    (wsServiceSpy as any).gameState$.next({
      ...mockState,
      version: 2
    });

    expect(wsServiceSpy.connect).toHaveBeenCalledWith('match-xyz');
    expect(store.turn().number).toBe(2);
    expect(component.me()!.name).toBe('AshRivero');
    expect(component.opp()!.name).toBe('BrockSteel');
    expect(component.log().length).toBe(2); // Initial log + version 2 update log
  });

  it('should disconnect from WS on component destruction', () => {
<<<<<<< HEAD
    const mockState: any = {
=======
    const mockState: GameStateResponseDTO = {
>>>>>>> feature/flashfire-corrections
      matchId: 'match-xyz',
      version: 1,
      turnNumber: 1,
      activePlayerIndex: 0,
      currentPhase: 'MAIN',
      pendingSelectionRequest: null,
      self: { playerId: 'AshRivero', active: null, bench: [], hand: [], deckSize: 60, prizeCount: 6 },
      opponent: { playerId: 'BrockSteel', active: null, bench: [], handSize: 7, deckSize: 60, prizeCount: 6 },
      discardPile: [],
      opponentDiscardPile: [],
      log: []
    };
    matchBackendSpy.getMatchState.and.returnValue(of(mockState));
    store.updateState(mockState);
    wsServiceSpy.connect.and.returnValue(of(mockState));

    fixture.detectChanges();
    component.ngOnDestroy();

    expect(wsServiceSpy.disconnect).toHaveBeenCalled();
  });

<<<<<<< HEAD
  describe('Campaign Victory/Defeat Overlays', () => {
    beforeEach(() => {
      wsServiceSpy.connect.and.returnValue(of({} as any));
    });

    it('should render campaign victory Step 1 with green theme but hide Step 2 initially', () => {
      const mockState: any = {
        matchId: 'match-xyz',
        version: 1,
        turnNumber: 1,
        activePlayerIndex: 0,
        currentPhase: 'FINISHED',
        winnerId: 'AshRivero',
        victoryReason: 'Opponent conceded',
        pendingSelectionRequest: null,
        self: { playerId: 'AshRivero', active: null, bench: [], hand: [], deckSize: 60, prizeCount: 6 },
        opponent: { playerId: 'Bot-Brock', active: null, bench: [], handSize: 7, deckSize: 60, prizeCount: 6 },
        discardPile: [],
        opponentDiscardPile: [],
        log: []
      };

      matchBackendSpy.getMatchState.and.returnValue(of(mockState));
      store.updateState(mockState);
      
      // Step 1: campaignVictoryStep is 1
      component.campaignVictoryStep.set(1);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      
      // Backdrop and scanline should exist
      expect(compiled.querySelector('.gym-victory-backdrop')).toBeTruthy();
      expect(compiled.querySelector('.victory-rays')).toBeTruthy();

      // Title should say VICTORIA in green theme
      const titleEl = compiled.querySelector('.gym-victory-text');
      expect(titleEl).toBeTruthy();
      expect(titleEl?.textContent?.trim()).toBe('VICTORIA');

      // Reason text should be visible
      const reasonEl = compiled.querySelector('.gym-victory-backdrop p.font-chunky');
      expect(reasonEl?.textContent).toContain('Opponent conceded');

      // Step 2 wrapper should exist but NOT be active (max-height should be hidden via active class absent)
      const step2Wrap = compiled.querySelector('.gym-victory-step-2-wrap');
      expect(step2Wrap).toBeTruthy();
      expect(step2Wrap?.classList.contains('active')).toBeFalse();
    });

    it('should show Step 2 details (MVP, rewards, button) when campaignVictoryStep is 2', () => {
      const mockState: any = {
        matchId: 'match-xyz',
        version: 1,
        turnNumber: 1,
        activePlayerIndex: 0,
        currentPhase: 'FINISHED',
        winnerId: 'AshRivero',
        victoryReason: 'Opponent conceded',
        pendingSelectionRequest: null,
        self: { playerId: 'AshRivero', active: null, bench: [], hand: [], deckSize: 60, prizeCount: 6 },
        opponent: { playerId: 'Bot-Brock', active: null, bench: [], handSize: 7, deckSize: 60, prizeCount: 6 },
        discardPile: [],
        opponentDiscardPile: [],
        log: []
      };

      matchBackendSpy.getMatchState.and.returnValue(of(mockState));
      store.updateState(mockState);
      
      spyOn(component, 'mvpCardId').and.returnValue('xy1-1');
      spyOn(component, 'mvpCardDamage').and.returnValue(70);
      spyOn(component, 'coinsGained').and.returnValue(50);
      spyOn(component, 'xpGained').and.returnValue(50);
      
      // Step 2 active
      component.campaignVictoryStep.set(2);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      
      // Step 2 wrapper should now be active
      const step2Wrap = compiled.querySelector('.gym-victory-step-2-wrap');
      expect(step2Wrap?.classList.contains('active')).toBeTrue();

      // MVP card section title and damage badge
      expect(compiled.textContent).toContain('CARTA MÁS VALIOSA (MVP)');
      expect(compiled.textContent).toContain('Daño Infligido: 70 HP');

      // Rewards
      expect(compiled.textContent).toContain('Recompensas Obtenidas');
      expect(compiled.textContent).toContain('+50');

      // CTA Button should render green theme
      const ctaBtn = compiled.querySelector('.gym-btn-green');
      expect(ctaBtn).toBeTruthy();
      expect(ctaBtn?.textContent?.trim()).toBe('Volver a la Campaña');
    });

    it('should trigger router navigation on CTA click', () => {
      const mockState: any = {
        matchId: 'match-xyz',
        version: 1,
        turnNumber: 1,
        activePlayerIndex: 0,
        currentPhase: 'FINISHED',
        winnerId: 'AshRivero',
        victoryReason: 'Opponent conceded',
        pendingSelectionRequest: null,
        self: { playerId: 'AshRivero', active: null, bench: [], hand: [], deckSize: 60, prizeCount: 6 },
        opponent: { playerId: 'Bot-Brock', active: null, bench: [], handSize: 7, deckSize: 60, prizeCount: 6 },
        discardPile: [],
        opponentDiscardPile: [],
        log: []
      };
      matchBackendSpy.getMatchState.and.returnValue(of(mockState));
      store.updateState(mockState);

      component.campaignVictoryStep.set(2);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const ctaBtn = compiled.querySelector('.gym-btn-green') as HTMLButtonElement;
      
      ctaBtn.click();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/lobby']);
    });
=======
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
>>>>>>> feature/flashfire-corrections
  });
});
