import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter, map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { IconComponent } from '../../../shared/ui/icon/icon.component';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, IconComponent],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NavbarComponent {
  private router = inject(Router);

  navItems = [
    { id: 'lobby',   label: 'Inicio',  icon: 'home',  path: '/' },
    { id: 'deck',    label: 'Mazos',   icon: 'cards', path: '/deck' },
    { id: 'social',  label: 'Social',  icon: 'users', path: '/social' },
    { id: 'profile', label: 'Perfil',  icon: 'user',  path: '/profile' },
    { id: 'admin',   label: 'Admin',   icon: 'shield', path: '/admin', adminOnly: true },
  ];

  conn = { online: true, ping: 38, region: 'SAE-1' };

  currentPath = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((e: NavigationEnd) => e.urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  );

  isActive(path: string): boolean {
    const url = (this.currentPath() as string) || '/';
    if (path === '/') return url === '/';
    return url.startsWith(path);
  }
}
