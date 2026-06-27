import { Injectable, computed, signal } from '@angular/core';
import { PokemonTcgCard } from '../models/game-state.models';

const BASIC_ENERGY_SUBTYPES = ['Basic Energy', 'Special Energy'];
const BASIC_ENERGY_NAMES = [
  'Fire Energy', 'Water Energy', 'Grass Energy', 'Lightning Energy',
  'Psychic Energy', 'Fighting Energy', 'Darkness Energy', 'Metal Energy',
  'Fairy Energy', 'Dragon Energy', 'Colorless Energy',
];

/** Retorna true si la carta es una Energía Básica (sin límite de copias) */
function isBasicEnergy(card: PokemonTcgCard): boolean {
  return (
    card.supertype === 'Energy' &&
    (card.subtypes.some((s) => BASIC_ENERGY_SUBTYPES.includes(s)) ||
      BASIC_ENERGY_NAMES.some((n) => card.name.includes(n)))
  );
}

/** Retorna true si la carta es un Pokémon Básico */
function isBasicPokemon(card: PokemonTcgCard): boolean {
  return card.supertype === 'Pokémon' && card.subtypes.includes('Basic');
}

/** Retorna true si la carta es AS TÁCTICO (ACE SPEC) */
function isAceSpec(card: PokemonTcgCard): boolean {
  return card.subtypes.includes('ACE SPEC');
}

export interface DeckValidation {
  errors: string[];
  isValid: boolean;
}

@Injectable({ providedIn: 'root' })
export class DeckStore {
  /** Lista completa de cartas en el mazo (con repeticiones para múltiples copias) */
  private _deckCards = signal<PokemonTcgCard[]>([]);

  /** Nombre del mazo actual */
  readonly deckName = signal('Mi Mazo');

  // ── Selectors (computed) ──────────────────────────────────────────────────

  readonly deckCards = computed(() => this._deckCards());

  readonly totalCount = computed(() => this._deckCards().length);

  /** Mapa: nombre de carta → cantidad en el mazo */
  readonly cardCounts = computed(() => {
    const map = new Map<string, number>();
    for (const c of this._deckCards()) {
      map.set(c.name, (map.get(c.name) ?? 0) + 1);
    }
    return map;
  });

  /** Cantidad agrupada por id (para mostrar badge en la colección) */
  readonly cardCountById = computed(() => {
    const map = new Map<string, number>();
    for (const c of this._deckCards()) {
      map.set(c.id, (map.get(c.id) ?? 0) + 1);
    }
    return map;
  });

  /** Cartas agrupadas para el panel de mazo (id → {card, count}) */
  readonly deckGrouped = computed(() => {
    const map = new Map<string, { card: PokemonTcgCard; count: number }>();
    for (const c of this._deckCards()) {
      const existing = map.get(c.id);
      if (existing) {
        existing.count++;
      } else {
        map.set(c.id, { card: c, count: 1 });
      }
    }
    return [...map.values()].sort((a, b) => {
      const order = (card: PokemonTcgCard) =>
        card.supertype === 'Energy' ? 3 : card.supertype === 'Trainer' ? 2 : 1;
      return order(a.card) - order(b.card) || a.card.name.localeCompare(b.card.name);
    });
  });

  // ── Validaciones RF-04 ────────────────────────────────────────────────────

  readonly aceSpecCount = computed(() =>
    this._deckCards().filter(isAceSpec).length
  );

  readonly basicPokemonCount = computed(() =>
    this._deckCards().filter(isBasicPokemon).length
  );

  readonly validation = computed<DeckValidation>(() => {
    const errors: string[] = [];
    const total = this.totalCount();
    const counts = this.cardCounts();

    // RF-04 Regla 1: 60 cartas exactas
    if (total < 60) {
      errors.push(`Te faltan ${60 - total} carta${60 - total !== 1 ? 's' : ''} para llegar a 60.`);
    } else if (total > 60) {
      errors.push(`Tenés ${total - 60} carta${total - 60 !== 1 ? 's' : ''} de más (máximo 60).`);
    }

    // RF-04 Regla 2: Máximo 4 copias (excepto Energías Básicas)
    const violations: string[] = [];
    for (const [name, count] of counts) {
      const card = this._deckCards().find((c) => c.name === name);
      if (card && !isBasicEnergy(card) && count > 4) {
        violations.push(`${name} (${count}/4)`);
      }
    }
    if (violations.length > 0) {
      errors.push(`Demasiadas copias: ${violations.join(', ')}.`);
    }

    // RF-04 Regla 3: Al menos 1 Pokémon Básico
    if (this.basicPokemonCount() === 0) {
      errors.push('Necesitás al menos 1 Pokémon Básico.');
    }

    // RF-04 Regla 4: Máximo 1 AS TÁCTICO (ACE SPEC)
    if (this.aceSpecCount() > 1) {
      errors.push(`Solo podés tener 1 carta AS TÁCTICO (tenés ${this.aceSpecCount()}).`);
    }

    return { errors, isValid: errors.length === 0 };
  });

  readonly isValid = computed(() => this.validation().isValid);

  // ── Acciones ──────────────────────────────────────────────────────────────

  /** Agrega una carta al mazo respetando las reglas */
  addCard(card: PokemonTcgCard): void {
    const total = this.totalCount();
    if (total >= 60) return;

    const count = this.cardCounts().get(card.name) ?? 0;
    if (!isBasicEnergy(card) && count >= 4) return;

    this._deckCards.update((d) => [...d, card]);
  }

  /** Elimina UNA copia de la carta del mazo */
  removeCard(cardId: string): void {
    const deck = this._deckCards();
    // findLastIndex no disponible en ES2022 — búsqueda manual desde el final
    let idx = -1;
    for (let i = deck.length - 1; i >= 0; i--) {
      if (deck[i].id === cardId) { idx = i; break; }
    }
    if (idx < 0) return;
    this._deckCards.update((d) => d.filter((_, i) => i !== idx));
  }

  /** Elimina TODAS las copias de una carta */
  removeAllCopies(cardId: string): void {
    this._deckCards.update((d) => d.filter((c) => c.id !== cardId));
  }

  /** Limpia el mazo completo */
  clearDeck(): void {
    this._deckCards.set([]);
  }

  /** Carga un mazo preexistente (ej. desde la API) */
  loadDeck(cards: PokemonTcgCard[], name?: string): void {
    this._deckCards.set(cards);
    if (name) this.deckName.set(name);
  }
}
