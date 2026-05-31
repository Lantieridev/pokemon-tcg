import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserProfileResponseDTO {
  username: string;
  createdAt: string;
  avatarIcon: string;
  description: string;
  activeTitle: string;
  level: number;
  xp: number;
  xpToNextLevel: number;
  mmr: number;
  pokecoins: number;
  battlePoints: number;
  statistics: {
    matchesPlayed: number;
    matchesWon: number;
    matchesLost: number;
    winRate: number;
  };
  // other fields omitted for brevity
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8081/api/users';

  getProfile(username: string): Observable<UserProfileResponseDTO> {
    return this.http.get<UserProfileResponseDTO>(`${this.API_URL}/${username}/profile`);
  }
}
