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
    if (!av) return false;
    const emojis = ['ash', 'misty', 'brock', 'gary', 'serena', 'red', 'default_trainer'];
    return !emojis.includes(av);
  }

  getAvatarUrl(av: string | undefined): string {
    if (!av) return '';
    
    if (av === 'Bulbasaur Clásico' || av === 'bulbasaur_classic') return 'assets/store/avatar_bulbasaur.png';
    if (av === 'Charmander Fuego' || av === 'charmander_fire') return 'assets/store/avatar_charmander.png';
    if (av === 'Squirtle Agua' || av === 'squirtle_water') return 'assets/store/avatar_squirtle.png';
    if (av === 'Ash Ketchum' || av === 'ash_avatar') return 'assets/store/avatar_ash.png';
    if (av === 'Misty' || av === 'misty_avatar') return 'assets/store/avatar_misty.png';
    if (av === 'Brock' || av === 'brock_avatar') return 'assets/store/avatar_brock.png';
    if (av === 'Charizard 3D Premium' || av === 'charizard_3d') return 'assets/store/avatar_charizard_3d.png';
    if (av === 'Mewtwo Legendario' || av === 'mewtwo_3d') return 'assets/store/avatar_mewtwo_3d.png';
    if (av === 'Pikachu Chibi' || av === 'pikachu_cute') return 'assets/store/avatar_pikachu_cute.png';
    if (av === 'collector_legend') return 'assets/store/avatar_collector.png';

    const normalizedValue = av.toLowerCase()
      .normalize("NFD").replace(/[\u0300-\u036f]/g, "")
      .replace(/\s+/g, '_')
      .replace(/[^a-z0-9_]/g, '');
      
    const prefix = normalizedValue.startsWith('avatar_') ? '' : 'avatar_';
    return `assets/achievements/avatars/${prefix}${normalizedValue}.png`;
  }

  getAvatarEmoji(icon: string | undefined): string {
    if (!icon) return '🎒';
    switch (icon.toLowerCase()) {
      case 'ash': return '🧢';
      case 'misty': return '💧';
      case 'brock': return '🪨';
      case 'gary': return '👑';
      case 'serena': return '🎀';
      case 'red': return '⚡';
      default: return '🎒';
    }
  }
}
