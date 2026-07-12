import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { NavbarComponent } from './navbar.component';
import { FriendsSidebarComponent } from '../../../shared/components/friends-sidebar/friends-sidebar.component';
import { PublicProfileModalComponent } from '../../../shared/components/public-profile-modal/public-profile-modal.component';
import { ChatModalComponent } from '../../../shared/components/chat-modal/chat-modal.component';
import { AuthService } from '../../services/auth.service';
import { FriendsApiService } from '../../services/friends-api.service';
import { FriendsWsService } from '../../services/friends-ws.service';
import { environment } from '../../../../environments/environment';

function loginAs(username: string): void {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: username, exp: Math.floor(Date.now() / 1000) + 3600 }));
  localStorage.setItem('jwt', `${header}.${payload}.signature`);
  localStorage.setItem('username', username);
  localStorage.setItem('userId', '1');
}

describe('NavbarComponent', () => {
  let fixture: ComponentFixture<NavbarComponent>;
  let component: NavbarComponent;
  let httpMock: HttpTestingController;
  let friendsApi: FriendsApiService;
  let friendsWs: FriendsWsService;

  async function setup(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [NavbarComponent, HttpClientTestingModule],
      providers: [provideRouter([])],
    })
      .overrideComponent(NavbarComponent, {
        remove: { imports: [FriendsSidebarComponent, PublicProfileModalComponent, ChatModalComponent] },
        add: { schemas: [NO_ERRORS_SCHEMA] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    friendsApi = TestBed.inject(FriendsApiService);
    friendsWs = TestBed.inject(FriendsWsService);
  }

  afterEach(() => {
    localStorage.clear();
    httpMock.verify();
  });

  describe('as a guest (not logged in)', () => {
    beforeEach(async () => {
      localStorage.clear();
      await setup();
    });

    it('shows "Invitado" as the username', () => {
      expect(component.username).toBe('Invitado');
    });

    it('does not fetch a profile or pending requests', () => {
      fixture.detectChanges();
      httpMock.expectNone(`${environment.apiUrl}/users/Invitado/profile`);
      expect(component.pendingRequestsCount()).toBe(0);
    });

    it('renders the trainer chip but not the authenticated nav links', () => {
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Mazos');
    });
  });

  describe('as a logged-in user', () => {
    beforeEach(async () => {
      loginAs('AshRivero');
      await setup();
    });

    it('exposes the real username and its uppercase initial', () => {
      expect(component.username).toBe('AshRivero');
      expect(component.userInitial).toBe('A');
    });

    it('loads the profile and pending requests on init', () => {
      fixture.detectChanges();

      const profileReq = httpMock.expectOne(`${environment.apiUrl}/users/AshRivero/profile`);
      profileReq.flush({ xp: 100, pokecoins: 50, battlePoints: 3, mmr: 1200 });

      const pendingReq = httpMock.expectOne(`${environment.apiUrl}/friends/requests`);
      pendingReq.flush([{ friendUsername: 'Misty', status: 'PENDING' }]);

      expect(component.pendingRequestsCount()).toBe(1);
    });

    it('renders the authenticated nav links once logged in', () => {
      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/users/AshRivero/profile`).flush({ xp: 0, pokecoins: 0, battlePoints: 0, mmr: 1000 });
      httpMock.expectOne(`${environment.apiUrl}/friends/requests`).flush([]);

      expect(fixture.nativeElement.textContent).toContain('Mazos');
    });
  });

  describe('isActive', () => {
    beforeEach(async () => {
      localStorage.clear();
      await setup();
      fixture.detectChanges();
    });

    it('treats the root path as the lobby being active', () => {
      expect(component.isActive('/lobby')).toBeTrue();
    });

    it('matches a path by prefix for non-lobby routes', () => {
      (component as any).currentPath = () => '/deck/123';
      expect(component.isActive('/deck')).toBeTrue();
    });

    it('does not match an unrelated path', () => {
      (component as any).currentPath = () => '/store';
      expect(component.isActive('/ranking')).toBeFalse();
    });
  });

  describe('profile modal', () => {
    beforeEach(async () => {
      localStorage.clear();
      await setup();
      fixture.detectChanges();
    });

    it('opens the profile modal with the fetched public profile', () => {
      component.openProfileModal('Misty');
      const req = httpMock.expectOne(`${environment.apiUrl}/users/Misty/profile/public`);
      req.flush({ username: 'Misty', displayName: 'Misty' } as any);

      expect(component.isProfileModalOpen()).toBeTrue();
      expect(component.selectedProfile()?.username).toBe('Misty');
    });

    it('closes the modal and clears the selected profile', () => {
      component.selectedProfile.set({ username: 'Misty' } as any);
      component.isProfileModalOpen.set(true);

      component.closeProfileModal();

      expect(component.isProfileModalOpen()).toBeFalse();
      expect(component.selectedProfile()).toBeNull();
    });
  });

  describe('chat modal', () => {
    beforeEach(async () => {
      localStorage.clear();
      await setup();
      fixture.detectChanges();
    });

    it('opens a chat with a friend', () => {
      const friend = { friendUsername: 'Brock', status: 'ACCEPTED' } as any;
      component.openChatModal(friend);
      expect(component.selectedChatFriend()?.friendUsername).toBe('Brock');
    });

    it('closes the chat when clicking the already-open friend again', () => {
      const friend = { friendUsername: 'Brock', status: 'ACCEPTED' } as any;
      component.openChatModal(friend);
      component.openChatModal(friend);
      expect(component.selectedChatFriend()).toBeNull();
    });

    it('clears the unread count for that friend when their chat is opened', () => {
      component.unreadMessagesPerUser.set({ Brock: 3, Misty: 1 });
      component.unreadMessagesCount.set(4);

      component.openChatModal({ friendUsername: 'Brock', status: 'ACCEPTED' } as any);

      expect(component.unreadMessagesPerUser()['Brock']).toBeUndefined();
      expect(component.unreadMessagesCount()).toBe(1);
    });

    it('closeChatModal clears the selected friend', () => {
      component.selectedChatFriend.set({ friendUsername: 'Brock' } as any);
      component.closeChatModal();
      expect(component.selectedChatFriend()).toBeNull();
    });
  });

  describe('custom chat notifications', () => {
    beforeEach(async () => {
      localStorage.clear();
      await setup();
      fixture.detectChanges();
    });

    it('shows a custom notification and auto-dismisses it after 4 seconds', fakeAsync(() => {
      component.showCustomNotification({ senderUsername: 'Brock', content: 'Hey!' });
      expect(component.customNotification()).toEqual({ sender: 'Brock', message: 'Hey!' });

      tick(4001);
      expect(component.customNotification()).toBeNull();
    }));

    it('opens the chat with the sender and clears the notification', () => {
      component.customNotification.set({ sender: 'Brock', message: 'hi' });
      component.openChatWithSender('Brock');

      expect(component.selectedChatFriend()?.friendUsername).toBe('Brock');
      expect(component.customNotification()).toBeNull();
    });
  });

  describe('incoming websocket messages', () => {
    beforeEach(async () => {
      loginAs('AshRivero');
      await setup();
      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/users/AshRivero/profile`).flush({ xp: 0, pokecoins: 0, battlePoints: 0, mmr: 1000 });
      httpMock.expectOne(`${environment.apiUrl}/friends/requests`).flush([]);
    });

    it('increments the unread count for an incoming message from someone else', () => {
      friendsWs.messages$.next({ senderUsername: 'Misty', content: 'Hi!' } as any);
      expect(component.unreadMessagesCount()).toBe(1);
      expect(component.unreadMessagesPerUser()['Misty']).toBe(1);
    });

    it('ignores a message the current user sent themselves (echo)', () => {
      friendsWs.messages$.next({ senderUsername: 'AshRivero', content: 'Hi!' } as any);
      expect(component.unreadMessagesCount()).toBe(0);
    });

    it('does not increment the unread count when that chat is already open', () => {
      component.selectedChatFriend.set({ friendUsername: 'Misty', status: 'ACCEPTED' } as any);
      friendsWs.messages$.next({ senderUsername: 'Misty', content: 'Hi!' } as any);
      expect(component.unreadMessagesCount()).toBe(0);
    });
  });
});
