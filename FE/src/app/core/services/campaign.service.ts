import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CampaignProgressResponse, ChallengeResponse } from '../models/campaign.models';

/**
 * Servicio HTTP para la Campaña PvE.
 * Endpoints consumidos:
 *   GET  /api/campaign/progress
 *   POST /api/campaign/challenge/{nodeId}?deckId=
 *
 * El JWT se inyecta automáticamente via authInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class CampaignService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8081/api/campaign';

  /**
   * Obtiene el progreso de campaña del usuario autenticado.
   */
  getProgress(): Observable<CampaignProgressResponse> {
    return this.http.get<CampaignProgressResponse>(`${this.API_URL}/progress`);
  }

  /**
   * Inicia un desafío PvE contra el líder del nodo especificado.
   * @param nodeId ID del nodo de campaña (1-8)
   * @param deckId ID del mazo del jugador
   */
  challengeNode(nodeId: number, deckId: number): Observable<ChallengeResponse> {
    return this.http.post<ChallengeResponse>(
      `${this.API_URL}/challenge/${nodeId}?deckId=${deckId}`,
      {}
    );
  }
}
