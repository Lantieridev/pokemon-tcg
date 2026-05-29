import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { PokemonTcgCard } from '../models/game-state.models';

const CACHE_KEY = 'xy1_cards_cache';
const API_URL = 'https://api.pokemontcg.io/v2/cards?q=set.id:xy1&pageSize=250';

@Injectable({ providedIn: 'root' })
export class PokemonTcgService {
  private http = inject(HttpClient);

  /** Las 146 cartas del set XY1 */
  readonly cards = signal<PokemonTcgCard[]>([]);
  readonly isLoading = signal(false);
  readonly loadError = signal<string | null>(null);

  /**
   * Carga las cartas XY1 UNA SOLA VEZ.
   * Primero revisa localStorage (caché). Si no hay, hace GET a la API.
   * Llamar solo desde ngOnInit del DeckComponent.
   */
  loadCards(): void {
    // Si ya están cargadas en memoria, no hacer nada
    if (this.cards().length > 0) return;

    // Intentar desde caché
    const cached = localStorage.getItem(CACHE_KEY);
    if (cached) {
      try {
        const parsed: PokemonTcgCard[] = JSON.parse(cached);
        if (parsed && parsed.length > 0) {
          this.cards.set(parsed);
          return;
        }
      } catch {
        localStorage.removeItem(CACHE_KEY);
      }
    }

    // Fetch desde la API (solo si no hay caché)
    this.isLoading.set(true);
    this.loadError.set(null);

    this.http
      .get<{ data: PokemonTcgCard[] }>(API_URL)
      .pipe(
        tap((res) => {
          const data = res.data || [];
          localStorage.setItem(CACHE_KEY, JSON.stringify(data));
          this.cards.set(data);
          this.isLoading.set(false);
        })
      )
      .subscribe({
        error: (err) => {
          this.isLoading.set(false);
          this.loadError.set('No se pudo cargar el catálogo. Verificá tu conexión.');
          console.error('PokemonTcg API error:', err);
        },
      });
  }

  /** Limpia el caché forzando un re-fetch en la próxima llamada */
  clearCache(): void {
    localStorage.removeItem(CACHE_KEY);
    this.cards.set([]);
  }
}
