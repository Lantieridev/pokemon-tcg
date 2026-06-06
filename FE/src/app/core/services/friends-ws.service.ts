import { Injectable, inject } from '@angular/core';
import { Subject } from 'rxjs';
import { ChatMessageDTO, ChallengeDTO } from '../models/friends.models';
import { AuthService } from './auth.service';
import { Client, Stomp } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';

@Injectable({
  providedIn: 'root'
})
export class FriendsWsService {
  private authService = inject(AuthService);
  private stompClient: any = null;
  
  public messages$ = new Subject<ChatMessageDTO>();
  public challenges$ = new Subject<ChallengeDTO>();

  connect(): void {
    if (this.stompClient && this.stompClient.connected) return;

    const token = this.authService.token;
    if (!token) return;

    // Use SockJS
    const socket = new SockJS('http://localhost:8081/ws');
    this.stompClient = Stomp.over(() => socket);
    
    this.stompClient.debug = () => {}; // Disable debug logs

    this.stompClient.connect({ 'Authorization': 'Bearer ' + token }, () => {
      this.subscribeToQueues();
    });
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
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.send('/app/chat.private', {}, JSON.stringify(message));
    }
  }

  sendChallenge(challenge: ChallengeDTO): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.send('/app/challenge.private', {}, JSON.stringify(challenge));
    }
  }

  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.disconnect();
      this.stompClient = null;
    }
  }
}
