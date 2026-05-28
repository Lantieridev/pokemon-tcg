import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TYPE_COLORS, CardMock } from '../../shared/data/cards.mock';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { EnergyPipComponent } from '../../shared/ui/energy-pip/energy-pip.component';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { AuthService } from '../../core/services/auth.service';

interface Filters {
  types: Set<string>;
  hp: 'any' | 'low' | 'mid' | 'high';
  kind: 'all' | 'pokemon' | 'trainer' | 'energy';
  search: string;
}

@Component({
  selector: 'app-deck',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, EnergyPipComponent],
  templateUrl: './deck.html',
  styleUrl: './deck.css'
})
export class Deck implements OnInit {
  private tcgService = inject(PokemonTcgService);
  private authService = inject(AuthService);
  private http = inject(HttpClient);

  allCards: CardMock[] = [];
  cardsMap = new Map<string, CardMock>();
  typeColors = TYPE_COLORS;

  deckName = signal<string>('Charizard Rush');
  deck = signal<string[]>([]);
  isLoading = signal<boolean>(true);

  filters = signal<Filters>({
    types: new Set(),
    hp: 'any',
    kind: 'all',
    search: '',
  });

  hover = signal<string | null>(null);

  ngOnInit() {
    this.loadCards();
    this.loadDraft();
  }

  private loadCards() {
    this.tcgService.getCards().subscribe({
      next: (apiCards) => {
        this.allCards = apiCards.map(c => this.mapApiCard(c));
        this.allCards.forEach(c => this.cardsMap.set(c.id, c));
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error fetching cards', err);
        this.isLoading.set(false);
      }
    });
  }

  private mapApiCard(c: any): CardMock {
    let type = 'colorless';
    const supertype = c.supertype || '';
    if (supertype === 'Trainer') {
      type = 'trainer';
    } else if (supertype === 'Energy') {
      const nameLower = (c.name || '').toLowerCase();
      if (nameLower.includes('fire')) type = 'fire';
      else if (nameLower.includes('water')) type = 'water';
      else if (nameLower.includes('grass')) type = 'grass';
      else if (nameLower.includes('lightning') || nameLower.includes('electric')) type = 'lightning';
      else if (nameLower.includes('psychic')) type = 'psychic';
      else if (nameLower.includes('fighting')) type = 'fighting';
      else type = 'colorless';
    } else if (c.types && c.types.length > 0) {
      type = c.types[0].toLowerCase();
    }

    return {
      id: c.id,
      name: c.name,
      type: type,
      hp: c.hp ? parseInt(c.hp, 10) : undefined,
      img: c.images?.small || c.images?.large || '',
      energy: supertype === 'Energy'
    };
  }

  private loadDraft() {
    const draftName = localStorage.getItem('deck_draft_name');
    const draftCards = localStorage.getItem('deck_draft_cards');
    if (draftName) {
      this.deckName.set(draftName);
    }
    if (draftCards) {
      this.deck.set(JSON.parse(draftCards));
    } else {
      // Default initial deck (empty or fallback)
      this.deck.set([]);
    }
  }

  saveDraft() {
    localStorage.setItem('deck_draft_name', this.deckName());
    localStorage.setItem('deck_draft_cards', JSON.stringify(this.deck()));
    alert('Borrador guardado localmente.');
  }

  saveDeck() {
    const userId = this.authService.userId;
    if (!userId) {
      alert('Error: Debes estar autenticado para guardar el mazo.');
      return;
    }

    if (this.stats().total !== 60) {
      alert('El mazo debe tener exactamente 60 cartas.');
      return;
    }

    const groupedCards = this.deckGrouped().map(([cardId, quantity]) => ({
      cardId,
      quantity
    }));

    const payload = {
      userId,
      name: this.deckName(),
      cards: groupedCards
    };

    this.http.post('http://localhost:8081/api/decks', payload).subscribe({
      next: (res: any) => {
        alert('Mazo guardado correctamente en el servidor.');
        // Limpiamos borrador local al guardar
        localStorage.removeItem('deck_draft_name');
        localStorage.removeItem('deck_draft_cards');
        // Opcional: guardar ID de mazo activo para jugar
        if (res && res.id) {
          localStorage.setItem('active_deck_id', res.id.toString());
        }
      },
      error: (err) => {
        console.error('Error saving deck to BE', err);
        alert('Error al guardar el mazo: ' + (err.error?.message || err.error || 'Intenta de nuevo.'));
      }
    });
  }

