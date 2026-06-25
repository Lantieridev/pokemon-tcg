import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { DeckStore } from '../../core/store/deck.store';
import { DeckRequestDTO, DeckResponseDTO, DeckSummaryDTO } from '../../core/models/game-state.models';

@Injectable({ providedIn: 'root' })
export class DeckApiService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private deckStore = inject(DeckStore);
  private readonly API_URL = 'http://localhost:8081/api/decks';

  /**
   * POST /api/decks
   * Guarda el mazo actual del DeckStore en el backend.
   * Body: { userId, name, cards: [{cardId, quantity}] }
   */
  saveDeck(deckName: string): Observable<DeckResponseDTO> {
    const userId = this.authService.userId;
    if (!userId) throw new Error('No hay usuario autenticado para guardar el mazo.');

    // Agrupar cartas por cardId con su cantidad
    const grouped = this.deckStore.deckGrouped();
    const cards: DeckRequestDTO['cards'] = grouped.map(({ card, count }) => ({
      cardId: card.id,
      quantity: count,
    }));

    const body: DeckRequestDTO = { userId, name: deckName, cards };
    return this.http.post<DeckResponseDTO>(this.API_URL, body);
  }

  /**
   * GET /api/decks/user/{userId}
   * Devuelve los mazos armados por el usuario.
   */
  getDecksByUserId(userId: number): Observable<DeckSummaryDTO[]> {
    return this.http.get<DeckSummaryDTO[]>(`${this.API_URL}/user/${userId}`);
  }

  /**
   * GET /api/decks/{id}
   * Devuelve un mazo completo por su ID.
   */
  getDeckById(id: number): Observable<DeckResponseDTO> {
    return this.http.get<DeckResponseDTO>(`${this.API_URL}/${id}`);
  }

  /**
   * GET /api/decks/templates
   * Devuelve las plantillas de mazos predefinidos.
   */
  getTemplates(): Observable<DeckSummaryDTO[]> {
    return this.http.get<DeckSummaryDTO[]>(`${this.API_URL}/templates`);
  }

  /**
   * POST /api/decks/users/{userId}/clone/{templateId}
   * Clona una plantilla para un usuario.
   */
  cloneTemplate(userId: number, templateId: number): Observable<DeckResponseDTO> {
    return this.http.post<DeckResponseDTO>(`${this.API_URL}/users/${userId}/clone/${templateId}`, {});
  }
}

