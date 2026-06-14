import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent, CoinIconComponent } from '../lobby-aurora/ui/aurora-ui.components';
import { StoreService, StoreItemDTO } from '../../core/services/store.service';
import { ToastService } from '../../core/services/toast.service';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';
import { PackService } from '../../core/services/pack.service';

@Component({
  selector: 'app-store',
  standalone: true,
  imports: [CommonModule, IconComponent, CoinIconComponent],
  templateUrl: './store.component.html',
  styleUrl: './store.component.css'
})
export class StoreComponent implements OnInit {
  private storeService = inject(StoreService);
  private packService = inject(PackService);
  private profileService = inject(ProfileService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);

  items = signal<StoreItemDTO[]>([]);
  pokecoins = signal<number>(0);
  userPacks = signal<number>(0);
  loading = signal<boolean>(true);
  
  openingPack = signal<boolean>(false);
  packResult = signal<any>(null);
  openedCards = signal<any[]>([]);

  categories = ['AVATAR', 'TITLE', 'PACK'];
  selectedCategory = signal<string>('AVATAR');

  filteredItems = computed(() => {
    return this.items().filter(item => item.itemType === this.selectedCategory());
  });

  setCategory(cat: string) {
    this.selectedCategory.set(cat);
  }

  getImageSrc(item: StoreItemDTO): string {
    if (item.itemType === 'AVATAR') {
      if (item.imageUrl === 'bulbasaur_classic') return 'assets/store/avatar_bulbasaur.png';
      if (item.imageUrl === 'charmander_fire') return 'assets/store/avatar_charmander.png';
      if (item.imageUrl === 'squirtle_water') return 'assets/store/avatar_squirtle.png';
      if (item.imageUrl === 'ash_avatar') return 'assets/store/avatar_ash.png';
      if (item.imageUrl === 'misty_avatar') return 'assets/store/avatar_misty.png';
      if (item.imageUrl === 'brock_avatar') return 'assets/store/avatar_brock.png';
      if (item.imageUrl === 'charizard_3d') return 'assets/store/avatar_charizard_3d.png';
      if (item.imageUrl === 'mewtwo_3d') return 'assets/store/avatar_mewtwo_3d.png';
      if (item.imageUrl === 'pikachu_cute') return 'assets/store/avatar_pikachu_cute.png';
      if (item.imageUrl === 'collector_legend') return 'assets/store/avatar_collector.png';

      return item.imageUrl ? `assets/achievements/avatars/${item.imageUrl}.png` : 'assets/achievements/avatars/avatar_belt_white.png';
    }
    if (item.itemType === 'PACK') {
        if (item.imageUrl === 'pack_jungle') return 'assets/store/pack_jungle.png';
        if (item.imageUrl === 'pack_fossil') return 'assets/store/pack_fossil.png';
        if (item.imageUrl === 'pack_rocket') return 'assets/store/pack_rocket.png';
        return 'assets/store/pack_base.png';
    }
    return item.imageUrl || 'assets/achievements/avatars/avatar_winner_badge.png';
  }

  ngOnInit() {
    this.loadStore();
    this.loadBalance();
  }

  loadStore() {
    this.storeService.getAvailableItems().subscribe({
      next: (res) => {
        this.items.set(res);
        this.loading.set(false);
      },
      error: (err) => {
        this.toastService.error('Error al cargar la tienda');
        this.loading.set(false);
      }
    });
  }

  loadBalance() {
    if (this.authService.username) {
      this.profileService.getProfile(this.authService.username).subscribe({
        next: (profile) => {
          this.pokecoins.set(profile.pokecoins);
          this.userPacks.set(profile.packs || 0);
        },
        error: () => {}
      });
    }
  }

  buyItem(item: StoreItemDTO) {
    if (this.pokecoins() < item.price) {
      this.toastService.error('Pokecoins insuficientes');
      return;
    }

    this.storeService.buyItem(item.id).subscribe({
      next: () => {
        this.toastService.success(`¡Has comprado ${item.name}!`);
        this.loadBalance(); // Refresh balance and packs
      },
      error: (err) => {
        this.toastService.error(err.error?.message || 'Error al procesar la compra');
      }
    });
  }

  openPack() {
    if (this.userPacks() <= 0) {
      this.toastService.error('No tienes sobres para abrir');
      return;
    }
    
    this.openingPack.set(true);
    this.packService.openPack().subscribe({
      next: (result) => {
        this.openedCards.set(result.cards);
        this.packResult.set(result);
        this.loadBalance(); // Refresh
        setTimeout(() => this.openingPack.set(false), 1000);
      },
      error: (err) => {
        this.toastService.error(err.error?.message || 'Error al abrir el sobre');
        this.openingPack.set(false);
      }
    });
  }

  closePackResult() {
    this.packResult.set(null);
    this.openedCards.set([]);
  }
}
