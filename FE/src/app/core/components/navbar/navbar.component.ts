import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter, map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProfileService, UserProfileResponseDTO } from '../../services/profile.service';
import { LogoComponent, TrainerChipComponent, IconComponent, BallIconComponent } from '../../../features/lobby-aurora/ui/aurora-ui.components';
import { FriendsSidebarComponent } from '../../../shared/components/friends-sidebar/friends-sidebar.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, LogoComponent, TrainerChipComponent, IconComponent, BallIconComponent, FriendsSidebarComponent],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NavbarComponent implements OnInit {
  private router = inject(Router);
  private authService = inject(AuthService);
  private profileService = inject(ProfileService);

  profileData = signal<UserProfileResponseDTO | null>(null);

  currentPath = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((e: NavigationEnd) => e.urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  );

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

