import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TYPE_COLORS, CardMock } from '../../shared/data/cards.mock';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { EnergyPipComponent } from '../../shared/ui/energy-pip/energy-pip.component';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { DeckService, DeckCardRequest } from '../../core/services/deck.service';

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
  private deckService = inject(DeckService);

  allCards = signal<CardMock[]>([]);
  cardsMap = signal<Record<string, CardMock>>({});
  typeColors = TYPE_COLORS;

  deck = signal<string[]>([]);
  deckName = signal<string>('Mi Mazo XY1');
  isSaving = signal(false);
  saveMessage = signal('');

  filters = signal<Filters>({
    types: new Set(),
    hp: 'any',
    kind: 'all',
    search: '',
  });

  hover = signal<string | null>(null);

  ngOnInit() {
    this.tcgService.getCards().subscribe(cards => {
      const mapped: CardMock[] = cards.map(c => {
        let type = 'colorless';
        if (c.supertype === 'Pokémon' && c.types && c.types.length > 0) {
          type = c.types[0].toLowerCase();
        } else if (c.supertype === 'Trainer') {
          type = 'trainer';
        } else if (c.supertype === 'Energy') {
          const namePart = c.name.split(' ')[0].toLowerCase();
          type = Object.keys(this.typeColors).includes(namePart) ? namePart : 'colorless';
        }

        return {
          id: c.id,
          name: c.name,
          type: type,
          hp: c.hp ? parseInt(c.hp.replace(/[^0-9]/g, ''), 10) : undefined,
          img: c.images?.small || '',
          energy: c.supertype === 'Energy'
        };
      });

      const map: Record<string, CardMock> = {};
      mapped.forEach(c => map[c.id] = c);

      this.allCards.set(mapped);
      this.cardsMap.set(map);
    });
  }

  filtered = computed(() => {
    const f = this.filters();
    return this.allCards().filter((c) => {
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
    const currentMap = this.cardsMap();
    
    currentDeck.forEach((id) => {
      const c = currentMap[id];
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
    const currentMap = this.cardsMap();
    return [...map.entries()].sort((a, b) => {
      const ca = currentMap[a[0]];
      const cb = currentMap[b[0]];
      const order = (c: CardMock) => c?.energy ? 3 : c?.type === 'trainer' ? 2 : 1;
      return order(ca) - order(cb) || a[0].localeCompare(b[0]);
    });
  });

  getCard(id: string): CardMock {
    return this.cardsMap()[id];
  }

  getCardCount(id: string): number {
    return this.deck().filter((d) => d === id).length;
  }

  addCard(id: string) {
    const currentMap = this.cardsMap();
    if (!currentMap[id]) return;

    const currentDeck = this.deck();
    const count = currentDeck.filter((d) => d === id).length;
    const isEnergy = currentMap[id].energy;
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

  saveDeck() {
    if (this.stats().total !== 60 || this.isSaving()) return;
    
    this.isSaving.set(true);
    this.saveMessage.set('');

    const currentDeck = this.deck();
    const map = new Map<string, number>();
    currentDeck.forEach((id) => map.set(id, (map.get(id) || 0) + 1));
    
    const deckCards: DeckCardRequest[] = Array.from(map.entries()).map(([id, qty]) => ({
      cardId: id,
      quantity: qty
    }));

    this.deckService.saveDeck(this.deckName(), deckCards).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.saveMessage.set('¡Mazo guardado correctamente!');
        this.deck.set([]); // Limpiar mazo tras guardarlo
      },
      error: (err) => {
        this.isSaving.set(false);
        this.saveMessage.set('Error al guardar el mazo.');
        console.error(err);
      }
    });
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
