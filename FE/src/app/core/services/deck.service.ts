import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

export interface DeckCardRequest {
  cardId: string;
  quantity: number;
}

export interface DeckRequest {
  userId: number;
  name: string;
  cards: DeckCardRequest[];
}

@Injectable({
  providedIn: 'root'
})
export class DeckService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private readonly API_URL = `${environment.apiUrl}/decks`;

  /**
   * Guarda un mazo en la base de datos haciendo un POST a /api/decks.
   * Agrega el token JWT del AuthService en las cabeceras.
   */
  saveDeck(name: string, cards: DeckCardRequest[], userId: number = 1): Observable<any> {
    const token = this.authService.token;
    let headers = new HttpHeaders();
    
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    const payload: DeckRequest = {
      userId,
      name,
      cards
    };

    return this.http.post(this.API_URL, payload, { headers });
  }

  /**
   * Obtiene todos los mazos
   */
  getAllDecks(): Observable<any> {
    const token = this.authService.token;
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.get(this.API_URL, { headers });
  }
}
