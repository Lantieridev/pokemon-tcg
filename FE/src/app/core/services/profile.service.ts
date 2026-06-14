import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CardStatDTO {
  cardId: string;
  cardName: string;
  pokemonType: string;
  timesPlayed: number;
  damageDealt: number;
  damageReceived: number;
  kosMade: number;
  kosSuffered: number;
}

export interface EnergyStatDTO {
  energyType: string;
  count: number;
}

export interface AdvancedStatsDTO {
  pokemonStats: CardStatDTO[];
  energyStats: EnergyStatDTO[];
  totalDamageDealt: number;
  totalDamageReceived: number;
  totalKOsMade: number;
  totalKOsSuffered: number;
}

export interface UserProfileResponseDTO {
  username: string;
  createdAt: string;
  avatarIcon: string;
  description: string;
  activeTitle: string;
  selectedMedals?: string;
  level: number;
  xp: number;
  xpToNextLevel: number;
  mmr: number;
  pokecoins: number;
  battlePoints: number;
  packs: number;
  stardust: number;
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
    winStreak: number;
  };
  honors: Record<string, number>;
  unlockedTitles: string[];
  unlockedAvatars?: string[];
  showcase: {
    slotPosition: number;
    cardId: string;
    cardName: string;
  }[];
  showcasedDeck: {
    id: number;
    name: string;
  } | null;
  advancedStats: AdvancedStatsDTO | null;
}

export interface UserAchievementProgressDTO {
  title: string;
  category: string; // "NIVEL", "VICTORIAS", "PARTIDAS_JUGADAS", "COLECCION", "HONORES", "DEFECTO"
  unlocked: boolean;
  requirement: string;
  progress: number;
  target: number;
  rewardType?: 'MEDALLA' | 'FOTO_PERFIL' | 'TITULO';
  rewardValue?: string;
}

export interface MatchHistoryItemDTO {
  matchId: number;
  opponent: string;
  status: string;
  result: string;
  date: string;
  playerStatsJson?: string;
  opponentStatsJson?: string;
}

export interface SliceMatchHistoryResponseDTO {
  content: MatchHistoryItemDTO[];
  first: boolean;
  last: boolean;
  number: number;
  size: number;
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

  updateProfile(request: { avatarIcon: string; description: string; activeTitle: string; selectedMedals?: string; }): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/profile`, request);
  }

  updateShowcase(request: { slots: { slotPosition: number; cardId: string; }[] }): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/profile/showcase`, request);
  }

  updateShowcaseDeck(deckId: number | null): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/profile/showcase/deck`, { deckId });
  }

  getUserHistory(page = 0, size = 10): Observable<SliceMatchHistoryResponseDTO> {
    return this.http.get<SliceMatchHistoryResponseDTO>(`${this.API_URL}/me/history?page=${page}&size=${size}`);
  }
}
