import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { AuthService } from './auth.service';
import { GameStateResponseDTO, DeckResponseDTO, DeckSummaryDTO } from '../models/game-state.models';

@Injectable({ providedIn: 'root' })
export class MatchBackendService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private readonly MATCHES_URL = 'http://localhost:8081/api/matches';
  private readonly DECKS_URL = 'http://localhost:8081/api/decks';

  /**
   * GET /api/matches/{matchId}/state
   * Requiere header X-Player-Id (además del JWT que inyecta el interceptor).
   * El backend verifica que principal.getName() === playerId (seguridad).
   */
  getMatchState(matchId: string): Observable<GameStateResponseDTO> {
    if (matchId === 'dev-match-001') {
      return of({
        matchId: 'dev-match-001',
        version: 1,
        turnNumber: 1,
        activePlayerIndex: 0,
        currentPhase: 'MAIN',
        pendingSelectionRequest: null,
        self: {
          playerId: this.authService.username ?? 'Tú',
          active: {
            cardId: 'xy1-11',
            name: 'Charizard EX',
            pokemonType: 'FIRE',
            maxHp: 180,
            damageCounters: 3,
            isEx: true,
            weaknessType: 'WATER',
            resistanceType: null,
            attachedEnergies: ['FIRE', 'FIRE', 'COLORLESS'],
            retreatCost: 2,
            hasToolAttached: false,
            attacks: [
              { name: 'Combustion', baseDamage: 60, energyCost: ['FIRE', 'COLORLESS', 'COLORLESS'] },
            ],
            statusConditions: []
          },
          bench: [
            { cardId: 'xy1-4', name: 'Charmander', pokemonType: 'FIRE', maxHp: 60, damageCounters: 0, isEx: false, weaknessType: 'WATER', resistanceType: null, attachedEnergies: [], retreatCost: 1, hasToolAttached: false, attacks: [], statusConditions: [] }
          ],
          hand: ['xy1-1', 'xy1-2', 'xy1-3'],
          deckSize: 45,
          prizeCount: 6
        },
        opponent: {
          playerId: 'Bot Ash',
          active: {
            cardId: 'xy9-40',
            name: 'Greninja EX',
            pokemonType: 'WATER',
            maxHp: 170,
            damageCounters: 0,
            isEx: true,
            weaknessType: 'GRASS',
            resistanceType: null,
            attachedEnergies: ['WATER'],
            retreatCost: 1,
            hasToolAttached: false,
            attacks: [],
            statusConditions: []
          },
          bench: [],
          handSize: 5,
          deckSize: 45,
          prizeCount: 6
        }
      });
    }

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
   * GET /api/decks
   * Lista todos los mazos del usuario.
   */
  getDecks(): Observable<DeckSummaryDTO[]> {
    return this.http.get<DeckSummaryDTO[]>(this.DECKS_URL);
  }

  /**
   * GET /api/decks/{id}
   */
  getDeck(id: number): Observable<DeckResponseDTO> {
    return this.http.get<DeckResponseDTO>(`${this.DECKS_URL}/${id}`);
  }
}
