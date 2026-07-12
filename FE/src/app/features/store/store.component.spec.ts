import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { StoreComponent } from './store.component';
import { ToastService } from '../../core/services/toast.service';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { environment } from '../../../environments/environment';
import { StoreItemDTO } from '../../core/services/store.service';

function loginAs(username: string): void {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: username, exp: Math.floor(Date.now() / 1000) + 3600 }));
  localStorage.setItem('jwt', `${header}.${payload}.signature`);
  localStorage.setItem('username', username);
  localStorage.setItem('userId', '1');
}

function makeItem(overrides: Partial<StoreItemDTO> = {}): StoreItemDTO {
  return {
    id: 1,
    name: 'Avatar Pikachu',
    description: 'desc',
    price: 100,
    itemType: 'AVATAR',
    imageUrl: 'pikachu_cute',
    ...overrides,
  };
}

describe('StoreComponent', () => {
  let fixture: ComponentFixture<StoreComponent>;
  let component: StoreComponent;
  let httpMock: HttpTestingController;
  let toastService: ToastService;
  let tcgService: PokemonTcgService;

  beforeEach(async () => {
    localStorage.clear();
    loginAs('AshRivero');
    await TestBed.configureTestingModule({
      imports: [StoreComponent, HttpClientTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(StoreComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
    tcgService = TestBed.inject(PokemonTcgService);
    // loadCards() only skips its HTTP fetch when cards() is already non-empty.
    tcgService.cards.set([{ id: 'placeholder' } as any]);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  function flushBalance(overrides: any = {}): void {
    httpMock.expectOne(`${environment.apiUrl}/users/AshRivero/profile`).flush({
      pokecoins: 500, packs: 0, packsInventory: {}, unlockedAvatars: [], unlockedTitles: [],
      ...overrides,
    });
  }

  it('loads the store items and account balance on init', () => {
    fixture.detectChanges();
    expect(component.loading()).toBeTrue();

    httpMock.expectOne(`${environment.apiUrl}/store/items`).flush([makeItem()]);
    flushBalance({ pokecoins: 1234 });

    expect(component.loading()).toBeFalse();
    expect(component.items().length).toBe(1);
    expect(component.pokecoins()).toBe(1234);
  });

  it('shows a toast and stops loading when the store fetch fails', () => {
    spyOn(toastService, 'error');
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/store/items`).flush('err', { status: 500, statusText: 'Server Error' });
    flushBalance();

    expect(toastService.error).toHaveBeenCalledWith('Error al cargar la tienda');
    expect(component.loading()).toBeFalse();
  });

  describe('filteredItems', () => {
    beforeEach(() => {
      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/store/items`).flush([
        makeItem({ id: 1, itemType: 'AVATAR', name: 'Avatar A' }),
        makeItem({ id: 2, itemType: 'PACK', name: 'Sobre Base', imageUrl: 'pack_base' }),
        makeItem({ id: 3, itemType: 'AVATAR', name: 'Coleccionista Estrella' }),
      ]);
      flushBalance();
    });

    it('shows AVATAR items by default, excluding the special "Coleccionista Estrella" item', () => {
      expect(component.filteredItems().map(i => i.name)).toEqual(['Avatar A']);
    });

    it('shows PACK items when that category is selected', () => {
      component.setCategory('PACK');
      expect(component.filteredItems().map(i => i.id)).toEqual([2]);
    });

    it('shows owned MY_PACKS items from the store catalog when the user has quantity > 0', () => {
      component.packsInventory.set({ pack_base: 3 });
      component.setCategory('MY_PACKS');
      expect(component.filteredItems().length).toBe(1);
      expect(component.filteredItems()[0].id).toBe(2);
    });

    it('adds inventory-only packs (reward packs not in the store catalog) to MY_PACKS', () => {
      component.packsInventory.set({ pack_exclusiva_evento: 2 });
      component.setCategory('MY_PACKS');

      const packs = component.filteredItems();
      expect(packs.length).toBe(1);
      expect(packs[0].id).toBe(-1);
      expect(packs[0].name).toBe('Sobre Exclusiva evento');
    });

    it('excludes inventory packs with 0 quantity from MY_PACKS', () => {
      component.packsInventory.set({ pack_base: 0 });
      component.setCategory('MY_PACKS');
      expect(component.filteredItems().length).toBe(0);
    });
  });

  describe('getImageSrc', () => {
    it('maps a known avatar imageUrl to its static asset', () => {
      expect(component.getImageSrc(makeItem({ itemType: 'AVATAR', imageUrl: 'ash_avatar' }))).toBe('assets/store/avatar_ash.png');
    });

    it('builds a generic avatar asset path for an unrecognized avatar', () => {
      expect(component.getImageSrc(makeItem({ itemType: 'AVATAR', imageUrl: 'some_new_skin' }))).toBe('assets/achievements/avatars/some_new_skin.png');
    });

    it('falls back to the default avatar when there is no imageUrl', () => {
      expect(component.getImageSrc(makeItem({ itemType: 'AVATAR', imageUrl: '' }))).toBe('assets/achievements/avatars/avatar_belt_white.png');
    });

    it('builds a rewards-path image for a PACK item', () => {
      expect(component.getImageSrc(makeItem({ itemType: 'PACK', imageUrl: 'pack_kanto_2' }))).toBe('assets/images/rewards/pack_kanto_2.png');
    });

    it('falls back to the winner badge for any other type without an imageUrl', () => {
      expect(component.getImageSrc(makeItem({ itemType: 'TITLE', imageUrl: '' }))).toBe('assets/achievements/avatars/avatar_winner_badge.png');
    });
  });

  describe('getOpeningPackImage', () => {
    it('uses the common pack image for the base pack types', () => {
      component.openingPackType.set('pack_base');
      expect(component.getOpeningPackImage()).toBe('assets/images/rewards/pack_comun.png');
      component.openingPackType.set('pack_xy_base');
      expect(component.getOpeningPackImage()).toBe('assets/images/rewards/pack_comun.png');
    });

    it('builds the image path from the pack type for other packs', () => {
      component.openingPackType.set('pack_kanto_2');
      expect(component.getOpeningPackImage()).toBe('assets/images/rewards/pack_kanto_2.png');
    });
  });

  describe('isOwned', () => {
    it('is always false for PACK items (infinitely purchasable)', () => {
      component.ownedAvatars.set(['pikachu_cute']);
      expect(component.isOwned(makeItem({ itemType: 'PACK', imageUrl: 'pikachu_cute' }))).toBeFalse();
    });

    it('checks ownedAvatars by name or imageUrl for AVATAR items', () => {
      component.ownedAvatars.set(['Avatar Pikachu']);
      expect(component.isOwned(makeItem({ itemType: 'AVATAR', name: 'Avatar Pikachu' }))).toBeTrue();
      expect(component.isOwned(makeItem({ itemType: 'AVATAR', name: 'Something Else', imageUrl: 'not_owned' }))).toBeFalse();
    });

    it('checks ownedTitles by name or imageUrl for TITLE items', () => {
      component.ownedTitles.set(['Campeón']);
      expect(component.isOwned(makeItem({ itemType: 'TITLE', name: 'Campeón' }))).toBeTrue();
    });

    it('is false for an unrecognized item type', () => {
      expect(component.isOwned(makeItem({ itemType: 'COSMETIC' as any }))).toBeFalse();
    });
  });

  describe('confirmPurchase', () => {
    beforeEach(() => {
      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/store/items`).flush([]);
      flushBalance({ pokecoins: 500 });
    });

    it('does nothing when there is no item staged for purchase', () => {
      component.confirmPurchase();
      httpMock.expectNone(`${environment.apiUrl}/store/buy`);
    });

    it('refuses the purchase and shows a toast when pokecoins are insufficient', () => {
      spyOn(toastService, 'error');
      component.selectedItemToBuy.set(makeItem({ price: 1000 }));

      component.confirmPurchase();

      expect(toastService.error).toHaveBeenCalledWith('Pokecoins insuficientes');
      httpMock.expectNone(`${environment.apiUrl}/store/buy`);
    });

    it('buys the item, shows a success toast, refreshes balance, and clears staging', () => {
      spyOn(toastService, 'success');
      component.selectedItemToBuy.set(makeItem({ id: 7, name: 'Avatar Raro', price: 300 }));

      component.confirmPurchase();
      expect(component.isPurchasing()).toBeTrue();

      const req = httpMock.expectOne(`${environment.apiUrl}/store/buy`);
      expect(req.request.body).toEqual({ itemId: 7 });
      req.flush(null);

      expect(toastService.success).toHaveBeenCalledWith('¡Has comprado Avatar Raro!');
      expect(component.selectedItemToBuy()).toBeNull();
      expect(component.isPurchasing()).toBeFalse();

      flushBalance();
    });

    it('shows the server error message and clears the purchasing flag on failure', () => {
      spyOn(toastService, 'error');
      component.selectedItemToBuy.set(makeItem({ price: 100 }));

      component.confirmPurchase();
      httpMock.expectOne(`${environment.apiUrl}/store/buy`)
        .flush({ message: 'Ítem agotado' }, { status: 400, statusText: 'Bad Request' });

      expect(toastService.error).toHaveBeenCalledWith('Ítem agotado');
      expect(component.isPurchasing()).toBeFalse();
    });
  });

  describe('openPack', () => {
    beforeEach(() => {
      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/store/items`).flush([]);
    });

    it('refuses to open a pack type with no inventory and no generic packs', () => {
      spyOn(toastService, 'error');
      flushBalance({ packs: 0, packsInventory: {} });

      component.openPack('pack_kanto_2');

      expect(toastService.error).toHaveBeenCalledWith('No tienes sobres de este tipo para abrir');
      httpMock.expectNone(`${environment.apiUrl}/packs/open?packType=pack_kanto_2`);
    });

    it('allows opening pack_base via the legacy generic userPacks counter', () => {
      flushBalance({ packs: 2, packsInventory: {} });

      component.openPack('pack_base');

      expect(component.openingPack()).toBeTrue();
      httpMock.expectOne(`${environment.apiUrl}/packs/open?packType=pack_base`)
        .flush({ cards: [{ cardId: 'xy1-4', isFoil: false, isDuplicate: false }], coinsRefunded: 0 });

      expect(component.openedCards().length).toBe(1);
      flushBalance({ packs: 1, packsInventory: {} });
    });

    it('opens a pack from packsInventory and auto-closes the animation after 2s', fakeAsync(() => {
      flushBalance({ packsInventory: { pack_kanto_2: 1 } });

      component.openPack('pack_kanto_2');
      httpMock.expectOne(`${environment.apiUrl}/packs/open?packType=pack_kanto_2`)
        .flush({ cards: [], coinsRefunded: 0 });
      flushBalance({ packsInventory: { pack_kanto_2: 0 } });

      expect(component.openingPack()).toBeTrue();
      tick(2000);
      expect(component.openingPack()).toBeFalse();
    }));

    it('shows the server error message and stops the opening animation on failure', () => {
      spyOn(toastService, 'error');
      flushBalance({ packsInventory: { pack_kanto_2: 1 } });

      component.openPack('pack_kanto_2');
      httpMock.expectOne(`${environment.apiUrl}/packs/open?packType=pack_kanto_2`)
        .flush({ message: 'Sin stock' }, { status: 400, statusText: 'Bad Request' });

      expect(toastService.error).toHaveBeenCalledWith('Sin stock');
      expect(component.openingPack()).toBeFalse();
    });
  });

  describe('closePackResult', () => {
    it('clears the pack result and opened cards', () => {
      component.packResult.set({ cards: [] });
      component.openedCards.set([{ cardId: 'xy1-4' } as any]);
      component.closePackResult();
      expect(component.packResult()).toBeNull();
      expect(component.openedCards()).toEqual([]);
    });
  });

  describe('zoomCard', () => {
    it('resolves the large image and type/subtypes from the live card registry when available', () => {
      tcgService.cards.set([
        { id: 'xy1-4', name: 'Charizard', images: { large: 'https://example.com/large.png' }, types: ['Fire'], subtypes: ['Stage 2'] } as any,
      ]);

      // The pulled card's own cardName takes precedence over the registry name when present.
      component.zoomCard({ cardId: 'xy1-4', cardName: 'Fallback Name', isFoil: false, isDuplicate: false });

      const zoomed = component.zoomedCard();
      expect(zoomed.img).toBe('https://example.com/large.png');
      expect(zoomed.name).toBe('Fallback Name');
      expect(zoomed.type).toBe('fire');
      expect(zoomed.subtypes).toEqual(['Stage 2']);
    });

    it('falls back to the registry name when the pulled card has none', () => {
      tcgService.cards.set([
        { id: 'xy1-4', name: 'Charizard', images: { large: 'https://example.com/large.png' }, types: ['Fire'], subtypes: [] } as any,
      ]);

      component.zoomCard({ cardId: 'xy1-4', isFoil: false, isDuplicate: false } as any);

      expect(component.zoomedCard().name).toBe('Charizard');
    });

    it('falls back to a constructed hi-res URL and the pulled-card name when not in the registry', () => {
      component.zoomCard({ cardId: 'xy1-4', cardName: 'Charmander', isFoil: false, isDuplicate: false });

      const zoomed = component.zoomedCard();
      expect(zoomed.img).toBe('https://images.pokemontcg.io/xy1/4_hires.png');
      expect(zoomed.name).toBe('Charmander');
      expect(zoomed.type).toBe('colorless');
      expect(zoomed.rarity).toBe('Common');
    });
  });
});
