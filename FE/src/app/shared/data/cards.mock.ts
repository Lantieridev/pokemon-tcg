// src/app/shared/data/cards.mock.ts

const IMG = (set: string, n: number, hi = true) =>
  `https://images.pokemontcg.io/${set}/${n}${hi ? '_hires' : ''}.png`;

export interface CardMock {
  id: string;
  name: string;
  type: string;
  hp?: number;
  img: string;
  energy?: boolean;
}

export const CARDS: Record<string, CardMock> = {
  // ── Active / Bench Pokémon ────────────────────────────────────────────────
  charizard:   { id:'charizard',   name:'Charizard',   type:'fire',     hp:120, img: IMG('base1', 4) },
  blastoise:   { id:'blastoise',   name:'Blastoise',   type:'water',    hp:100, img: IMG('base1', 2) },
  venusaur:    { id:'venusaur',    name:'Venusaur',    type:'grass',    hp:100, img: IMG('base1', 15) },
  pikachu:     { id:'pikachu',     name:'Pikachu',     type:'lightning', hp:40, img: IMG('base1', 58, false) },
  raichu:      { id:'raichu',      name:'Raichu',      type:'lightning', hp:80, img: IMG('base1', 14) },
  alakazam:    { id:'alakazam',    name:'Alakazam',    type:'psychic',  hp:80,  img: IMG('base1', 1) },
  machamp:     { id:'machamp',     name:'Machamp',     type:'fighting', hp:100, img: IMG('base1', 8) },
  mewtwo:      { id:'mewtwo',      name:'Mewtwo',      type:'psychic',  hp:60,  img: IMG('base1', 10) },
  gyarados:    { id:'gyarados',    name:'Gyarados',    type:'water',    hp:100, img: IMG('base1', 6) },
  ninetales:   { id:'ninetales',   name:'Ninetales',   type:'fire',     hp:80,  img: IMG('base1', 12) },
  zapdos:      { id:'zapdos',      name:'Zapdos',      type:'lightning', hp:90, img: IMG('base1', 16) },
  hitmonchan:  { id:'hitmonchan',  name:'Hitmonchan',  type:'fighting', hp:70,  img: IMG('base1', 7) },

  // Stage 1 / basics for variety in deck builder
  charmander:  { id:'charmander',  name:'Charmander',  type:'fire',     hp:50, img: IMG('base1', 46, false) },
  charmeleon:  { id:'charmeleon',  name:'Charmeleon',  type:'fire',     hp:80, img: IMG('base1', 24, false) },
  squirtle:    { id:'squirtle',    name:'Squirtle',    type:'water',    hp:40, img: IMG('base1', 63, false) },
  wartortle:   { id:'wartortle',   name:'Wartortle',   type:'water',    hp:70, img: IMG('base1', 42, false) },
  bulbasaur:   { id:'bulbasaur',   name:'Bulbasaur',   type:'grass',    hp:40, img: IMG('base1', 44, false) },
  ivysaur:     { id:'ivysaur',     name:'Ivysaur',     type:'grass',    hp:60, img: IMG('base1', 30, false) },
  abra:        { id:'abra',        name:'Abra',        type:'psychic',  hp:30, img: IMG('base1', 43, false) },
  kadabra:     { id:'kadabra',     name:'Kadabra',     type:'psychic',  hp:60, img: IMG('base1', 32, false) },
  machop:      { id:'machop',      name:'Machop',      type:'fighting', hp:50, img: IMG('base1', 52, false) },
  machoke:     { id:'machoke',     name:'Machoke',     type:'fighting', hp:80, img: IMG('base1', 34, false) },

  // ── Trainers / Items ──────────────────────────────────────────────────────
  bill:        { id:'bill',        name:'Bill',           type:'trainer', img: IMG('base1', 91, false) },
  proforak:    { id:'proforak',    name:'Professor Oak',  type:'trainer', img: IMG('base1', 88, false) },
  energyremoval:{id:'energyremoval',name:"Energy Removal", type:'trainer', img: IMG('base1', 92, false) },
  computersearch:{id:'computersearch',name:'Computer Search',type:'trainer',img: IMG('base1', 71, false) },
  superenergyremoval:{id:'superenergyremoval',name:'Super Energy Removal', type:'trainer', img: IMG('base1', 79, false) },
  potion:      { id:'potion',      name:'Potion',         type:'trainer', img: IMG('base1', 94, false) },

  // ── Energies ──────────────────────────────────────────────────────────────
  e_grass:     { id:'e_grass',     name:'Grass Energy',    type:'grass',     energy:true, img: 'https://images.pokemontcg.io/xy1/132.png' },
  e_fire:      { id:'e_fire',      name:'Fire Energy',     type:'fire',      energy:true, img: 'https://images.pokemontcg.io/xy1/133.png' },
  e_water:     { id:'e_water',     name:'Water Energy',    type:'water',     energy:true, img: 'https://images.pokemontcg.io/xy1/134.png' },
  e_lightning: { id:'e_lightning', name:'Lightning Energy',type:'lightning', energy:true, img: 'https://images.pokemontcg.io/xy1/135.png' },
  e_psychic:   { id:'e_psychic',   name:'Psychic Energy',  type:'psychic',   energy:true, img: 'https://images.pokemontcg.io/xy1/136.png' },
  e_fighting:  { id:'e_fighting',  name:'Fighting Energy', type:'fighting',  energy:true, img: 'https://images.pokemontcg.io/xy1/137.png' },
  e_darkness:  { id:'e_darkness',  name:'Darkness Energy', type:'darkness',  energy:true, img: 'https://images.pokemontcg.io/xy1/138.png' },
  e_metal:     { id:'e_metal',     name:'Metal Energy',    type:'metal',     energy:true, img: 'https://images.pokemontcg.io/xy1/139.png' },
  e_fairy:     { id:'e_fairy',     name:'Fairy Energy',    type:'fairy',     energy:true, img: 'https://images.pokemontcg.io/xy1/140.png' },
  e_colorless: { id:'e_colorless', name:'Double Colorless',type:'colorless', energy:true, img: 'https://images.pokemontcg.io/xy1/130.png' },
};

