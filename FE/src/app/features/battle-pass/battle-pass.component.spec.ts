import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BattlePassComponent } from './battle-pass.component';
import { ToastService } from '../../core/services/toast.service';
import { environment } from '../../../environments/environment';
import { BattlePassLevelDTO, BattlePassStatusDTO } from '../../core/services/battle-pass.service';

function makeLevel(level: number, overrides: Partial<BattlePassLevelDTO> = {}): BattlePassLevelDTO {
  return {
    level,
    requiredXp: level * 100,
    freeRewardType: 'COINS',
    freeRewardAmount: 50,
    freeRewardValue: '',
    premiumRewardType: 'PACK',
    premiumRewardAmount: 1,
    premiumRewardValue: '',
    ...overrides,
  };
}

function makeStatus(overrides: Partial<BattlePassStatusDTO> = {}): BattlePassStatusDTO {
  return {
    isPremium: false,
    currentXp: 0,
    currentLevel: 0,
    claimedFreeLevel: 0,
    claimedPremiumLevel: 0,
    levels: Array.from({ length: 3 }, (_, i) => makeLevel(i + 1)),
    ...overrides,
  };
}

function loginAs(username: string): void {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: username, exp: Math.floor(Date.now() / 1000) + 3600 }));
  localStorage.setItem('jwt', `${header}.${payload}.signature`);
  localStorage.setItem('username', username);
  localStorage.setItem('userId', '1');
}

