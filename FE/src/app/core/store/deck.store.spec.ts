import { TestBed } from '@angular/core/testing';
import { DeckStore } from './deck.store';
import { PokemonTcgCard } from '../models/game-state.models';

describe('DeckStore', () => {
  let store: DeckStore;

  const mockPokemon: PokemonTcgCard = {
    id: 'xy1-1',
    name: 'Ivysaur',
    supertype: 'Pokémon',
    subtypes: ['Basic'],
    types: ['Grass'],
    images: { small: 'ivy.png', large: 'ivy_large.png' },
    set: { id: 'xy1' }
  };

  const mockAceSpec: PokemonTcgCard = {
    id: 'xy1-100',
    name: 'Computer Search',
    supertype: 'Trainer',
    subtypes: ['Item', 'ACE SPEC'],
    images: { small: 'search.png', large: 'search_large.png' },
    set: { id: 'xy1' }
  };

  const mockBasicEnergy: PokemonTcgCard = {
    id: 'xy1-2',
    name: 'Fire Energy',
    supertype: 'Energy',
    subtypes: ['Basic Energy'],
    types: ['Fire'],
    images: { small: 'fire.png', large: 'fire_large.png' },
    set: { id: 'xy1' }
  };

  const mockSpecialEnergy: PokemonTcgCard = {
    id: 'xy1-3',
    name: 'Double Colorless Energy',
    supertype: 'Energy',
    subtypes: ['Special Energy'],
    types: ['Colorless'],
    images: { small: 'double.png', large: 'double_large.png' },
    set: { id: 'xy1' }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DeckStore]
    });
    store = TestBed.inject(DeckStore);
  });

  it('should initialize with empty deck', () => {
    expect(store.totalCount()).toBe(0);
    expect(store.deckCards().length).toBe(0);
    expect(store.deckName()).toBe('');
    expect(store.isValid()).toBeFalse();
  });

  it('should add and remove cards, updating counts', () => {
    store.addCard(mockPokemon);
    expect(store.totalCount()).toBe(1);
    expect(store.cardCounts().get('Ivysaur')).toBe(1);
    expect(store.cardCountById().get('xy1-1')).toBe(1);

    store.addCard(mockPokemon);
    expect(store.totalCount()).toBe(2);
    expect(store.cardCounts().get('Ivysaur')).toBe(2);

    store.removeCard('xy1-1');
    expect(store.totalCount()).toBe(1);
    expect(store.cardCounts().get('Ivysaur')).toBe(1);
  });

  it('should remove all copies of a card', () => {
    store.addCard(mockPokemon);
    store.addCard(mockPokemon);
    store.addCard(mockBasicEnergy);
    expect(store.totalCount()).toBe(3);

    store.removeAllCopies('xy1-1');
    expect(store.totalCount()).toBe(1);
    expect(store.cardCounts().get('Ivysaur')).toBeUndefined();
    expect(store.cardCounts().get('Fire Energy')).toBe(1);
  });

  it('should clear all cards', () => {
    store.addCard(mockPokemon);
    store.addCard(mockBasicEnergy);
    expect(store.totalCount()).toBe(2);

    store.clearDeck();
    expect(store.totalCount()).toBe(0);
  });

  it('should load pre-existing deck', () => {
    store.loadDeck([mockPokemon, mockBasicEnergy], 'Water Power');
    expect(store.totalCount()).toBe(2);
    expect(store.deckName()).toBe('Water Power');
  });

  it('should load deck from DTO format using a catalog', () => {
    const dtos = [
      { cardId: 'xy1-1', quantity: 2 },
      { cardId: 'xy1-2', quantity: 5 }
    ];
    const catalog = [mockPokemon, mockBasicEnergy];

    store.loadFromRequestDTOs(dtos, catalog, 'Loaded Deck');
    expect(store.totalCount()).toBe(7);
    expect(store.deckName()).toBe('Loaded Deck');
    expect(store.cardCountById().get('xy1-1')).toBe(2);
    expect(store.cardCountById().get('xy1-2')).toBe(5);
  });

  it('should enforce rule validation (60 cards exact, basic pokemon presence, ace spec limit, 4 copy limit)', () => {
    // 1. Initially empty - invalid (lacks pokemon, not 60 cards)
    let validation = store.validation();
    expect(validation.isValid).toBeFalse();
    expect(validation.errors).toContain('Te faltan 60 cartas para llegar a 60.');
    expect(validation.errors).toContain('Necesitás al menos 1 Pokémon Básico.');

    // 2. Add 60 energies - invalid (lacks basic pokemon)
    const sixtyEnergies = Array(60).fill(mockBasicEnergy);
    store.loadDeck(sixtyEnergies, 'Only Energy');
    validation = store.validation();
    expect(validation.isValid).toBeFalse();
    expect(validation.errors.length).toBe(1);
    expect(validation.errors[0]).toBe('Necesitás al menos 1 Pokémon Básico.');

    // 3. Add 1 basic pokemon + 59 energies - VALID
    const validDeck = [mockPokemon, ...Array(59).fill(mockBasicEnergy)];
    store.loadDeck(validDeck, 'Valid Deck');
    validation = store.validation();
    expect(validation.isValid).toBeTrue();
    expect(validation.errors.length).toBe(0);

    // 4. Exceed 60 cards
    const sixtyOneCards = [mockPokemon, ...Array(60).fill(mockBasicEnergy)];
    store.loadDeck(sixtyOneCards, 'Too Many');
    validation = store.validation();
    expect(validation.isValid).toBeFalse();
    expect(validation.errors[0]).toBe('Tenés 1 carta de más (máximo 60).');

    // 5. Exceed 4 copies of a non-basic-energy card (like pokemon)
    // Let's create a deck of 5 pokemon and 55 basic energies
    const tooManyPokemons = [
      ...Array(5).fill(mockPokemon),
      ...Array(55).fill(mockBasicEnergy)
    ];
    store.loadDeck(tooManyPokemons, 'Too Many Pokemons');
    validation = store.validation();
    expect(validation.isValid).toBeFalse();
    expect(validation.errors[0]).toContain('Demasiadas copias: Ivysaur (5/4)');

    // 6. Exceed 1 ACE SPEC card
    const tooManyAceSpecs = [
      mockPokemon,
      mockAceSpec,
      mockAceSpec,
      ...Array(57).fill(mockBasicEnergy)
    ];
    store.loadDeck(tooManyAceSpecs, 'Too Many Ace Specs');
    validation = store.validation();
    expect(validation.isValid).toBeFalse();
    expect(validation.errors).toContain('Solo podés tener 1 carta AS TÁCTICO (tenés 2).');
  });

  it('should prevent adding non-basic-energy card beyond 4 copies via addCard', () => {
    // Fill with 3 pokemon
    for (let i = 0; i < 3; i++) {
      store.addCard(mockPokemon);
    }
    expect(store.totalCount()).toBe(3);

    // Adding 4th copy - allowed
    store.addCard(mockPokemon);
    expect(store.totalCount()).toBe(4);

    // Adding 5th copy - blocked
    store.addCard(mockPokemon);
    expect(store.totalCount()).toBe(4);
  });
});
