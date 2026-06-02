import { Component, inject, OnInit, ViewEncapsulation, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService, UserProfileResponseDTO, UserAchievementProgressDTO, MatchHistoryItemDTO } from '../../core/services/profile.service';
import { DeckApiService } from '../deck/deck-api.service';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { StatComponent, IconComponent, TrainerChipComponent, AmbientComponent, LogoComponent } from '../lobby-aurora/ui/aurora-ui.components';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-profile-aurora',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, StatComponent, IconComponent, TrainerChipComponent, AmbientComponent, LogoComponent],
  encapsulation: ViewEncapsulation.None,
  template: `
    <div class="scene v-aurora" style="position: fixed; inset: 0; z-index: 9999; overflow-y: auto;">
      <!-- Background Mesh -->
      <div class="mesh" style="opacity: 0.5;">
        <span style="width: 540px; height: 540px; left: -120px; top: -160px; background: var(--m1);"></span>
        <span style="width: 620px; height: 620px; left: 360px; top: -240px; background: var(--m2); animation-delay: -5s;"></span>
      </div>
      <aurora-ambient></aurora-ambient>
      <div class="bd-noise"></div>
      <div class="bd-vignette"></div>

      <!-- Toast notification -->
      @if (toastMessage) {
        <div
          [style.background]="toastType === 'success' ? 'linear-gradient(135deg,#1a2a1a,#243024)' : 'linear-gradient(135deg,#2a1a1a,#302424)'"
          [style.border]="toastType === 'success' ? '1px solid #4caf5088' : '1px solid #f4433688'"
          [style.color]="toastType === 'success' ? '#81c784' : '#ef9a9a'"
          style="
            position: fixed; bottom: 32px; left: 50%; transform: translateX(-50%);
            z-index: 99999; padding: 14px 28px; border-radius: 12px; font-size: 14px;
            font-weight: 600; letter-spacing: 0.01em; white-space: nowrap;
            box-shadow: 0 8px 32px rgba(0,0,0,0.5); animation: toastIn 0.25s ease;">
          {{ toastMessage }}
        </div>
      }

      <!-- Topbar -->
      <div style="position: sticky; top: 0; left: 0; right: 0; height: 92px; display: flex; align-items: center; justify-content: space-between; padding: 0 44px; z-index: 10; background: linear-gradient(180deg, var(--bg) 0%, transparent 100%);">
        <aurora-logo></aurora-logo>
        <div style="display: flex; align-items: center; gap: 22px;">
          <nav style="display: flex; gap: 26px; font-size: 13.5px; font-weight: 600; color: var(--mut);">
            <a routerLink="/lobby" style="text-decoration: none; color: var(--mut);">Inicio</a>
            <a routerLink="/deck" style="text-decoration: none; color: var(--mut);">Mazos</a>
            <a routerLink="/profile" style="text-decoration: none; color: var(--txt);">Perfil</a>
          </nav>
          <aurora-trainer-chip [name]="username" [initial]="userInitial" [mmr]="profileData?.mmr?.toString() || '1200'"></aurora-trainer-chip>
        </div>
      </div>

      <!-- Profile Content -->
      <div style="position: relative; max-width: 1000px; margin: 0 auto; padding: 40px 20px; z-index: 5; display: flex; flex-direction: column; gap: 40px;">
        
        <!-- Header Profile -->
        <div class="fu" style="display: flex; align-items: center; gap: 30px;">
          <div class="avatar" style="width: 100px; height: 100px; font-size: 44px; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 0 6px var(--bg), 0 0 0 10px var(--accent); background: var(--surface); border-radius: 50%;">
            {{ getAvatarEmoji(profileData?.avatarIcon) }}
          </div>
          <div>
            <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
              <h1 class="display" style="font-size: 54px; font-weight: 700; margin: 0; color: var(--txt); line-height: 1;">{{ username }}</h1>
              @if (profileData?.activeTitle) {
                <span style="background: linear-gradient(135deg, var(--accent2) 0%, rgba(255,255,255,0.05) 100%); border: 1px solid var(--line); color: var(--txt); padding: 4px 10px; border-radius: 8px; font-size: 11.5px; font-weight: 800; letter-spacing: 0.05em; text-transform: uppercase;">
                  🏅 {{ profileData?.activeTitle }}
                </span>
              }
            </div>
            
            <div style="color: var(--mut); font-weight: 600; letter-spacing: 0.05em; font-size: 14.5px; margin-top: 8px; font-style: italic; max-width: 480px; line-height: 1.4;">
              {{ profileData?.description || 'Sin descripción de entrenador.' }}
            </div>
          </div>
          
          <div style="margin-left: auto; display: flex; gap: 24px; padding: 20px 30px; background: var(--surface); border: 1px solid var(--line); border-radius: 20px; backdrop-filter: blur(10px);">
            <aurora-stat [v]="totalWins.toString()" k="Victorias" [accent]="true"></aurora-stat>
            <div style="width: 1px; background: var(--line);"></div>
            <aurora-stat [v]="totalLosses.toString()" k="Derrotas"></aurora-stat>
            <div style="width: 1px; background: var(--line);"></div>
            <aurora-stat [v]="overallWinRate + '%'" k="Win Rate" [accent]="true"></aurora-stat>
          </div>
        </div>

        <!-- Layout 2 Columns -->
        <div class="fu" style="display: grid; grid-template-columns: 2fr 1fr; gap: 40px; animation-delay: 0.1s;">
          
          <!-- Left Column (Tabs) -->
          <div>
            <!-- Tabs Header -->
            <div style="display: flex; gap: 12px; margin-bottom: 24px; border-bottom: 1px solid var(--line); padding-bottom: 12px;">
              <button (click)="activeTab = 'showcase'" [class.active-tab]="activeTab === 'showcase'" class="tab-btn">Vitrina y Mazo</button>
              <button (click)="activeTab = 'achievements'" [class.active-tab]="activeTab === 'achievements'" class="tab-btn">Logros y Títulos</button>
              <button (click)="activeTab = 'history'" [class.active-tab]="activeTab === 'history'" class="tab-btn">Historial de Partidas</button>
            </div>

            <!-- Tab content: Showcase -->
            @if (activeTab === 'showcase') {
              <div style="display: flex; flex-direction: column; gap: 30px;">
                <!-- Card Showcase -->
                <div>
                  <div class="eyebrow" style="margin-bottom: 12px; color: var(--accent2);">Vitrina de Cartas Destacadas</div>
                  <div style="font-size: 12px; color: var(--mut); margin-bottom: 16px;">💡 Puedes arrastrar y soltar cartas del buscador directamente en los slots, o hacer clic en ellos.</div>
                  <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px;">
                    @for (pos of [1, 2, 3]; track pos) {
                      @let slot = getShowcaseSlot(pos);
                      <div class="showcase-slot" 
                           (click)="openCardSelector(pos)"
                           (dragover)="allowDrop($event)"
                           (drop)="handleDropOnSlot($event, pos)">
                        @if (slot) {
                          <img [src]="getCardImageById(slot.cardId)" [alt]="slot.cardName" style="max-height: 100%; max-width: 100%; pointer-events: none;" />
                          <button class="remove-btn" (click)="removeCardFromShowcase(pos, $event)">×</button>
                        } @else {
                          <div style="text-align: center; color: var(--mut); padding: 20px; pointer-events: none;">
                            <div style="font-size: 28px; margin-bottom: 8px;">+</div>
                            <div style="font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em;">Vacío</div>
                          </div>
                        }
                      </div>
                    }
                  </div>
                </div>

                <!-- Featured Deck -->
                <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px);">
                  <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Mazo Destacado</div>
                  
                  @if (profileData?.showcasedDeck) {
                    <div style="display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; background: rgba(255,255,255,0.03); border: 1px solid var(--line); border-radius: 14px; margin-bottom: 20px;">
                      <div style="display: flex; align-items: center; gap: 14px;">
                        <div style="font-size: 32px;">🎴</div>
                        <div>
                          <div style="font-weight: 700; font-size: 16px; color: var(--txt);">{{ profileData?.showcasedDeck?.name }}</div>
                          <div style="font-size: 11px; color: var(--mut);">ID: #{{ profileData?.showcasedDeck?.id }}</div>
                        </div>
                      </div>
                      <button class="ghost-btn sm" (click)="removeShowcasedDeck()" style="font-size: 11px; padding: 6px 12px; height: auto;">Quitar</button>
                    </div>
                  } @else {
                    <div style="text-align: center; padding: 24px; border: 1px dashed var(--line); border-radius: 14px; margin-bottom: 20px; color: var(--mut); font-size: 13.5px;">
                      No tenés ningún mazo destacado en tu perfil.
                    </div>
                  }
                  
                  <div class="form-group" style="margin-bottom: 0;">
                    <label class="form-label" style="font-size: 11px; margin-bottom: 8px;">Cambiar Mazo Destacado</label>
                    @if (userDecks.length > 0) {
                      <select [ngModel]="profileData?.showcasedDeck?.id" (ngModelChange)="selectShowcasedDeck($event)" class="form-input" style="background-color: var(--bg2); width: 100%;">
                        <option [value]="null">-- Seleccionar Mazo --</option>
                        @for (deck of userDecks; track deck.id) {
                          <option [value]="deck.id">{{ deck.name }}</option>
                        }
                      </select>
                    } @else {
                      <div style="font-size: 12.5px; color: var(--mut); font-style: italic; margin-top: 4px;">Aún no tenés mazos creados. ¡Ve al constructor de mazos para crear uno!</div>
                    }
                  </div>
                </div>
              </div>
            }

            <!-- Tab content: Achievements -->
            @if (activeTab === 'achievements') {
              <div>
                <div class="eyebrow" style="margin-bottom: 20px; color: var(--accent2);">Logros y Títulos de Entrenador</div>
                <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px); display: flex; flex-direction: column; gap: 16px; max-height: 520px; overflow-y: auto;" class="scroll">
                  @if (achievements.length === 0) {
                    <div style="text-align: center; color: var(--mut); padding: 40px;">No se encontraron logros.</div>
                  }
                  @for (ach of achievements; track ach.title) {
                    <div style="display: flex; flex-direction: column; gap: 10px; padding: 16px; background: rgba(255,255,255,0.02); border: 1px solid var(--line); border-radius: 14px;" [style.opacity]="ach.unlocked ? '1' : '0.7'">
                      <div style="display: flex; align-items: flex-start; justify-content: space-between;">
                        <div>
                          <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
                            <span style="font-weight: 700; font-size: 15px; color: var(--txt);">{{ ach.title }}</span>
                            <span [style.background]="getCategoryColor(ach.category)" style="font-size: 9px; font-weight: 800; padding: 2px 6px; border-radius: 6px; color: #111; text-transform: uppercase;">{{ ach.category }}</span>
                          </div>
                          <div style="font-size: 12.5px; color: var(--mut); margin-top: 6px;">{{ ach.requirement }}</div>
                        </div>
                        <div style="font-size: 18px;">
                          {{ ach.unlocked ? '✅' : '🔒' }}
                        </div>
                      </div>
                      
                      <!-- Progress bar -->
                      <div style="display: flex; align-items: center; gap: 12px; margin-top: 4px;">
                        <div style="flex: 1; height: 6px; background: rgba(255,255,255,0.05); border-radius: 3px; overflow: hidden;">
                          <div [style.width]="(Math.min(ach.progress, ach.target) / ach.target * 100) + '%'" [style.background]="ach.unlocked ? 'linear-gradient(90deg, #46e08a, #27ae60)' : 'linear-gradient(90deg, var(--accent), var(--accent2))'" style="height: 100%; border-radius: 3px;"></div>
                        </div>
                        <span class="num" style="font-size: 12px; color: var(--dim); width: 65px; text-align: right;">{{ ach.progress }} / {{ ach.target }}</span>
                      </div>
                    </div>
                  }
                </div>
              </div>
            }

            <!-- Tab content: Match History (Real) -->
            @if (activeTab === 'history') {
              <div>
                <div class="eyebrow" style="margin-bottom: 20px; color: var(--accent2);">Historial Reciente</div>
                <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; overflow: hidden; backdrop-filter: blur(10px);">
                  @if (loadingHistory) {
                    <div style="text-align: center; padding: 40px; color: var(--mut);">Cargando historial de partidas...</div>
                  } @else if (matchesHistory.length === 0) {
                    <div style="text-align: center; padding: 40px; color: var(--mut);">No se registran partidas en tu historial.</div>
                  } @else {
                    @for (m of matchesHistory; track m.matchId) {
                      <div style="display: grid; grid-template-columns: 60px 1fr 1fr 120px; align-items: center; padding: 16px 20px; border-bottom: 1px solid var(--line); transition: background 0.2s;" class="match-row-hover">
                        <div [style.color]="m.result === 'WIN' ? '#46e08a' : 'var(--accent)'" style="font-family: var(--display); font-size: 24px; font-weight: 700;">
                          {{ m.result === 'WIN' ? 'W' : 'L' }}
                        </div>
                        <div>
                          <div style="font-weight: 700; font-size: 15px;">Partida #{{ m.matchId }}</div>
                          <div style="font-size: 11px; color: var(--mut); text-transform: uppercase; letter-spacing: 0.05em; margin-top: 4px;">{{ m.status }}</div>
                        </div>
                        <div>
                          <div style="font-weight: 600; font-size: 14px; color: var(--dim);">vs {{ m.opponent }}</div>
                        </div>
                        <div style="text-align: right; color: var(--dim); font-size: 12px; font-weight: 600;">
                          {{ m.date | date:'dd/MM/yyyy HH:mm' }}
                        </div>
                      </div>
                    }
                  }
                </div>
              </div>
            }
          </div>

          <!-- Right Column (Stats / Info) -->
          <div>
            <!-- Level / Customization Block -->
            <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px); margin-bottom: 24px;">
              <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
                <div>
                  <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em;">Progreso</div>
                  <div style="font-family: var(--display); font-size: 24px; font-weight: 700;">Nivel {{ profileData?.level || 1 }}</div>
                </div>
                <button class="ghost-btn sm" (click)="openEditModal()" style="padding: 6px 14px; font-size: 12px; font-weight: 700; height: auto;">
                  Editar Perfil
                </button>
              </div>
              
              <!-- XP Bar -->
              <div style="height: 8px; background: rgba(255,255,255,0.05); border-radius: 4px; overflow: hidden; margin-bottom: 8px;">
                <div [style.width]="((profileData?.xp || 0) / (profileData?.xpToNextLevel || 100) * 100) + '%'" style="height: 100%; background: linear-gradient(90deg, var(--accent), var(--accent2)); border-radius: 4px;"></div>
              </div>
              <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--mut); font-weight: 600;">
                <span>XP: {{ profileData?.xp || 0 }} / {{ profileData?.xpToNextLevel || 100 }}</span>
                <span>{{ Math.round(((profileData?.xp || 0) / (profileData?.xpToNextLevel || 100) * 100)) }}%</span>
              </div>
              
              <!-- Coins / Battle Points -->
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 24px; border-top: 1px solid var(--line); padding-top: 20px;">
                <div>
                  <div style="font-size: 11px; color: var(--mut); font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em;">Pokécoins</div>
                  <div class="num" style="font-size: 18px; font-weight: 700; color: #ffce32; display: flex; align-items: center; gap: 6px; margin-top: 4px;">
                    🪙 {{ profileData?.pokecoins || 0 }}
                  </div>
                </div>
                <div>
                  <div style="font-size: 11px; color: var(--mut); font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em;">Puntos Batalla</div>
                  <div class="num" style="font-size: 18px; font-weight: 700; color: #a855f7; display: flex; align-items: center; gap: 6px; margin-top: 4px;">
                    🏆 {{ profileData?.battlePoints || 0 }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Extended Stats Block -->
            <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px); margin-bottom: 24px;">
              <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Estadísticas Detalladas</div>
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                <div style="background: rgba(255,255,255,0.02); border: 1px solid var(--line); border-radius: 12px; padding: 12px; text-align: center;">
                  <div style="font-size: 10px; color: var(--mut); font-weight: 600; text-transform: uppercase;">Vict. Perfectas</div>
                  <div class="num" style="font-size: 18px; font-weight: 700; color: #ffce32; margin-top: 4px;">🏆 {{ profileData?.statistics?.perfectWins || 0 }}</div>
                </div>
                <div style="background: rgba(255,255,255,0.02); border: 1px solid var(--line); border-radius: 12px; padding: 12px; text-align: center;">
                  <div style="font-size: 10px; color: var(--mut); font-weight: 600; text-transform: uppercase;">Remontadas</div>
                  <div class="num" style="font-size: 18px; font-weight: 700; color: #ff7a3d; margin-top: 4px;">🔥 {{ profileData?.statistics?.comebackWins || 0 }}</div>
                </div>
                <div style="background: rgba(255,255,255,0.02); border: 1px solid var(--line); border-radius: 12px; padding: 12px; text-align: center;">
                  <div style="font-size: 10px; color: var(--mut); font-weight: 600; text-transform: uppercase;">KOs Totales</div>
                  <div class="num" style="font-size: 18px; font-weight: 700; color: var(--accent); margin-top: 4px;">⚡ {{ profileData?.statistics?.totalKos || 0 }}</div>
                </div>
                <div style="background: rgba(255,255,255,0.02); border: 1px solid var(--line); border-radius: 12px; padding: 12px; text-align: center;">
                  <div style="font-size: 10px; color: var(--mut); font-weight: 600; text-transform: uppercase;">Entrenadores J.</div>
                  <div class="num" style="font-size: 18px; font-weight: 700; color: #b8b8cc; margin-top: 4px;">🃏 {{ profileData?.statistics?.trainerCardsPlayed || 0 }}</div>
                </div>
              </div>
              <div style="background: rgba(255,255,255,0.02); border: 1px solid var(--line); border-radius: 12px; padding: 12px; text-align: center; margin-top: 12px;">
                <div style="font-size: 10px; color: var(--mut); font-weight: 600; text-transform: uppercase;">Daño Total Infligido</div>
                <div class="num" style="font-size: 20px; font-weight: 700; color: #4aa3ff; margin-top: 4px;">💥 {{ profileData?.statistics?.totalDamageDealt || 0 }}</div>
              </div>
            </div>

            <!-- Honors received -->
            <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px); margin-bottom: 24px;">
              <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Honores Recibidos</div>
              <div style="display: flex; flex-direction: column; gap: 12px;">
                <div style="display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; background: rgba(255,255,255,0.02); border: 1px solid var(--line); border-radius: 10px;">
                  <span style="display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; color: var(--txt);">🤝 Buen Deportista</span>
                  <span class="num" style="font-size: 16px; font-weight: 700; color: #46e08a;">{{ profileData?.honors?.['GOOD_SPORTSMAN'] || 0 }}</span>
                </div>
                <div style="display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; background: rgba(255,255,255,0.02); border: 1px solid var(--line); border-radius: 10px;">
                  <span style="display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; color: var(--txt);">😊 Amigable</span>
                  <span class="num" style="font-size: 16px; font-weight: 700; color: #ffce32;">{{ profileData?.honors?.['FRIENDLY'] || 0 }}</span>
                </div>
                <div style="display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; background: rgba(255,255,255,0.02); border: 1px solid var(--line); border-radius: 10px;">
                  <span style="display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; color: var(--txt);">🧠 Gran Estratega</span>
                  <span class="num" style="font-size: 16px; font-weight: 700; color: #4aa3ff;">{{ profileData?.honors?.['GREAT_STRATEGIST'] || 0 }}</span>
                </div>
              </div>
            </div>

            <!-- Archetypes (Donut) -->
            <div>
              <div class="eyebrow" style="margin-bottom: 20px; color: var(--accent2);">Arquetipos Top</div>
              <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px);">
                <div style="display: flex; justify-content: center; margin-bottom: 30px;">
                  <div [style.background]="'conic-gradient(' + donutStops + ')'" style="width: 140px; height: 140px; border-radius: 50%; display: flex; align-items: center; justify-content: center; position: relative;">
                    <div style="width: 100px; height: 100px; border-radius: 50%; background: var(--bg2); position: absolute;"></div>
                    <div style="position: relative; text-align: center;">
                      <div class="num" style="font-size: 24px; font-weight: 700; line-height: 1; color: var(--txt);">{{ overallWinRate }}%</div>
                    </div>
                  </div>
                </div>
                <div style="display: flex; flex-direction: column; gap: 14px;">
                  @for (a of archetypes; track a.name) {
                    <div style="display: flex; align-items: center; justify-content: space-between;">
                      <div style="display: flex; align-items: center; gap: 10px;">
                        <div [style.background]="a.color" style="width: 12px; height: 12px; border-radius: 50%;"></div>
                        <div style="font-size: 14px; font-weight: 600; color: var(--txt);">{{ a.name }}</div>
                      </div>
                      <div class="num" style="font-size: 13px; color: var(--mut);">{{ a.wins }} - {{ a.losses }}</div>
                    </div>
                  }
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>

    <!-- MODAL: EDIT PROFILE -->
    @if (showEditModal) {
      <div class="modal-backdrop" (click)="closeEditModal()">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <h2 style="font-family: var(--display); font-size: 24px; font-weight: 700; margin-top: 0; margin-bottom: 20px;">Editar Perfil de Entrenador</h2>
          
          <!-- Avatar Icon selection -->
          <div class="form-group">
            <label class="form-label">Avatar</label>
            <div style="display: flex; gap: 12px; justify-content: space-between; flex-wrap: wrap;">
              @for (av of avatars; track av) {
                <div class="avatar-option" [class.selected]="editAvatarIcon === av" (click)="editAvatarIcon = av">
                  {{ getAvatarEmoji(av) }}
                </div>
              }
            </div>
          </div>

          <!-- Description selection -->
          <div class="form-group">
            <label class="form-label">Descripción</label>
            <textarea [(ngModel)]="editDescription" class="form-input" rows="3" maxlength="150" placeholder="Escribe tu descripción de entrenador..." style="resize: none;"></textarea>
            <div style="text-align: right; font-size: 11px; color: var(--mut); margin-top: 4px;">
              {{ editDescription.length }} / 150
            </div>
          </div>

          <!-- Title selection -->
          <div class="form-group">
            <label class="form-label">Título Activo</label>
            <select [(ngModel)]="editActiveTitle" class="form-input" style="background-color: var(--bg2);">
              <option value="Ninguno">Ninguno</option>
              @for (title of profileData?.unlockedTitles; track title) {
                <option [value]="title">{{ title }}</option>
              }
            </select>
            @if (!profileData?.unlockedTitles || profileData?.unlockedTitles?.length === 0) {
              <div style="font-size: 11.5px; color: var(--mut); margin-top: 6px; font-style: italic;">Aún no has desbloqueado títulos. Cumple logros para obtenerlos.</div>
            }
          </div>

          <!-- Actions -->
          <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 30px;">
            <button class="ghost-btn" (click)="closeEditModal()" [disabled]="savingProfile">Cancelar</button>
            <button class="cta" (click)="saveProfile()" [disabled]="savingProfile" style="padding: 10px 24px; font-size: 13px;">
              @if (savingProfile) { Guardando... } @else { Guardar Cambios }
            </button>
          </div>
        </div>
      </div>
    }

    <!-- MODAL: CARD SELECTOR FOR SHOWCASE -->
    @if (showCardSelector) {
      <div class="modal-backdrop" (click)="closeCardSelector()">
        <div class="modal-card modal-card-lg" (click)="$event.stopPropagation()">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
            <h2 style="font-family: var(--display); font-size: 24px; font-weight: 700; margin: 0;">Seleccionar Carta para Vitrina</h2>
            <button (click)="closeCardSelector()" style="background: transparent; border: none; color: var(--txt); font-size: 24px; cursor: pointer;">&times;</button>
          </div>

          <!-- Filter / Information note -->
          <div style="background: rgba(255,255,255,0.02); border: 1px solid var(--line); border-radius: 12px; padding: 14px; margin-bottom: 16px; font-size: 12.5px; color: var(--dim); line-height: 1.4;">
            💡 Selecciona cualquier carta de la colección para destacarla en tu vitrina de perfil.
          </div>

          <div style="position: relative; margin-bottom: 20px;">
            <input type="text" [(ngModel)]="cardSearchQuery" placeholder="Buscar carta por nombre..." style="width: 100%; background: rgba(255,255,255,0.05); border: 1px solid var(--line); color: var(--txt); padding: 12px 16px; border-radius: 12px; outline: none; font-family: 'Manrope'; font-size: 14px;" />
          </div>

          <!-- Cards Grid -->
          <div style="flex: 1; min-height: 0; display: flex; flex-direction: column;">
            @if (filteredShowcaseCards.length === 0) {
              <div style="flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 20px; color: var(--mut); text-align: center;">
                <div style="font-size: 40px; margin-bottom: 10px;">🃏</div>
                <div style="font-weight: bold; margin-bottom: 6px;">No se encontraron cartas</div>
                <div>Intenta buscar con otro nombre.</div>
              </div>
            } @else {
              <div class="card-select-grid scroll" style="flex: 1; overflow-y: auto;">
                @for (card of filteredShowcaseCards; track card.id) {
                  <div class="card-select-item" 
                       (click)="selectCardForShowcase(card.id)"
                       draggable="true" 
                       (dragstart)="handleDragStart($event, card.id)">
                    <img [src]="card.images?.small || card.images?.large" [alt]="card.name" style="width: 100%; height: 100%; object-fit: contain; pointer-events: none;" />
                  </div>
                }
              </div>
            }
          </div>
        </div>
      </div>
    }

    <style>
      .match-row-hover:hover { background: rgba(255,255,255,0.03); cursor: pointer; }
      .tab-btn { background: transparent; border: none; color: var(--mut); font-family: 'Manrope'; font-weight: 700; font-size: 13.5px; padding: 8px 18px; border-radius: 10px; cursor: pointer; transition: all 0.2s; }
      .tab-btn:hover { color: var(--txt); }
      .tab-btn.active-tab { background: rgba(255,255,255,0.1); color: var(--txt); box-shadow: 0 2px 8px rgba(0,0,0,0.2); }
      
      .showcase-slot {
        background: rgba(255, 255, 255, 0.02);
        border: 2px dashed var(--line);
        border-radius: 16px;
        aspect-ratio: 5/7;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.2s;
        position: relative;
        overflow: hidden;
      }
      .showcase-slot:hover {
        background: rgba(255, 255, 255, 0.05);
        border-color: var(--accent);
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(255, 46, 62, 0.15);
      }
      .showcase-slot img {
        width: 100%;
        height: 100%;
        object-fit: contain;
        transition: transform 0.3s;
      }
      .showcase-slot:hover img {
        transform: scale(1.05);
      }
      .remove-btn {
        position: absolute;
        top: 8px;
        right: 8px;
        background: rgba(255, 46, 62, 0.85);
        border: none;
        color: white;
        border-radius: 50%;
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        font-weight: bold;
        z-index: 5;
        opacity: 0;
        transition: opacity 0.2s;
      }
      .showcase-slot:hover .remove-btn {
        opacity: 1;
      }
      
      .modal-backdrop {
        position: fixed;
        inset: 0;
        background: rgba(0,0,0,0.75);
        backdrop-filter: blur(8px);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
        animation: fadeIn 0.2s ease-out;
      }
      .modal-card {
        background: var(--bg2);
        border: 1px solid var(--line);
        border-radius: 24px;
        width: 90%;
        max-width: 500px;
        padding: 30px;
        box-shadow: 0 20px 40px rgba(0,0,0,0.55);
        position: relative;
        animation: scaleUp 0.2s ease-out;
      }
      .modal-card-lg {
        max-width: 760px;
        max-height: 85vh;
        display: flex;
        flex-direction: column;
      }
      
      .form-group {
        margin-bottom: 22px;
      }
      .form-label {
        display: block;
        font-weight: 700;
        font-size: 13px;
        color: var(--mut);
        text-transform: uppercase;
        margin-bottom: 8px;
        letter-spacing: 0.05em;
      }
      .form-input {
        width: 100%;
        background: rgba(255,255,255,0.05);
        border: 1px solid var(--line);
        color: var(--txt);
        padding: 12px 16px;
        border-radius: 12px;
        outline: none;
        font-family: 'Manrope';
        font-size: 14.5px;
        transition: border-color 0.2s;
      }
      .form-input:focus {
        border-color: var(--accent);
      }
      
      .avatar-option {
        width: 54px;
        height: 54px;
        border-radius: 50%;
        border: 2px solid transparent;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 26px;
        cursor: pointer;
        background: rgba(255,255,255,0.05);
        transition: all 0.2s;
      }
      .avatar-option:hover {
        transform: scale(1.1);
        background: rgba(255,255,255,0.1);
      }
      .avatar-option.selected {
        border-color: var(--accent);
        background: rgba(255,46,62,0.15);
        box-shadow: 0 0 12px rgba(255, 46, 62, 0.4);
      }
      
      .card-select-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
        gap: 16px;
        overflow-y: auto;
        padding-right: 8px;
        margin-top: 15px;
      }
      .card-select-item {
        cursor: pointer;
        transition: transform 0.2s;
        aspect-ratio: 5/7;
        position: relative;
        border-radius: 8px;
        overflow: hidden;
      }
      .card-select-item:hover {
        transform: translateY(-4px) scale(1.04);
        box-shadow: 0 8px 16px rgba(0,0,0,0.6);
      }
      
      @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
      @keyframes scaleUp { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }
      @keyframes toastIn { from { opacity: 0; transform: translateX(-50%) translateY(16px); } to { opacity: 1; transform: translateX(-50%) translateY(0); } }
    </style>
  `
})
export class ProfileAuroraComponent implements OnInit {
  private authService = inject(AuthService);
  private profileService = inject(ProfileService);
  private deckApi = inject(DeckApiService);
  private tcgService = inject(PokemonTcgService);
  private cdr = inject(ChangeDetectorRef);