  filtered = computed(() => {
    const f = this.filters();
    return this.allCards.filter((c) => {
      if (f.search && !c.name.toLowerCase().includes(f.search.toLowerCase())) return false;
      if (f.kind === 'pokemon' && (c.energy || c.type === 'trainer')) return false;
      if (f.kind === 'trainer' && c.type !== 'trainer') return false;
      if (f.kind === 'energy' && !c.energy) return false;
      if (f.types.size > 0 && !f.types.has(c.type)) return false;
      if (f.hp !== 'any' && c.hp) {
        if (f.hp === 'low'  && c.hp >  60) return false;
        if (f.hp === 'mid'  && (c.hp <= 60 || c.hp > 90)) return false;
        if (f.hp === 'high' && c.hp <= 90) return false;
      }
      return true;
    });
  });

  stats = computed(() => {
    const counts = { pokemon: 0, trainer: 0, energy: 0 };
    const typeCounts: Record<string, number> = {};
    const currentDeck = this.deck();
    
    currentDeck.forEach((id) => {
      const c = this.cardsMap.get(id);
      if (!c) return;
      if (c.energy) counts.energy++;
      else if (c.type === 'trainer') counts.trainer++;
      else counts.pokemon++;
      typeCounts[c.type] = (typeCounts[c.type] || 0) + 1;
    });
    return { counts, typeCounts, total: currentDeck.length };
  });

  deckGrouped = computed(() => {
    const map = new Map<string, number>();
    this.deck().forEach((id) => map.set(id, (map.get(id) || 0) + 1));
    return [...map.entries()].sort((a, b) => {
      const ca = this.cardsMap.get(a[0]);
      const cb = this.cardsMap.get(b[0]);
      const order = (c?: CardMock) => c?.energy ? 3 : c?.type === 'trainer' ? 2 : 1;
      return order(ca) - order(cb) || a[0].localeCompare(b[0]);
    });
  });

  getCard(id: string): CardMock {
    return this.cardsMap.get(id) || { id, name: 'Cargando...', type: 'colorless', img: '' };
  }

  getCardCount(id: string): number {
    return this.deck().filter((d) => d === id).length;
  }

  addCard(id: string) {
    const currentDeck = this.deck();
    const count = currentDeck.filter((d) => d === id).length;
    const card = this.cardsMap.get(id);
    if (!card) return;
    const isEnergy = card.energy;
    if (!isEnergy && count >= 4) return;
    if (currentDeck.length >= 60) return;
    this.deck.update(d => [...d, id]);
  }

  removeCard(id: string) {
    const currentDeck = this.deck();
    const idx = currentDeck.lastIndexOf(id);
    if (idx < 0) return;
    this.deck.update(d => d.slice(0, idx).concat(d.slice(idx + 1)));
  }

  handleDragStart(e: DragEvent, id: string) {
    if (e.dataTransfer) {
      e.dataTransfer.setData('text/cardId', id);
      e.dataTransfer.effectAllowed = 'copy';
    }
  }

  handleDropDeck(e: DragEvent) {
    e.preventDefault();
    if (e.dataTransfer) {
      const id = e.dataTransfer.getData('text/cardId');
      if (id) this.addCard(id);
    }
  }

  handleDropCollection(e: DragEvent) {
    e.preventDefault();
    if (e.dataTransfer) {
      const id = e.dataTransfer.getData('text/cardId');
      if (id && this.deck().includes(id)) this.removeCard(id);
    }
  }

  allowDrop(e: DragEvent) {
    e.preventDefault();
  }

  toggleType(t: string) {
    const currentTypes = new Set(this.filters().types);
    currentTypes.has(t) ? currentTypes.delete(t) : currentTypes.add(t);
    this.filters.update(f => ({ ...f, types: currentTypes }));
  }

  setKind(kind: 'all' | 'pokemon' | 'trainer' | 'energy') {
    this.filters.update(f => ({ ...f, kind }));
  }

  setHp(hp: 'any' | 'low' | 'mid' | 'high') {
    this.filters.update(f => ({ ...f, hp }));
  }

  setSearch(search: string) {
    this.filters.update(f => ({ ...f, search }));
  }
}
