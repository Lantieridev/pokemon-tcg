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
    perfectWins: number;
    comebackWins: number;
    totalKos: number;
    trainerCardsPlayed: number;
    totalDamageDealt: number;
  };
  honors: Record<string, number>;
  unlockedTitles: string[];
  showcase: {
    slotPosition: number;
    cardId: string;
    cardName: string;
  }[];
  showcasedDeck: {
    id: number;
    name: string;
  } | null;
}

export interface UserAchievementProgressDTO {
  title: string;
  category: string; // "NIVEL", "VICTORIAS", "PARTIDAS_JUGADAS", "COLECCION", "HONORES", "DEFECTO"
  unlocked: boolean;
  requirement: string;
  progress: number;
  target: number;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8081/api/users';

  getProfile(username: string): Observable<UserProfileResponseDTO> {
    return this.http.get<UserProfileResponseDTO>(`${this.API_URL}/${username}/profile`);
  }

  getAchievements(username: string): Observable<UserAchievementProgressDTO[]> {
    return this.http.get<UserAchievementProgressDTO[]>(`${this.API_URL}/${username}/profile/achievements`);
  }

  updateProfile(request: { avatarIcon: string; description: string; activeTitle: string; }): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/profile`, request);
  }

  updateShowcase(request: { slots: { slotPosition: number; cardId: string; }[] }): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/profile/showcase`, request);
  }

  updateShowcaseDeck(deckId: number | null): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/profile/showcase/deck`, { deckId });
  }
}

