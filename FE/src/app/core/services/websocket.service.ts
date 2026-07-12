import { Injectable, inject, NgZone } from '@angular/core';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { MatchStore } from '../store/match.store';
import { GameStateResponseDTO, ActionRequestDTO } from '../models/game-state.models';
import { ToastService } from './toast.service';

/**
 * Servicio STOMP para la comunicación en tiempo real del tablero de juego.
 *
 * Protocolo (WebSocketConfig.java):
 *  - Endpoint: /ws (SockJS)
 *  - Suscripción: /topic/match/{matchId}/player/{playerId}
 *  - Publicación: /app/match/{matchId}/action con header "playerId"
 *  - Auth: STOMP CONNECT header "Authorization: Bearer {jwt}"
 */
@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private authService = inject(AuthService);
  private matchStore = inject(MatchStore);
  private ngZone = inject(NgZone);
  private toastService = inject(ToastService);

  private stompClient: Client | null = null;
  private messageSubject = new Subject<GameStateResponseDTO>();
  private chatSubject = new Subject<any>();
  private errorSubject = new Subject<string>();
  private currentMatchId: string | null = null;

  /** Observable de actualizaciones del GameState */
  readonly gameState$ = this.messageSubject.asObservable();
  readonly chatMessage$ = this.chatSubject.asObservable();
  readonly error$ = this.errorSubject.asObservable();

  /**
   * Conecta al WebSocket y se suscribe al canal del jugador.
   * Automáticamente actualiza el MatchStore con cada estado recibido.
   *
   * @param matchId - ID de la partida
   * @returns Observable<GameStateResponseDTO> para usos adicionales
   */
  connect(matchId: string): Observable<GameStateResponseDTO> {
    const token = this.authService.token;
    const username = this.authService.username;

    if (!token || !username) {
      throw new Error('Debes estar autenticado para conectarte al tablero.');
    }

    // Desconectar sesión previa si existe
    if (this.stompClient) {
      this.stompClient.deactivate();
    }

    this.currentMatchId = matchId;

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(`${environment.wsUrl}`),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: () => {
        // Descomentar para debug: console.log(str)
      },
    });

    this.stompClient.onConnect = () => {
      console.log(`[WS] Conectado a partida ${matchId} como ${username}`);

      // Suscripción al canal privado del jugador (Niebla de guerra)
      const topic = `/topic/match/${matchId}/player/${username}`;
      this.stompClient!.subscribe(topic, (message) => {
        if (message.body) {
          try {
            const body = JSON.parse(message.body) as GameStateResponseDTO;
            this.ngZone.run(() => {
              // Actualizar el store automáticamente
              this.matchStore.updateState(body);
              this.messageSubject.next(body);
            });
          } catch (err) {
            console.error('[WS] Error parseando GameState:', err);
          }
        }
      });
      
      this.stompClient!.subscribe(`/topic/chat/${matchId}`, (message) => {
        if (message.body) {
          try {
            this.ngZone.run(() => {
              this.chatSubject.next(JSON.parse(message.body));
            });
          } catch (err) {
            console.error('[WS] Error parseando ChatMessage:', err);
          }
        }
      });
      
      this.stompClient!.subscribe(`/topic/match/${matchId}/player/${username}/errors`, (message) => {
        if (message.body) {
          try {
            const body = JSON.parse(message.body);
            this.ngZone.run(() => {
              this.errorSubject.next(body.error);
            });
          } catch (err) {
            console.error('[WS] Error parseando ErrorMessage:', err);
          }
        }
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('[WS] Error STOMP:', frame.headers['message'], frame.body);
      const rawMessage = frame.headers['message'] || 'Error en la partida';
      const friendlyMessage = this.mapFriendlyErrorMessage(rawMessage);
      this.toastService.error(friendlyMessage);
    };

    this.stompClient.onDisconnect = () => {
      console.log('[WS] Desconectado del tablero');
    };

    this.stompClient.activate();

    return this.gameState$;
  }

  /**
   * Envía una acción del jugador al backend.
   *
   * IMPORTANTE: El backend valida que el header "playerId" coincida
   * con el principal autenticado (GameWebSocketController.java).
   *
   * @param matchId - ID de la partida
   * @param action - ActionRequestDTO tipado
   */
  sendAction(matchId: string, action: ActionRequestDTO): void {
    const username = this.authService.username;

    if (!this.stompClient?.connected) {
      console.warn('[WS] Intentando enviar acción sin conexión activa');
      return;
    }

    if (!username) {
      console.error('[WS] Sin usuario autenticado');
      return;
    }

    this.stompClient.publish({
      destination: `/app/match/${matchId}/action`,
      headers: { playerId: username },
      body: JSON.stringify(action),
    });
  }

  sendChatMessage(matchId: string, message: string): void {
    const username = this.authService.username;
    if (this.stompClient?.connected && username) {
      this.stompClient.publish({
        destination: `/app/chat/${matchId}`,
        body: JSON.stringify({ sender: username, message })
      });
    }
  }

  /** Desconecta el cliente STOMP y resetea el store */
  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
    this.currentMatchId = null;
    this.matchStore.reset();
  }

  get isConnected(): boolean {
    return this.stompClient?.connected ?? false;
  }

  get matchId(): string | null {
    return this.currentMatchId;
  }

  private mapFriendlyErrorMessage(raw: string): string {
    const lower = raw.toLowerCase();
    if (lower.includes('retreat_blocked_by_poison_barrier')) {
      return 'No puedes retirar a este Pokémon porque está envenenado y el oponente posee a Dragalge (Poison Barrier).';
    }
    if (lower.includes('not_your_turn')) {
      return 'No es tu turno.';
    }
    if (lower.includes('ability_already_used_this_turn')) {
      return 'Ya has usado esta habilidad este turno.';
    }
    if (lower.includes('no_fairy_energy_attached')) {
      return 'Debes tener al menos una energía Hada (Fairy) asignada para usar esta habilidad.';
    }
    if (lower.includes('no_damage_to_heal')) {
      return 'El Pokémon no tiene daño para curar.';
    }
    return raw;
  }
}
