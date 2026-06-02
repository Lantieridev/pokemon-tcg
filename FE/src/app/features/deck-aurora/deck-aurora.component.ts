import { Component, OnInit, computed, inject, signal, ViewEncapsulation } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { DeckStore } from '../../core/store/deck.store';
import { DeckApiService } from '../deck/deck-api.service';
import { PokemonTcgCard } from '../../core/models/game-state.models';
import { RouterModule } from '@angular/router';
import { LogoComponent, TrainerChipComponent, AmbientComponent, IconComponent, EnergyTypeComponent } from '../lobby-aurora/ui/aurora-ui.components';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService, UserProfileResponseDTO } from '../../core/services/profile.service';

interface Filters {
  search: string;
  supertype: 'all' | 'Pokémon' | 'Trainer' | 'Energy';
  subtype: string;
}

@Component({
  selector: 'app-deck-aurora',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LogoComponent, TrainerChipComponent, AmbientComponent, IconComponent, EnergyTypeComponent, NgOptimizedImage],
  encapsulation: ViewEncapsulation.None,
  template: `
    <div class="scene v-aurora" style="position: fixed; inset: 0; z-index: 9999; overflow: hidden; display: flex; flex-direction: column;">
      <!-- Background Mesh -->
      <div class="mesh" style="opacity: 0.5;">
        <span style="width: 540px; height: 540px; left: -120px; top: -160px; background: var(--m1);"></span>
        <span style="width: 620px; height: 620px; left: 360px; top: -240px; background: var(--m2); animation-delay: -5s;"></span>
      </div>
      <aurora-ambient></aurora-ambient>
      <div class="bd-noise"></div>
      <div class="bd-vignette"></div>

      <!-- Topbar -->
      <div style="flex: 0 0 auto; height: 92px; display: flex; align-items: center; justify-content: space-between; padding: 0 44px; z-index: 10; background: linear-gradient(180deg, var(--bg) 0%, transparent 100%);">
        <aurora-logo></aurora-logo>
        <div style="display: flex; align-items: center; gap: 22px;">
          <nav style="display: flex; gap: 26px; font-size: 13.5px; font-weight: 600; color: var(--mut);">
            <a routerLink="/lobby" style="text-decoration: none; color: var(--mut);">Inicio</a>
            <a routerLink="/deck" style="text-decoration: none; color: var(--txt);">Mazos</a>
            <a routerLink="/profile" style="text-decoration: none; color: var(--mut);">Perfil</a>
          </nav>
          <aurora-trainer-chip [name]="username" [initial]="userInitial" [mmr]="profileData?.mmr?.toString() || '1000'"></aurora-trainer-chip>
        </div>
      </div>

      <!-- Main Deck Builder Area -->
      <div class="fu" style="flex: 1; display: flex; gap: 24px; padding: 0 44px 44px; z-index: 5; height: calc(100vh - 92px);">
        
        <!-- Left: Collection & Filters -->
        <div style="flex: 1; display: flex; flex-direction: column; background: var(--surface); border: 1px solid var(--line); border-radius: 24px; backdrop-filter: blur(10px); overflow: hidden;">
          <!-- Filters Header -->
          <div style="padding: 20px 24px; border-bottom: 1px solid var(--line); display: flex; gap: 16px; align-items: center;">
            <div style="position: relative; flex: 1; max-width: 300px;">
              <input type="text" [ngModel]="filters().search" (ngModelChange)="setFilter('search', $event)" placeholder="Buscar carta..." style="width: 100%; background: rgba(255,255,255,0.05); border: 1px solid var(--line); color: var(--txt); padding: 10px 16px; border-radius: 12px; outline: none; font-family: 'Manrope'; font-size: 14px;" />
            </div>
            
            <div style="display: flex; gap: 8px; background: rgba(0,0,0,0.2); padding: 4px; border-radius: 14px; border: 1px solid var(--line);">
              <button [class.active-tab]="filters().supertype === 'all'" (click)="setSupertype('all')" class="tab-btn">Todos</button>
              <button [class.active-tab]="filters().supertype === 'Pokémon'" (click)="setSupertype('Pokémon')" class="tab-btn">Pokémon</button>
              <button [class.active-tab]="filters().supertype === 'Trainer'" (click)="setSupertype('Trainer')" class="tab-btn">Trainer</button>
              <button [class.active-tab]="filters().supertype === 'Energy'" (click)="setSupertype('Energy')" class="tab-btn">Energy</button>
            </div>
          </div>

          <!-- Cards Grid -->
          <div class="scroll" style="flex: 1; overflow-y: auto; padding: 24px;">
            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 20px;">
              @for (card of filteredCards(); track card.id) {
                <div class="card-item" [class.disabled]="!canAdd(card)" draggable="true" (dragstart)="handleDragStart($event, card)" (click)="addCard(card)">
                  <div style="position: relative; aspect-ratio: 5/7; border-radius: 8px; overflow: hidden; box-shadow: 0 8px 16px rgba(0,0,0,0.6); border: 1px solid rgba(255,255,255,0.1);">
                    <img [ngSrc]="getCardImage(card)" [alt]="card.name" fill style="object-fit: contain;" />
                    @if (getCardCount(card.id) > 0) {
                      <div class="count-badge num">{{ getCardCount(card.id) }}</div>
                    }
                  </div>
                </div>
              }
            </div>
          </div>
        </div>

        <!-- Right: Active Deck Sidebar -->
        <div style="width: 380px; flex: 0 0 auto; display: flex; flex-direction: column; background: var(--surface); border: 1px solid var(--line); border-radius: 24px; backdrop-filter: blur(10px); overflow: hidden;" (dragover)="allowDrop($event)" (drop)="handleDropDeck($event)">
          
          <!-- Deck Header -->
          <div style="padding: 20px 24px; border-bottom: 1px solid var(--line);">
            <div class="eyebrow" style="color: var(--accent2);">Mazo Activo</div>
            <div style="display: flex; justify-content: space-between; align-items: flex-end; margin-top: 8px;">
              <input [ngModel]="deckName()" (ngModelChange)="deckStore.deckName.set($event)" style="font-family: var(--display); font-size: 28px; background: transparent; border: none; color: var(--txt); outline: none; width: 100%; font-weight: 700;" placeholder="Nombre del mazo" />
              <div class="num" [style.color]="isValid() ? '#46e08a' : 'var(--accent)'" style="font-size: 24px; font-weight: 700;">{{ totalCount() }}<span style="font-size: 14px; color: var(--mut);">/60</span></div>
            </div>
            
            <!-- Deck Stats -->
            <div style="display: flex; gap: 16px; margin-top: 16px;">
              <div style="display: flex; align-items: center; gap: 6px;"><div style="width:10px; height:10px; border-radius:50%; background: #4aa3ff;"></div> <span class="num" style="font-size: 13px;">{{ deckStats().pokemon }}</span></div>
              <div style="display: flex; align-items: center; gap: 6px;"><div style="width:10px; height:10px; border-radius:50%; background: #b8b8cc;"></div> <span class="num" style="font-size: 13px;">{{ deckStats().trainer }}</span></div>
              <div style="display: flex; align-items: center; gap: 6px;"><div style="width:10px; height:10px; border-radius:50%; background: #ffce32;"></div> <span class="num" style="font-size: 13px;">{{ deckStats().energy }}</span></div>
              <button (click)="clearDeck()" class="ghost-btn sm" style="margin-left: auto; height: 28px; padding: 0 10px; font-size: 11px;">Vaciar</button>
            </div>
          </div>

          <!-- Deck Cards List -->
          <div class="scroll" style="flex: 1; overflow-y: auto; padding: 20px;">
            <div style="display: flex; flex-direction: column; gap: 10px;">
              @for (group of deckGrouped(); track group.card.id) {
                <div class="deck-row" (click)="removeCard(group.card.id)" (contextmenu)="$event.preventDefault(); removeAll(group.card.id)">
                  <div style="width: 40px; height: 56px; border-radius: 4px; overflow: hidden; position: relative;">
                    <img [ngSrc]="getCardImage(group.card)" [alt]="group.card.name" fill style="object-fit: cover;" />
                  </div>
                  <div style="flex: 1; padding: 0 10px;">
                    <div style="font-weight: 700; font-size: 14px;">{{ group.card.name }}</div>
                    <div style="font-size: 11px; color: var(--mut); text-transform: uppercase;">{{ group.card.supertype }}</div>
                  </div>
                  <div class="num" style="font-size: 18px; font-weight: 700; color: var(--accent2); padding: 0 10px;">x{{ group.count }}</div>
                </div>
              }
            </div>
          </div>

          <!-- Deck Footer (Save) -->
          <div style="padding: 20px; border-top: 1px solid var(--line); background: rgba(0,0,0,0.2);">
            @if (saveError()) {
              <div style="color: var(--accent); font-size: 12px; margin-bottom: 10px; text-align: center; font-weight: 600;">{{ saveError() }}</div>
            }
            @if (saveSuccess()) {
              <div style="color: #46e08a; font-size: 12px; margin-bottom: 10px; text-align: center; font-weight: 600;">¡Mazo guardado con éxito!</div>
            }
            <button class="cta" style="width: 100%; height: 64px; justify-content: center; gap: 12px;" [disabled]="!isValid() || saveLoading()" (click)="saveDeck()" [style.opacity]="!isValid() ? '0.5' : '1'">
              @if (saveLoading()) {
                <div class="pokespin" style="width: 20px; height: 20px;"></div>
                <span>Guardando...</span>
              } @else {
                <span>Guardar Mazo</span>
                <aurora-icon n="decks" [s]="20"></aurora-icon>
              }
            </button>
            @if (!isValid()) {
              <div style="text-align: center; font-size: 11px; color: var(--mut); margin-top: 10px;">{{ validation().errors[0] || 'Necesitas 60 cartas para guardar.' }}</div>
            }
          </div>
        </div>
      </div>
    </div>
    <style>
      .tab-btn { background: transparent; border: none; color: var(--mut); font-family: 'Manrope'; font-weight: 700; font-size: 13px; padding: 8px 16px; border-radius: 10px; cursor: pointer; transition: all 0.2s; }
      .tab-btn:hover { color: var(--txt); }
      .tab-btn.active-tab { background: rgba(255,255,255,0.1); color: var(--txt); box-shadow: 0 2px 8px rgba(0,0,0,0.2); }
      
      .card-item { cursor: pointer; transition: transform 0.2s, filter 0.2s; }
      .card-item:hover { transform: translateY(-4px) scale(1.05); filter: drop-shadow(0 10px 20px rgba(0,0,0,0.8)); }
      .card-item.disabled { opacity: 0.4; filter: grayscale(1); cursor: not-allowed; }
      .card-item.disabled:hover { transform: none; }
      
      .count-badge { position: absolute; top: -8px; right: -8px; width: 28px; height: 28px; border-radius: 50%; background: var(--accent); color: white; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 14px; border: 2px solid var(--bg2); box-shadow: 0 2px 6px rgba(0,0,0,0.5); z-index: 10; }
      
      .deck-row { display: flex; align-items: center; background: rgba(255,255,255,0.03); border: 1px solid var(--line); border-radius: 8px; padding: 4px; cursor: pointer; transition: background 0.2s, border-color 0.2s; }
      .deck-row:hover { background: rgba(255,46,62,0.1); border-color: rgba(255,46,62,0.3); }
    </style>
  `
})
export class DeckAuroraComponent implements OnInit {
  readonly tcgService = inject(PokemonTcgService);
  readonly deckStore = inject(DeckStore);
  private deckApi = inject(DeckApiService);
  private authService = inject(AuthService);
  private profileService = inject(ProfileService);

