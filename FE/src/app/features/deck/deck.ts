import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { DeckStore } from '../../core/store/deck.store';
import { DeckApiService } from './deck-api.service';
import { PokemonTcgCard } from '../../core/models/game-state.models';

interface Filters {
  search: string;
  supertype: 'all' | 'Pokémon' | 'Trainer' | 'Energy';
  subtype: string;
}

@Component({
  selector: 'app-deck',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './deck.html',
  styleUrl: './deck.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Deck implements OnInit {
  // ── Inyecciones ────────────────────────────────────────────────────────────
  readonly tcgService = inject(PokemonTcgService);
  readonly deckStore = inject(DeckStore);
  private deckApi = inject(DeckApiService);

  // ── Estado UI local ────────────────────────────────────────────────────────
  readonly filters = signal<Filters>({ search: '', supertype: 'all', subtype: '' });
  readonly hover = signal<string | null>(null);
  readonly saveLoading = signal(false);
  readonly saveSuccess = signal(false);
  readonly saveError = signal<string | null>(null);

  // ── Alias del store ────────────────────────────────────────────────────────
  readonly deckGrouped = this.deckStore.deckGrouped;
  readonly totalCount = this.deckStore.totalCount;
  readonly validation = this.deckStore.validation;
  readonly isValid = this.deckStore.isValid;
  readonly cardCountById = this.deckStore.cardCountById;
  readonly deckName = this.deckStore.deckName;

  // ── Colección filtrada ────────────────────────────────────────────────────
  readonly filteredCards = computed(() => {
    const f = this.filters();
    return this.tcgService.cards().filter((card) => {
      if (
        f.search &&
        !card.name.toLowerCase().includes(f.search.toLowerCase())
      )
        return false;
      if (f.supertype !== 'all' && card.supertype !== f.supertype) return false;
      if (f.subtype && !card.subtypes.includes(f.subtype)) return false;
      return true;
    });
  });

  // ── Stats del mazo ────────────────────────────────────────────────────────
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

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.tcgService.loadCards();
  }

  // ── Acciones de mazo ─────────────────────────────────────────────────────
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
    const isBasicEnergy =
      card.supertype === 'Energy' &&
      (card.subtypes.includes('Basic Energy') || card.subtypes.includes('Basic'));
    return isBasicEnergy || count < 4;
  }

  // ── Guardar mazo ──────────────────────────────────────────────────────────
  saveDeck(): void {
    if (!this.isValid()) return;
    this.saveLoading.set(true);
    this.saveError.set(null);
    this.saveSuccess.set(false);

    const name = this.deckName();
    this.deckApi.saveDeck(name, 'VALID').subscribe({
      next: (res) => {
        this.saveLoading.set(false);
        this.saveSuccess.set(true);
        console.log('[Deck] Guardado con ID:', res.id);
        setTimeout(() => this.saveSuccess.set(false), 3000);
      },
      error: (err) => {
        this.saveLoading.set(false);
        this.saveError.set(
          err.error?.message ?? 'Error al guardar el mazo. Intentá de nuevo.'
        );
      },
    });
  }

  // ── Filtros ───────────────────────────────────────────────────────────────
  setFilter(key: keyof Filters, value: string): void {
    this.filters.update((f) => ({ ...f, [key]: value }));
  }

  setSupertype(supertype: string): void {
    if (
      supertype === 'all' ||
      supertype === 'Pokémon' ||
      supertype === 'Trainer' ||
      supertype === 'Energy'
    ) {
      this.filters.update((f) => ({ ...f, supertype }));
    }
  }

  // ── Drag & Drop ───────────────────────────────────────────────────────────
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

  // ── Helpers de imagen ─────────────────────────────────────────────────────
  getCardImage(card: PokemonTcgCard): string {
    return card.images?.small ?? card.images?.large ?? '';
  }

  getTypeColor(types: string[] | undefined): string {
    const type = types?.[0]?.toLowerCase() ?? '';
    const colors: Record<string, string> = {
      fire: '#ff7a3d',
      water: '#4aa3ff',
      grass: '#5ad27a',
      lightning: '#ffcc33',
      psychic: '#c87bff',
      fighting: '#d97d4a',
      darkness: '#5a4a6a',
      metal: '#b8b8cc',
      fairy: '#ff8fd4',
      dragon: '#7038f8',
      colorless: '#cfd6e4',
    };
    return colors[type] ?? '#9aa9c7';
  }
}