export const TYPE_COLORS: Record<string, { hex: string, glow: string, rgb: string }> = {
  fire:      { hex: '#ff7a3d', glow: 'rgba(255,122,61,0.55)', rgb: '255,122,61' },
  water:     { hex: '#4aa3ff', glow: 'rgba(74,163,255,0.55)', rgb: '74,163,255' },
  grass:     { hex: '#5ad27a', glow: 'rgba(90,210,122,0.5)',  rgb: '90,210,122' },
  lightning: { hex: '#ffcc33', glow: 'rgba(255,204,51,0.55)', rgb: '255,204,51' },
  psychic:   { hex: '#c87bff', glow: 'rgba(200,123,255,0.5)', rgb: '200,123,255' },
  fighting:  { hex: '#d97d4a', glow: 'rgba(217,125,74,0.5)',  rgb: '217,125,74' },
  darkness:  { hex: '#5a4a6a', glow: 'rgba(90,74,106,0.5)',    rgb: '90,74,106' },
  metal:     { hex: '#b8b8cc', glow: 'rgba(184,184,204,0.5)',  rgb: '184,184,204' },
  fairy:     { hex: '#ff8fd4', glow: 'rgba(255,143,212,0.5)',  rgb: '255,143,212' },
  dragon:    { hex: '#7038f8', glow: 'rgba(112,56,248,0.5)',   rgb: '112,56,248' },
  colorless: { hex: '#cfd6e4', glow: 'rgba(207,214,228,0.4)', rgb: '207,214,228' },
  trainer:   { hex: '#b5a07a', glow: 'rgba(181,160,122,0.4)', rgb: '181,160,122' },
};