  readonly filters = signal<Filters>({ search: '', supertype: 'all', subtype: '' });
  readonly saveLoading = signal(false);
  readonly saveSuccess = signal(false);
  readonly saveError = signal<string | null>(null);

  profileData: UserProfileResponseDTO | null = null;

  readonly deckGrouped = this.deckStore.deckGrouped;
  readonly totalCount = this.deckStore.totalCount;
  readonly validation = this.deckStore.validation;
  readonly isValid = this.deckStore.isValid;
  readonly cardCountById = this.deckStore.cardCountById;
  readonly deckName = this.deckStore.deckName;

  get username(): string {
    return this.authService.username ?? 'Invitado';
  }

  get userInitial(): string {
    return this.username.charAt(0).toUpperCase();
  }

  readonly filteredCards = computed(() => {
    const f = this.filters();
    return this.tcgService.cards().filter((card) => {
      if (f.search && !card.name.toLowerCase().includes(f.search.toLowerCase())) return false;
      if (f.supertype !== 'all' && card.supertype !== f.supertype) return false;
      if (f.subtype && !card.subtypes.includes(f.subtype)) return false;
      return true;
    });
  });

  readonly deckStats = computed(() => {
    const cards = this.deckStore.deckCards();
    const counts = { pokemon: 0, trainer: 0, energy: 0 };
    for (const c of cards) {
      if (c.supertype === 'Pokémon') counts.pokemon++;
      else if (c.supertype === 'Trainer') counts.trainer++;
      else counts.energy++;
    }
    return counts;
  });