  profileData: UserProfileResponseDTO | null = null;
  achievements: UserAchievementProgressDTO[] = [];
  userDecks: any[] = [];
  activeTab: 'showcase' | 'achievements' | 'history' = 'showcase';

  // Edit Profile form state
  showEditModal = false;
  editDescription = '';
  editActiveTitle = '';
  editAvatarIcon = '';
  savingProfile = false;
  avatars = ['ash', 'misty', 'brock', 'gary', 'serena', 'red'];

  // Toast notification state
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimeout: ReturnType<typeof setTimeout> | null = null;

  // Card selector modal state
  showCardSelector = false;
  selectedSlotPosition: number | null = null;
  cardSearchQuery = '';

  // Match History state
  matchesHistory: MatchHistoryItemDTO[] = [];
  loadingHistory = false;

  get username(): string {
    return this.authService.username ?? 'Invitado';
  }

  get userInitial(): string {
    return this.username.charAt(0).toUpperCase();
  }

  ngOnInit(): void {
    this.tcgService.loadCards();
    this.loadProfile();
  }

  loadProfile(): void {
    if (this.username !== 'Invitado') {
      this.profileService.getProfile(this.username).subscribe({
        next: (data) => {
          this.profileData = data;
          this.loadAchievements();
          this.loadUserDecks();
          this.loadHistory();
        },
        error: (err) => console.error('Error fetching profile', err)
      });
    }
  }

