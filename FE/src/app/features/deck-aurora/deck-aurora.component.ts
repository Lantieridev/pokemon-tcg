import { Component, OnInit, computed, inject, signal, ViewEncapsulation, ChangeDetectorRef } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { DeckStore } from '../../core/store/deck.store';
import { DeckApiService } from '../deck/deck-api.service';
import { PokemonTcgCard, DeckSummaryDTO } from '../../core/models/game-state.models';
import { RouterModule } from '@angular/router';
import { LogoComponent, TrainerChipComponent, AmbientComponent, IconComponent, EnergyTypeComponent } from '../lobby-aurora/ui/aurora-ui.components';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService, UserProfileResponseDTO } from '../../core/services/profile.service';
import { TutorialService } from '../../core/services/tutorial.service';

interface Filters {
  search: string;
  supertype: 'all' | 'Pokémon' | 'Trainer' | 'Energy';
  subtype: string;
}

@Component({
  selector: 'app-deck-aurora',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LogoComponent, TrainerChipComponent, AmbientComponent, IconComponent, NgOptimizedImage],
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

      <!-- Topbar spacer -->
      <div style="flex: 0 0 auto; height: 92px; z-index: 10;"></div>

      <div class="fu" style="flex: 1; display: flex; gap: 24px; padding: 0 44px 44px; z-index: 5; height: calc(100vh - 92px);">
        <!-- VISTA 1: LISTADO DE MAZOS -->
        @if (currentView() === 'list') {
          <div style="flex: 1; display: flex; flex-direction: column; background: var(--surface); border: 1px solid var(--line); border-radius: 24px; backdrop-filter: blur(10px); padding: 40px; overflow-y: auto;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px;">
              <h1 style="font-family: var(--display); font-size: 42px; margin: 0; color: var(--txt);">Tus Mazos</h1>
            </div>
            
            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 24px;">
              <!-- Tarjeta Crear Nuevo -->
              <div class="create-card" (click)="currentView.set('create_options')">
                <div style="font-size: 48px; color: var(--accent); margin-bottom: 10px;">+</div>
                <div style="font-family: 'Manrope'; font-weight: 700; font-size: 18px; color: var(--txt);">Crear Mazo</div>
              </div>

              <!-- Lista de Mazos -->
              @for (deck of decks(); track deck.id) {
                <div class="deck-list-card">
                  <div style="flex: 1;" (click)="openDeck(deck.id)">
                    <div style="font-family: 'Manrope'; font-weight: 800; font-size: 20px; color: var(--txt); margin-bottom: 8px;">{{ deck.name }}</div>
                    <div style="font-size: 13px; color: var(--mut); margin-bottom: 4px;">{{ deck.totalCards }}/60 Cartas</div>
                    <div style="font-size: 12px; font-weight: 700; display: inline-block; padding: 4px 10px; border-radius: 10px;"
                         [style.background]="deck.status === 'VALID' ? 'rgba(70, 224, 138, 0.2)' : 'rgba(255, 122, 61, 0.2)'"
                         [style.color]="deck.status === 'VALID' ? '#46e08a' : '#ff7a3d'">
                      {{ deck.status === 'VALID' ? 'Válido' : 'Borrador' }}
                    </div>
                  </div>
                  <button class="delete-btn" (click)="$event.stopPropagation(); deleteDeck(deck.id)" title="Eliminar Mazo">✕</button>
                </div>
              }
            </div>
          </div>
        }

        <!-- VISTA 2: OPCIONES DE CREACIÓN -->
        @if (currentView() === 'create_options') {
          <div style="flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; background: var(--surface); border: 1px solid var(--line); border-radius: 24px; backdrop-filter: blur(10px); padding: 40px;">
            <button class="ghost-btn" style="position: absolute; top: 30px; left: 30px;" (click)="currentView.set('list')">← Volver</button>
            <h2 style="font-family: var(--display); font-size: 36px; margin-bottom: 40px;">Elegí el modo de creación</h2>
            
            <div style="display: flex; gap: 30px;">
              <div class="mode-card" (click)="startCustomDeck()">
                <div style="font-size: 64px; margin-bottom: 20px;">🛠️</div>
                <h3 style="font-family: 'Manrope'; font-size: 24px; margin: 0 0 10px;">Personalizado</h3>
                <p style="color: var(--mut); font-size: 14px; text-align: center;">Armá tu mazo carta por carta con total libertad.</p>
              </div>
              <div class="mode-card" (click)="currentView.set('wizard_options')">
                <div style="font-size: 64px; margin-bottom: 20px;">🧠</div>
                <h3 style="font-family: 'Manrope'; font-size: 24px; margin: 0 0 10px;">Inteligente</h3>
                <p style="color: var(--mut); font-size: 14px; text-align: center;">El asistente creará un mazo legal y balanceado para vos.</p>
              </div>
            </div>
          </div>
        }

        <!-- VISTA 3: SELECCIÓN DE TIPO (WIZARD) -->
        @if (currentView() === 'wizard_options') {
          <div style="flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; background: var(--surface); border: 1px solid var(--line); border-radius: 24px; backdrop-filter: blur(10px); padding: 40px; position: relative;">
            <button class="ghost-btn" style="position: absolute; top: 30px; left: 30px;" (click)="currentView.set('create_options')">← Volver</button>
            
            <h2 style="font-family: var(--display); font-size: 36px; margin-bottom: 10px;">¿De qué tipo querés tu mazo?</h2>
            <p style="color: var(--mut); margin-bottom: 40px;">Seleccioná un elemento para que el Asistente construya tu mazo base.</p>
            
            @if (generatingWizard()) {
              <div style="display: flex; flex-direction: column; align-items: center; justify-content: center;">
                <div class="pokespin" style="width: 60px; height: 60px; margin-bottom: 20px;"></div>
                <div style="font-family: 'Manrope'; font-weight: 700; font-size: 18px; color: var(--accent);">El Asistente está construyendo el mazo...</div>
              </div>
            } @else {
              <div style="display: flex; flex-wrap: wrap; justify-content: center; gap: 20px; max-width: 800px; margin-bottom: 40px;">
                @for (t of wizardTypes; track t.id) {
                  <div class="type-circle-btn" [class.selected]="selectedWizardTypes().includes(t.id)" [style.--tcolor]="t.color" (click)="toggleWizardType(t.id)">
                    <div class="type-circle" [style.background]="t.color">
                      <span class="type-emoji">{{ t.emoji }}</span>
                    </div>
                    <span class="type-name">{{ t.label }}</span>
                  </div>
                }
              </div>
              
              <button class="cta" style="height: 56px; font-size: 18px; padding: 0 40px;" [disabled]="selectedWizardTypes().length === 0" (click)="generateWizardDeck()">
                ✨ Construir Mazo Inteligente
              </button>
            }
          </div>
        }

        <!-- VISTA 4: CONSTRUCTOR -->
        @if (currentView() === 'builder') {
          <!-- Left: Collection & Filters -->
          <div style="flex: 1; display: flex; flex-direction: column; background: var(--surface); border: 1px solid var(--line); border-radius: 24px; backdrop-filter: blur(10px); overflow: hidden;">
            <div style="padding: 20px 24px; border-bottom: 1px solid var(--line); display: flex; gap: 16px; align-items: center;">
              <button class="ghost-btn sm" (click)="currentView.set('list')">← Volver</button>
              <div style="position: relative; flex: 1; max-width: 300px;">
                <input type="text" [ngModel]="filters().search" (ngModelChange)="setFilter('search', $event)" placeholder="Buscar carta..." style="width: 100%; background: rgba(255,255,255,0.05); border: 1px solid var(--line); color: var(--txt); padding: 10px 16px; border-radius: 12px; outline: none; font-family: 'Manrope'; font-size: 14px;" />
              </div>
              
              <div style="display: flex; gap: 8px; background: rgba(0,0,0,0.2); padding: 4px; border-radius: 14px; border: 1px solid var(--line); align-items: center;">
                <button [class.active-tab]="filters().supertype === 'all'" (click)="setSupertype('all')" class="tab-btn">Todos</button>
                <button [class.active-tab]="filters().supertype === 'Pokémon'" (click)="setSupertype('Pokémon')" class="tab-btn">Pokémon</button>
                <button [class.active-tab]="filters().supertype === 'Trainer'" (click)="setSupertype('Trainer')" class="tab-btn">Trainer</button>
                <button [class.active-tab]="filters().supertype === 'Energy'" (click)="setSupertype('Energy')" class="tab-btn">Energy</button>
                <button class="help-trigger-btn" (click)="triggerHelp()" title="Ver Tutorial">
                  <aurora-icon n="help" [s]="13"></aurora-icon>
                </button>
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
                <div style="flex: 1; display: flex; align-items: center; border-bottom: 2px dashed rgba(255,255,255,0.2); padding-bottom: 4px; margin-right: 16px; transition: border-color 0.2s;">
                  <input [ngModel]="deckName()" (ngModelChange)="deckStore.deckName.set($event)" style="font-family: var(--display); font-size: 28px; background: transparent; border: none; color: var(--txt); outline: none; width: 100%; font-weight: 700;" placeholder="Nombre del mazo" />
                  <span style="font-size: 16px; opacity: 0.5; margin-left: 8px;">✏️</span>
                </div>
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
            <div id="cartas-mazo" class="scroll" style="flex: 1; overflow-y: auto; padding: 20px;">
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
                <div style="color: #46e08a; font-size: 12px; margin-bottom: 10px; text-align: center; font-weight: 600;">¡Guardado exitoso!</div>
              }
              <button id="boton-autocompletar" class="cta secondary" style="width: 100%; height: 48px; justify-content: center; gap: 8px; background: rgba(255,255,255,0.1); border: 1px solid var(--line); margin-bottom: 10px;" [disabled]="autoCompleting()" (click)="autoComplete()">
                @if (autoCompleting()) {
                  <div class="pokespin" style="width: 20px; height: 20px;"></div>
                } @else {
                  <aurora-icon n="decks" [s]="20"></aurora-icon>
                  <span>Autocompletar</span>
                }
              </button>
              <div style="display: flex; gap: 10px;">
                <!-- Botón Borrador -->
                <button class="ghost-btn" style="flex: 1; height: 48px; border: 1px solid rgba(255,255,255,0.2);" [disabled]="saveLoading() || totalCount() === 0" (click)="saveDraft()">
                  Borrador
                </button>
                <!-- Botón Mazo Válido -->
                <button class="cta" style="flex: 1; height: 48px; justify-content: center;" [disabled]="!isValid() || saveLoading()" (click)="saveValidDeck()" [style.opacity]="!isValid() ? '0.5' : '1'">
                  Guardar
                </button>
              </div>
              @if (!isValid()) {
                <div style="text-align: center; font-size: 11px; color: var(--mut); margin-top: 10px;">{{ validation().errors[0] || 'Necesitas 60 cartas para guardar como válido.' }}</div>
              }
            </div>
          </div>
        }
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

      .help-trigger-btn {
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 50%;
        width: 26px;
        height: 26px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        color: var(--accent2, #fbbf24);
        cursor: pointer;
        transition: all 0.2s ease;
        padding: 0;
        margin-left: 8px;
        flex-shrink: 0;
      }
      .help-trigger-btn:hover {
        background: rgba(255, 255, 255, 0.12);
        border-color: var(--accent2, #fbbf24);
        box-shadow: 0 0 8px rgba(251, 191, 36, 0.35);
      }

      /* Nuevos estilos */
      .create-card { border: 2px dashed rgba(255,255,255,0.2); border-radius: 16px; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; cursor: pointer; transition: all 0.2s; background: rgba(0,0,0,0.2); }
      .create-card:hover { border-color: var(--accent); background: rgba(255,46,62,0.1); transform: translateY(-4px); }
      
      .deck-list-card { background: rgba(255,255,255,0.05); border: 1px solid var(--line); border-radius: 16px; padding: 24px; cursor: pointer; transition: all 0.2s; display: flex; flex-direction: column; position: relative; }
      .deck-list-card:hover { background: rgba(255,255,255,0.1); transform: translateY(-4px); box-shadow: 0 10px 20px rgba(0,0,0,0.3); }
      .delete-btn { position: absolute; top: 16px; right: 16px; background: rgba(255,0,0,0.2); border: none; color: #ff4a4a; width: 32px; height: 32px; border-radius: 50%; font-weight: bold; cursor: pointer; opacity: 0; transition: all 0.2s; }
      .deck-list-card:hover .delete-btn { opacity: 1; }
      .delete-btn:hover { background: rgba(255,0,0,0.5); color: white; transform: scale(1.1); }

      .mode-card { width: 300px; padding: 40px; background: rgba(255,255,255,0.05); border: 1px solid var(--line); border-radius: 20px; display: flex; flex-direction: column; align-items: center; cursor: pointer; transition: all 0.2s; }
      .mode-card:hover { background: rgba(255,255,255,0.1); transform: translateY(-8px); border-color: var(--accent); box-shadow: 0 16px 32px rgba(255,46,62,0.2); }

      .type-circle-btn { display: flex; flex-direction: column; align-items: center; gap: 12px; cursor: pointer; transition: transform 0.2s; opacity: 0.6; filter: grayscale(0.5); }
      .type-circle-btn:hover { transform: translateY(-8px) scale(1.05); opacity: 0.8; filter: grayscale(0.2); }
      .type-circle-btn.selected { opacity: 1; filter: grayscale(0); transform: translateY(-8px) scale(1.1); }
      .type-circle { width: 100px; height: 100px; border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 10px 20px rgba(0,0,0,0.5); border: 4px solid rgba(255,255,255,0.2); transition: all 0.2s; }
      .type-circle-btn:hover .type-circle { border-color: rgba(255,255,255,0.5); box-shadow: 0 15px 30px var(--tcolor); }
      .type-circle-btn.selected .type-circle { border-color: white; box-shadow: 0 0 40px var(--tcolor), inset 0 0 20px rgba(255,255,255,0.5); }
      .type-emoji { font-size: 48px; }
      .type-name { font-family: 'Manrope'; font-weight: 700; color: var(--txt); text-transform: uppercase; letter-spacing: 1px; font-size: 14px; }
    </style>
  `
})
export class DeckAuroraComponent implements OnInit {
  readonly tcgService = inject(PokemonTcgService);
  readonly deckStore = inject(DeckStore);
  private tutorialService = inject(TutorialService);
  private deckApi = inject(DeckApiService);
  private authService = inject(AuthService);
  private profileService = inject(ProfileService);

  private cdr = inject(ChangeDetectorRef);

  // --- Views y Estado ---
  currentView = signal<'list' | 'create_options' | 'wizard_options' | 'builder'>('list');
  decks = signal<DeckSummaryDTO[]>([]);
  editingDeckId = signal<number | null>(null);
  generatingWizard = signal(false);
  selectedWizardTypes = signal<string[]>([]);

  readonly wizardTypes = [
    { id: 'fire', label: 'Fuego', color: '#ff7a3d', emoji: '🔥' },
    { id: 'water', label: 'Agua', color: '#4aa3ff', emoji: '💧' },
    { id: 'grass', label: 'Planta', color: '#5ad27a', emoji: '🌿' },
    { id: 'lightning', label: 'Eléctrico', color: '#ffcc33', emoji: '⚡' },
    { id: 'psychic', label: 'Psíquico', color: '#c87bff', emoji: '🔮' },
    { id: 'fighting', label: 'Lucha', color: '#d97d4a', emoji: '✊' },
    { id: 'darkness', label: 'Siniestro', color: '#5a4a6a', emoji: '🌑' },
    { id: 'metal', label: 'Metálico', color: '#b8b8cc', emoji: '⚙️' },
    { id: 'dragon', label: 'Dragón', color: '#7038f8', emoji: '🐉' },
    { id: 'fairy', label: 'Hada', color: '#ff8fd4', emoji: '✨' },
    { id: 'colorless', label: 'Incoloro', color: '#cfd6e4', emoji: '⚪' }
  ];

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
    this.tutorialService.triggerTutorial('deck');
    if (this.username !== 'Invitado') {
      this.loadDecks();
    }
  }

  loadDecks() {
    const userId = this.authService.userId;
    if (userId) {
      this.deckApi.getDecksByUserId(userId).subscribe({
        next: (res) => {
          this.decks.set(res);
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Error fetching decks', err)
      });
    }
  }

  startCustomDeck() {
    this.deckStore.clearDeck();
    this.deckStore.deckName.set('Mi Mazo Personalizado');
    this.editingDeckId.set(null);
    this.currentView.set('builder');
  }

  openDeck(deckId: number) {
    this.deckApi.getDeckById(deckId).subscribe({
      next: (deck) => {
        this.deckStore.clearDeck();
        this.deckStore.deckName.set(deck.name);
        this.editingDeckId.set(deck.id);
        this.deckStore.loadFromRequestDTOs(deck.cards, this.tcgService.cards(), deck.name);
        this.currentView.set('builder');
      },
      error: (err) => console.error('Error loading deck details', err)
    });
  }

  deleteDeck(deckId: number) {
    if (confirm('¿Estás seguro de que querés eliminar este mazo?')) {
      this.deckApi.deleteDeck(deckId).subscribe({
        next: () => {
          this.loadDecks();
        },
        error: (err) => alert('Error eliminando mazo: ' + err.message)
      });
    }
  }

  toggleWizardType(typeId: string) {
    this.selectedWizardTypes.update((types) => {
      if (types.includes(typeId)) {
        return types.filter(t => t !== typeId);
      } else {
        return [...types, typeId];
      }
    });
  }

  generateWizardDeck() {
    const types = this.selectedWizardTypes();
    if (types.length === 0) return;
    
    // Join the types to pass a combined theme, e.g., "fire y water"
    const theme = types.join(' y ');

    this.generatingWizard.set(true);
    this.deckApi.generateWizardDeck(theme).subscribe({
      next: (cards) => {
        this.deckStore.clearDeck();
        this.deckStore.loadFromRequestDTOs(cards, this.tcgService.cards(), `Mazo Inteligente: ${theme.toUpperCase()}`);
        this.editingDeckId.set(null);
        this.generatingWizard.set(false);
        this.selectedWizardTypes.set([]); // Reset for next time
        this.currentView.set('builder');
      },
      error: (err) => {
        console.error('Wizard error', err);
        alert('Error al generar mazo con el Asistente.');
        this.generatingWizard.set(false);
      }
    });
  }

  saveValidDeck() {
    this.doSave('VALID');
  }

  saveDraft() {
    this.doSave('DRAFT');
  }

  private doSave(status: 'VALID' | 'DRAFT') {
    this.saveLoading.set(true);
    this.saveError.set(null);
    this.saveSuccess.set(false);

    const name = this.deckName();
    const id = this.editingDeckId();

    const request = id ? this.deckApi.updateDeck(id, name, status) : this.deckApi.saveDeck(name, status);

    request.subscribe({
      next: (res) => {
        this.saveLoading.set(false);
        this.saveSuccess.set(true);
        if (!id) this.editingDeckId.set(res.id); // Set the ID so subsequent saves are updates
        this.loadDecks();
        setTimeout(() => this.saveSuccess.set(false), 3000);
      },
      error: (err) => {
        this.saveLoading.set(false);
        this.saveError.set(err.error?.message ?? 'Error al guardar el mazo.');
      },
    });
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

  readonly autoCompleting = signal(false);

  autoComplete(): void {
    this.autoCompleting.set(true);
    this.saveError.set(null);

    this.deckApi.autocompleteDeck().subscribe({
      next: (res: { cardId: string; quantity: number }[]) => {
        this.deckStore.clearDeck();
        for (const cardData of res) {
          const cardObj = this.tcgService.cards().find(c => c.id === cardData.cardId);
          if (cardObj) {
            for (let i = 0; i < cardData.quantity; i++) {
              this.deckStore.addCard(cardObj);
            }
          }
        }
        this.autoCompleting.set(false);
      },
      error: (err: any) => {
        this.autoCompleting.set(false);
        this.saveError.set(err.error?.message ?? 'Error al autocompletar el mazo.');
      }
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

  triggerHelp(): void {
    this.tutorialService.triggerTutorial('deck', true);
  }
}
