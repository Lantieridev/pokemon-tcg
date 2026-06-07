import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FriendsApiService } from '../../../core/services/friends-api.service';
import { FriendsWsService } from '../../../core/services/friends-ws.service';
import { FriendshipDTO } from '../../../core/models/friends.models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-friends-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './friends-sidebar.component.html',
  styleUrls: ['./friends-sidebar.component.css']
})
export class FriendsSidebarComponent implements OnInit {
  isOpen = signal(false);
  activeTab: 'friends' | 'requests' = 'friends';
  friends: FriendshipDTO[] = [];
  requests: FriendshipDTO[] = [];
  newFriendUsername = '';

  @Output() onOpenChat = new EventEmitter<FriendshipDTO>();
  @Output() onOpenProfile = new EventEmitter<string>();

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
    }
  }

  loadData() {
    this.friendsApi.getActiveFriends().subscribe(res => { this.friends = res; this.cdr.markForCheck(); });
    this.friendsApi.getPendingRequests().subscribe(res => { this.requests = res; this.cdr.markForCheck(); });
  }

  sendRequest() {
    if (!this.newFriendUsername.trim()) return;
    this.friendsApi.sendFriendRequest(this.newFriendUsername).subscribe({
      next: () => {
        this.toastService.success('Solicitud enviada correctamente');
        this.newFriendUsername = '';
      },
      error: (err) => {
        const msg = err.error?.message || '';
        if (msg.includes('already exists')) {
          this.toastService.error('La solicitud a este usuario ya fue enviada');
        } else {
          this.toastService.error(msg || 'Error al enviar la solicitud');
        }
        this.newFriendUsername = '';
      }
    });
  }

  friendToDelete: FriendshipDTO | null = null;

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
    this.friendToDelete = friend;
  }

  cancelRemoveFriend() {
    this.friendToDelete = null;
  }

  removeFriend() {
    if (!this.friendToDelete) return;
    this.friendsApi.removeFriend(this.friendToDelete.id).subscribe(() => {
      this.toastService.success('Amigo eliminado correctamente');
      this.friendToDelete = null;
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
