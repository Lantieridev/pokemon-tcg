import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DeckAuroraComponent } from './deck-aurora.component';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { DeckStore } from '../../core/store/deck.store';
import { DeckApiService } from '../deck/deck-api.service';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';
import { TutorialService } from '../../core/services/tutorial.service';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

describe('DeckAuroraComponent', () => {
  let component: DeckAuroraComponent;
  let fixture: ComponentFixture<DeckAuroraComponent>;
  let deckApiSpy: jasmine.SpyObj<DeckApiService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    deckApiSpy = jasmine.createSpyObj('DeckApiService', ['deleteDeck', 'getDecksByUserId']);
    deckApiSpy.getDecksByUserId.and.returnValue(of([]));
    
    authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      username: 'TestUser',
      userId: 1
    });

    const tcgSpy = jasmine.createSpyObj('PokemonTcgService', ['loadCards'], {
      cards: signal([])
    });

    const profileSpy = jasmine.createSpyObj('ProfileService', ['getProfile']);
    const tutorialSpy = jasmine.createSpyObj('TutorialService', ['triggerTutorial']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, DeckAuroraComponent],
      providers: [
        { provide: DeckApiService, useValue: deckApiSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: PokemonTcgService, useValue: tcgSpy },
        { provide: ProfileService, useValue: profileSpy },
        { provide: TutorialService, useValue: tutorialSpy },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } },
        DeckStore
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DeckAuroraComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize and not delete anything if deckToDelete is null', () => {
    component.deckToDelete.set(null);
    component.deleteDeck();
    expect(deckApiSpy.deleteDeck).not.toHaveBeenCalled();
  });

  it('should call deleteDeck API and reset state on success', () => {
    deckApiSpy.deleteDeck.and.returnValue(of(undefined as any));
    component.deckToDelete.set(123);
    
    component.deleteDeck();
    
    expect(deckApiSpy.deleteDeck).toHaveBeenCalledWith(123);
    expect(component.deckToDelete()).toBeNull();
    expect(deckApiSpy.getDecksByUserId).toHaveBeenCalled();
  });

  it('should handle error when deleting deck, reset state, and show alert', () => {
    spyOn(window, 'alert');
    deckApiSpy.deleteDeck.and.returnValue(throwError(() => new Error('Server error')));
    component.deckToDelete.set(456);
    
    component.deleteDeck();
    
    expect(deckApiSpy.deleteDeck).toHaveBeenCalledWith(456);
    expect(component.deckToDelete()).toBeNull();
    expect(window.alert).toHaveBeenCalledWith('Error eliminando mazo: Server error');
  });
});
