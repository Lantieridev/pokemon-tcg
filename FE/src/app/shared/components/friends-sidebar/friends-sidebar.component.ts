import { Component, OnInit, inject, signal, Output, EventEmitter, input, ChangeDetectorRef } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { FriendsApiService } from '../../../core/services/friends-api.service';
import { FriendsWsService } from '../../../core/services/friends-ws.service';
import { FriendshipDTO } from '../../../core/models/friends.models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-friends-sidebar',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './friends-sidebar.component.html',
  styleUrls: ['./friends-sidebar.component.css']
})
export class FriendsSidebarComponent implements OnInit {
  isOpen = signal(false);
  activeTab = signal<'friends' | 'requests'>('friends');
  friends = signal<FriendshipDTO[]>([]);
  requests = signal<FriendshipDTO[]>([]);
  newFriendUsername = signal('');
  friendToDelete = signal<FriendshipDTO | null>(null);

  unreadMessages = input<Record<string, number>>({});

  @Output() onOpenChat = new EventEmitter<FriendshipDTO>();
  @Output() onOpenProfile = new EventEmitter<string>();
  @Output() onSidebarClose = new EventEmitter<void>();
  @Output() onRequestsUpdated = new EventEmitter<number>();

  private toastService = inject(ToastService);

  constructor(
    private friendsApi: FriendsApiService,
    private friendsWs: FriendsWsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadData();
    this.friendsWs.connect();
  }

  toggleSidebar() {
    this.isOpen.update(v => !v);
    if (this.isOpen()) {
      this.loadData();
    } else {
      this.onSidebarClose.emit();
    }
  }

  loadData() {
    this.friendsApi.getActiveFriends().subscribe(res => { this.friends.set(res); this.cdr.markForCheck(); });
    this.friendsApi.getPendingRequests().subscribe(res => { 
      this.requests.set(res); 
      this.onRequestsUpdated.emit(res.length);
      this.cdr.markForCheck(); 
    });
  }

  sendRequest() {
    const target = this.newFriendUsername().trim();
    if (!target) return;
    this.friendsApi.sendFriendRequest(target).subscribe({
      next: () => {
        this.toastService.success('Solicitud enviada correctamente');
        this.newFriendUsername.set('');
      },
      error: (err) => {
        const msg = err.error?.message || '';
        if (msg.includes('already exists')) {
          this.toastService.error('La solicitud a este usuario ya fue enviada');
        } else {
          this.toastService.error(msg || 'Error al enviar la solicitud');
        }
        this.newFriendUsername.set('');
      }
    });
  }

  acceptRequest(id: number) {
    this.friendsApi.acceptFriendRequest(id).subscribe({
      next: () => {
        this.toastService.success('Solicitud aceptada correctamente');
        this.loadData();
      },
      error: (err) => this.toastService.error(err.error.message || 'Error al aceptar solicitud')
    });
  }

  rejectRequest(id: number) {
    this.friendsApi.rejectFriendRequest(id).subscribe(() => this.loadData());
  }

  confirmRemoveFriend(friend: FriendshipDTO) {
    this.friendToDelete.set(friend);
  }

  cancelRemoveFriend() {
    this.friendToDelete.set(null);
  }

  removeFriend() {
    const friend = this.friendToDelete();
    if (!friend) return;
    this.friendsApi.removeFriend(friend.id).subscribe(() => {
      this.toastService.success('Amigo eliminado correctamente');
      this.friendToDelete.set(null);
      this.loadData();
    });
  }

  viewProfile(username: string) {
    this.onOpenProfile.emit(username);
  }

  openChat(friend: FriendshipDTO) {
    this.onOpenChat.emit(friend);
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
