import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { DeckStore } from '../../core/store/deck.store';
import { DeckRequestDTO, DeckResponseDTO, DeckCardRequestDTO } from '../../core/models/game-state.models';

@Injectable({ providedIn: 'root' })
export class DeckApiService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private deckStore = inject(DeckStore);
  private readonly API_URL = 'http://localhost:8081/api/decks';

  /**
   * POST /api/decks
   */
  saveDeck(deckName: string, status: 'VALID' | 'DRAFT'): Observable<DeckResponseDTO> {
    const userId = this.authService.userId;
    if (!userId) throw new Error('No hay usuario autenticado para guardar el mazo.');

    const grouped = this.deckStore.deckGrouped();
    const cards: DeckRequestDTO['cards'] = grouped.map(({ card, count }) => ({
      cardId: card.id,
      quantity: count,
    }));

    const body: DeckRequestDTO = { userId, name: deckName, status, cards };
    return this.http.post<DeckResponseDTO>(this.API_URL, body);
  }

  /**
   * PUT /api/decks/{id}
   */
  updateDeck(id: number, deckName: string, status: 'VALID' | 'DRAFT'): Observable<DeckResponseDTO> {
    const userId = this.authService.userId;
    if (!userId) throw new Error('No hay usuario autenticado para guardar el mazo.');

    const grouped = this.deckStore.deckGrouped();
    const cards: DeckRequestDTO['cards'] = grouped.map(({ card, count }) => ({
      cardId: card.id,
      quantity: count,
    }));

    const body: DeckRequestDTO = { userId, name: deckName, status, cards };
    return this.http.put<DeckResponseDTO>(`${this.API_URL}/${id}`, body);
  }

  /**
   * DELETE /api/decks/{id}
   */
  deleteDeck(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  /**
   * GET /api/decks/user/{userId}
   */
  getDecksByUserId(userId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/user/${userId}`);
  }

  /**
   * POST /api/decks/assistant/autocomplete
   * Solicita al asistente autocompletar el mazo actual hasta 60 cartas.
   */
  autocompleteDeck(): Observable<{ cardId: string, quantity: number }[]> {
    const userId = this.authService.userId;
    if (!userId) throw new Error('No hay usuario autenticado.');

    const grouped = this.deckStore.deckGrouped();
    const cards = grouped.map(({ card, count }) => ({
      cardId: card.id,
      quantity: count,
    }));

    return this.http.post<{ cardId: string, quantity: number }[]>(`${this.API_URL}/assistant/autocomplete`, cards);
  }

  /**
   * GET /api/decks/{id}
   */
  getDeckById(id: number): Observable<DeckResponseDTO> {
    return this.http.get<DeckResponseDTO>(`${this.API_URL}/${id}`);
  }

  cloneTemplate(userId: number, templateId: number): Observable<DeckResponseDTO> {
    return this.http.post<DeckResponseDTO>(`${this.API_URL}/users/${userId}/clone/${templateId}`, {});
  }

  generateWizardDeck(theme: string): Observable<DeckCardRequestDTO[]> {
    return this.http.post<DeckCardRequestDTO[]>(`${this.API_URL}/assistant/wizard`, { theme });
  }
}
