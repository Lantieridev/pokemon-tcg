import { Injectable, inject } from '@angular/core';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private authService = inject(AuthService);
  private stompClient: Client | null = null;
  private messageSubject = new Subject<any>();

  public connect(matchId: string): Observable<any> {
    const token = this.authService.token;
    const username = this.authService.username;

    if (!token || !username) {
      throw new Error('Must be authenticated to connect to WS');
    }

    // Disconnect previous connection if any
    if (this.stompClient) {
      this.stompClient.deactivate();
    }

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: (str) => {
        // console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.stompClient.onConnect = (frame) => {
      console.log('Connected to WS: ' + frame);
      const topic = `/topic/match/${matchId}/player/${username}`;
      this.stompClient!.subscribe(topic, (message) => {
        if (message.body) {
          const body = JSON.parse(message.body);
          this.messageSubject.next(body);
        }
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    this.stompClient.activate();

    return this.messageSubject.asObservable();
  }

  public sendAction(matchId: string, actionPayload: any) {
    const username = this.authService.username;
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: `/app/match/${matchId}/action`,
        headers: { playerId: username! },
        body: JSON.stringify(actionPayload)
      });
    }
  }

  public disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }
}
