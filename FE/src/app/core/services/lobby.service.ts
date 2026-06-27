import { Injectable, inject, signal, computed, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';
import { DeckSummaryDTO, DeckResponseDTO } from '../models/game-state.models';

// ── Tipos ─────────────────────────────────────────────────────────────────────

export type LobbyStatus = 'idle' | 'waiting' | 'found';

export interface LobbyResponse {
  status: 'WAITING' | 'MATCH_READY';
  matchId?: string;
  roomCode?: string;
  opponentId?: string;
}

// ── Servicio ──────────────────────────────────────────────────────────────────

/**
 * Servicio que gestiona el flujo de inicio de partida:
 *  - Partida pública: cola de matchmaking vía POST /api/lobby/queue
 *  - Partida privada: creación de sala y unión vía /api/lobby/room
 *
 * Escucha eventos MATCH_READY por WebSocket en /topic/lobby/{username}
 * y navega automáticamente al tablero cuando la partida está lista.
 */
@Injectable({ providedIn: 'root' })
export class LobbyService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private authService = inject(AuthService);

  private readonly BASE = 'http://localhost:8081/api/lobby';
  private lobbyStompClient: Client | null = null;

  // ── Signals de estado ──────────────────────────────────────────────────────

  readonly queueStatus = signal<LobbyStatus>('idle');
  readonly roomStatus = signal<LobbyStatus>('idle');
  readonly roomCode = signal<string | null>(null);
  readonly pendingMatchId = signal<string | null>(null);
  readonly opponentId = signal<string | null>(null);
  readonly lobbyError = signal<string | null>(null);

  readonly isSearching = computed(() => this.queueStatus() === 'waiting');
  readonly isWaitingInRoom = computed(() => this.roomStatus() === 'waiting');

  // ── Mazos del usuario ──────────────────────────────────────────────────────

  readonly decks = signal<DeckSummaryDTO[]>([]);
  readonly selectedDeckId = signal<number | null>(
    localStorage.getItem('selected_deck_id') ? Number(localStorage.getItem('selected_deck_id')) : null
  );
  readonly activeDeckDetails = signal<DeckResponseDTO | null>(null);

  constructor() {
    effect(() => {
      const deckId = this.selectedDeckId();
      if (deckId !== null) {
        localStorage.setItem('selected_deck_id', String(deckId));
        this.loadActiveDeckDetails(deckId);
      } else {
        localStorage.removeItem('selected_deck_id');
        this.activeDeckDetails.set(null);
      }
    });
  }

  async loadActiveDeckDetails(deckId: number): Promise<void> {
    try {
      const details = await firstValueFrom(
        this.http.get<DeckResponseDTO>(`http://localhost:8081/api/decks/${deckId}`)
      );
      this.activeDeckDetails.set(details);
    } catch (e) {
      console.warn('[LobbyService] No se pudieron cargar los detalles del mazo activo:', e);
    }
  }

  async loadDecks(): Promise<void> {
    try {
      const userId = this.authService.userId;
      if (!userId) {
        console.warn('[LobbyService] No authenticated user ID found.');
        return;
      }
      const decks = await firstValueFrom(
        this.http.get<DeckSummaryDTO[]>(`http://localhost:8081/api/decks/user/${userId}`)
      );
      this.decks.set(decks ?? []);
      
      const currentSelected = this.selectedDeckId();
      const stillExists = decks && decks.some(d => d.id === currentSelected);
      if (!stillExists) {
        if (decks && decks.length > 0) {
          this.selectedDeckId.set(decks[0].id);
        } else {
          this.selectedDeckId.set(null);
        }
      }
    } catch (e) {
      console.warn('[LobbyService] No se pudieron cargar los mazos:', e);
    }
  }

  // ── Conexión WebSocket de Lobby ────────────────────────────────────────────

  /**
   * Conecta el cliente STOMP al canal privado del lobby del jugador.
   * Se suscribe a /topic/lobby/{username} y reacciona al evento MATCH_READY.
   * Debe llamarse antes de joinQueue() o createRoom() para capturar la notificación.
   */
  connectLobbyWebSocket(): void {
    const token = this.authService.token;
    const username = this.authService.username;
    if (!token || !username) return;

    // Reusar conexión si ya está activa
    if (this.lobbyStompClient?.connected) return;

    this.lobbyStompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
    });

    this.lobbyStompClient.onConnect = () => {
      console.log('[LobbyWS] Conectado al canal de lobby');
      this.lobbyStompClient!.subscribe(
        `/topic/lobby/${username}`,
        (message) => {
          if (message.body) {
            try {
              const evt = JSON.parse(message.body) as LobbyResponse;
              if (evt.status === 'MATCH_READY' && evt.matchId) {
                this.onMatchReady(evt.matchId, evt.opponentId ?? '');
              }
            } catch (e) {
              console.error('[LobbyWS] Error parseando evento:', e);
            }
          }
        }
      );
    };

    this.lobbyStompClient.onStompError = (frame) => {
      console.error('[LobbyWS] STOMP error:', frame.headers['message']);
    };

    this.lobbyStompClient.activate();
  }

  disconnectLobbyWebSocket(): void {
    if (this.lobbyStompClient) {
      this.lobbyStompClient.deactivate();
      this.lobbyStompClient = null;
    }
  }

  // ── Partida Pública ────────────────────────────────────────────────────────

  async joinPublicQueue(isRanked: boolean = false): Promise<void> {
    const deckId = this.selectedDeckId();
    if (!deckId) {
      this.lobbyError.set('Seleccioná un mazo antes de buscar partida.');
      return;
    }
    this.lobbyError.set(null);
    this.queueStatus.set('waiting');

    try {
      const res = await firstValueFrom(
        this.http.post<LobbyResponse>(`${this.BASE}/queue`, { deckId, isRanked })
      );
      if (res.status === 'MATCH_READY' && res.matchId) {
        // Emparejado inmediatamente (había alguien en la cola)
        this.onMatchReady(res.matchId, res.opponentId ?? '');
      }
      // Si status=WAITING, el WS nos avisará cuando llegue el rival
    } catch (e: any) {
      this.queueStatus.set('idle');
      this.lobbyError.set('Error al unirse a la cola. Intentá de nuevo.');
      console.error('[LobbyService] joinQueue error:', e);
    }
  }

  async leavePublicQueue(): Promise<void> {
    this.queueStatus.set('idle');
    try {
      await firstValueFrom(this.http.delete(`${this.BASE}/queue`));
    } catch (e) {
      console.warn('[LobbyService] leaveQueue error (ignorado):', e);
    }
  }

  // ── Sala Privada ───────────────────────────────────────────────────────────

  async createPrivateRoom(): Promise<void> {
    const deckId = this.selectedDeckId();
    if (!deckId) {
      this.lobbyError.set('Seleccioná un mazo antes de crear la sala.');
      return;
    }
    this.lobbyError.set(null);
    this.roomStatus.set('waiting');
    this.roomCode.set(null);

    try {
      const res = await firstValueFrom(
        this.http.post<LobbyResponse>(`${this.BASE}/room`, { deckId })
      );
      if (res.roomCode) {
        this.roomCode.set(res.roomCode);
      }
      // El WS nos notificará cuando el rival use el código
    } catch (e: any) {
      this.roomStatus.set('idle');
      this.lobbyError.set('Error al crear la sala. Intentá de nuevo.');
      console.error('[LobbyService] createRoom error:', e);
    }
  }

  async joinPrivateRoom(code: string): Promise<void> {
    const deckId = this.selectedDeckId();
    if (!deckId) {
      this.lobbyError.set('Seleccioná un mazo antes de unirte.');
      return;
    }
    if (!code.trim()) {
      this.lobbyError.set('Ingresá el código de sala.');
      return;
    }
    this.lobbyError.set(null);

    try {
      const res = await firstValueFrom(
        this.http.post<LobbyResponse>(
          `${this.BASE}/room/${code.trim().toUpperCase()}/join`,
          { deckId }
        )
      );
      if (res.status === 'MATCH_READY' && res.matchId) {
        this.onMatchReady(res.matchId, res.opponentId ?? '');
      }
    } catch (e: any) {
      const msg = e?.error?.message ?? 'Código inválido o sala no encontrada.';
      this.lobbyError.set(msg);
      console.error('[LobbyService] joinRoom error:', e);
    }
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private onMatchReady(matchId: string, opponent: string): void {
    this.pendingMatchId.set(matchId);
    this.opponentId.set(opponent);
    this.queueStatus.set('found');
    this.roomStatus.set('found');
    this.disconnectLobbyWebSocket();
    this.router.navigate(['/battle', matchId]);
  }

  reset(): void {
    this.queueStatus.set('idle');
    this.roomStatus.set('idle');
    this.roomCode.set(null);
    this.pendingMatchId.set(null);
    this.opponentId.set(null);
    this.lobbyError.set(null);
    this.disconnectLobbyWebSocket();
  }
}
