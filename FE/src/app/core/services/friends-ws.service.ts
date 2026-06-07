import { Injectable, inject } from '@angular/core';
import { Subject } from 'rxjs';
import { ChatMessageDTO, ChallengeDTO } from '../models/friends.models';
import { AuthService } from './auth.service';
import { Client, Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

@Injectable({
  providedIn: 'root'
})
export class FriendsWsService {
  private authService = inject(AuthService);
  private stompClient: any = null;
  
  public messages$ = new Subject<ChatMessageDTO>();
  public challenges$ = new Subject<ChallengeDTO>();

  connect(): void {
    if (this.stompClient && this.stompClient.active) return;

    const token = this.authService.token;
    if (!token) return;

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
      connectHeaders: {
        Authorization: 'Bearer ' + token
      },
      debug: function (str) {
        // console.log(str);
      },
      onConnect: () => {
        this.subscribeToQueues();
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      }
    });

    this.stompClient.activate();
  }

  private subscribeToQueues(): void {
    if (!this.stompClient) return;
    
    // Obtenemos el username desde el token decodificado o del authService si tiene un getter.
    // Asumimos que authService tiene username (muy común) o extraeremos del payload si no.
    const username = (this.authService as any).username || localStorage.getItem('username');

    if (!username) return;

    this.stompClient.subscribe(`/user/${username}/queue/messages`, (msg: any) => {
      if (msg.body) {
        this.messages$.next(JSON.parse(msg.body));
      }
    });

    this.stompClient.subscribe(`/user/${username}/queue/challenges`, (msg: any) => {
      if (msg.body) {
        this.challenges$.next(JSON.parse(msg.body));
      }
    });
  }

  sendChatMessage(message: ChatMessageDTO): void {
    if (this.stompClient && this.stompClient.active) {
      this.stompClient.publish({ destination: '/app/chat.private', body: JSON.stringify(message) });
    }
  }

  sendChallenge(challenge: ChallengeDTO): void {
    if (this.stompClient && this.stompClient.active) {
      this.stompClient.publish({ destination: '/app/challenge.private', body: JSON.stringify(challenge) });
    }
  }

  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }
}