describe('BattlePassComponent', () => {
  let fixture: ComponentFixture<BattlePassComponent>;
  let component: BattlePassComponent;
  let httpMock: HttpTestingController;
  let toastService: ToastService;

  beforeEach(async () => {
    localStorage.clear();
    loginAs('AshRivero');
    await TestBed.configureTestingModule({
      imports: [BattlePassComponent, HttpClientTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(BattlePassComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  function flushStatus(status: BattlePassStatusDTO): void {
    httpMock.expectOne(`${environment.apiUrl}/battle-pass/status`).flush(status);
    httpMock.expectOne(`${environment.apiUrl}/users/AshRivero/profile`).flush({ pokecoins: 500 });
  }

  it('starts loading and fetches status + profile pokecoins on init', () => {
    fixture.detectChanges();
    expect(component.loading()).toBeTrue();

    flushStatus(makeStatus());

    expect(component.loading()).toBeFalse();
    expect(component.pokecoins()).toBe(500);
  });

  it('shows a toast and stops loading when the status fetch fails', () => {
    spyOn(toastService, 'error');
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiUrl}/battle-pass/status`).flush('err', { status: 500, statusText: 'Server Error' });
    httpMock.expectOne(`${environment.apiUrl}/users/AshRivero/profile`).flush({ pokecoins: 0 });

    expect(toastService.error).toHaveBeenCalledWith('Error al cargar el Pase de Batalla');
    expect(component.loading()).toBeFalse();
  });

  describe('progressPercentage', () => {
    it('is 0 with no status loaded', () => {
      expect(component.progressPercentage()).toBe(0);
    });

    it('computes progress toward the next level from currentXp', () => {
      fixture.detectChanges();
      flushStatus(makeStatus({
        currentLevel: 1,
        currentXp: 150,
        levels: [makeLevel(1, { requiredXp: 100 }), makeLevel(2, { requiredXp: 200 })],
      }));

      // range = 200-100=100, currentLevelXp = 150-100=50 -> 50%
      expect(component.progressPercentage()).toBe(50);
    });

    it('caps at 100 when already at the max level', () => {
      fixture.detectChanges();
      flushStatus(makeStatus({ currentLevel: 3, currentXp: 999, levels: [makeLevel(1), makeLevel(2), makeLevel(3)] }));
      expect(component.progressPercentage()).toBe(100);
    });
  });

  describe('pagination', () => {
    beforeEach(() => {
      fixture.detectChanges();
      const levels = Array.from({ length: 25 }, (_, i) => makeLevel(i + 1));
      flushStatus(makeStatus({ currentLevel: 0, levels }));
    });

    it('computes total pages from the level count and page size (10)', () => {
      expect(component.totalPages()).toBe(3);
    });

    it('shows only the first page of levels initially', () => {
      expect(component.displayedLevels().length).toBe(10);
      expect(component.displayedLevels()[0].level).toBe(1);
    });

    it('advances to the next page', () => {
      component.nextPage();
      expect(component.currentPage()).toBe(1);
      expect(component.displayedLevels()[0].level).toBe(11);
    });

    it('does not advance past the last page', () => {
      component.nextPage();
      component.nextPage();
      component.nextPage();
      component.nextPage();
      expect(component.currentPage()).toBe(2);
    });

    it('goes back a page', () => {
      component.nextPage();
      component.prevPage();
      expect(component.currentPage()).toBe(0);
    });

    it('does not go below page 0', () => {
      component.prevPage();
      expect(component.currentPage()).toBe(0);
    });
  });

  describe('loadData auto-navigation to the current level page', () => {
    it('jumps to the page containing the current level on initial load', () => {
      fixture.detectChanges();
      const levels = Array.from({ length: 25 }, (_, i) => makeLevel(i + 1));
      flushStatus(makeStatus({ currentLevel: 15, levels }));

      // level 15 -> floor(15/10) = page 1
      expect(component.currentPage()).toBe(1);
    });

    it('does not re-navigate when preservePage is true', () => {
      fixture.detectChanges();
      const levels = Array.from({ length: 25 }, (_, i) => makeLevel(i + 1));
      flushStatus(makeStatus({ currentLevel: 15, levels }));
      expect(component.currentPage()).toBe(1);

      component.currentPage.set(2);
      component.loadData(true);
      httpMock.expectOne(`${environment.apiUrl}/battle-pass/status`).flush(makeStatus({ currentLevel: 15, levels }));
      httpMock.expectOne(`${environment.apiUrl}/users/AshRivero/profile`).flush({ pokecoins: 500 });

      expect(component.currentPage()).toBe(2);
    });
  });

  describe('canClaimFree', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('is false when the level is beyond the current level', () => {
      flushStatus(makeStatus({ currentLevel: 1, levels: [makeLevel(1), makeLevel(2)] }));
      expect(component.canClaimFree(2)).toBeFalse();
    });

    it('is false when already claimed', () => {
      flushStatus(makeStatus({ currentLevel: 3, claimedFreeLevel: 2, levels: [makeLevel(1), makeLevel(2), makeLevel(3)] }));
      expect(component.canClaimFree(2)).toBeFalse();
    });

    it('is true for the next unclaimed level with a free reward', () => {
      flushStatus(makeStatus({ currentLevel: 3, claimedFreeLevel: 1, levels: [makeLevel(1), makeLevel(2), makeLevel(3)] }));
      expect(component.canClaimFree(2)).toBeTrue();
    });

    it('is false when an earlier unclaimed level with a reward must be claimed first', () => {
      flushStatus(makeStatus({
        currentLevel: 3,
        claimedFreeLevel: 0,
        levels: [makeLevel(1, { freeRewardType: 'COINS' }), makeLevel(2), makeLevel(3)],
      }));
      // level 1 has an unclaimed free reward, so level 2 can't be claimed first
      expect(component.canClaimFree(2)).toBeFalse();
    });
  });

  describe('canClaimPremium', () => {
    beforeEach(() => fixture.detectChanges());

    it('is false when the user is not premium', () => {
      flushStatus(makeStatus({ isPremium: false, currentLevel: 3, levels: [makeLevel(1)] }));
      expect(component.canClaimPremium(1)).toBeFalse();
    });

    it('is true for the next unclaimed premium level when premium', () => {
      flushStatus(makeStatus({ isPremium: true, currentLevel: 2, claimedPremiumLevel: 0, levels: [makeLevel(1), makeLevel(2)] }));
      expect(component.canClaimPremium(1)).toBeTrue();
    });
  });

  describe('claimReward', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushStatus(makeStatus({
        currentLevel: 2,
        levels: [makeLevel(1, { freeRewardType: 'COINS', freeRewardValue: 'x', freeRewardAmount: 50 }), makeLevel(2)],
      }));
    });

    it('sets claimedReward with the resolved free-reward details on success', () => {
      component.claimReward(1, false);
      httpMock.expectOne(`${environment.apiUrl}/battle-pass/claim?level=1&isPremium=false`).flush(null);

      expect(component.claimedReward()).toEqual({ type: 'COINS', value: 'x', amount: 50, isPremium: false });
    });

    it('shows the server error message on failure', () => {
      spyOn(toastService, 'error');
      component.claimReward(1, false);
      httpMock.expectOne(`${environment.apiUrl}/battle-pass/claim?level=1&isPremium=false`)
        .flush({ message: 'Ya reclamado' }, { status: 400, statusText: 'Bad Request' });

      expect(toastService.error).toHaveBeenCalledWith('Ya reclamado');
    });
  });

  describe('purchase premium flow', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushStatus(makeStatus());
    });

    it('refuses to open the purchase modal without enough pokecoins', () => {
      spyOn(toastService, 'error');
      component.pokecoins.set(500);
      component.openPurchaseModal();

      expect(toastService.error).toHaveBeenCalledWith('No tienes suficientes Pokécoins (1000)');
      expect(component.showPurchaseModal()).toBeFalse();
    });

    it('opens the purchase modal with enough pokecoins', () => {
      component.pokecoins.set(1500);
      component.openPurchaseModal();
      expect(component.showPurchaseModal()).toBeTrue();
    });

    it('confirms the purchase, shows a success toast, and reloads', () => {
      spyOn(toastService, 'success');
      component.showPurchaseModal.set(true);
      component.confirmPurchasePremium();

      expect(component.showPurchaseModal()).toBeFalse();
      httpMock.expectOne(`${environment.apiUrl}/battle-pass/purchase-premium`).flush(null);
      expect(toastService.success).toHaveBeenCalledWith('¡Has desbloqueado el Pase Premium!');

      flushStatus(makeStatus());
    });

    it('shows an error toast when the purchase fails', () => {
      spyOn(toastService, 'error');
      component.confirmPurchasePremium();
      httpMock.expectOne(`${environment.apiUrl}/battle-pass/purchase-premium`)
        .flush({ message: 'Fondos insuficientes' }, { status: 400, statusText: 'Bad Request' });

      expect(toastService.error).toHaveBeenCalledWith('Fondos insuficientes');
    });
  });

  describe('getRewardTypeName', () => {
    it('translates known reward types to Spanish', () => {
      expect(component.getRewardTypeName('AVATAR')).toBe('Avatar Exclusivo');
      expect(component.getRewardTypeName('coins')).toBe('Pokémonedas');
    });

    it('returns an empty string for null', () => {
      expect(component.getRewardTypeName(null)).toBe('');
    });

    it('returns the raw type for an unrecognized value', () => {
      expect(component.getRewardTypeName('MYSTERY')).toBe('MYSTERY');
    });
  });

  describe('getRewardImage', () => {
    it('returns the coin stack image for COINS regardless of value', () => {
      expect(component.getRewardImage('COINS', null)).toBe('assets/images/rewards/coins_stack.png');
    });

    it('normalizes a value into a slug-based asset path with the type prefix', () => {
      expect(component.getRewardImage('AVATAR', 'Pikachu Chibi')).toBe('assets/images/rewards/avatar_pikachu_chibi.png');
    });

    it('falls back to a generic asset per type when there is no value', () => {
      expect(component.getRewardImage('PACK', null)).toBe('assets/store/pack_base.png');
      expect(component.getRewardImage('TITLE', null)).toBe('assets/rewards/title_scroll.png');
    });

    it('returns an empty string for a null type', () => {
      expect(component.getRewardImage(null, null)).toBe('');
    });
  });

  describe('onImageError', () => {
    it('swaps the broken image src for the type-specific fallback', () => {
      const img = document.createElement('img');
      img.src = 'https://example.com/original.png';
      component.onImageError({ target: img }, 'PACK');
      expect(img.src).toContain('assets/store/pack_base.png');
    });

    it('does not loop if the fallback itself is already the current src', () => {
      const img = document.createElement('img');
      img.src = `${location.origin}/assets/store/pack_base.png`;
      const originalSrc = img.src;
      component.onImageError({ target: img }, 'PACK');
      expect(img.src).toBe(originalSrc);
    });
  });
});
