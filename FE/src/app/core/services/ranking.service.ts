import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RankingDto {
  username: string;
  mmr: number;
  tier: string;
  rankedMatchesPlayed: number;
}

export interface SliceRankingDto {
  content: RankingDto[];
  last: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class RankingService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/rankings`;

  getTopGlobal(page: number = 0, size: number = 50): Observable<SliceRankingDto> {
    return this.http.get<SliceRankingDto>(`${this.API_URL}?page=${page}&size=${size}`);
  }
}