  loadAchievements(): void {
    this.profileService.getAchievements(this.username).subscribe({
      next: (data) => this.achievements = data,
      error: (err) => console.error('Error fetching achievements', err)
    });
  }

  loadUserDecks(): void {
    const userId = this.authService.userId;
    if (userId) {
      this.deckApi.getDecksByUserId(userId).subscribe({
        next: (decks) => this.userDecks = decks,
        error: (err) => console.error('Error fetching user decks', err)
      });
    }
  }

  loadHistory(): void {
    this.loadingHistory = true;
    this.profileService.getUserHistory(0, 10).subscribe({
      next: (res) => {
        this.matchesHistory = res.content || [];
        this.loadingHistory = false;
      },
      error: (err) => {
        console.error('Error fetching match history', err);
        this.loadingHistory = false;
      }
    });
  }

  getAvatarEmoji(icon: string | undefined): string {
    if (!icon) return '🎒';
    switch (icon.toLowerCase()) {
      case 'ash': return '🧢';
      case 'misty': return '💧';
      case 'brock': return '🪨';
      case 'gary': return '👑';
      case 'serena': return '🎀';
      case 'red': return '⚡';
      default: return '🎒';
    }
  }

  // Edit Profile Modal
  openEditModal(): void {
    this.editDescription = this.profileData?.description || '';
    this.editActiveTitle = this.profileData?.activeTitle || 'Ninguno';
    this.editAvatarIcon = this.profileData?.avatarIcon || 'ash';
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
  }

