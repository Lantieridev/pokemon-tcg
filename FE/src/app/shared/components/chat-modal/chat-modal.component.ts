import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FriendshipDTO, ChatMessageDTO } from '../../../core/models/friends.models';
import { FriendsApiService } from '../../../core/services/friends-api.service';
import { FriendsWsService } from '../../../core/services/friends-ws.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-modal.component.html',
  styleUrls: ['./chat-modal.component.css']
})
export class ChatModalComponent implements OnInit, OnDestroy {
  @Input() friend!: FriendshipDTO;
  @Output() close = new EventEmitter<void>();

  messages: ChatMessageDTO[] = [];
  newMessage = '';
  private sub?: Subscription;

  constructor(
    private friendsApi: FriendsApiService,
    private friendsWs: FriendsWsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.friendsApi.getChatHistory(this.friend.friendUsername).subscribe(history => {
      this.messages = history;
      this.cdr.detectChanges();
      this.scrollToBottom();
    });

    this.sub = this.friendsWs.messages$.subscribe(msg => {
      if (msg.senderUsername === this.friend.friendUsername || msg.receiverUsername === this.friend.friendUsername) {
        this.messages = [...this.messages, msg];
        this.cdr.detectChanges();
        this.scrollToBottom();
      }
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  sendMessage() {
    if (!this.newMessage.trim()) return;
    
    this.friendsWs.sendChatMessage({
      senderUsername: '',
      receiverUsername: this.friend.friendUsername,
      content: this.newMessage
    });
    this.newMessage = '';
  }

  sendChallenge() {
    const lobbyId = 'lobby_' + Date.now();
    this.friendsWs.sendChallenge({
      senderUsername: '',
      receiverUsername: this.friend.friendUsername,
      lobbyId
    });
  }

  onClose() {
    this.close.emit();
  }

  private scrollToBottom() {
    setTimeout(() => {
      const container = document.querySelector('.chat-messages');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }, 100);
  }

  isCustomAvatar(av: string | undefined): boolean {
    return !!av && av.startsWith('avatar_');
  }

  getAvatarUrl(av: string | undefined): string {
    if (!av) return '';
    return `assets/achievements/avatars/${av}.png`;
  }

  getAvatarEmoji(icon: string | undefined): string {
    if (!icon) return '👤';
    switch (icon.toLowerCase()) {
      case 'ash': return '🧢';
      case 'misty': return '💧';
      case 'brock': return '🪨';
      case 'gary': return '👑';
      case 'serena': return '🎀';
      case 'red': return '⚡';
      default: return '👤';
    }
  }
}
