import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class MatchBackendService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private readonly API_URL = 'http://localhost:8081/api/matches';

  private getHeaders(): HttpHeaders {
    const token = this.authService.token;
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  public createMatch(playerAId: string, playerBId: string, deckAId: number, deckBId: number): Observable<{ matchId: string }> {
    const body = { playerAId, playerBId, deckAId, deckBId };
    return this.http.post<{ matchId: string }>(this.API_URL, body, { headers: this.getHeaders() });
  }

  public getMatchState(matchId: string): Observable<any> {
    const username = this.authService.username;
    let headers = this.getHeaders();
    if (username) {
      headers = headers.append('X-Player-Id', username);
    }
    return this.http.get<any>(`${this.API_URL}/${matchId}/state`, { headers });
  }
}
