import { ChangeDetectionStrategy, Component, inject, OnInit, signal, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter, map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProfileService, UserProfileResponseDTO } from '../../services/profile.service';
import { LogoComponent, TrainerChipComponent, IconComponent, BallIconComponent } from '../../../features/lobby-aurora/ui/aurora-ui.components';
import { FriendsSidebarComponent } from '../../../shared/components/friends-sidebar/friends-sidebar.component';
import { PublicProfileModalComponent } from '../../../shared/components/public-profile-modal/public-profile-modal.component';
import { ChatModalComponent } from '../../../shared/components/chat-modal/chat-modal.component';
import { PublicProfileDTO, FriendshipDTO } from '../../models/friends.models';
import { FriendsApiService } from '../../services/friends-api.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, LogoComponent, TrainerChipComponent, IconComponent, BallIconComponent, FriendsSidebarComponent, PublicProfileModalComponent, ChatModalComponent],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NavbarComponent implements OnInit {
  private router = inject(Router);
  private authService = inject(AuthService);
  private profileService = inject(ProfileService);
  private friendsApi = inject(FriendsApiService);
  private toastService = inject(ToastService);
  private cdr = inject(ChangeDetectorRef);

  profileData = signal<UserProfileResponseDTO | null>(null);

  selectedProfile = signal<PublicProfileDTO | null>(null);
  isProfileModalOpen = signal(false);

  selectedChatFriend = signal<FriendshipDTO | null>(null);

  currentPath = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((e: NavigationEnd) => e.urlAfterRedirects)
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
    this.selectedChatFriend.set(friend);
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

  ngOnInit(): void {
    if (this.username !== 'Invitado') {
      this.profileService.getProfile(this.username).subscribe({
        next: (data) => this.profileData.set(data),
        error: (err) => console.error('Error fetching profile for navbar', err)
      });
    }
  }
}

