import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class PokemonTcgService {
  private http = inject(HttpClient);
  private readonly CACHE_KEY = 'pokemon_cards_xy1';
  private readonly API_URL = 'https://api.pokemontcg.io/v2/cards?q=set.id:xy1&pageSize=250&page=1';

  /**
   * Obtiene las cartas de la expansión XY1.
   * Si ya están en localStorage, las retorna desde ahí para evitar llamados excesivos a la API.
   */
  getCards(): Observable<any[]> {
    const cached = localStorage.getItem(this.CACHE_KEY);
    if (cached) {
      return of(JSON.parse(cached));
    }

    return this.http.get<{ data: any[] }>(this.API_URL).pipe(
      map(response => response.data),
      tap(cards => {
        // Guardamos las cartas en localStorage como caché
        localStorage.setItem(this.CACHE_KEY, JSON.stringify(cards));
      })
    );
  }
}
