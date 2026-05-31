import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { AuthService } from '../../core/services/auth.service';
import { MatchBackendService } from '../../core/services/match-backend.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { MatchStore } from '../../core/store/match.store';

@Component({
  selector: 'app-lobby',
  standalone: true,
  imports: [CommonModule, IconComponent],
  templateUrl: './lobby.html',
  styleUrl: './lobby.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LobbyComponent {
  private router = inject(Router);
  private authService = inject(AuthService);
  private matchBackendService = inject(MatchBackendService);
  private wsService = inject(WebSocketService);
  private matchStore = inject(MatchStore);

  isSearching = signal(false);

  formats = [
    { id: 'standard', name: 'Estándar', desc: 'Cartas permitidas de la temporada actual', icon: 'star', active: true },
    { id: 'expanded', name: 'Expandido', desc: 'Todas las cartas de cualquier expansión', icon: 'cards', active: false },
    { id: 'theme', name: 'Temático', desc: 'Solo mazos preconstruidos', icon: 'shield', active: false }
  ];

  async startMatch() {
    this.isSearching.set(true);
    this.startSearchTimer();
    try {
      if (!this.authService.isAuthenticated()) {
        await this.authService.ensureDevUserAuthenticated('AshRivero', 'password123');
      }
      const currentUsername = this.authService.username ?? 'AshRivero';

      const res = await firstValueFrom(this.matchBackendService.createMatch(currentUsername, 'GarryBot', 1, 1));
      const matchId = res.matchId;

      // Cargar estado inicial
      const initialState = await firstValueFrom(this.matchBackendService.getMatchState(matchId));
      if (initialState) this.matchStore.updateState(initialState);

      // Conectar WebSocket (el store se actualiza automáticamente en el servicio)
      this.wsService.connect(matchId).subscribe();

      this.router.navigate(['/battle', matchId]);
    } catch (e) {
      console.error('Failed to start match', e);
    } finally {
      this.isSearching.set(false);
      this.stopSearch();
    }
  }

  setFormat(id: string) {
    this.formats = this.formats.map(f => ({ ...f, active: f.id === id }));
  }

  elapsed = signal(0);
  private searchInterval: any;

  private startSearchTimer() {
    this.elapsed.set(0);
    this.searchInterval = setInterval(() => {
      this.elapsed.update(e => e + 1);
    }, 1000);
  }

  public stopSearch() {
    if (this.searchInterval) clearInterval(this.searchInterval);
  }

  get elapsedStr() {
    const min = Math.floor(this.elapsed() / 60).toString().padStart(2, '0');
    const sec = (this.elapsed() % 60).toString().padStart(2, '0');
    return `${min}:${sec}`;
  }
}
