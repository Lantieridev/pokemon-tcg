import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FriendsSidebarComponent } from './friends-sidebar.component';
import { ToastService } from '../../../core/services/toast.service';
import { environment } from '../../../../environments/environment';

describe('FriendsSidebarComponent', () => {
  let fixture: ComponentFixture<FriendsSidebarComponent>;
  let component: FriendsSidebarComponent;
  let httpMock: HttpTestingController;
  let toastService: ToastService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FriendsSidebarComponent, HttpClientTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(FriendsSidebarComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
  });

  afterEach(() => httpMock.verify());

  function flushInitialLoad(friends: any[] = [], requests: any[] = []): void {
    httpMock.expectOne(`${environment.apiUrl}/friends/list`).flush(friends);
    httpMock.expectOne(`${environment.apiUrl}/friends/requests`).flush(requests);
  }

  it('loads friends and pending requests on init, and reports the request count', () => {
    fixture.detectChanges();
    const emitted: number[] = [];
    component.onRequestsUpdated.subscribe((n: number) => emitted.push(n));

    flushInitialLoad(
      [{ id: 1, friendUsername: 'Misty', status: 'ACCEPTED' }],
      [{ id: 2, friendUsername: 'Brock', status: 'PENDING' }],
    );

    expect(component.friends().length).toBe(1);
    expect(component.requests().length).toBe(1);
    expect(emitted).toEqual([1]);
  });

  describe('toggleSidebar', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialLoad();
    });

    it('opens the sidebar and reloads data', () => {
      component.toggleSidebar();
      expect(component.isOpen()).toBeTrue();
      flushInitialLoad([{ id: 1, friendUsername: 'Misty' } as any]);
      expect(component.friends().length).toBe(1);
    });

    it('closes the sidebar and emits onSidebarClose without reloading', () => {
      component.toggleSidebar(); // open
      flushInitialLoad();

      let closed = false;
      component.onSidebarClose.subscribe(() => (closed = true));
      component.toggleSidebar(); // close

      expect(component.isOpen()).toBeFalse();
      expect(closed).toBeTrue();
      httpMock.expectNone(`${environment.apiUrl}/friends/list`);
    });
  });

  describe('sendRequest', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialLoad();
    });

    it('does nothing for a blank username', () => {
      component.newFriendUsername.set('   ');
      component.sendRequest();
      httpMock.expectNone(`${environment.apiUrl}/friends/request`);
    });

    it('sends the trimmed username and shows a success toast, clearing the input', () => {
      spyOn(toastService, 'success');
      component.newFriendUsername.set('  Gary  ');

      component.sendRequest();

      const req = httpMock.expectOne(`${environment.apiUrl}/friends/request`);
      expect(req.request.body).toEqual({ targetUsername: 'Gary' });
      req.flush({});

      expect(toastService.success).toHaveBeenCalledWith('Solicitud enviada correctamente');
      expect(component.newFriendUsername()).toBe('');
    });

    it('shows a specific message when the request already exists', () => {
      spyOn(toastService, 'error');
      component.newFriendUsername.set('Gary');
      component.sendRequest();

      const req = httpMock.expectOne(`${environment.apiUrl}/friends/request`);
      req.flush({ message: 'Friend request already exists' }, { status: 409, statusText: 'Conflict' });

      expect(toastService.error).toHaveBeenCalledWith('La solicitud a este usuario ya fue enviada');
    });

    it('shows the server error message for other failures', () => {
      spyOn(toastService, 'error');
      component.newFriendUsername.set('Gary');
      component.sendRequest();

      const req = httpMock.expectOne(`${environment.apiUrl}/friends/request`);
      req.flush({ message: 'Usuario no encontrado' }, { status: 404, statusText: 'Not Found' });

      expect(toastService.error).toHaveBeenCalledWith('Usuario no encontrado');
    });
  });

  describe('friend requests', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialLoad();
    });

    it('accepts a request and reloads the lists', () => {
      spyOn(toastService, 'success');
      component.acceptRequest(2);

      httpMock.expectOne(`${environment.apiUrl}/friends/accept/2`).flush({});
      expect(toastService.success).toHaveBeenCalledWith('Solicitud aceptada correctamente');
      flushInitialLoad();
    });

    it('rejects a request and reloads the lists', () => {
      component.rejectRequest(2);
      httpMock.expectOne(`${environment.apiUrl}/friends/reject/2`).flush({});
      flushInitialLoad();
    });
  });

  describe('removing a friend', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialLoad();
    });

    it('stages a friend for removal without calling the API yet', () => {
      const friend = { id: 5, friendUsername: 'Brock' } as any;
      component.confirmRemoveFriend(friend);
      expect(component.friendToDelete()).toEqual(friend);
      httpMock.expectNone(`${environment.apiUrl}/friends/remove/5`);
    });

    it('cancels the pending removal', () => {
      component.confirmRemoveFriend({ id: 5 } as any);
      component.cancelRemoveFriend();
      expect(component.friendToDelete()).toBeNull();
    });

    it('removes the staged friend, shows a toast, clears staging, and reloads', () => {
      spyOn(toastService, 'success');
      component.confirmRemoveFriend({ id: 5, friendUsername: 'Brock' } as any);

      component.removeFriend();

      httpMock.expectOne(`${environment.apiUrl}/friends/remove/5`).flush({});
      expect(toastService.success).toHaveBeenCalledWith('Amigo eliminado correctamente');
      expect(component.friendToDelete()).toBeNull();
      flushInitialLoad();
    });

    it('does nothing if removeFriend is called with nothing staged', () => {
      component.removeFriend();
      httpMock.expectNone((req) => req.url.includes('/friends/remove/'));
    });
  });

  describe('outputs', () => {
    it('emits onOpenProfile with the username', () => {
      fixture.detectChanges();
      flushInitialLoad();
      let emitted = '';
      component.onOpenProfile.subscribe((u: string) => (emitted = u));
      component.viewProfile('Misty');
      expect(emitted).toBe('Misty');
    });

    it('emits onOpenChat with the friend', () => {
      fixture.detectChanges();
      flushInitialLoad();
      let emitted: any = null;
      const friend = { friendUsername: 'Misty' } as any;
      component.onOpenChat.subscribe((f: any) => (emitted = f));
      component.openChat(friend);
      expect(emitted).toBe(friend);
    });
  });

  describe('avatar helpers (shared with chat-modal)', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialLoad();
    });

    it('isCustomAvatar treats known emoji keys as not custom', () => {
      expect(component.isCustomAvatar('brock')).toBeFalse();
      expect(component.isCustomAvatar('charizard_3d')).toBeTrue();
      expect(component.isCustomAvatar(undefined)).toBeFalse();
    });

    it('getAvatarUrl maps known names and normalizes unknown ones', () => {
      expect(component.getAvatarUrl('Brock')).toBe('assets/store/avatar_brock.png');
      expect(component.getAvatarUrl(undefined)).toBe('');
      expect(component.getAvatarUrl('Campeón É')).toBe('assets/achievements/avatars/avatar_campeon_e.png');
    });

    it('getAvatarEmoji maps known icons case-insensitively and falls back', () => {
      expect(component.getAvatarEmoji('GARY')).toBe('👑');
      expect(component.getAvatarEmoji(undefined)).toBe('🎒');
    });
  });
});