  showToast(message: string, type: 'success' | 'error' = 'success'): void {
    this.toastMessage = message;
    this.toastType = type;
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
    this.toastTimeout = setTimeout(() => { this.toastMessage = ''; }, 3500);
  }

  saveProfile(): void {
    if (this.savingProfile) return;
    this.savingProfile = true;

    const activeTitleVal = this.editActiveTitle === 'Ninguno' ? '' : this.editActiveTitle;
    const payload = {
      description: this.editDescription.trim(),
      activeTitle: activeTitleVal,
      avatarIcon: this.editAvatarIcon
    };

    // Timeout de seguridad: si el request tarda más de 10s, libera el botón
    const safetyTimeout = setTimeout(() => {
      if (this.savingProfile) {
        this.savingProfile = false;
        this.showToast('❌ El servidor tardó demasiado. Intentá de nuevo.', 'error');
        this.cdr.detectChanges();
      }
    }, 10000);

    this.profileService.updateProfile(payload).subscribe({
      next: () => {
        clearTimeout(safetyTimeout);

        // 1. Actualizar datos localmente de inmediato
        if (this.profileData) {
          this.profileData = {
            ...this.profileData,
            description: payload.description,
            activeTitle: payload.activeTitle || this.profileData.activeTitle,
            avatarIcon: payload.avatarIcon
          };
        }

        // 2. Cerrar el modal y liberar el botón
        this.savingProfile = false;
        this.showEditModal = false;

        // 3. Forzar actualización del DOM
        this.cdr.detectChanges();

        // 4. Mostrar toast de éxito
        this.showToast('✅ Perfil guardado correctamente');
      },
      error: (err) => {
        clearTimeout(safetyTimeout);
        this.savingProfile = false;
        const status = err?.status;
        let msg = err?.error?.message || err?.message || 'Error al guardar el perfil';
        if (status === 401 || status === 403) {
          msg = 'Tu sesión expiró. Cerrá sesión (arriba a la derecha) y volvé a entrar.';
        }
        this.showToast('❌ ' + msg, 'error');
        this.cdr.detectChanges();
        console.error('Error updating profile [status=' + status + ']', err);
      }
    });
  }

