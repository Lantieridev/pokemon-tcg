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
  private toastService = inject(ToastService);
  private cdr = inject(ChangeDetectorRef);
  private destroy$ = new Subject<void>();

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
    if (this.friendsSidebar) {
      this.friendsSidebar.toggleSidebar();
    } else {
      console.error('FriendsSidebarComponent is undefined');
    }
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
    }
    this.cdr.markForCheck();
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
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

