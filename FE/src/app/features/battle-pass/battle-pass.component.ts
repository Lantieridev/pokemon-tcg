import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattlePassService, BattlePassStatusDTO, BattlePassLevelDTO } from '../../core/services/battle-pass.service';
import { ToastService } from '../../core/services/toast.service';
import { IconComponent, CoinIconComponent } from '../lobby-aurora/ui/aurora-ui.components';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-battle-pass',
  standalone: true,
  imports: [CommonModule, IconComponent, CoinIconComponent],
  templateUrl: './battle-pass.component.html',
  styleUrl: './battle-pass.component.css'
})
export class BattlePassComponent implements OnInit {
  private battlePassService = inject(BattlePassService);
  private profileService = inject(ProfileService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);

  status = signal<BattlePassStatusDTO | null>(null);
  pokecoins = signal<number>(0);
  loading = signal<boolean>(true);

  progressPercentage = computed(() => {
    const s = this.status();
    if (!s) return 0;
    
    // Find next level requirements
    const nextLevel = s.levels.find(l => l.level === s.currentLevel + 1);
    if (!nextLevel) return 100; // max level
    
    const prevRequired = s.currentLevel === 0 ? 0 : s.levels[s.currentLevel - 1].requiredXp;
    const levelXpRange = nextLevel.requiredXp - prevRequired;
    const currentLevelXp = s.currentXp - prevRequired;
    
    return Math.min(100, Math.max(0, (currentLevelXp / levelXpRange) * 100));
  });

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.loading.set(true);
    this.battlePassService.getStatus().subscribe({
      next: (res) => {
        this.status.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.toastService.error('Error al cargar el Pase de Batalla');
        this.loading.set(false);
      }
    });

    if (this.authService.username) {
      this.profileService.getProfile(this.authService.username).subscribe({
        next: (profile) => this.pokecoins.set(profile.pokecoins),
        error: () => {}
      });
    }
  }

  claimReward(level: number, isPremium: boolean) {
    this.battlePassService.claimReward(level, isPremium).subscribe({
      next: () => {
        this.toastService.success('¡Recompensa reclamada con éxito!');
        this.loadData();
      },
      error: (err) => {
        this.toastService.error(err.error?.message || 'Error al reclamar la recompensa');
      }
    });
  }

  purchasePremium() {
    if (this.pokecoins() < 1000) {
      this.toastService.error('No tienes suficientes Pokécoins (1000)');
      return;
    }

    if (confirm('¿Deseas comprar el Pase de Batalla Premium por 1000 Pokécoins?')) {
      this.battlePassService.purchasePremium().subscribe({
        next: () => {
          this.toastService.success('¡Has desbloqueado el Pase Premium!');
          this.loadData();
        },
        error: (err) => {
          this.toastService.error(err.error?.message || 'Error al comprar el pase');
        }
      });
    }
  }

  getRewardImage(type: string, value: string): string {
    if (!type) return '';
    switch (type.toUpperCase()) {
      case 'COINS': return 'assets/store/coins_stack.png';
      case 'PACK': return 'assets/store/pack_base.png';
      case 'TITLE': return 'assets/achievements/avatars/avatar_winner_badge.png';
      case 'AVATAR': return `assets/store/avatar_${value || 'ash'}.png`; // Simplified for demo
      default: return '';
    }
  }
}
