import { ChangeDetectionStrategy, Component, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FieldPokemonComponent } from '../../shared/ui/field-pokemon/field-pokemon.component';
import { EnergyPipComponent } from '../../shared/ui/energy-pip/energy-pip.component';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { CARDS } from '../../shared/data/cards.mock';

const INITIAL_LOG = [
  { t:8, who:'BrockSteel', kind:'attack', txt:'Alakazam ataca con Confuse Ray', mine:false },
  { t:8, who:'BrockSteel', kind:'energy', txt:'Energía Psíquica adjuntada a Alakazam', mine:false },
  { t:7, who:'AshRivero',  kind:'attack', txt:'Charizard usa Lanzallamas · 80 daño', mine:true },
  { t:7, who:'AshRivero',  kind:'discard',txt:'2 Energías Fuego descartadas', mine:true },
  { t:7, who:'AshRivero',  kind:'energy', txt:'Energía Fuego adjuntada', mine:true },
  { t:6, who:'BrockSteel', kind:'prize',  txt:'Toma 1 carta de premio', mine:false },
  { t:6, who:'BrockSteel', kind:'ko',     txt:'¡Machamp KO!', mine:false },
  { t:5, who:'AshRivero',  kind:'attack', txt:'Hitmonchan ataca · 40 daño', mine:true },
  { t:5, who:'AshRivero',  kind:'draw',   txt:'Robó carta', mine:true },
  { t:4, who:'BrockSteel', kind:'bench',  txt:'Subió Gyarados a la banca', mine:false },
];

const INITIAL_CHAT = [
  { from:'opp', text:'¡Buena suerte!', t:'19:42' },
  { from:'me',  text:'¡Buena suerte!', t:'19:42' },
  { from:'opp', text:'¡Buena jugada!', t:'19:46' },
  { from:'me',  text:'¡Gracias!',      t:'19:46' },
  { from:'opp', text:'Estuvo cerca…',  t:'19:51' },
];

const QUICK_CHAT = [
  '¡Buena suerte!',
  '¡Buena jugada!',
  '¡Gracias!',
  'Estuvo cerca…',
  'Hmm…',
  '¡GG!',
];

@Component({
  selector: 'app-battle',
  standalone: true,
  imports: [CommonModule, FieldPokemonComponent, EnergyPipComponent, IconComponent],
  templateUrl: './battle.html',
  styleUrl: './battle.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BattleComponent implements AfterViewChecked {
  Math = Math;
  cards = CARDS;
  
  @ViewChild('scrollRef') scrollRef!: ElementRef;
  @ViewChild('chatRef') chatRef!: ElementRef;

  me = signal({
    name: 'AshRivero', tag: 'LIGA ORO · 1842', avatar: 'AR',
    active: { card: 'charizard', energies: ['fire','fire','fire'], damage: 40, status: 'none' },
    bench: [
      { card: 'blastoise',  energies: ['water','water'],           damage: 10 },
      { card: 'pikachu',    energies: ['lightning'],               damage: 0 },
      { card: 'venusaur',   energies: ['grass','grass','grass'],   damage: 0 },
      { card: 'hitmonchan', energies: ['fighting'],                damage: 20 },
      { card: 'mewtwo',     energies: ['psychic','psychic'],       damage: 0 },
    ],
    prizes: [true, true, true, true, false, false],
    deckCount: 28,
    discard: ['charmander','potion','e_fire'],
    hand: ['e_fire','blastoise','bill','e_water','charmeleon','proforak','e_lightning','e_grass','potion'],
  });

  opp = signal({
    name: 'BrockSteel', tag: 'LIGA DIAMANTE · 2104', avatar: 'BS',
    active: { card: 'alakazam', energies: ['psychic','psychic'], damage: 30, status: 'none' },
    bench: [
      { card: 'gyarados',  energies: ['water','water','water'], damage: 20 },
      { card: 'ninetales', energies: ['fire','fire'],            damage: 0 },
      { card: 'machamp',   energies: ['fighting','fighting'],   damage: 50 },
      { card: 'raichu',    energies: ['lightning'],              damage: 0 },
    ],
    prizes: [true, true, true, true, true, false],
    deckCount: 24,
    discardCount: 7,
    handCount: 5,
  });

  turn = signal({ number: 8, owner: 'me', timer: 47 });
  log = signal(INITIAL_LOG);
  chat = signal(INITIAL_CHAT);
  quickChat = QUICK_CHAT;

  menu = signal<any>(null);

  get isMyTurn() { return this.turn().owner === 'me'; }

  get myEmptyBench() { return Array(Math.max(0, 5 - this.me().bench.length)).fill(0); }
  get oppEmptyBench() { return Array(Math.max(0, 5 - this.opp().bench.length)).fill(0); }

  // Array for hand count
  get oppHandArray() { return Array(this.opp().handCount).fill(0); }
  
  get handCount() { return 6; } // Normal density
  get visibleHand() { return this.me().hand.slice(0, this.handCount); }
  get spread() { return 6; }

  ngAfterViewChecked() {
    if (this.scrollRef?.nativeElement) this.scrollRef.nativeElement.scrollTop = 0;
    if (this.chatRef?.nativeElement) this.chatRef.nativeElement.scrollTop = this.chatRef.nativeElement.scrollHeight;
  }

  sendChat(text: string) {
    this.chat.update(c => [...c, { from:'me', text, t: new Date().toTimeString().slice(0,5) }]);
    setTimeout(() => {
      const replies = ['¡Buena suerte!', 'Hmm…', '¡Buena jugada!', '¡GG!'];
      const text = replies[Math.floor(Math.random()*replies.length)];
      this.chat.update(c => [...c, { from:'opp', text, t: new Date().toTimeString().slice(0,5) }]);
    }, 1200);
  }

  openMenu(e: MouseEvent, ent: any) {
    e.stopPropagation();
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    this.menu.set({
      x: rect.right,
      y: rect.top + rect.height / 2,
      cardId: ent.card,
      damage: ent.damage,
    });
  }

  closeMenu() {
    this.menu.set(null);
  }

  onLeave() {
    console.log('Leave match');
  }

  getKindStyles(kind: string) {
    const kindStyles: Record<string, { color: string, label: string }> = {
      attack:  { color:'var(--p-red)',    label:'ATQ' },
      energy:  { color:'var(--p-yellow)', label:'ENG' },
      discard: { color:'#9aa9c7',         label:'DSC' },
      prize:   { color:'#5ad27a',         label:'PRZ' },
      ko:      { color:'#fff',            label:'KO!' },
      draw:    { color:'#7aa3ff',         label:'DRW' },
      bench:   { color:'#c87bff',         label:'BNC' },
    };
    return kindStyles[kind] || kindStyles['attack'];
  }
}
