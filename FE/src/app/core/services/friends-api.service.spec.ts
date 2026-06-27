import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FriendsApiService } from './friends-api.service';
import { FriendshipDTO, PublicProfileDTO, ChatMessageDTO } from '../models/friends.models';

describe('FriendsApiService', () => {
  let service: FriendsApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [FriendsApiService]
    });
    service = TestBed.inject(FriendsApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should sendFriendRequest', () => {
    service.sendFriendRequest('GaryOak').subscribe();
    const req = httpMock.expectOne('http://localhost:8081/api/friends/request');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ targetUsername: 'GaryOak' });
    req.flush({});
  });

  it('should getActiveFriends', () => {
    const mockFriends: FriendshipDTO[] = [];
    service.getActiveFriends().subscribe((friends) => {
      expect(friends).toEqual(mockFriends);
    });
    const req = httpMock.expectOne('http://localhost:8081/api/friends/list');
    expect(req.request.method).toBe('GET');
    req.flush(mockFriends);
  });

  it('should getPendingRequests', () => {
    const mockRequests: FriendshipDTO[] = [];
    service.getPendingRequests().subscribe((requests) => {
      expect(requests).toEqual(mockRequests);
    });
    const req = httpMock.expectOne('http://localhost:8081/api/friends/requests');
    expect(req.request.method).toBe('GET');
    req.flush(mockRequests);
  });

  it('should acceptFriendRequest', () => {
    service.acceptFriendRequest(12).subscribe();
    const req = httpMock.expectOne('http://localhost:8081/api/friends/accept/12');
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should rejectFriendRequest', () => {
    service.rejectFriendRequest(34).subscribe();
    const req = httpMock.expectOne('http://localhost:8081/api/friends/reject/34');
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should removeFriend', () => {
    service.removeFriend(56).subscribe();
    const req = httpMock.expectOne('http://localhost:8081/api/friends/remove/56');
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('should getPublicProfile', () => {
    const mockProfile = { username: 'AshRivero' } as any as PublicProfileDTO;
    service.getPublicProfile('AshRivero').subscribe((profile) => {
      expect(profile).toEqual(mockProfile);
    });
    const req = httpMock.expectOne('http://localhost:8081/api/users/AshRivero/profile/public');
    expect(req.request.method).toBe('GET');
    req.flush(mockProfile);
  });

  it('should getChatHistory', () => {
    const mockChat: ChatMessageDTO[] = [];
    service.getChatHistory('GaryOak').subscribe((chat) => {
      expect(chat).toEqual(mockChat);
    });
    const req = httpMock.expectOne('http://localhost:8081/api/friends/chat/GaryOak');
    expect(req.request.method).toBe('GET');
    req.flush(mockChat);
  });
});
