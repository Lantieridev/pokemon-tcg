import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CARDS, TYPE_COLORS, CardMock } from '../../shared/data/cards.mock';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { EnergyPipComponent } from '../../shared/ui/energy-pip/energy-pip.component';

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
export class Deck {
  allCards = Object.values(CARDS);
  typeColors = TYPE_COLORS;

  deck = signal<string[]>([
    'charizard','charizard',
    'charmeleon','charmeleon','charmeleon',
    'charmander','charmander','charmander','charmander',
    'ninetales','ninetales',
    'pikachu','pikachu',
    'raichu',
    'bill','bill','bill','bill',
    'proforak','proforak',
    'computersearch','computersearch',
    'potion','potion','potion',
    'energyremoval','energyremoval',
    'e_fire','e_fire','e_fire','e_fire','e_fire','e_fire','e_fire','e_fire','e_fire','e_fire',
    'e_fire','e_fire','e_fire','e_fire',
    'e_lightning','e_lightning','e_lightning','e_lightning','e_lightning','e_lightning',
  ]);

  filters = signal<Filters>({
    types: new Set(),
    hp: 'any',
    kind: 'all',
    search: '',
  });

  hover = signal<string | null>(null);

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
      const c = CARDS[id];
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
      const ca = CARDS[a[0]];
      const cb = CARDS[b[0]];
      const order = (c: CardMock) => c?.energy ? 3 : c?.type === 'trainer' ? 2 : 1;
      return order(ca) - order(cb) || a[0].localeCompare(b[0]);
    });
  });

  getCard(id: string): CardMock {
    return CARDS[id];
  }

  getCardCount(id: string): number {
    return this.deck().filter((d) => d === id).length;
  }

  addCard(id: string) {
    const currentDeck = this.deck();
    const count = currentDeck.filter((d) => d === id).length;
    const isEnergy = CARDS[id].energy;
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