  // Card Showcase
  getShowcaseSlot(position: number) {
    return this.profileData?.showcase?.find(s => s.slotPosition === position);
  }

  getCardImageById(cardId: string): string {
    const card = this.tcgService.cards().find(c => c.id === cardId);
    return card?.images?.small ?? card?.images?.large ?? '';
  }

  openCardSelector(slotPosition: number): void {
    this.selectedSlotPosition = slotPosition;
    this.cardSearchQuery = '';
    this.showCardSelector = true;
  }

  closeCardSelector(): void {
    this.showCardSelector = false;
    this.selectedSlotPosition = null;
  }

  selectCardForShowcase(cardId: string): void {
    if (this.selectedSlotPosition === null) return;
    
    this.profileService.updateShowcase({
      slots: [{ slotPosition: this.selectedSlotPosition, cardId }]
    }).subscribe({
      next: () => {
        this.closeCardSelector();
        this.loadProfile();
      },
      error: (err) => console.error('Error updating showcase card', err)
    });
  }

  removeCardFromShowcase(slotPosition: number, event: MouseEvent): void {
    event.stopPropagation(); // Evita abrir el modal
    if (!confirm('¿Querés quitar esta carta de la vitrina?')) return;

    this.profileService.updateShowcase({
      slots: [{ slotPosition, cardId: '' }]
    }).subscribe({
      next: () => this.loadProfile(),
      error: (err) => console.error('Error removing card from showcase', err)
    });
  }

