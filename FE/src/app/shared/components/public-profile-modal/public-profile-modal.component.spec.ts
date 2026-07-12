import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { PublicProfileModalComponent } from './public-profile-modal.component';
import { PokemonTcgService } from '../../../core/services/pokemon-tcg.service';

describe('PublicProfileModalComponent', () => {
  let fixture: ComponentFixture<PublicProfileModalComponent>;
  let component: PublicProfileModalComponent;
  let tcgService: PokemonTcgService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicProfileModalComponent, HttpClientTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(PublicProfileModalComponent);
    component = fixture.componentInstance;
    tcgService = TestBed.inject(PokemonTcgService);
    // Pre-populate so loadCards()'s in-memory guard skips the HTTP fetch entirely.
    tcgService.cards.set([
      { id: 'xy1-4', name: 'Charizard', images: { small: 'https://example.com/charizard.png' } } as any,
    ]);
    component.profile = { username: 'AshRivero' } as any;
  });

  it('emits close when onClose is called', () => {
    fixture.detectChanges();
    let closed = false;
    component.close.subscribe(() => (closed = true));
    component.onClose();
    expect(closed).toBeTrue();
  });

  describe('selectedMedalsList', () => {
    it('is empty when the profile has no selected medals', () => {
      component.profile = { username: 'AshRivero' } as any;
      expect(component.selectedMedalsList).toEqual([]);
    });

    it('splits a comma-separated medal string and drops empty entries', () => {
      component.profile = { username: 'AshRivero', selectedMedals: 'gold,,silver' } as any;
      expect(component.selectedMedalsList).toEqual(['gold', 'silver']);
    });
  });

  describe('win/loss stats', () => {
    it('defaults to 0 when statistics are missing', () => {
      component.profile = { username: 'AshRivero' } as any;
      expect(component.totalWins).toBe(0);
      expect(component.totalLosses).toBe(0);
      expect(component.overallWinRate).toBe(0);
    });

    it('reads the real values when statistics are present', () => {
      component.profile = { username: 'AshRivero', statistics: { matchesWon: 10, matchesLost: 4, winRate: 71.4 } } as any;
      expect(component.totalWins).toBe(10);
      expect(component.totalLosses).toBe(4);
      expect(component.overallWinRate).toBe(71.4);
    });
  });

  describe('getShowcaseSlot', () => {
    it('finds the showcase card at the given position', () => {
      component.profile = { username: 'AshRivero', showcase: [{ slotPosition: 2, cardId: 'xy1-4' }] } as any;
      expect(component.getShowcaseSlot(2)?.cardId).toBe('xy1-4');
    });

    it('returns undefined when no slot occupies that position', () => {
      component.profile = { username: 'AshRivero', showcase: [] } as any;
      expect(component.getShowcaseSlot(1)).toBeUndefined();
    });
  });

  describe('getCardImageById', () => {
    it('resolves the small image for a card in the registry', () => {
      expect(component.getCardImageById('xy1-4')).toBe('https://example.com/charizard.png');
    });

    it('returns an empty string for a card not in the registry', () => {
      expect(component.getCardImageById('unknown-id')).toBe('');
    });
  });

  describe('getTopPlayedPokemons', () => {
    it('returns an empty array without advancedStats', () => {
      component.profile = { username: 'AshRivero' } as any;
      expect(component.getTopPlayedPokemons()).toEqual([]);
    });

    it('sorts by timesPlayed descending and caps at 5', () => {
      const pokemonStats = Array.from({ length: 7 }, (_, i) => ({ pokemonType: 'FIRE', timesPlayed: i }));
      component.profile = { username: 'AshRivero', advancedStats: { pokemonStats } } as any;

      const top = component.getTopPlayedPokemons();
      expect(top.length).toBe(5);
      expect(top[0].timesPlayed).toBe(6);
      expect(top[4].timesPlayed).toBe(2);
    });

    it('filters by the selected element type', () => {
      component.profile = {
        username: 'AshRivero',
        advancedStats: {
          pokemonStats: [
            { pokemonType: 'FIRE', timesPlayed: 5 },
            { pokemonType: 'WATER', timesPlayed: 10 },
          ],
        },
      } as any;
      component.elementFilter = 'water';

      const top = component.getTopPlayedPokemons();
      expect(top.length).toBe(1);
      expect(top[0].pokemonType).toBe('WATER');
    });
  });

  describe('getTopAttackers', () => {
    it('sorts by damageDealt descending and caps at 5, ignoring the element filter', () => {
      const pokemonStats = Array.from({ length: 6 }, (_, i) => ({ pokemonType: 'FIRE', damageDealt: i * 10 }));
      component.profile = { username: 'AshRivero', advancedStats: { pokemonStats } } as any;

      const top = component.getTopAttackers();
      expect(top.length).toBe(5);
      expect(top[0].damageDealt).toBe(50);
    });

    it('returns an empty array without advancedStats', () => {
      component.profile = { username: 'AshRivero' } as any;
      expect(component.getTopAttackers()).toEqual([]);
    });
  });

  describe('getEnergyStats', () => {
    it('sorts energy stats by count descending', () => {
      component.profile = {
        username: 'AshRivero',
        advancedStats: { energyStats: [{ type: 'FIRE', count: 2 }, { type: 'WATER', count: 9 }] },
      } as any;

      const stats = component.getEnergyStats();
      expect(stats[0].type).toBe('WATER');
    });

    it('returns an empty array without advancedStats', () => {
      component.profile = { username: 'AshRivero' } as any;
      expect(component.getEnergyStats()).toEqual([]);
    });
  });

  describe('getTypeColor / getEnergyLabel', () => {
    it('maps known types to their color and Spanish label', () => {
      expect(component.getTypeColor('fire')).toBe('#ff7a3d');
      expect(component.getEnergyLabel('fire')).toBe('Fuego');
      expect(component.getTypeColor('WATER')).toBe('#4aa3ff');
      expect(component.getEnergyLabel('WATER')).toBe('Agua');
    });

    it('falls back to a neutral color and returns the raw type as the label for unknown types', () => {
      expect(component.getTypeColor('made-up')).toBe('var(--mut)');
      expect(component.getEnergyLabel('made-up')).toBe('made-up');
    });

    it('falls back to a neutral color for an empty type', () => {
      expect(component.getTypeColor('')).toBe('var(--mut)');
      expect(component.getEnergyLabel('')).toBe('Desconocido');
    });
  });

  describe('avatar helpers (shared with chat-modal/friends-sidebar)', () => {
    it('isCustomAvatar treats known emoji keys as not custom', () => {
      expect(component.isCustomAvatar('serena')).toBeFalse();
      expect(component.isCustomAvatar('legendary_skin')).toBeTrue();
    });

    it('getAvatarUrl maps a known name to its static asset', () => {
      expect(component.getAvatarUrl('Misty')).toBe('assets/store/avatar_misty.png');
    });

    it('getAvatarEmoji maps a known icon and falls back for unknown ones', () => {
      expect(component.getAvatarEmoji('red')).toBe('⚡');
      expect(component.getAvatarEmoji('nope')).toBe('🎒');
    });
  });
});
