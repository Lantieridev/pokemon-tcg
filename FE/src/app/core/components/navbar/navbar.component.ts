import { ChangeDetectionStrategy, Component, inject, OnInit, OnDestroy, signal, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter, map, takeUntil } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProfileService, UserProfileResponseDTO } from '../../services/profile.service';
import { LogoComponent, TrainerChipComponent, IconComponent, BallIconComponent, CoinIconComponent } from '../../../features/lobby-aurora/ui/aurora-ui.components';
import { FriendsSidebarComponent } from '../../../shared/components/friends-sidebar/friends-sidebar.component';
import { PublicProfileModalComponent } from '../../../shared/components/public-profile-modal/public-profile-modal.component';
import { ChatModalComponent } from '../../../shared/components/chat-modal/chat-modal.component';
import { PublicProfileDTO, FriendshipDTO } from '../../models/friends.models';
import { FriendsApiService } from '../../services/friends-api.service';
import { FriendsWsService } from '../../services/friends-ws.service';
import { ToastService } from '../../services/toast.service';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, LogoComponent, TrainerChipComponent, IconComponent, BallIconComponent, CoinIconComponent, FriendsSidebarComponent, PublicProfileModalComponent, ChatModalComponent],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NavbarComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private authService = inject(AuthService);
  private profileService = inject(ProfileService);
  private friendsApi = inject(FriendsApiService);
  private friendsWs = inject(FriendsWsService);
  private toastService = inject(ToastService);
  private cdr = inject(ChangeDetectorRef);
  private destroy$ = new Subject<void>();

  pendingRequestsCount = signal<number>(0);
  unreadMessagesCount = signal<number>(0);
  unreadMessagesPerUser = signal<Record<string, number>>({});
  customNotification = signal<{ sender: string, message: string } | null>(null);
  private notificationTimeout: any;

  profileData = signal<UserProfileResponseDTO | null>(null);

  selectedProfile = signal<PublicProfileDTO | null>(null);
  isProfileModalOpen = signal(false);

  selectedChatFriend = signal<FriendshipDTO | null>(null);

  currentPath = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((e: NavigationEnd) => {
        return e.urlAfterRedirects;
      })
    ),
    { initialValue: this.router.url }
  );

  @ViewChild(FriendsSidebarComponent) friendsSidebar!: FriendsSidebarComponent;

  toggleFriendsSidebar() {
    this.unreadMessagesCount.set(0);
    this.fetchPendingRequests(); // Refrescar al abrir
    if (this.friendsSidebar) {
      this.friendsSidebar.toggleSidebar();
    } else {
      console.error('FriendsSidebarComponent is undefined');
    }
  }

  fetchPendingRequests() {
    this.friendsApi.getPendingRequests().subscribe({
      next: (reqs) => {
        this.pendingRequestsCount.set(reqs.length);
        this.cdr.markForCheck();
      },
      error: () => { }
    });
  }

  openProfileModal(username: string) {
    this.friendsApi.getPublicProfile(username).subscribe({
      next: (profile) => {
        this.selectedProfile.set(profile);
        this.isProfileModalOpen.set(true);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.toastService.error(err.error?.message || 'Error al cargar perfil');
      }
    });
  }

  closeProfileModal() {
    this.isProfileModalOpen.set(false);
    this.selectedProfile.set(null);
  }

  openChatModal(friend: FriendshipDTO) {
    if (this.selectedChatFriend()?.friendUsername === friend.friendUsername) {
      this.closeChatModal();
    } else {
      this.selectedChatFriend.set(friend);
      // Reset unread count for this user
      this.unreadMessagesPerUser.update(prev => {
        const copy = { ...prev };
        const count = copy[friend.friendUsername] || 0;
        delete copy[friend.friendUsername];
        this.unreadMessagesCount.update(c => Math.max(0, c - count));
        return copy;
      });
    }
    this.cdr.markForCheck();
  }

  openChatWithSender(sender: string) {
    if (this.friendsSidebar && !this.friendsSidebar.isOpen()) {
      this.friendsSidebar.toggleSidebar();
    }
    const friend = this.friendsSidebar?.friends?.find(f => f.friendUsername === sender);
    if (friend) {
      this.openChatModal(friend);
    } else {
      this.openChatModal({ friendUsername: sender, status: 'ACCEPTED', avatarIcon: '' } as FriendshipDTO);
    }
    this.customNotification.set(null);
  }

  showCustomNotification(msg: any) {
    this.customNotification.set({ sender: msg.senderUsername, message: msg.content });
    if (this.notificationTimeout) clearTimeout(this.notificationTimeout);
    this.notificationTimeout = setTimeout(() => {
      this.customNotification.set(null);
      this.cdr.markForCheck();
    }, 4000);
  }

  closeChatModal() {
    this.selectedChatFriend.set(null);
  }

  isActive(path: string): boolean {
    const url = (this.currentPath() as string) || '/';
    if (path === '/lobby') return url === '/lobby' || url === '/';
    return url.startsWith(path);
  }

  get username(): string {
    return this.authService.username ?? 'Invitado';
  }

  get userInitial(): string {
    return this.username.charAt(0).toUpperCase();
  }

  animateCoins = signal(false);

  ngOnInit(): void {
    // Siempre nos suscribimos al perfil global (para que actualice si otro componente lo fetchea)
    this.profileService.profile$
      .pipe(takeUntil(this.destroy$))
      .subscribe(profile => {
        if (profile) {
          const currentCoins = this.profileData()?.pokecoins;
          if (currentCoins !== undefined && profile.pokecoins > currentCoins) {
            this.animateCoins.set(true);
            setTimeout(() => {
              this.animateCoins.set(false);
              this.cdr.markForCheck();
            }, 1500);
          }
          this.profileData.set(profile);
          this.cdr.markForCheck();
        }
      });

    // Si ya estamos logueados al iniciar, lo fetcheamos por las dudas
    if (this.username !== 'Invitado') {
      this.profileService.getProfile(this.username).subscribe({
        error: (err) => console.error('Error fetching profile for navbar', err)
      });

      this.fetchPendingRequests();
      // Polling cada 30 segundos para solicitudes de amistad
      setInterval(() => {
        if (this.username !== 'Invitado') this.fetchPendingRequests();
      }, 30000);

      // Escuchar nuevos mensajes del WebSocket
      this.friendsWs.messages$
        .pipe(takeUntil(this.destroy$))
        .subscribe(msg => {
          // Ignorar si soy el que envía el mensaje
          if (msg.senderUsername === this.username) return;

          // Ignorar si el chat con este amigo ya está abierto
          if (this.selectedChatFriend()?.friendUsername === msg.senderUsername) return;

          this.unreadMessagesPerUser.update(prev => ({
            ...prev,
            [msg.senderUsername]: (prev[msg.senderUsername] || 0) + 1
          }));
          this.unreadMessagesCount.update(c => c + 1);

          this.showCustomNotification(msg);
          this.cdr.markForCheck();
        });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