  // Showcase Drag & Drop support
  handleDragStart(event: DragEvent, cardId: string): void {
    if (event.dataTransfer) {
      event.dataTransfer.setData('text/cardId', cardId);
      event.dataTransfer.effectAllowed = 'copy';
    }
  }

  allowDrop(event: DragEvent): void {
    event.preventDefault();
  }

  handleDropOnSlot(event: DragEvent, slotPosition: number): void {
    event.preventDefault();
    if (!event.dataTransfer) return;
    const cardId = event.dataTransfer.getData('text/cardId');
    if (cardId) {
      this.profileService.updateShowcase({
        slots: [{ slotPosition, cardId }]
      }).subscribe({
        next: () => this.loadProfile(),
        error: (err) => console.error('Error dropping card on showcase', err)
      });
    }
  }

  // Showcased Deck
  selectShowcasedDeck(deckIdVal: any): void {
    const deckId = deckIdVal === 'null' || !deckIdVal ? null : Number(deckIdVal);
    this.profileService.updateShowcaseDeck(deckId).subscribe({
      next: () => this.loadProfile(),
      error: (err) => console.error('Error updating showcased deck', err)
    });
  }

  removeShowcasedDeck(): void {
    if (!confirm('¿Querés quitar el mazo destacado?')) return;
    this.profileService.updateShowcaseDeck(null).subscribe({
      next: () => this.loadProfile(),
      error: (err) => console.error('Error removing showcased deck', err)
    });
  }

