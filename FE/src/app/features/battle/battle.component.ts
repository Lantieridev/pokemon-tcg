import { ChangeDetectionStrategy, Component, signal, computed, ViewChild, ElementRef, AfterViewChecked, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FieldPokemonComponent } from '../../shared/ui/field-pokemon/field-pokemon.component';
import { EnergyPipComponent } from '../../shared/ui/energy-pip/energy-pip.component';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { CARDS } from '../../shared/data/cards.mock';
import { MatchStore } from '../../core/store/match.store';
import { WebSocketService } from '../../core/services/websocket.service';

const INITIAL_LOG = [
  { t:1, who:'System', kind:'bench', txt:'Partida iniciada', mine:true }
];

const INITIAL_CHAT = [
  { from:'opp', text:'¡Buena suerte!', t:'19:42' },
  { from:'me',  text:'¡Buena suerte!', t:'19:42' },
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
export class BattleComponent implements OnInit, OnDestroy, AfterViewChecked {
  Math = Math;
  cards = CARDS;

  private store = inject(MatchStore);
  private wsService = inject(WebSocketService);
  private router = inject(Router);
  
  @ViewChild('scrollRef') scrollRef!: ElementRef;
  @ViewChild('chatRef') chatRef!: ElementRef;

  // Bind properties to MatchStore computed properties with non-nullable fallback for TS template compiler
  me = computed(() => this.store.me() || {
    name: 'AshRivero', tag: 'LIGA ORO', avatar: 'AR',
    active: null,
    bench: [] as any[],
    prizes: Array(6).fill(true) as boolean[],
    deckCount: 60,
    discard: [] as string[],
    hand: [] as string[],
    handCount: 0,
    discardCount: 0
  });

  opp = computed(() => this.store.opp() || {
    name: 'GarryBot', tag: 'LIGA PLATA', avatar: 'GB',
    active: null,
    bench: [] as any[],
    prizes: Array(6).fill(true) as boolean[],
    deckCount: 60,
    handCount: 0,
    discardCount: 0
  });

  turn = computed(() => this.store.turn() || { number: 0, owner: 'me', timer: 60 });

  log = signal(INITIAL_LOG);
  chat = signal(INITIAL_CHAT);
  quickChat = QUICK_CHAT;

  menu = signal<any>(null);

  ngOnInit() {
    if (!this.store.isLoaded()) {
      this.router.navigate(['/lobby']);
      return;
    }
    const matchId = this.store.matchId();
    if (matchId) {
      this.wsService.connect(matchId).subscribe(state => {
        this.store.updateState(state);
        this.log.update(l => [
          ...l, 
          { t: state.version, who: 'System', kind: 'draw', txt: `Nueva actualización de estado (V${state.version})`, mine: true }
        ]);
      });
    }
  }

  ngOnDestroy() {
    this.wsService.disconnect();
  }

  get isMyTurn() { return this.turn()?.owner === 'me'; }

  get myEmptyBench() { return Array(Math.max(0, 5 - (this.me()?.bench?.length || 0))).fill(0); }
  get oppEmptyBench() { return Array(Math.max(0, 5 - (this.opp()?.bench?.length || 0))).fill(0); }

  // Array for hand count
  get oppHandArray() { return Array(this.opp()?.handCount || 0).fill(0); }
  
  get handCount() { return 6; } // Normal density
  get visibleHand() { return this.me()?.hand?.slice(0, this.handCount) || []; }
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
