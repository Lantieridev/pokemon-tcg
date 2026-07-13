import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProfileComponent } from './profile.component';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { environment } from '../../../environments/environment';

function loginAs(userId: number, username: string): void {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: username, exp: Math.floor(Date.now() / 1000) + 3600 }));
  localStorage.setItem('jwt', `${header}.${payload}.signature`);
  localStorage.setItem('username', username);
  localStorage.setItem('userId', String(userId));
}

describe('ProfileComponent', () => {
  let fixture: ComponentFixture<ProfileComponent>;
  let component: ProfileComponent;
  let httpMock: HttpTestingController;
  let tcgService: PokemonTcgService;

  beforeEach(async () => {
    localStorage.clear();
    loginAs(1, 'AshRivero');
    await TestBed.configureTestingModule({
      imports: [ProfileComponent, HttpClientTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    tcgService = TestBed.inject(PokemonTcgService);
    tcgService.cards.set([{ id: 'placeholder' } as any]); // skip loadCards() HTTP fetch
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  function flushInit(overrides: any = {}): void {
    httpMock.expectOne(`${environment.apiUrl}/users/AshRivero/profile`).flush({
      username: 'AshRivero', unlockedTitles: [], selectedMedals: '', showcase: [], packCollection: [],
      ...overrides,
    });
    httpMock.expectOne(`${environment.apiUrl}/users/AshRivero/profile/achievements`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/decks/mine`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/users/me/history?page=0&size=10`).flush({ content: [] });
  }

  describe('username / userInitial', () => {
    it('exposes the logged-in username and its uppercase initial', () => {
      expect(component.username).toBe('AshRivero');
      expect(component.userInitial).toBe('A');
    });
  });

  it('loads profile, achievements, decks, and history on init', () => {
    fixture.detectChanges();
    flushInit({ unlockedTitles: ['Campeón'] });

    expect(component.profileData?.unlockedTitles).toEqual(['Campeón']);
    expect(component.loadingHistory).toBeFalse();
  });

  describe('unlockedTitlesList / filteredUnlockedTitles', () => {
    it('defaults to Novato/Entrenador with no profile loaded', () => {
      expect(component.unlockedTitlesList).toEqual(['Novato', 'Entrenador']);
    });

    it('always appends Novato and Entrenador even when the profile has other titles', () => {
      fixture.detectChanges();
      flushInit({ unlockedTitles: ['Campeón'] });
      expect(component.unlockedTitlesList).toEqual(['Campeón', 'Novato', 'Entrenador']);
    });

    it('does not duplicate Novato/Entrenador if already present', () => {
      fixture.detectChanges();
      flushInit({ unlockedTitles: ['Novato', 'Campeón'] });
      expect(component.unlockedTitlesList).toEqual(['Novato', 'Campeón', 'Entrenador']);
    });

    it('filters titles by the search query, case-insensitively', () => {
      fixture.detectChanges();
      flushInit({ unlockedTitles: ['Campeón'] });
      component.titleSearchQuery = 'camp';
      expect(component.filteredUnlockedTitles).toEqual(['Campeón']);
    });
  });

  describe('availableAvatars', () => {
    it('includes the 6 default avatars with no profile or achievements loaded', () => {
      expect(component.availableAvatars.length).toBe(6);
    });

    it('adds unlocked FOTO_PERFIL achievement rewards', () => {
      fixture.detectChanges();
      flushInit();
      component.allAchievements = [
        { unlocked: true, rewardType: 'FOTO_PERFIL', rewardValue: 'avatar_legend' } as any,
        { unlocked: false, rewardType: 'FOTO_PERFIL', rewardValue: 'avatar_locked' } as any,
      ];
      expect(component.availableAvatars).toContain('avatar_legend');
      expect(component.availableAvatars).not.toContain('avatar_locked');
    });

    it('adds avatars unlocked via the profile (store purchases)', () => {
      fixture.detectChanges();
      flushInit({ unlockedAvatars: ['pikachu_cute'] });
      expect(component.availableAvatars).toContain('pikachu_cute');
    });

    it('does not duplicate an avatar available from multiple sources', () => {
      fixture.detectChanges();
      flushInit({ unlockedAvatars: ['avatar_winner_badge'] });
      const count = component.availableAvatars.filter(a => a === 'avatar_winner_badge').length;
      expect(count).toBe(1);
    });
  });

  describe('medals', () => {
    it('filters achievements down to MEDALLA reward type', () => {
      fixture.detectChanges();
      flushInit();
      component.achievements = [
        { rewardType: 'MEDALLA', unlocked: true } as any,
        { rewardType: 'FOTO_PERFIL', unlocked: true } as any,
      ];
      expect(component.medals.length).toBe(1);
    });

    it('counts and lists only unlocked medals', () => {
      fixture.detectChanges();
      flushInit();
      component.achievements = [
        { rewardType: 'MEDALLA', unlocked: true, title: 'Oro' } as any,
        { rewardType: 'MEDALLA', unlocked: false, title: 'Bloqueada' } as any,
      ];
      expect(component.unlockedMedalsCount).toBe(1);
      expect(component.unlockedMedals[0].title).toBe('Oro');
    });
  });

  describe('avatar helpers (shared pattern with other components)', () => {
    it('isCustomAvatar treats known emoji keys as not custom', () => {
      expect(component.isCustomAvatar('gary')).toBeFalse();
      expect(component.isCustomAvatar('avatar_legend')).toBeTrue();
    });

    it('getAvatarUrl maps a known name and normalizes unknown ones', () => {
      expect(component.getAvatarUrl('Brock')).toBe('assets/store/avatar_brock.png');
      expect(component.getAvatarUrl(undefined)).toBe('');
    });

    it('getAvatarEmoji maps a known icon and falls back for unknown ones', () => {
      expect(component.getAvatarEmoji('serena')).toBe('🎀');
      expect(component.getAvatarEmoji('nope')).toBe('🎒');
    });
  });

  describe('selectedMedalsList / getMedalTitle', () => {
    it('is empty with no profile data', () => {
      expect(component.selectedMedalsList).toEqual([]);
    });

    it('splits the comma-separated selected medals, dropping empties', () => {
      fixture.detectChanges();
      flushInit({ selectedMedals: 'gold,,silver' });
      expect(component.selectedMedalsList).toEqual(['gold', 'silver']);
    });

    it('resolves a medal title from allAchievements, falling back to "Medalla"', () => {
      component.allAchievements = [{ rewardValue: 'gold', title: 'Campeón de Oro' } as any];
      expect(component.getMedalTitle('gold')).toBe('Campeón de Oro');
      expect(component.getMedalTitle('unknown')).toBe('Medalla');
    });
  });

  describe('toggleMedalSelection', () => {
    it('does nothing for an undefined medal value', () => {
      component.toggleMedalSelection(undefined);
      expect(component.editSelectedMedals).toEqual([]);
    });

    it('adds a medal to the selection', () => {
      component.toggleMedalSelection('gold');
      expect(component.editSelectedMedals).toEqual(['gold']);
    });

    it('removes an already-selected medal', () => {
      component.editSelectedMedals = ['gold'];
      component.toggleMedalSelection('gold');
      expect(component.editSelectedMedals).toEqual([]);
    });

    it('refuses to select a 4th medal and shows a toast', () => {
      component.editSelectedMedals = ['a', 'b', 'c'];
      component.toggleMedalSelection('d');
      expect(component.editSelectedMedals).toEqual(['a', 'b', 'c']);
      expect(component.toastMessage).toContain('máximo de 3 medallas');
    });
  });

  describe('collection browsing', () => {
    beforeEach(() => {
      fixture.detectChanges();
      const packCollection = [
        { cardId: 'a', rarity: 'RARA', isFoil: false },
        { cardId: 'b', rarity: 'RARA', isFoil: true },
        { cardId: 'c', rarity: 'RARA', isFoil: false },
        { cardId: 'd', rarity: 'COMUN', isFoil: false },
      ];
      flushInit({ packCollection });
      component.setCollectionFilter('RARA');
    });

    it('counts cards by rarity', () => {
      expect(component.getCollectionCountByRarity('RARA')).toBe(3);
      expect(component.getCollectionCountByRarity('COMUN')).toBe(1);
    });

    it('resets the page when the filter changes', () => {
      component.collectionPage = 2;
      component.setCollectionFilter('COMUN');
      expect(component.collectionPage).toBe(0);
    });

    it('sorts foil cards before non-foil within the filtered rarity', () => {
      const filtered = component.getFilteredCollection();
      expect(filtered[0].cardId).toBe('b');
    });

    it('paginates the filtered collection in pages of 8', () => {
      const bigCollection = Array.from({ length: 20 }, (_, i) => ({ cardId: `c${i}`, rarity: 'RARA', isFoil: false }));
      component.profileData = { ...component.profileData, packCollection: bigCollection } as any;

      expect(component.getTotalCollectionPages()).toBe(3);
      expect(component.getPaginatedCollection().length).toBe(8);

      component.nextCollectionPage();
      expect(component.collectionPage).toBe(1);
      expect(component.getPaginatedCollection()[0].cardId).toBe('c8');
    });

    it('does not page past the last page or before the first', () => {
      component.collectionPage = 0;
      component.prevCollectionPage();
      expect(component.collectionPage).toBe(0);

      component.nextCollectionPage();
      component.nextCollectionPage();
      component.nextCollectionPage();
      component.nextCollectionPage();
      expect(component.collectionPage).toBe(component.getTotalCollectionPages() - 1);
    });

    it('maps rarity to its display color', () => {
      expect(component.getCollectionRarityColor('LEGENDARIA')).toBe('#ffce32');
      expect(component.getCollectionRarityColor('UNKNOWN')).toBe('#a1a1aa');
    });
  });

  describe('zoomCard', () => {
    it('resolves from the live registry, with pulled-card name taking precedence', () => {
      tcgService.cards.set([{ id: 'xy1-4', name: 'Charizard', images: { large: 'https://example.com/l.png' }, types: ['Fire'], subtypes: ['Stage 2'] } as any]);
      component.zoomCard({ cardId: 'xy1-4', cardName: 'Custom Name' });
      expect(component.zoomedCard.name).toBe('Custom Name');
      expect(component.zoomedCard.img).toBe('https://example.com/l.png');
      expect(component.zoomedCard.type).toBe('fire');
    });

    it('falls back to the registry name and Common rarity when unset', () => {
      tcgService.cards.set([{ id: 'xy1-4', name: 'Charizard', images: {}, subtypes: [] } as any]);
      component.zoomCard({ cardId: 'xy1-4' });
      expect(component.zoomedCard.name).toBe('Charizard');
      expect(component.zoomedCard.rarity).toBe('Common');
    });
  });

  describe('openEditModal / closeEditModal', () => {
    it('populates the edit form from the loaded profile', () => {
      fixture.detectChanges();
      flushInit({ description: 'Hola', activeTitle: 'Campeón', avatarIcon: 'ash', selectedMedals: 'gold,silver' });

      component.openEditModal();

      expect(component.editDescription).toBe('Hola');
      expect(component.editActiveTitle).toBe('Campeón');
      expect(component.editAvatarIcon).toBe('ash');
      expect(component.editSelectedMedals).toEqual(['gold', 'silver']);
      expect(component.showEditModal).toBeTrue();
    });

    it('defaults activeTitle to "Ninguno" and avatar to the default badge with no profile data', () => {
      component.openEditModal();
      expect(component.editActiveTitle).toBe('Ninguno');
      expect(component.editAvatarIcon).toBe('avatar_winner_badge');
    });

    it('closes the modal and clears the description error', () => {
      component.descriptionError = 'some error';
      component.showEditModal = true;
      component.closeEditModal();
      expect(component.showEditModal).toBeFalse();
      expect(component.descriptionError).toBe('');
    });
  });

  describe('validateDescription', () => {
    it('clears the error for clean text', () => {
      component.editDescription = 'Me encanta atrapar Pokémon';
      component.validateDescription();
      expect(component.descriptionError).toBe('');
    });

    it('flags a blocked word', () => {
      component.editDescription = 'sos un idiota';
      component.validateDescription();
      expect(component.descriptionError).toContain('Palabra no permitida');
    });

    it('flags leetspeak variants of a blocked word', () => {
      component.editDescription = '1d10ta'; // idiota with leet substitutions
      component.validateDescription();
      expect(component.descriptionError).toContain('Palabra no permitida');
    });

    it('flags accented variants after normalization', () => {
      component.editDescription = 'está gilipollas';
      component.validateDescription();
      expect(component.descriptionError).toContain('Palabra no permitida');
    });

    it('does not flag a blocked word that is a substring of a longer clean word', () => {
      // "culo" is blocked, but "vínculo" contains it as a substring - the \b word boundary must prevent a false positive
      component.editDescription = 'me encanta el vinculo con mi equipo';
      component.validateDescription();
      expect(component.descriptionError).toBe('');
    });
  });

  describe('showToast', () => {
    beforeEach(() => jasmine.clock().install());
    afterEach(() => jasmine.clock().uninstall());

    it('sets the message/type and auto-clears it after 3 seconds', () => {
      component.showToast('Listo', 'success');
      expect(component.toastMessage).toBe('Listo');
      expect(component.toastType).toBe('success');

      jasmine.clock().tick(3001);
      expect(component.toastMessage).toBe('');
    });

    it('restarts the timer when a new toast arrives before the previous one clears', () => {
      component.showToast('Primero');
      jasmine.clock().tick(2000);
      component.showToast('Segundo');
      jasmine.clock().tick(2000);
      // Only 2s since "Segundo" - the old 3s timer from "Primero" must not have fired independently
      expect(component.toastMessage).toBe('Segundo');
    });
  });

  describe('saveProfile', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInit({ description: 'old', activeTitle: 'old title' });
      component.editDescription = 'nueva desc';
      component.editActiveTitle = 'Ninguno';
      component.editAvatarIcon = 'ash';
      component.editSelectedMedals = ['gold'];
    });

    it('does nothing if already saving', () => {
      component.savingProfile = true;
      component.saveProfile();
      httpMock.expectNone(`${environment.apiUrl}/users/profile`);
    });

    it('sends an empty activeTitle when "Ninguno" is selected, and updates local state on success', () => {
      component.saveProfile();
      const req = httpMock.expectOne(`${environment.apiUrl}/users/profile`);
      expect(req.request.body.activeTitle).toBe('');
      req.flush(null);

      expect(component.profileData?.description).toBe('nueva desc');
      expect(component.savingProfile).toBeFalse();
      expect(component.showEditModal).toBeFalse();
      expect(component.toastMessage).toContain('guardado correctamente');
    });

    it('shows a session-expired message on a 401/403', () => {
      component.saveProfile();
      httpMock.expectOne(`${environment.apiUrl}/users/profile`)
        .flush({ message: 'irrelevant' }, { status: 401, statusText: 'Unauthorized' });

      expect(component.toastType).toBe('error');
      expect(component.toastMessage).toContain('sesión expiró');
    });

    it('shows the server error message for other failures', () => {
      component.saveProfile();
      httpMock.expectOne(`${environment.apiUrl}/users/profile`)
        .flush({ message: 'Descripción muy larga' }, { status: 400, statusText: 'Bad Request' });

      expect(component.toastMessage).toContain('Descripción muy larga');
    });
  });

  describe('showcase management', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInit({ showcase: [{ slotPosition: 1, cardId: 'xy1-4' }] });
    });

    it('finds the showcase card at a given slot position', () => {
      expect(component.getShowcaseSlot(1)?.cardId).toBe('xy1-4');
      expect(component.getShowcaseSlot(2)).toBeUndefined();
    });

    it('resolves a card image from the live registry, defaulting to empty', () => {
      tcgService.cards.set([{ id: 'xy1-4', images: { small: 'https://example.com/s.png' } } as any]);
      expect(component.getCardImageById('xy1-4')).toBe('https://example.com/s.png');
      expect(component.getCardImageById('unknown')).toBe('');
    });

    it('opens and closes the card selector for a slot', () => {
      component.openCardSelector(2);
      expect(component.showCardSelector).toBeTrue();
      expect(component.selectedSlotPosition).toBe(2);

      component.closeCardSelector();
      expect(component.showCardSelector).toBeFalse();
      expect(component.selectedSlotPosition).toBeNull();
    });

    it('selects a card for the showcase and reloads the profile', () => {
      component.selectedSlotPosition = 3;
      component.selectCardForShowcase('xy1-99');

      const req = httpMock.expectOne(`${environment.apiUrl}/users/profile/showcase`);
      expect(req.request.body).toEqual({ slots: [{ slotPosition: 3, cardId: 'xy1-99' }] });
      req.flush(null);

      flushInit({ showcase: [{ slotPosition: 3, cardId: 'xy1-99' }] });
      expect(component.profileData?.showcase?.[0].cardId).toBe('xy1-99');
    });

    it('does nothing when no slot is selected', () => {
      component.selectedSlotPosition = null;
      component.selectCardForShowcase('xy1-99');
      httpMock.expectNone(`${environment.apiUrl}/users/profile/showcase`);
    });

    it('removes a card after confirmation, stopping event propagation', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      const event = new MouseEvent('click');
      spyOn(event, 'stopPropagation');

      component.removeCardFromShowcase(1, event);

      expect(event.stopPropagation).toHaveBeenCalled();
      const req = httpMock.expectOne(`${environment.apiUrl}/users/profile/showcase`);
      expect(req.request.body).toEqual({ slots: [{ slotPosition: 1, cardId: '' }] });
      req.flush(null);
      flushInit();
    });

    it('does not remove a card when the confirmation is declined', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      component.removeCardFromShowcase(1, new MouseEvent('click'));
      httpMock.expectNone(`${environment.apiUrl}/users/profile/showcase`);
    });
  });

  describe('drag and drop', () => {
    it('sets the dragged card id and effect on drag start', () => {
      const setData = jasmine.createSpy('setData');
      const event = { dataTransfer: { setData, effectAllowed: '' } } as unknown as DragEvent;
      component.handleDragStart(event, 'xy1-4');
      expect(setData).toHaveBeenCalledWith('text/cardId', 'xy1-4');
      expect((event.dataTransfer as any).effectAllowed).toBe('copy');
    });

    it('prevents the default browser behavior on allowDrop', () => {
      const event = new DragEvent('dragover');
      spyOn(event, 'preventDefault');
      component.allowDrop(event);
      expect(event.preventDefault).toHaveBeenCalled();
    });

    it('drops a card onto a slot and reloads the profile', () => {
      fixture.detectChanges();
      flushInit();

      const getData = jasmine.createSpy('getData').and.returnValue('xy1-77');
      const event = { preventDefault: () => {}, dataTransfer: { getData } } as unknown as DragEvent;

      component.handleDropOnSlot(event, 2);

      const req = httpMock.expectOne(`${environment.apiUrl}/users/profile/showcase`);
      expect(req.request.body).toEqual({ slots: [{ slotPosition: 2, cardId: 'xy1-77' }] });
      req.flush(null);
      flushInit();
    });

    it('does nothing when the drop carries no card id', () => {
      const getData = jasmine.createSpy('getData').and.returnValue('');
      const event = { preventDefault: () => {}, dataTransfer: { getData } } as unknown as DragEvent;
      component.handleDropOnSlot(event, 2);
      httpMock.expectNone(`${environment.apiUrl}/users/profile/showcase`);
    });
  });

  describe('showcased deck', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInit();
    });

    it('selects a deck by numeric id', () => {
      component.selectShowcasedDeck('42');
      const req = httpMock.expectOne(`${environment.apiUrl}/users/profile/showcase/deck`);
      expect(req.request.body).toEqual({ deckId: 42 });
      req.flush(null);
      flushInit();
    });

    it('treats the string "null" or a falsy value as clearing the deck', () => {
      component.selectShowcasedDeck('null');
      const req = httpMock.expectOne(`${environment.apiUrl}/users/profile/showcase/deck`);
      expect(req.request.body).toEqual({ deckId: null });
      req.flush(null);
      flushInit();
    });

    it('removes the showcased deck after confirmation', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      component.removeShowcasedDeck();
      const req = httpMock.expectOne(`${environment.apiUrl}/users/profile/showcase/deck`);
      expect(req.request.body).toEqual({ deckId: null });
      req.flush(null);
      flushInit();
    });

    it('does not remove the deck when confirmation is declined', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      component.removeShowcasedDeck();
      httpMock.expectNone(`${environment.apiUrl}/users/profile/showcase/deck`);
    });
  });

  it('getCategoryColor maps known categories and falls back to gray', () => {
    expect(component.getCategoryColor('NIVEL')).toBe('#ffce32');
    expect(component.getCategoryColor('UNKNOWN')).toBe('#cfd6e4');
  });

  describe('filteredShowcaseCards', () => {
    it('filters the live card registry by the search query, case-insensitively', () => {
      tcgService.cards.set([
        { id: '1', name: 'Charizard' } as any,
        { id: '2', name: 'Blastoise' } as any,
      ]);
      component.cardSearchQuery = 'char';
      expect(component.filteredShowcaseCards.map((c: any) => c.name)).toEqual(['Charizard']);
    });
  });

  describe('topDecks', () => {
    it('returns placeholder decks when the user has none', () => {
      expect(component.topDecks.length).toBe(2);
      expect(component.topDecks[0].name).toBe('Mazo Fuego Inicial');
    });

    it('derives up to 4 decks deterministically from the user\'s real decks', () => {
      component.userDecks = [{ id: 5, name: 'Mi Mazo' }];
      const decks = component.topDecks;
      expect(decks.length).toBe(1);
      expect(decks[0].name).toBe('Mi Mazo');
      expect(decks[0].wins).toBe((5 * 7) % 15 + 2);
      expect(decks[0].losses).toBe((5 * 3) % 10 + 1);
    });
  });

  describe('stats getters', () => {
    it('default to 0 with no profile data', () => {
      expect(component.totalCardsPlayed).toBe(0);
      expect(component.overallWinRate).toBe(0);
      expect(component.totalWins).toBe(0);
      expect(component.totalLosses).toBe(0);
    });

    it('read real values once the profile is loaded', () => {
      fixture.detectChanges();
      flushInit({
        statistics: { matchesWon: 8, matchesLost: 2, winRate: 80, trainerCardsPlayed: 3 },
        advancedStats: { pokemonStats: [{ timesPlayed: 5 }, { timesPlayed: 2 }] },
      });
      expect(component.totalWins).toBe(8);
      expect(component.totalLosses).toBe(2);
      expect(component.overallWinRate).toBe(80);
      expect(component.totalCardsPlayed).toBe(10); // 5+2 pokemon + 3 trainer
    });
  });

  describe('donutStops', () => {
    it('is a flat neutral ring with no matches played', () => {
      expect(component.donutStops).toBe('var(--line) 0% 100%');
    });

    it('splits the ring proportionally between wins and losses', () => {
      fixture.detectChanges();
      flushInit({ statistics: { matchesWon: 3, matchesLost: 1, winRate: 75 } });
      expect(component.donutStops).toBe('var(--accent) 0% 75%, var(--mut) 75% 100%');
    });
  });

  it('toggleMatchExpand flips the expanded state for a given match id', () => {
    component.toggleMatchExpand(10);
    expect(component.expandedMatches[10]).toBeTrue();
    component.toggleMatchExpand(10);
    expect(component.expandedMatches[10]).toBeFalse();
  });

  describe('parseMatchStats', () => {
    it('returns null for undefined input', () => {
      expect(component.parseMatchStats(undefined)).toBeNull();
    });

    it('parses valid JSON', () => {
      expect(component.parseMatchStats('{"a":1}')).toEqual({ a: 1 });
    });

    it('returns null and does not throw on invalid JSON', () => {
      expect(component.parseMatchStats('{not json')).toBeNull();
    });
  });

  describe('sumValues / getEnergyList', () => {
    it('sumValues sums a map, treating null/undefined as 0', () => {
      expect(component.sumValues({ a: 1, b: 2 })).toBe(3);
      expect(component.sumValues(null)).toBe(0);
      expect(component.sumValues(undefined)).toBe(0);
    });

    it('getEnergyList converts a map to entries and drops zero counts', () => {
      expect(component.getEnergyList({ fire: 3, water: 0 })).toEqual([{ type: 'fire', count: 3 }]);
      expect(component.getEnergyList(null)).toEqual([]);
    });
  });

  describe('getMatchMvp', () => {
    it('returns null with no damage stats', () => {
      expect(component.getMatchMvp(null)).toBeNull();
      expect(component.getMatchMvp({ pokemonDamageDealt: {} })).toBeNull();
    });

    it('picks the card with the highest damage dealt', () => {
      tcgService.cards.set([{ id: 'a', name: 'Alpha' } as any]);
      const mvp = component.getMatchMvp({
        pokemonDamageDealt: { a: 50, b: 90 },
        pokemonKOsMade: { b: 2 },
      });
      expect(mvp?.cardId).toBe('b');
      expect(mvp?.damage).toBe(90);
      expect(mvp?.kos).toBe(2);
      expect(mvp?.name).toBe('b'); // not in registry, falls back to the raw id
    });
  });

  describe('type/energy helpers', () => {
    it('getTypeColor maps known types and falls back for unknown/empty', () => {
      expect(component.getTypeColor('FIRE')).toBe('#ff7a3d');
      expect(component.getTypeColor('')).toBe('var(--mut)');
      expect(component.getTypeColor('made-up')).toBe('var(--mut)');
    });

    it('getEnergyIconEmoji maps known types and falls back for unknown/empty', () => {
      expect(component.getEnergyIconEmoji('water')).toBe('💧');
      expect(component.getEnergyIconEmoji('')).toBe('⚪');
    });

    it('getEnergyLabel maps known types to Spanish and returns the raw type otherwise', () => {
      expect(component.getEnergyLabel('grass')).toBe('Planta');
      expect(component.getEnergyLabel('')).toBe('Desconocido');
      expect(component.getEnergyLabel('mystery')).toBe('mystery');
    });
  });

  describe('advanced stat rankings', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInit({
        advancedStats: {
          pokemonStats: Array.from({ length: 6 }, (_, i) => ({ pokemonType: i % 2 === 0 ? 'FIRE' : 'WATER', timesPlayed: i, damageDealt: i * 10 })),
          energyStats: [{ energyType: 'FIRE', count: 2 }, { energyType: 'WATER', count: 9 }],
        },
      });
    });

    it('getTopPlayedPokemons sorts by timesPlayed and caps at 5', () => {
      const top = component.getTopPlayedPokemons();
      expect(top.length).toBe(5);
      expect(top[0].timesPlayed).toBe(5);
    });

    it('getTopPlayedPokemons filters by the element filter', () => {
      component.elementFilter = 'fire';
      const top = component.getTopPlayedPokemons();
      expect(top.every(p => p.pokemonType === 'FIRE')).toBeTrue();
    });

    it('getTopAttackers sorts by damageDealt and caps at 5, ignoring the element filter', () => {
      component.elementFilter = 'fire';
      const top = component.getTopAttackers();
      expect(top.length).toBe(5);
      expect(top[0].damageDealt).toBe(50);
    });

    it('getEnergyStats sorts by count descending', () => {
      expect(component.getEnergyStats()[0].energyType).toBe('WATER');
    });
  });
});
