import { Component, OnInit, inject, signal, computed } from '@angular/core';

import { IconComponent, CoinIconComponent } from '../../shared/ui/ui-kit.components';
import { StoreService, StoreItemDTO } from '../../core/services/store.service';
import { ToastService } from '../../core/services/toast.service';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';
import { PackService } from '../../core/services/pack.service';
import { HoloCardComponent } from '../../shared/ui/holo-card/holo-card.component';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';

@Component({
  selector: 'app-store',
  standalone: true,
  imports: [CoinIconComponent, HoloCardComponent],
  templateUrl: './store.component.html',
  styleUrl: './store.component.css'
})
export class StoreComponent implements OnInit {
  private storeService = inject(StoreService);
  private packService = inject(PackService);
  private profileService = inject(ProfileService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);
  private tcgService = inject(PokemonTcgService);

  zoomedCard = signal<any | null>(null);

  items = signal<StoreItemDTO[]>([]);
  pokecoins = signal<number>(0);
  userPacks = signal<number>(0);
  packsInventory = signal<{ [key: string]: number }>({});
  loading = signal<boolean>(true);
  
  openingPack = signal<boolean>(false);
  openingPackType = signal<string>('pack_base');
  packResult = signal<any>(null);
  openedCards = signal<any[]>([]);

  ownedAvatars = signal<string[]>([]);
  ownedTitles = signal<string[]>([]);

  selectedItemToBuy = signal<StoreItemDTO | null>(null);
  isPurchasing = signal<boolean>(false);

  categories = ['AVATAR', 'PACK', 'MY_PACKS'];
  selectedCategory = signal<string>('AVATAR');

  filteredItems = computed(() => {
    if (this.selectedCategory() === 'MY_PACKS') {
      const inventoryPacks: StoreItemDTO[] = [];
      const storePackIds = new Set(this.items().map(i => i.imageUrl));
      
      // Agregar sobres comprables que el usuario tiene
      this.items().forEach(item => {
        if (item.itemType === 'PACK' && (this.packsInventory()[item.imageUrl || 'pack_base'] || 0) > 0) {
          inventoryPacks.push(item);
        }
      });
      
      // Agregar sobres del inventario que no están en la tienda (ej: recompensas exclusivas del pase)
      Object.keys(this.packsInventory()).forEach(key => {
        if (this.packsInventory()[key] > 0 && !storePackIds.has(key)) {
          // Generar nombre legible: "pack_kanto_base" -> "Kanto Base"
          let readableName = key.replace('pack_', '').replace(/_/g, ' ');
          readableName = readableName.replace('solluna', 'sol/luna');
          readableName = readableName.charAt(0).toUpperCase() + readableName.slice(1);
          
          inventoryPacks.push({
            id: -1, // ID dummy
            name: `Sobre ${readableName}`,
            description: 'Sobre obtenido como recompensa.',
            price: 0,
            itemType: 'PACK',
            imageUrl: key
          });
        }
      });
      return inventoryPacks;
    }
    return this.items()
      .filter(item => item.itemType === this.selectedCategory())
      .filter(item => item.name !== 'Coleccionista Estrella');
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
        return `assets/images/rewards/${item.imageUrl || 'pack_kanto_base'}.png`;
    }
    return item.imageUrl || 'assets/achievements/avatars/avatar_winner_badge.png';
  }

  getOpeningPackImage(): string {
    const type = this.openingPackType();
    if (type === 'pack_base' || type === 'pack_xy_base') return 'assets/images/rewards/pack_comun.png';
    return 'assets/images/rewards/' + type + '.png';
  }

  isOwned(item: StoreItemDTO): boolean {
    if (item.itemType === 'PACK') return false; // Se pueden comprar infinitos sobres
    if (item.itemType === 'AVATAR') {
      return this.ownedAvatars().includes(item.name) || this.ownedAvatars().includes(item.imageUrl);
    }
    if (item.itemType === 'TITLE') {
      return this.ownedTitles().includes(item.name) || this.ownedTitles().includes(item.imageUrl);
    }
    return false;
  }

  ngOnInit() {
    this.loadStore();
    this.loadBalance();
    this.tcgService.loadCards();
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
        this.packsInventory.set(profile.packsInventory || {});
        this.ownedAvatars.set(profile.unlockedAvatars || []);
          this.ownedTitles.set(profile.unlockedTitles || []);
        },
        error: () => {}
      });
    }
  }

  confirmPurchase() {
    const item = this.selectedItemToBuy();
    if (!item) return;

    if (this.pokecoins() < item.price) {
      this.toastService.error('Pokecoins insuficientes');
      return;
    }

    this.isPurchasing.set(true);
    this.storeService.buyItem(item.id).subscribe({
      next: () => {
        this.toastService.success(`¡Has comprado ${item.name}!`);
        this.loadBalance(); // Refresh balance, packs, and unlocked items
        this.selectedItemToBuy.set(null);
        this.isPurchasing.set(false);
      },
      error: (err) => {
        this.toastService.error(err.error?.message || 'Error al procesar la compra');
        this.isPurchasing.set(false);
      }
    });
  }

  openPack(packType: string = 'pack_base') {
    const amount = this.packsInventory()[packType] || 0;
    // Fallback if not in map but they have generic packs
    if (amount <= 0 && !(packType === 'pack_base' && this.userPacks() > 0)) {
      this.toastService.error('No tienes sobres de este tipo para abrir');
      return;
    }
    
    this.openingPackType.set(packType);
    this.openingPack.set(true);
    this.packService.openPack(packType).subscribe({
      next: (result) => {
        this.openedCards.set(result.cards);
        this.packResult.set(result);
        this.loadBalance(); // Refresh
        setTimeout(() => this.openingPack.set(false), 2000);
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

  zoomCard(card: any) {
    const fullCard = this.tcgService.cards().find(c => c.id === card.cardId);
    const hiresUrl = 'https://images.pokemontcg.io/' + card.cardId.split('-')[0] + '/' + card.cardId.split('-')[1] + '_hires.png';
    this.zoomedCard.set({
      img: fullCard?.images?.large ?? fullCard?.images?.small ?? hiresUrl,
      name: card.cardName || fullCard?.name || 'Pokémon',
      type: (fullCard?.types && fullCard?.types[0]?.toLowerCase()) || 'colorless',
      rarity: (fullCard as any)?.rarity || card.rarity || 'Common',
      subtypes: fullCard?.subtypes || []
    });
  }
}
