import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface BattlePassLevelDTO {
  level: number;
  requiredXp: number;
  freeRewardType: string;
  freeRewardAmount: number;
  freeRewardValue: string;
  premiumRewardType: string;
  premiumRewardAmount: number;
  premiumRewardValue: string;
}

export interface BattlePassStatusDTO {
  isPremium: boolean;
  currentXp: number;
  currentLevel: number;
  claimedFreeLevel: number;
  claimedPremiumLevel: number;
  levels: BattlePassLevelDTO[];
}

@Injectable({ providedIn: 'root' })
export class BattlePassService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/battle-pass`;

  getStatus(): Observable<BattlePassStatusDTO> {
    return this.http.get<BattlePassStatusDTO>(`${this.API_URL}/status`);
  }

  claimReward(level: number, isPremium: boolean): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/claim?level=${level}&isPremium=${isPremium}`, {});
  }

  purchasePremium(): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/purchase-premium`, {});
  }
}
