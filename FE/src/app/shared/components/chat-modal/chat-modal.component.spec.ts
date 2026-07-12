import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ChatModalComponent } from './chat-modal.component';
import { FriendsWsService } from '../../../core/services/friends-ws.service';
import { environment } from '../../../../environments/environment';

describe('ChatModalComponent', () => {
  let fixture: ComponentFixture<ChatModalComponent>;
  let component: ChatModalComponent;
  let httpMock: HttpTestingController;
  let friendsWs: FriendsWsService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatModalComponent, HttpClientTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(ChatModalComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    friendsWs = TestBed.inject(FriendsWsService);
    component.friend = { friendUsername: 'Misty', status: 'ACCEPTED' } as any;
  });

  afterEach(() => httpMock.verify());

  it('loads the chat history for the given friend on init', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/friends/chat/Misty`);
    req.flush([{ senderUsername: 'Misty', receiverUsername: 'me', content: 'hi' }]);

    expect(component.messages.length).toBe(1);
    expect(component.messages[0].content).toBe('hi');
  });

  it('appends an incoming websocket message addressed to/from this friend', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/friends/chat/Misty`).flush([]);

    friendsWs.messages$.next({ senderUsername: 'Misty', receiverUsername: 'me', content: 'yo' } as any);

    expect(component.messages.length).toBe(1);
    expect(component.messages[0].content).toBe('yo');
  });

  it('appends a message this user sent to the friend (receiverUsername match)', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/friends/chat/Misty`).flush([]);

    friendsWs.messages$.next({ senderUsername: 'me', receiverUsername: 'Misty', content: 'sup' } as any);

    expect(component.messages.length).toBe(1);
  });

  it('ignores a websocket message unrelated to this conversation', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/friends/chat/Misty`).flush([]);

    friendsWs.messages$.next({ senderUsername: 'Brock', receiverUsername: 'someoneElse', content: 'noise' } as any);

    expect(component.messages.length).toBe(0);
  });

  it('unsubscribes from the websocket feed on destroy', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/friends/chat/Misty`).flush([]);

    component.ngOnDestroy();
    friendsWs.messages$.next({ senderUsername: 'Misty', receiverUsername: 'me', content: 'too late' } as any);

    expect(component.messages.length).toBe(0);
  });

  describe('sendMessage', () => {
    beforeEach(() => {
      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/friends/chat/Misty`).flush([]);
    });

    it('sends the trimmed message via the websocket and clears the input', () => {
      spyOn(friendsWs, 'sendChatMessage');
      component.newMessage = 'hello there';

      component.sendMessage();

      expect(friendsWs.sendChatMessage).toHaveBeenCalledWith(
        jasmine.objectContaining({ receiverUsername: 'Misty', content: 'hello there' })
      );
      expect(component.newMessage).toBe('');
    });

    it('does not send an empty or whitespace-only message', () => {
      spyOn(friendsWs, 'sendChatMessage');
      component.newMessage = '   ';

      component.sendMessage();

      expect(friendsWs.sendChatMessage).not.toHaveBeenCalled();
    });
  });

  it('emits close when onClose is called', () => {
    let closed = false;
    component.close.subscribe(() => (closed = true));
    component.onClose();
    expect(closed).toBeTrue();
  });

  describe('isCustomAvatar', () => {
    it('treats undefined as not custom', () => {
      expect(component.isCustomAvatar(undefined)).toBeFalse();
    });

    it('treats a known emoji-style avatar key as not custom', () => {
      expect(component.isCustomAvatar('ash')).toBeFalse();
    });

    it('treats an unrecognized value as a custom avatar', () => {
      expect(component.isCustomAvatar('charizard_3d')).toBeTrue();
    });
  });

  describe('getAvatarUrl', () => {
    it('returns an empty string for undefined', () => {
      expect(component.getAvatarUrl(undefined)).toBe('');
    });

    it('maps a known named avatar to its static asset path', () => {
      expect(component.getAvatarUrl('Ash Ketchum')).toBe('assets/store/avatar_ash.png');
      expect(component.getAvatarUrl('ash_avatar')).toBe('assets/store/avatar_ash.png');
    });

    it('normalizes an unrecognized value into the achievements avatar path', () => {
      expect(component.getAvatarUrl('Mi Ávatar Épico')).toBe('assets/achievements/avatars/avatar_mi_avatar_epico.png');
    });

    it('does not double the avatar_ prefix when already present', () => {
      expect(component.getAvatarUrl('avatar_custom_thing')).toBe('assets/achievements/avatars/avatar_custom_thing.png');
    });
  });

  describe('getAvatarEmoji', () => {
    it('returns the backpack emoji for undefined or unrecognized icons', () => {
      expect(component.getAvatarEmoji(undefined)).toBe('🎒');
      expect(component.getAvatarEmoji('unknown')).toBe('🎒');
    });

    it('maps known icon keys to their emoji', () => {
      expect(component.getAvatarEmoji('ash')).toBe('🧢');
      expect(component.getAvatarEmoji('MISTY')).toBe('💧');
    });
  });
});
