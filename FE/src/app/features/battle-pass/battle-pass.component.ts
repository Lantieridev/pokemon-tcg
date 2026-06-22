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

  // Pagination State
  currentPage = signal<number>(0);
  pageSize = 10;

  displayedLevels = computed(() => {
    const s = this.status();
    if (!s) return [];
    const startIndex = this.currentPage() * this.pageSize;
    return s.levels.slice(startIndex, startIndex + this.pageSize);
  });

  totalPages = computed(() => {
    const s = this.status();
    if (!s) return 1;
    return Math.ceil(s.levels.length / this.pageSize);
  });

  nextPage() {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.update(p => p + 1);
    }
  }

  prevPage() {
    if (this.currentPage() > 0) {
      this.currentPage.update(p => p - 1);
    }
  }

  loadData(preservePage: boolean = false) {
    if (!this.status()) {
      this.loading.set(true);
    }
    
    this.battlePassService.getStatus().subscribe({
      next: (res) => {
        this.status.set(res);
        // Auto-navigate to current level page solo si no se pide preservar
        if (!preservePage) {
          const levelIdx = res.currentLevel;
          this.currentPage.set(Math.floor(levelIdx / this.pageSize));
        }
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

  claimedReward = signal<any>(null);

  closeClaimModal() {
    this.claimedReward.set(null);
    this.loadData(true);
  }

  claimReward(level: number, isPremium: boolean) {
    this.battlePassService.claimReward(level, isPremium).subscribe({
      next: () => {
        // Encontrar la recompensa reclamada para mostrar en el modal
        const levelData = this.status()?.levels.find(l => l.level === level);
        if (levelData) {
          const type = isPremium ? levelData.premiumRewardType : levelData.freeRewardType;
          const value = isPremium ? levelData.premiumRewardValue : levelData.freeRewardValue;
          const amount = isPremium ? levelData.premiumRewardAmount : levelData.freeRewardAmount;
          this.claimedReward.set({ type, value, amount, isPremium });
        }
        
        // Actualizamos el perfil global para que impacte en la tienda (sobres, monedas, títulos)
        if (this.authService.username) {
          this.profileService.loadUserProfile(this.authService.username).subscribe();
        }
      },
      error: (err) => {
        this.toastService.error(err.error?.message || 'Error al reclamar la recompensa');
      }
    });
  }

  showPurchaseModal = signal<boolean>(false);

  openPurchaseModal() {
    if (this.pokecoins() < 1000) {
      this.toastService.error('No tienes suficientes Pokécoins (1000)');
      return;
    }
    this.showPurchaseModal.set(true);
  }

  closePurchaseModal() {
    this.showPurchaseModal.set(false); this.loading.set(true);
  }

  confirmPurchasePremium() {
    this.showPurchaseModal.set(false);
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

  canClaimFree(level: number): boolean {
    const stat = this.status();
    if (!stat) return false;
    if (level > stat.currentLevel) return false;
    if (level <= stat.claimedFreeLevel) return false;
    
    for (let l = stat.claimedFreeLevel + 1; l < level; l++) {
      const lvlData = stat.levels.find(x => x.level === l);
      if (lvlData && lvlData.freeRewardType) {
        return false;
      }
    }
    
    const thisLvl = stat.levels.find(x => x.level === level);
    return !!(thisLvl && thisLvl.freeRewardType);
  }

  canClaimPremium(level: number): boolean {
    const stat = this.status();
    if (!stat || !stat.isPremium) return false;
    if (level > stat.currentLevel) return false;
    if (level <= stat.claimedPremiumLevel) return false;
    
    for (let l = stat.claimedPremiumLevel + 1; l < level; l++) {
      const lvlData = stat.levels.find(x => x.level === l);
      if (lvlData && lvlData.premiumRewardType) {
        return false;
      }
    }
    
    const thisLvl = stat.levels.find(x => x.level === level);
    return !!(thisLvl && thisLvl.premiumRewardType);
  }

  getRewardTypeName(type: string | null): string {
    if (!type) return '';
    switch(type.toUpperCase()) {
      case 'AVATAR': return 'Avatar Exclusivo';
      case 'PACK': return 'Sobre de Cartas';
      case 'TITLE': return 'Título de Jugador';
      case 'COINS': return 'Pokémonedas';
      case 'STARDUST': return 'Polvos Estelares';
      default: return type;
    }
  }

  getRewardImage(type: string | null, value: string | null): string {
    if (!type) return '';
    if (type.toUpperCase() === 'COINS') {
      return 'assets/images/rewards/coins_stack.png';
    }
    
    if (value) {
      let prefix = '';
      if (type.toUpperCase() === 'AVATAR') prefix = 'avatar_';
      if (type.toUpperCase() === 'PACK') prefix = 'pack_';
      if (type.toUpperCase() === 'TITLE') prefix = 'titulo_';
      
      const normalizedValue = value.toLowerCase()
        .normalize("NFD").replace(/[\u0300-\u036f]/g, "") // Eliminar tildes
        .replace(/\s+/g, '_')
        .replace(/[^a-z0-9_]/g, '');
        
      return `assets/images/rewards/${prefix}${normalizedValue}.png`;
    }
    
    // Default fallback based on type if no value
    switch (type.toUpperCase()) {
      case 'COINS': return 'assets/achievements/medals/medal_coins_1k.png';
      case 'PACK': return 'assets/store/pack_base.png';
      case 'TITLE': return 'assets/rewards/title_scroll.png';
      case 'AVATAR': return 'assets/rewards/avatar_box.png';
      case 'STARDUST': return 'assets/rewards/stardust.png';
      default: return 'assets/achievements/avatars/avatar_versatility_3d.png';
    }
  }

  onImageError(event: any, type: string) {
    // Si la imagen generada aún no existe, cargamos un placeholder seguro
    const target = event.target as HTMLImageElement;
    let fallback = 'assets/achievements/avatars/avatar_versatility_3d.png';
    
    if (type.toUpperCase() === 'AVATAR') fallback = 'assets/rewards/avatar_box.png';
    else if (type.toUpperCase() === 'PACK') fallback = 'assets/store/pack_base.png';
    else if (type.toUpperCase() === 'TITLE') fallback = 'assets/rewards/title_scroll.png';
    else if (type.toUpperCase() === 'COINS') fallback = 'assets/achievements/medals/medal_coins_1k.png';
    
    // Evitar loop infinito si el fallback tampoco carga
    if (!target.src.includes(fallback)) {
      target.src = fallback;
    }
  }
}
