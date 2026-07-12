import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FriendshipDTO, PublicProfileDTO, ChatMessageDTO } from '../models/friends.models';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FriendsApiService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}`;

  sendFriendRequest(targetUsername: string): Observable<any> {
    return this.http.post(`${this.API_URL}/friends/request`, { targetUsername });
  }

  getActiveFriends(): Observable<FriendshipDTO[]> {
    return this.http.get<FriendshipDTO[]>(`${this.API_URL}/friends/list`);
  }

  getPendingRequests(): Observable<FriendshipDTO[]> {
    return this.http.get<FriendshipDTO[]>(`${this.API_URL}/friends/requests`);
  }

  acceptFriendRequest(id: number): Observable<any> {
    return this.http.put(`${this.API_URL}/friends/accept/${id}`, {});
  }

  rejectFriendRequest(id: number): Observable<any> {
    return this.http.put(`${this.API_URL}/friends/reject/${id}`, {});
  }

  removeFriend(id: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/friends/remove/${id}`);
  }

  getPublicProfile(username: string): Observable<PublicProfileDTO> {
    return this.http.get<PublicProfileDTO>(`${this.API_URL}/users/${username}/profile/public`);
  }

  getChatHistory(username: string): Observable<ChatMessageDTO[]> {
    return this.http.get<ChatMessageDTO[]>(`${this.API_URL}/friends/chat/${username}`);
  }
}
