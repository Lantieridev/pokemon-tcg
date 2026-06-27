import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { LobbyAuroraComponent } from './lobby-aurora.component';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { DeckApiService } from '../deck/deck-api.service';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';
import { TutorialService } from '../../core/services/tutorial.service';
import { LobbyService } from '../../core/services/lobby.service';
import { MatchBackendService } from '../../core/services/match-backend.service';
import { signal } from '@angular/core';
import { of, BehaviorSubject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

describe('LobbyAuroraComponent', () => {
  let component: LobbyAuroraComponent;
  let fixture: ComponentFixture<LobbyAuroraComponent>;
  let lobbySpy: jasmine.SpyObj<LobbyService>;
  let tcgSpy: jasmine.SpyObj<PokemonTcgService>;
  let deckApiSpy: jasmine.SpyObj<DeckApiService>;

  beforeEach(async () => {
    lobbySpy = jasmine.createSpyObj('LobbyService', ['reset', 'loadDecks', 'connectLobbyWebSocket', 'disconnectLobbyWebSocket'], {
      selectedDeckId: signal<number | null>(null),
      queueStatus: signal('idle'),
      roomStatus: signal('idle'),
      decks: signal([])
    });

    tcgSpy = jasmine.createSpyObj('PokemonTcgService', ['loadCards'], {
      cards: signal([
        { id: 'fire-1', name: 'Charizard', supertype: 'Pokémon', images: { small: 'charizard.png' }, types: ['Fire'] },
        { id: 'water-1', name: 'Squirtle', supertype: 'Pokémon', images: { small: 'squirtle.png' }, types: ['Water'] },
        { id: 'energy-1', name: 'Fire Energy', supertype: 'Energy', images: { small: 'f-energy.png' } }
      ])
    });

    deckApiSpy = jasmine.createSpyObj('DeckApiService', ['getDeckById']);
    
    const authSpy = jasmine.createSpyObj('AuthService', [], { username: 'Test' });
    const profileSpy = jasmine.createSpyObj('ProfileService', ['getProfile']);
    const tutorialSpy = jasmine.createSpyObj('TutorialService', ['triggerTutorial']);
    const matchSpy = jasmine.createSpyObj('MatchBackendService', ['createBotMatch']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, LobbyAuroraComponent],
      providers: [
        { provide: LobbyService, useValue: lobbySpy },
        { provide: PokemonTcgService, useValue: tcgSpy },
        { provide: DeckApiService, useValue: deckApiSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: ProfileService, useValue: profileSpy },
        { provide: TutorialService, useValue: tutorialSpy },
        { provide: MatchBackendService, useValue: matchSpy },
        { provide: ActivatedRoute, useValue: {} },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LobbyAuroraComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch full deck details when selectedDeckId changes and compute deckRailData', fakeAsync(() => {
    const mockDeck = {
      id: 1,
      name: 'My Fire Deck',
      cards: [
        { cardId: 'fire-1', quantity: 2 },
        { cardId: 'energy-1', quantity: 10 }
      ]
    };
    
    deckApiSpy.getDeckById.and.returnValue(of(mockDeck as any));
    
    // Trigger the effect by changing the signal
    lobbySpy.selectedDeckId.set(1);
    
    // Wait for effect to run
    fixture.detectChanges();
    tick();

    expect(deckApiSpy.getDeckById).toHaveBeenCalledWith(1);
    expect(component.fullSelectedDeck()?.name).toBe('My Fire Deck');
    
    const railData = component.deckRailData();
    expect(railData).toBeTruthy();
    expect(railData?.name).toBe('My Fire Deck');
    expect(railData?.totalCount).toBe(12);
    expect(railData?.energyTypes).toContain('fire');
    expect(railData?.cards.length).toBe(2);
    expect(railData?.cards[0].name).toBe('Charizard');
  }));

  it('should clear fullSelectedDeck if selectedDeckId is null', fakeAsync(() => {
    lobbySpy.selectedDeckId.set(null);
    fixture.detectChanges();
    tick();

    expect(component.fullSelectedDeck()).toBeNull();
    expect(component.deckRailData()).toBeNull();
  }));
});
