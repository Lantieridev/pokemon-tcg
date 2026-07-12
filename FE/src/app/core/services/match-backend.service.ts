import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { GameStateResponseDTO, DeckResponseDTO, DeckSummaryDTO } from '../models/game-state.models';

@Injectable({ providedIn: 'root' })
export class MatchBackendService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private readonly MATCHES_URL = `${environment.apiUrl}/matches`;
  private readonly DECKS_URL = `${environment.apiUrl}/decks`;

  /**
   * GET /api/matches/{matchId}/state
   * Requiere header X-Player-Id (además del JWT que inyecta el interceptor).
   * El backend verifica que principal.getName() === playerId (seguridad).
   */
  getMatchState(matchId: string): Observable<GameStateResponseDTO> {
    const username = this.authService.username ?? '';
    return this.http.get<GameStateResponseDTO>(
      `${this.MATCHES_URL}/${matchId}/state`,
      { headers: new HttpHeaders({ 'X-Player-Id': username }) }
    );
  }

  getChatHistory(matchId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.MATCHES_URL}/${matchId}/chat`);
  }

  /**
   * POST /api/matches
   * Crea una nueva partida entre dos jugadores.
   */
  createMatch(
    playerAId: string,
    playerBId: string,
    deckAId: number,
    deckBId: number
  ): Observable<{ matchId: string }> {
    return this.http.post<{ matchId: string }>(this.MATCHES_URL, {
      playerAId,
      playerBId,
      deckAId,
      deckBId,
    });
  }

  /**
   * GET /api/decks/mine
   * Lista todos los mazos del usuario autenticado (resuelto por el backend desde el JWT).
   */
  getDecks(): Observable<DeckSummaryDTO[]> {
    if (!this.authService.userId) {
      return of([]);
    }
    return this.http.get<DeckSummaryDTO[]>(`${this.DECKS_URL}/mine`);
  }

  /**
   * GET /api/decks/templates
   * Lista los mazos por defecto del sistema
   */
  getTemplates(): Observable<DeckSummaryDTO[]> {
    return this.http.get<DeckSummaryDTO[]>(`${this.DECKS_URL}/templates`);
  }

  /**
   * GET /api/decks/{id}
   */
  getDeck(id: number): Observable<DeckResponseDTO> {
    return this.http.get<DeckResponseDTO>(`${this.DECKS_URL}/${id}`);
  }

  /**
   * POST /api/matches/bot
   * Crea una nueva partida contra el Bot.
   */
  createBotMatch(
    playerId: string,
    deckId: number
  ): Observable<{ matchId: string }> {
    return this.http.post<{ matchId: string }>(`${this.MATCHES_URL}/bot`, {
      playerAId: playerId,
      deckAId: deckId,
      playerBId: 'Bot-001',
      deckBId: deckId // El backend usará deckAId internamente para el bot
    });
  }

  /**
   * POST /api/matches/{matchId}/surrender
   * Abandona explícitamente la partida.
   */
  surrenderMatch(matchId: string): Observable<void> {
    const username = this.authService.username ?? '';
    return this.http.post<void>(
      `${this.MATCHES_URL}/${matchId}/surrender`,
      {},
      { headers: new HttpHeaders({ 'X-Player-Id': username }) }
    );
  }
}