  // Helpers
  getCategoryColor(category: string): string {
    switch (category) {
      case 'NIVEL': return '#ffce32'; // Gold
      case 'VICTORIAS': return '#4aa3ff'; // Blue
      case 'PARTIDAS_JUGADAS': return '#a855f7'; // Purple
      case 'COLECCION': return '#5ad27a'; // Green
      case 'HONORES': return '#ff3b47'; // Red
      default: return '#cfd6e4'; // Gray
    }
  }

  get filteredShowcaseCards() {
    const query = this.cardSearchQuery.toLowerCase().trim();
    return this.tcgService.cards().filter((card) => {
      // Filtro de búsqueda por nombre
      if (query && !card.name.toLowerCase().includes(query)) return false;
      return true;
    });
  }

  archetypes = [
    { name:'Charizard Rush',  wins: 22, losses: 8,  color:'var(--accent)' },
    { name:'Pikachu Toolbox', wins: 14, losses: 9,  color:'var(--accent2)' },
    { name:'Blastoise Pivot', wins: 6,  losses: 4,  color:'#4aa3ff' },
    { name:'Venusaur Stall',  wins: 5,  losses: 7,  color:'#46e08a' },
  ];

  get overallWinRate(): number {
    return this.profileData?.statistics?.winRate ?? 0;
  }

  get totalWins(): number {
    return this.profileData?.statistics?.matchesWon ?? 0;
  }

  get totalLosses(): number {
    return this.profileData?.statistics?.matchesLost ?? 0;
  }

  get donutStops(): string {
    const w = this.totalWins;
    const l = this.totalLosses;
    const total = w + l;
    if (total === 0) return 'var(--line) 0% 100%';
    const wSpan = (w / total) * 100;
    return `var(--accent) 0% ${wSpan}%, var(--mut) ${wSpan}% 100%`;
  }

  Math = Math;
}