  ngOnInit(): void {
    this.tcgService.loadCards();
    if (this.username !== 'Invitado') {
      this.profileService.getProfile(this.username).subscribe({
        next: (data) => this.profileData = data,
        error: (err) => console.error('Error fetching profile', err)
      });
    }
  }

  addCard(card: PokemonTcgCard): void {
    this.deckStore.addCard(card);
  }

  removeCard(cardId: string): void {
    this.deckStore.removeCard(cardId);
  }

  removeAll(cardId: string): void {
    this.deckStore.removeAllCopies(cardId);
  }

  clearDeck(): void {
    if (confirm('¿Querés vaciar el mazo?')) {
      this.deckStore.clearDeck();
    }
  }

  getCardCount(cardId: string): number {
    return this.cardCountById().get(cardId) ?? 0;
  }

  canAdd(card: PokemonTcgCard): boolean {
    if (this.totalCount() >= 60) return false;
    const count = this.cardCountById().get(card.id) ?? 0;
    const isBasicEnergy = card.supertype === 'Energy' && (card.subtypes.includes('Basic Energy') || card.subtypes.includes('Basic'));
    return isBasicEnergy || count < 4;
  }

  saveDeck(): void {
    if (!this.isValid()) return;
    this.saveLoading.set(true);
    this.saveError.set(null);
    this.saveSuccess.set(false);

    const name = this.deckName();
    this.deckApi.saveDeck(name).subscribe({
      next: (res) => {
        this.saveLoading.set(false);
        this.saveSuccess.set(true);
        setTimeout(() => this.saveSuccess.set(false), 3000);
      },
      error: (err) => {
        this.saveLoading.set(false);
        this.saveError.set(err.error?.message ?? 'Error al guardar el mazo. Intentá de nuevo.');
      },
    });
  }

  setFilter(key: keyof Filters, value: string): void {
    this.filters.update((f) => ({ ...f, [key]: value }));
  }

  setSupertype(supertype: string): void {
    if (['all', 'Pokémon', 'Trainer', 'Energy'].includes(supertype)) {
      this.filters.update((f) => ({ ...f, supertype: supertype as any }));
    }
  }

  handleDragStart(e: DragEvent, card: PokemonTcgCard): void {
    if (e.dataTransfer) {
      e.dataTransfer.setData('text/cardId', card.id);
      e.dataTransfer.effectAllowed = 'copy';
    }
  }

  handleDropDeck(e: DragEvent): void {
    e.preventDefault();
    if (!e.dataTransfer) return;
    const cardId = e.dataTransfer.getData('text/cardId');
    const card = this.tcgService.cards().find((c) => c.id === cardId);
    if (card) this.addCard(card);
  }

  allowDrop(e: DragEvent): void {
    e.preventDefault();
  }

  getCardImage(card: PokemonTcgCard): string {
    return card.images?.small ?? card.images?.large ?? '';
  }
}
