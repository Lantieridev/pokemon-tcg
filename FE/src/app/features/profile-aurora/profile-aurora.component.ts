import { Component, inject, OnInit, ViewEncapsulation, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService, UserProfileResponseDTO, UserAchievementProgressDTO, MatchHistoryItemDTO, CardStatDTO, EnergyStatDTO } from '../../core/services/profile.service';
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

      <!-- Topbar spacer (Navbar is handled globally by app-navbar) -->
      <div style="height: 92px; width: 100%; flex: 0 0 auto;"></div>

      <!-- Profile Content -->
      <div style="position: relative; max-width: 1000px; margin: 0 auto; padding: 40px 20px; z-index: 5; display: flex; flex-direction: column; gap: 40px;">
        
        <!-- Header Profile -->
        <div class="fu" style="display: flex; align-items: center; gap: 30px;">
          <div class="profile-avatar-container">
            @if (isCustomAvatar(profileData?.avatarIcon)) {
              <img [src]="getAvatarUrl(profileData?.avatarIcon)" style="width: 100%; height: 100%; object-fit: cover;" />
            } @else {
              {{ getAvatarEmoji(profileData?.avatarIcon) }}
            }
          </div>
          <div>
            <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
              <h1 class="display name-energy" style="font-size: 42px; font-weight: 700; margin: 0; line-height: 1.1; letter-spacing: -0.01em;">{{ username }}</h1>
              @if (profileData?.activeTitle) {
                <span style="background: linear-gradient(135deg, var(--accent2) 0%, rgba(255,255,255,0.05) 100%); border: 1px solid var(--line); color: var(--txt); padding: 4px 10px; border-radius: 8px; font-size: 11.5px; font-weight: 800; letter-spacing: 0.05em; text-transform: uppercase; font-family: 'Space Grotesk', sans-serif;">
                  {{ profileData?.activeTitle }}
                </span>
              }
              <!-- Selected Medals Showcase -->
              @if (selectedMedalsList.length > 0) {
                <div style="display: flex; gap: 8px; align-items: center; margin-left: 8px;">
                  @for (medal of selectedMedalsList; track medal) {
                    <div class="profile-subcard" style="width: 38px; height: 38px; border-radius: 10px; display: flex; align-items: center; justify-content: center; padding: 0;" [title]="getMedalTitle(medal)">
                       <img [src]="'assets/achievements/medals/' + medal + '.png'" style="width: 26px; height: 26px; object-fit: contain; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.35));" />
                    </div>
                  }
                </div>
              }
            </div>
            
            <div style="color: var(--mut); font-weight: 500; font-size: 13.5px; margin-top: 8px; font-style: italic; max-width: 480px; line-height: 1.5; font-family: 'Plus Jakarta Sans', sans-serif;">
              {{ profileData?.description || 'Sin descripción de entrenador.' }}
            </div>
          </div>
          
          <div class="profile-card" style="margin-left: auto; display: flex; gap: 24px; padding: 20px 30px;">
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
              <button (click)="activeTab = 'stats'" [class.active-tab]="activeTab === 'stats'" class="tab-btn">Estadísticas</button>
              <button (click)="activeTab = 'history'" [class.active-tab]="activeTab === 'history'" class="tab-btn">Historial de Partidas</button>
            </div>

            <!-- Tab content: Showcase -->
            @if (activeTab === 'showcase') {
              <div style="display: flex; flex-direction: column; gap: 30px;">
                <!-- Card Showcase -->
                <div>
                  <div class="eyebrow" style="margin-bottom: 12px; color: var(--accent2);">Vitrina de Cartas Destacadas</div>
                  <div style="font-size: 12px; color: var(--mut); margin-bottom: 16px;">Puedes arrastrar y soltar cartas del buscador directamente en los slots, o hacer clic en ellos.</div>
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
 
                <!-- Showcase Deck Section -->
                <div class="profile-card">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <div>
                      <div class="eyebrow" style="color: var(--accent2);">Mazo Destacado</div>
                      <div style="font-size: 12.5px; color: var(--mut); margin-top: 4px;">Elegí el mazo que querés mostrar en tu perfil público.</div>
                    </div>
                  </div>
 
                  @if (profileData?.showcasedDeck) {
                    <div class="profile-subcard" style="display: flex; align-items: center; gap: 20px; padding: 16px; margin-bottom: 20px;">
                      <div style="width: 44px; height: 44px; border-radius: 10px; background: linear-gradient(135deg, var(--accent), var(--accent2)); display: flex; align-items: center; justify-content: center;">
                        <aurora-icon n="decks" [s]="22" style="color: var(--on-accent);"></aurora-icon>
                      </div>
                      <div>
                        <div style="font-weight: 700; font-size: 16px; color: var(--txt);">{{ profileData?.showcasedDeck?.name }}</div>
                        <div style="font-size: 11px; color: var(--mut);">ID: #{{ profileData?.showcasedDeck?.id }}</div>
                      </div>
                      <button (click)="removeShowcasedDeck()" style="margin-left: auto; background: transparent; border: 1px solid #ef444455; color: #ef4444; padding: 6px 12px; border-radius: 8px; font-size: 11.5px; font-weight: 700; cursor: pointer; transition: all 0.15s;" class="danger-btn-hover">
                        Quitar
                      </button>
                    </div>
                  } @else {
                    <div style="padding: 24px; border: 1px dashed var(--line); border-radius: 14px; text-align: center; color: var(--mut); font-size: 13.5px; font-weight: 600; margin-bottom: 20px;">
                      No tenés ningún mazo destacado en este momento.
                    </div>
                  }
 
                  <div class="form-group" style="margin: 0;">
                    <label class="form-label">Cambiar Mazo Destacado</label>
                    <div style="display: flex; gap: 12px;">
                      <select [ngModel]="profileData?.showcasedDeck?.id" (ngModelChange)="selectShowcasedDeck($event)" class="form-input select-dark" style="flex: 1;">
                        <option [value]="null">— Seleccionar Mazo —</option>
                        @for (d of userDecks; track d.id) {
                          <option [value]="d.id">{{ d.name }}</option>
                        }
                      </select>
                    </div>
                  </div>
                </div>
              </div>
            }

            <!-- Tab content: Achievements -->
            @if (activeTab === 'achievements') {
              <div>
                <div class="eyebrow" style="margin-bottom: 20px; color: var(--accent2);">
                  Logros y Títulos de Entrenador
                  <div style="font-size: 14.5px; color: var(--mut); margin-top: 6px; font-family: Space Grotesk, sans-serif; text-transform: none; letter-spacing: normal;">
                    {{ unlockedAchievementsCount }}/{{ achievements.length }}
                  </div>
                </div>

                <!-- Medallero -->
                <div class="profile-card" style="margin-bottom: 24px;">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                    <div style="font-family: var(--display); font-size: 16px; font-weight: 700; letter-spacing: 0.02em; display: flex; align-items: center; gap: 8px; color: var(--txt);">
                      Medallero de Logros 
                      <span style="font-size: 12.5px; color: var(--mut); font-weight: 600;">({{ unlockedMedalsCount }} / 25)</span>
                    </div>
                  </div>
                  <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(60px, 1fr)); gap: 12px;">
                    @for (medal of medals; track medal.title) {
                      <div class="medal-item" [class.locked]="!medal.unlocked">
                        <img [src]="'assets/achievements/medals/' + medal.rewardValue + '.png'" [alt]="medal.title" loading="lazy" />
                        @if (!medal.unlocked) {
                          <div style="position: absolute; bottom: 3px; right: 3px; font-size: 7px; background: rgba(0,0,0,0.7); border-radius: 4px; padding: 2px 4px; display: flex; align-items: center; justify-content: center; color: #ffb8b8; border: 0.5px solid rgba(255,255,255,0.15); font-family: 'Space Grotesk', sans-serif; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em;">Bloq</div>
                        }
                        <!-- Tooltip -->
                        <div class="medal-tooltip">
                          <div class="medal-tooltip-title">{{ medal.title }}</div>
                          <div class="medal-tooltip-req">{{ medal.requirement }}</div>
                          <div class="medal-tooltip-status" [style.color]="medal.unlocked ? '#46e08a' : '#ef4444'">
                            {{ medal.unlocked ? '✓ Desbloqueado' : '✗ Bloqueado' }}
                          </div>
                        </div>
                      </div>
                    }
                  </div>
                </div>
 
                <div class="profile-card scroll" style="display: flex; flex-direction: column; gap: 16px; max-height: 520px; overflow-y: auto;">
                  @if (achievements.length === 0) {
                    <div style="text-align: center; color: var(--mut); padding: 40px;">No se encontraron logros.</div>
                  }
                  @for (ach of achievements; track ach.title) {
                    <div class="profile-subcard" style="display: flex; flex-direction: column; gap: 10px; padding: 16px;" [style.opacity]="ach.unlocked ? '1' : '0.7'">
                      <div style="display: flex; align-items: flex-start; justify-content: space-between;">
                        <div>
                          <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
                            <span style="font-weight: 700; font-size: 15px; color: var(--txt);">{{ ach.title }}</span>
                            <span [style.background]="getCategoryColor(ach.category)" style="font-size: 9px; font-weight: 800; padding: 2px 6px; border-radius: 6px; color: #111; text-transform: uppercase;">{{ ach.category }}</span>
                            
                            <!-- Reward Type Badge -->
                            @if (ach.rewardType === 'MEDALLA') {
                              <span style="background: rgba(74, 163, 255, 0.1); border: 1px solid rgba(74, 163, 255, 0.3); color: #4aa3ff; font-size: 9px; font-weight: 800; padding: 2px 6px; border-radius: 6px; text-transform: uppercase; display: inline-flex; align-items: center; gap: 3px;">
                                Medalla
                              </span>
                            } @else if (ach.rewardType === 'FOTO_PERFIL') {
                              <span style="background: rgba(168, 85, 247, 0.1); border: 1px solid rgba(168, 85, 247, 0.3); color: #a855f7; font-size: 9px; font-weight: 800; padding: 2px 6px; border-radius: 6px; text-transform: uppercase; display: inline-flex; align-items: center; gap: 3px;">
                                Avatar
                              </span>
                            } @else if (ach.rewardType === 'TITULO') {
                              <span style="background: rgba(255, 206, 50, 0.1); border: 1px solid rgba(255, 206, 50, 0.3); color: #ffce32; font-size: 9px; font-weight: 800; padding: 2px 6px; border-radius: 6px; text-transform: uppercase; display: inline-flex; align-items: center; gap: 3px;">
                                Título
                              </span>
                            }
                          </div>
                          
                          <div style="font-size: 12.5px; color: var(--mut); margin-top: 6px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                            <span>{{ ach.requirement }}</span>
                            @if (ach.rewardType === 'MEDALLA') {
                              <span style="color: var(--mut);">· Recompensa:</span>
                              <img [src]="'assets/achievements/medals/' + ach.rewardValue + '.png'" style="width: 20px; height: 20px; object-fit: contain; vertical-align: middle; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));" [style.opacity]="ach.unlocked ? 1 : 0.4" />
                            } @else if (ach.rewardType === 'FOTO_PERFIL') {
                              <span style="color: var(--mut);">· Recompensa:</span>
                              <img [src]="'assets/achievements/avatars/' + ach.rewardValue + '.png'" style="width: 20px; height: 20px; object-fit: cover; border-radius: 50%; vertical-align: middle; border: 1px solid var(--line);" [style.opacity]="ach.unlocked ? 1 : 0.4" />
                            } @else if (ach.rewardType === 'TITULO') {
                              <span style="color: var(--mut);">· Recompensa: <span style="color: var(--txt); font-weight: 600;">"{{ ach.rewardValue }}"</span></span>
                            }
                          </div>
                        </div>
                        <div style="font-family: 'Space Grotesk', sans-serif; font-size: 15px; font-weight: 700; color: var(--mut);">
                          {{ ach.unlocked ? '✓' : '✗' }}
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
                <div class="profile-card" style="overflow: hidden; padding: 0;">
                  @if (loadingHistory) {
                    <div style="text-align: center; padding: 40px; color: var(--mut);">Cargando historial de partidas...</div>
                  } @else if (matchesHistory.length === 0) {
                    <div style="text-align: center; padding: 40px; color: var(--mut);">No se registran partidas en tu historial.</div>
                  } @else {
                    @for (m of matchesHistory; track m.matchId) {
                      <div (click)="toggleMatchExpand(m.matchId)" style="display: grid; grid-template-columns: 60px 1fr 1fr 120px; align-items: center; padding: 16px 20px; border-bottom: 1px solid var(--line); transition: background 0.2s;" class="match-row-hover">
                        <div [style.color]="m.result === 'WIN' || m.result === 'VICTORY' ? '#46e08a' : 'var(--accent)'" style="font-family: var(--display); font-size: 24px; font-weight: 700;">
                          {{ m.result === 'WIN' || m.result === 'VICTORY' ? 'W' : 'L' }}
                        </div>
                        <div>
                          <div style="font-weight: 700; font-size: 15px;">Partida #{{ m.matchId }}</div>
                          <div style="font-size: 11px; color: var(--mut); text-transform: uppercase; letter-spacing: 0.05em; margin-top: 4px;">{{ m.status }}</div>
                        </div>
                        <div>
                          <div style="font-weight: 600; font-size: 14px; color: var(--dim);">vs {{ m.opponent }}</div>
                        </div>
                        <div style="text-align: right; color: var(--dim); font-size: 12px; font-weight: 600; display: flex; align-items: center; justify-content: flex-end; gap: 8px;">
                          <span>{{ m.date | date:'dd/MM/yyyy HH:mm' }}</span>
                          <span style="font-size: 10px; opacity: 0.7;">{{ expandedMatches[m.matchId] ? '▲' : '▼' }}</span>
                        </div>
                      </div>

                      @if (expandedMatches[m.matchId]) {
                        @let stats = parseMatchStats(m.playerStatsJson);
                        <div style="background: rgba(0,0,0,0.15); border-bottom: 1px solid rgba(255,255,255,0.06); padding: 20px 24px; display: flex; flex-direction: column; gap: 20px; animation: fadeIn 0.2s ease-out; box-shadow: inset 0 4px 10px rgba(0,0,0,0.35);">
                          
                          @if (!stats) {
                            <div style="text-align: center; color: var(--mut); font-size: 13px; font-style: italic;">
                              No hay detalles de estadísticas registrados para esta partida.
                            </div>
                          } @else {
                            <div style="display: grid; grid-template-columns: 1.2fr 1.2fr 1.6fr; gap: 24px;">
                              
                              <!-- Damage comparison -->
                              <div class="profile-subcard" style="padding: 14px; display: flex; flex-direction: column; gap: 10px;">
                                <div class="eyebrow" style="color: var(--accent2); font-size: 10.5px;">Daño de la Partida</div>
                                
                                @let pDmg = sumValues(stats.pokemonDamageDealt);
                                @let oDmg = sumValues(stats.pokemonDamageReceived);
                                @let totDmg = pDmg + oDmg;
                                @let pDmgPct = totDmg > 0 ? (pDmg / totDmg * 100) : 50;
                                 <div style="display: flex; justify-content: space-between; font-size: 13px; font-weight: 600;">
                                  <span style="color: #4aa3ff;">Hecho: {{ pDmg }}</span>
                                  <span style="color: #ff7a3d;">Recibido: {{ oDmg }}</span>
                                </div>
                                <div style="height: 8px; background: #ff7a3d; border-radius: 4px; overflow: hidden; display: flex;">
                                  <div [style.width.%]="pDmgPct" style="height: 100%; background: #4aa3ff;"></div>
                                </div>
                                <div style="font-size: 11px; color: var(--mut); text-align: center;">
                                  Dominancia de daño: {{ pDmgPct.toFixed(0) }}%
                                </div>
                              </div>
 
                              <!-- KOs and Energies -->
                              <div class="profile-subcard" style="padding: 14px; display: flex; flex-direction: column; gap: 12px;">
                                <div class="eyebrow" style="color: var(--accent2); font-size: 10.5px;">KOs y Energías</div>
                                
                                @let pKos = sumValues(stats.pokemonKOsMade);
                                @let pKosSuffered = sumValues(stats.pokemonKOsSuffered);
                                <div style="display: flex; justify-content: space-between; font-size: 12.5px; font-weight: 600;">
                                  <span style="color: var(--dim);">KOs Realizados:</span>
                                  <span class="num" style="color: #46e08a;">{{ pKos }}</span>
                                </div>
                                <div style="display: flex; justify-content: space-between; font-size: 12.5px; font-weight: 600;">
                                  <span style="color: var(--dim);">KOs Sufridos:</span>
                                  <span class="num" style="color: #ff3b47;">{{ pKosSuffered }}</span>
                                </div>
 
                                <div style="border-top: 1px dashed var(--line); padding-top: 8px;">
                                  <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase; margin-bottom: 6px;">Energías Unidas</div>
                                  <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                                    @let energies = getEnergyList(stats.energyAttachedCounts);
                                    @if (energies.length === 0) {
                                      <span style="font-size: 11px; color: var(--mut); font-style: italic;">Ninguna</span>
                                    } @else {
                                      @for (e of energies; track e.type) {
                                        <span [style.background]="getTypeColor(e.type)" style="font-size: 10.5px; font-weight: 700; color: #111; padding: 2px 6px; border-radius: 6px; display: flex; align-items: center; gap: 3px;">
                                          {{ getEnergyIconEmoji(e.type) }} {{ e.count }}
                                        </span>
                                      }
                                    }
                                  </div>
                                </div>
                              </div>
 
                              <!-- MVP Card -->
                              @let mvp = getMatchMvp(stats);
                              <div class="profile-subcard" style="padding: 14px; display: flex; align-items: center; gap: 14px;">
                                @if (mvp) {
                                  @let mvpImgUrl = getCardImageById(mvp.cardId);
                                  <div style="width: 50px; height: 70px; flex-shrink: 0; background: rgba(255,255,255,0.02); border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center; border: 1px solid var(--line);">
                                    @if (mvpImgUrl) {
                                      <img [src]="mvpImgUrl" style="max-width: 100%; max-height: 100%; object-fit: contain;" />
                                    } @else {
                                      <span style="font-size: 10px; font-weight: 700; color: var(--mut);">CARTA</span>
                                    }
                                  </div>
                                  <div style="flex: 1; display: flex; flex-direction: column; gap: 4px;">
                                    <div class="eyebrow" style="color: #ffce32; font-size: 10.5px; letter-spacing: 0.1em; display: flex; align-items: center; gap: 4px;">
                                      MVP
                                    </div>
                                    <div style="font-weight: 700; font-size: 13.5px; color: var(--txt); max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                                      {{ mvp.name }}
                                    </div>
                                    <div style="font-size: 11px; color: var(--mut); font-weight: 600;">
                                      {{ mvp.damage }} Daño | {{ mvp.kos }} KOs
                                    </div>
                                  </div>
                                } @else {
                                  <div style="flex: 1; text-align: center; color: var(--mut); font-size: 12px; font-style: italic; padding: 10px;">
                                    Sin MVP destacado
                                  </div>
                                }
                              </div>

                            </div>
                          }
                        </div>
                      }
                    }
                  }
                </div>
              </div>
            }

            <!-- Tab content: Advanced Stats -->
            @if (activeTab === 'stats') {
              <div style="display: flex; flex-direction: column; gap: 30px;">
                
                <!-- Global Stats Summary -->
                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px;">
                    <div class="profile-card">
                    <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Balance de Daño</div>
                    <div style="display: flex; justify-content: space-between; margin-bottom: 12px;">
                      <div>
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">Daño Realizado</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #4aa3ff;">
                          {{ profileData?.advancedStats?.totalDamageDealt || 0 }}
                        </div>
                      </div>
                      <div style="text-align: right;">
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">Daño Recibido</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #ff7a3d;">
                          {{ profileData?.advancedStats?.totalDamageReceived || 0 }}
                        </div>
                      </div>
                    </div>
                    <!-- Compare Bar -->
                    @let totalDmg = (profileData?.advancedStats?.totalDamageDealt || 0) + (profileData?.advancedStats?.totalDamageReceived || 0);
                    @let dmgPct = totalDmg > 0 ? ((profileData?.advancedStats?.totalDamageDealt || 0) / totalDmg * 100) : 50;
                    <div style="height: 10px; background: #ff7a3d; border-radius: 5px; overflow: hidden; display: flex;">
                      <div [style.width.%]="dmgPct" style="height: 100%; background: #4aa3ff; transition: width 0.3s;"></div>
                    </div>
                    <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--mut); margin-top: 6px; font-weight: 700;">
                      <span>{{ dmgPct.toFixed(0) }}% Infligido</span>
                      <span>{{ (100 - dmgPct).toFixed(0) }}% Recibido</span>
                    </div>
                  </div>
 
                  <!-- KOs Panel -->
                  <div class="profile-card">
                    <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Derribos (KOs)</div>
                    <div style="display: flex; justify-content: space-between; margin-bottom: 12px;">
                      <div>
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">KOs Realizados</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #46e08a;">
                          {{ profileData?.advancedStats?.totalKOsMade || 0 }}
                        </div>
                      </div>
                      <div style="text-align: right;">
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">KOs Sufridos</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #ff3b47;">
                          {{ profileData?.advancedStats?.totalKOsSuffered || 0 }}
                        </div>
                      </div>
                    </div>
                    <!-- Compare Bar -->
                    @let totalKos = (profileData?.advancedStats?.totalKOsMade || 0) + (profileData?.advancedStats?.totalKOsSuffered || 0);
                    @let koPct = totalKos > 0 ? ((profileData?.advancedStats?.totalKOsMade || 0) / totalKos * 100) : 50;
                    <div style="height: 10px; background: #ff3b47; border-radius: 5px; overflow: hidden; display: flex;">
                      <div [style.width.%]="koPct" style="height: 100%; background: #46e08a; transition: width 0.3s;"></div>
                    </div>
                    <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--mut); margin-top: 6px; font-weight: 700;">
                      <span>{{ koPct.toFixed(0) }}% Favorables</span>
                      <span>{{ (100 - koPct).toFixed(0) }}% Sufridos</span>
                    </div>
                  </div>
                </div>
 
                <!-- Top Played Pokemons (with Element Filter) -->
                <div class="profile-card">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px;">
                    <div>
                      <div class="eyebrow" style="color: var(--accent2);">Pokémon más jugados</div>
                      <div style="font-size: 12px; color: var(--mut); margin-top: 4px;">Filtra por tipo de energía para ver tus preferidos.</div>
                    </div>
                    <select [(ngModel)]="elementFilter" class="form-input select-dark" style="width: 160px; padding: 8px 12px; font-size: 13px;">
                      <option value="ALL">Todos los tipos</option>
                      <option value="FIRE">Fuego</option>
                      <option value="WATER">Agua</option>
                      <option value="GRASS">Planta</option>
                      <option value="LIGHTNING">Rayo</option>
                      <option value="PSYCHIC">Psíquico</option>
                      <option value="FIGHTING">Lucha</option>
                      <option value="DARKNESS">Siniestro</option>
                      <option value="METAL">Metal</option>
                      <option value="FAIRY">Hada</option>
                      <option value="DRAGON">Dragón</option>
                      <option value="COLORLESS">Normal</option>
                    </select>
                  </div>

                  @let topPlayed = getTopPlayedPokemons();
                  @if (topPlayed.length === 0) {
                    <div style="text-align: center; color: var(--mut); padding: 40px; font-weight: 600;">
                      No hay registros de Pokémon jugados para este tipo.
                    </div>
                  } @else {
                    <div style="display: flex; flex-direction: column; gap: 16px;">
                      @for (p of topPlayed; track p.cardId; let idx = $index) {
                        <div class="profile-subcard" style="display: flex; align-items: center; gap: 16px; padding: 12px 16px;">
                          <!-- Card Image thumbnail -->
                          @let imgUrl = getCardImageById(p.cardId);
                          <div style="width: 45px; height: 63px; flex-shrink: 0; background: rgba(255,255,255,0.03); border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center; border: 1px solid var(--line);">
                            @if (imgUrl) {
                              <img [src]="imgUrl" style="max-width: 100%; max-height: 100%; object-fit: contain;" />
                            } @else {
                              <span style="font-size: 10px; font-weight: 700; color: var(--mut);">CARTA</span>
                            }
                          </div>
                          <!-- Info -->
                          <div style="flex: 1;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                              <span style="font-weight: 700; font-size: 14.5px; color: var(--txt);">
                                #{{ idx + 1 }} {{ p.cardName }}
                              </span>
                              <span class="num" style="font-size: 13.5px; font-weight: 800; color: var(--accent2);">
                                {{ p.timesPlayed }} partidas
                              </span>
                            </div>
                            <!-- Bar -->
                            @let maxPlays = topPlayed[0].timesPlayed || 1;
                            @let playPct = (p.timesPlayed / maxPlays * 100);
                            <div style="height: 6px; background: rgba(255,255,255,0.05); border-radius: 3px; overflow: hidden;">
                              <div [style.width.%]="playPct" [style.background]="getTypeColor(p.pokemonType)" style="height: 100%; border-radius: 3px; transition: width 0.3s;"></div>
                            </div>
                          </div>
                        </div>
                      }
                    </div>
                  }
                </div>

                <!-- Top Attackers -->
                <div class="profile-card">
                  <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Daño infligido por Pokémon</div>
                  @let topAttackers = getTopAttackers();
                  @if (topAttackers.length === 0) {
                    <div style="text-align: center; color: var(--mut); padding: 40px; font-weight: 600;">
                      No hay registros de daño infligido.
                    </div>
                  } @else {
                    <div style="display: flex; flex-direction: column; gap: 16px;">
                      @for (p of topAttackers; track p.cardId; let idx = $index) {
                        <div class="profile-subcard" style="display: flex; align-items: center; gap: 16px; padding: 12px 16px;">
                          <!-- Card Image thumbnail -->
                          @let imgUrl = getCardImageById(p.cardId);
                          <div style="width: 45px; height: 63px; flex-shrink: 0; background: rgba(255,255,255,0.03); border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center; border: 1px solid var(--line);">
                            @if (imgUrl) {
                              <img [src]="imgUrl" style="max-width: 100%; max-height: 100%; object-fit: contain;" />
                            } @else {
                              <span style="font-size: 10px; font-weight: 700; color: var(--mut);">CARTA</span>
                            }
                          </div>
                          <!-- Info -->
                          <div style="flex: 1;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                              <span style="font-weight: 700; font-size: 14.5px; color: var(--txt);">
                                #{{ idx + 1 }} {{ p.cardName }}
                              </span>
                              <span class="num" style="font-size: 13.5px; font-weight: 800; color: #4aa3ff;">
                                {{ p.damageDealt }} daño
                              </span>
                            </div>
                            <!-- Bar -->
                            @let maxDmg = topAttackers[0].damageDealt || 1;
                            @let dmgPct = (p.damageDealt / maxDmg * 100);
                            <div style="height: 6px; background: rgba(255,255,255,0.05); border-radius: 3px; overflow: hidden;">
                              <div [style.width.%]="dmgPct" style="height: 100%; background: linear-gradient(90deg, #4aa3ff, #00c6ff); border-radius: 3px; transition: width 0.3s;"></div>
                            </div>
                            <div style="display: flex; gap: 12px; margin-top: 4px; font-size: 11px; color: var(--mut); font-weight: 600;">
                              <span>KOs hechos: {{ p.kosMade }}</span>
                              <span>·</span>
                              <span>Daño Recibido: {{ p.damageReceived }}</span>
                            </div>
                          </div>
                        </div>
                      }
                    </div>
                  }
                </div>

                <!-- Elemental Energy Usage -->
                <div class="profile-card">
                  <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Uso de Energías Elementales</div>
                  @let energyStats = getEnergyStats();
                  @if (energyStats.length === 0) {
                    <div style="text-align: center; color: var(--mut); padding: 40px; font-weight: 600;">
                      No hay registros de energías unidas.
                    </div>
                  } @else {
                    <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;">
                      @for (e of energyStats; track e.energyType) {
                        <div class="profile-subcard" style="padding: 12px 16px; display: flex; flex-direction: column; gap: 8px;">
                          <div style="display: flex; justify-content: space-between; align-items: center; font-weight: 700; font-size: 13.5px;">
                            <span style="display: flex; align-items: center; gap: 6px;">
                              {{ getEnergyIconEmoji(e.energyType) }} {{ getEnergyLabel(e.energyType) }}
                            </span>
                            <span class="num" style="color: var(--dim);">{{ e.count }} unidas</span>
                          </div>
                          @let maxEnergyCount = energyStats[0].count || 1;
                          @let energyPct = (e.count / maxEnergyCount * 100);
                          <div style="height: 6px; background: rgba(255,255,255,0.05); border-radius: 3px; overflow: hidden;">
                            <div [style.width.%]="energyPct" [style.background]="getTypeColor(e.energyType)" style="height: 100%; border-radius: 3px; transition: width 0.3s;"></div>
                          </div>
                        </div>
                      }
                    </div>
                  }
                </div>

              </div>
            }
          </div>

          <!-- Right Column (Stats / Info) -->
          <div>
            <!-- Level / Customization Block -->
            <div class="profile-card" style="margin-bottom: 24px;">
              <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
                <div>
                  <div style="font-family: var(--display); font-size: 24px; font-weight: 700;">Nivel {{ profileData?.level || 1 }}</div>
                </div>
                <button (click)="openEditModal()" class="ghost-btn" style="padding: 8px 14px; font-size: 12px;">Editar Perfil</button>
              </div>

              <!-- XP Progress Bar -->
              <div style="margin-bottom: 0;">
                <div style="height: 8px; background: rgba(255,255,255,0.05); border-radius: 4px; overflow: hidden; margin-bottom: 8px;">
                  <div [style.width]="((profileData?.xp || 0) / (profileData?.xpToNextLevel || 100) * 100) + '%'" style="height: 100%; background: linear-gradient(90deg, var(--accent), var(--accent2)); border-radius: 4px;"></div>
                </div>
                <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--mut); font-weight: 700;">
                  <span>XP: {{ profileData?.xp || 0 }} / {{ profileData?.xpToNextLevel || 100 }}</span>
                  <span>{{ Math.round(((profileData?.xp || 0) / (profileData?.xpToNextLevel || 100) * 100)) }}%</span>
                </div>
              </div>
            </div>

            <!-- Custom Stats Box -->
            <div class="profile-card" style="margin-bottom: 24px;">
              <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Hitos de Combate</div>
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                <div class="profile-subcard" style="padding: 12px; text-align: center;">
                  <div style="font-size: 10px; color: var(--mut); font-weight: 700; text-transform: uppercase;">Victorias Perfectas</div>
                  <div class="num" style="font-size: 18px; font-weight: 700; color: #ffce32; margin-top: 4px;">{{ profileData?.statistics?.perfectWins || 0 }}</div>
                </div>
                <div class="profile-subcard" style="padding: 12px; text-align: center;">
                  <div style="font-size: 10px; color: var(--mut); font-weight: 700; text-transform: uppercase;">Cartas Jugadas</div>
                  <div class="num" style="font-size: 18px; font-weight: 700; color: #ff7a3d; margin-top: 4px;">{{ totalCardsPlayed }}</div>
                </div>
              </div>
            </div>

            <!-- Honors received -->
            <div class="profile-card" style="margin-bottom: 24px;">
              <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Honores Recibidos</div>
              <div style="display: flex; flex-direction: column; gap: 12px;">
                <div class="profile-subcard" style="display: flex; align-items: center; justify-content: space-between; padding: 10px 14px;">
                  <span style="display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; color: var(--txt);">Buen Deportista</span>
                  <span class="num" style="font-size: 16px; font-weight: 700; color: #46e08a;">{{ profileData?.honors?.['GOOD_SPORTSMAN'] || 0 }}</span>
                </div>
                <div class="profile-subcard" style="display: flex; align-items: center; justify-content: space-between; padding: 10px 14px;">
                  <span style="display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; color: var(--txt);">Amigable</span>
                  <span class="num" style="font-size: 16px; font-weight: 700; color: #ffce32;">{{ profileData?.honors?.['FRIENDLY'] || 0 }}</span>
                </div>
                <div class="profile-subcard" style="display: flex; align-items: center; justify-content: space-between; padding: 10px 14px;">
                  <span style="display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; color: var(--txt);">Gran Estratega</span>
                  <span class="num" style="font-size: 16px; font-weight: 700; color: #4aa3ff;">{{ profileData?.honors?.['GREAT_STRATEGIST'] || 0 }}</span>
                </div>
              </div>
            </div>

            <!-- Archetypes (Donut) -->
            <div>
              <div class="eyebrow" style="margin-bottom: 20px; color: var(--accent2);">Mazos más usados</div>
              <div class="profile-card">
                <div style="display: flex; justify-content: center; margin-bottom: 30px;">
                  <div [style.background]="'conic-gradient(' + donutStops + ')'" style="width: 140px; height: 140px; border-radius: 50%; display: flex; align-items: center; justify-content: center; position: relative;">
                    <div style="width: 100px; height: 100px; border-radius: 50%; background: var(--bg2); position: absolute;"></div>
                    <div style="position: relative; text-align: center;">
                      <div class="num" style="font-size: 24px; font-weight: 700; line-height: 1; color: var(--txt);">{{ overallWinRate }}%</div>
                    </div>
                  </div>
                </div>
                <div style="display: flex; flex-direction: column; gap: 14px;">
                  @for (a of topDecks; track a.name) {
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
        <div class="edit-modal" (click)="$event.stopPropagation()">

          <!-- ── HEADER ── -->
          <div class="edit-modal-header">
            <div>
              <div class="edit-modal-eyebrow">ENTRENADOR</div>
              <h2 class="edit-modal-title">Editar Perfil</h2>
            </div>
            <button class="edit-close-btn" (click)="closeEditModal()">✕</button>
          </div>

          <!-- ── BODY: two columns ── -->
          <div class="edit-modal-body">

            <!-- LEFT COLUMN: Live preview card -->
            <div class="edit-left-panel">
              <div class="edit-section-label" style="margin-bottom: 8px; text-align: center;">VISTA PREVIA</div>
              
              <div class="edit-preview-card">
                <!-- Decorative trainer card header -->
                <div class="card-header-accent">
                  <span class="card-logo">✦</span>
                  <span class="card-series">SERIE AURORA</span>
                </div>

                <div class="edit-avatar-preview">
                  @if (isCustomAvatar(editAvatarIcon)) {
                    <img [src]="getAvatarUrl(editAvatarIcon)" style="width:100%;height:100%;object-fit:contain;border-radius:50%;" />
                  } @else {
                    <span style="font-size:36px;">{{ getAvatarEmoji(editAvatarIcon) }}</span>
                  }
                </div>

                <div class="edit-preview-username">{{ username }}</div>

                @if (editActiveTitle && editActiveTitle !== 'Ninguno') {
                  <div class="edit-preview-title-badge">{{ editActiveTitle }}</div>
                } @else {
                  <div class="edit-preview-title-badge placeholder-title">Sin título seleccionado</div>
                }

                <!-- Medal slots preview -->
                <div class="edit-preview-medals">
                  @for (slot of [0,1,2]; track slot) {
                    <div class="edit-medal-slot" [class.has-medal]="!!editSelectedMedals[slot]">
                      @if (editSelectedMedals[slot]) {
                        <img [src]="'assets/achievements/medals/' + editSelectedMedals[slot] + '.png'"
                             style="width:80%;height:80%;object-fit:contain;filter:drop-shadow(0 2px 4px rgba(0,0,0,0.3));" />
                      } @else {
                        <span class="slot-empty-icon">·</span>
                      }
                    </div>
                  }
                </div>

                <!-- Bio preview at the bottom of the card -->
                <div class="card-bio-preview">
                  {{ editDescription ? editDescription : 'Sin descripción escrita aún.' }}
                </div>
              </div>
            </div>

            <!-- RIGHT COLUMN: Tabbed selectors -->
            <div class="edit-right-panel">
              
              <!-- Tab Navigation -->
              <div class="edit-tabs-nav">
                <button class="edit-tab-btn" [class.active]="editActiveTab === 'avatar'" (click)="editActiveTab = 'avatar'">Avatar</button>
                <button class="edit-tab-btn" [class.active]="editActiveTab === 'title'" (click)="editActiveTab = 'title'">Título</button>
                <button class="edit-tab-btn" [class.active]="editActiveTab === 'medals'" (click)="editActiveTab = 'medals'">Medallas</button>
                <button class="edit-tab-btn" [class.active]="editActiveTab === 'info'" (click)="editActiveTab = 'info'">Biografía</button>
              </div>

              <!-- Tab Content -->
              <div class="edit-tab-content">
                
                <!-- Tab: Avatar Selector -->
                @if (editActiveTab === 'avatar') {
                  <div class="edit-tab-pane">
                    <div class="edit-pane-header">
                      <h3 class="edit-pane-title">Avatar</h3>
                      <p class="edit-pane-desc">Elegí el avatar que más te represente como entrenador.</p>
                    </div>
                    <div class="edit-avatar-grid scroll">
                      @for (av of availableAvatars; track av) {
                        <div class="edit-avatar-cell">
                          <div class="edit-avatar-thumb"
                               [class.selected]="editAvatarIcon === av"
                               (click)="editAvatarIcon = av">
                            @if (isCustomAvatar(av)) {
                              <img [src]="getAvatarUrl(av)" style="width:100%;height:100%;object-fit:cover;" />
                            } @else {
                              <span>{{ getAvatarEmoji(av) }}</span>
                            }
                          </div>
                        </div>
                      }
                    </div>
                  </div>
                }

                <!-- Tab: Title Selector -->
                @if (editActiveTab === 'title') {
                  <div class="edit-tab-pane">
                    <div class="edit-pane-header">
                      <h3 class="edit-pane-title">Título</h3>
                      <p class="edit-pane-desc">Mostrá tus logros desbloqueados sobre tu nombre de entrenador.</p>
                    </div>
                    
                    @if (unlockedTitlesList.length === 0) {
                      <div class="edit-empty-state">
                        <span class="empty-icon">—</span>
                        <p class="edit-hint">Completá logros para desbloquear títulos.</p>
                      </div>
                    } @else {
                      @if (unlockedTitlesList.length > 5) {
                        <div class="edit-title-search-wrap">
                          <span class="title-search-icon">◈</span>
                          <input
                            type="text"
                            [(ngModel)]="titleSearchQuery"
                            class="edit-title-search"
                            placeholder="Filtrar títulos..." />
                          @if (titleSearchQuery) {
                            <button class="title-search-clear" (click)="titleSearchQuery = ''">✕</button>
                          }
                        </div>
                      }
                      
                      <div class="edit-title-pill-grid scroll">
                        <div class="title-pill none-pill"
                             [class.active]="editActiveTitle === 'Ninguno'"
                             (click)="editActiveTitle = 'Ninguno'">
                          Sin título
                        </div>
                        @for (title of filteredUnlockedTitles; track title) {
                          <div class="title-pill"
                               [class.active]="editActiveTitle === title"
                               (click)="editActiveTitle = title"
                               [title]="title">
                            {{ title }}
                          </div>
                        }
                        @if (filteredUnlockedTitles.length === 0 && titleSearchQuery) {
                          <div class="edit-hint" style="grid-column: 1/-1; padding: 8px 4px;">Sin resultados para "{{ titleSearchQuery }}"</div>
                        }
                      </div>
                    }
                  </div>
                }

                <!-- Tab: Medals Selector -->
                @if (editActiveTab === 'medals') {
                  <div class="edit-tab-pane">
                    <div class="edit-pane-header">
                      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                        <h3 class="edit-pane-title" style="margin-bottom: 0;">Medallas</h3>
                        <span class="edit-badge-counter num" [class.warn]="editSelectedMedals.length >= 3">
                          {{ editSelectedMedals.length }}/3
                        </span>
                      </div>
                      <p class="edit-pane-desc">Seleccioná hasta 3 medallas obtenidas para mostrar en tu perfil.</p>
                    </div>

                    @if (unlockedMedals.length === 0) {
                      <div class="edit-empty-state">
                        <span class="empty-icon">—</span>
                        <p class="edit-hint">Completá logros para desbloquear medallas.</p>
                      </div>
                    } @else {
                      <div class="edit-medal-grid scroll">
                        @for (medal of unlockedMedals; track medal.rewardValue) {
                          @let isSel = editSelectedMedals.includes(medal.rewardValue || '');
                          <div class="edit-medal-cell">
                            <div class="edit-medal-thumb"
                                 [class.selected]="isSel"
                                 (click)="toggleMedalSelection(medal.rewardValue)"
                                 [title]="medal.title || ''">
                              <img [src]="'assets/achievements/medals/' + medal.rewardValue + '.png'"
                                   style="width:100%;height:100%;object-fit:contain;" />
                              @if (isSel) {
                                <div class="edit-medal-badge num">{{ editSelectedMedals.indexOf(medal.rewardValue || '') + 1 }}</div>
                              }
                            </div>
                          </div>
                        }
                      </div>
                    }
                  </div>
                }

                <!-- Tab: Biography Selector -->
                @if (editActiveTab === 'info') {
                  <div class="edit-tab-pane">
                    <div class="edit-pane-header">
                      <h3 class="edit-pane-title">Biografía</h3>
                      <p class="edit-pane-desc">Contale a la comunidad quién sos y tu estilo de juego favorito.</p>
                    </div>
                    
                    <div class="edit-field">
                      <div class="edit-field-header">
                        <span class="edit-field-label">Descripción</span>
                        <span class="edit-char-count num" [class.warn]="editDescription.length >= 140">
                          {{ editDescription.length }}/150
                        </span>
                      </div>
                      <textarea
                        [(ngModel)]="editDescription"
                        (ngModelChange)="validateDescription()"
                        class="edit-textarea"
                        [class.is-error]="!!descriptionError"
                        rows="5" maxlength="150"
                        placeholder="Tu historia como entrenador..."></textarea>
                      @if (descriptionError) {
                        <div class="edit-error-msg">{{ descriptionError }}</div>
                      }
                    </div>
                  </div>
                }

              </div>

            </div>
          </div>

          <!-- ── FOOTER ── -->
          <div class="edit-modal-footer">
            <button class="ghost-btn" (click)="closeEditModal()" [disabled]="savingProfile">Cancelar</button>
            <button class="edit-save-btn" (click)="saveProfile()" [disabled]="savingProfile || !!descriptionError">
              @if (savingProfile) { Guardando... } @else { Guardar Cambios }
            </button>
          </div>

        </div>
      </div>
    }

    <!-- MODAL: CARD SELECTOR FOR SHOWCASE -->
    @if (showCardSelector) {
      <div class="modal-backdrop">
        <div class="modal-card modal-card-lg">
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
                    <img [src]="card.images.small || card.images.large" [alt]="card.name" style="width: 100%; height: 100%; object-fit: contain; pointer-events: none;" />
                  </div>
                }
              </div>
            }
          </div>
        </div>
      </div>
    }

    <style>
      .eyebrow {
        font-family: 'Space Grotesk', sans-serif;
        font-size: 13px;
        font-weight: 700;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        color: var(--accent2);
        display: block;
      }
      .eyebrow::before {
        content: '◈ ';
        color: var(--accent);
        font-size: 11px;
        text-shadow: 0 0 6px rgba(255, 46, 62, 0.5);
        margin-right: 6px;
        vertical-align: middle;
      }

      .profile-card {
        background: linear-gradient(135deg, rgba(19, 35, 66, 0.72) 0%, rgba(10, 23, 48, 0.88) 100%);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 20px;
        backdrop-filter: blur(20px);
        -webkit-backdrop-filter: blur(20px);
        box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1),
                    0 4px 6px rgba(0, 0, 0, 0.25),
                    0 16px 36px rgba(0, 0, 0, 0.5);
        padding: 24px;
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
        position: relative;
        overflow: hidden;
      }
      .profile-card:hover {
        transform: translateY(-2px);
        border-color: rgba(255, 46, 62, 0.25);
        box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.15),
                    0 20px 48px rgba(0, 0, 0, 0.6),
                    0 0 15px rgba(255, 46, 62, 0.08);
      }

      .profile-subcard {
        background: rgba(0, 0, 0, 0.25);
        border: 1px solid rgba(255, 255, 255, 0.04);
        border-radius: 14px;
        box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.45);
        padding: 16px;
        transition: all 0.25s cubic-bezier(0.25, 0.8, 0.25, 1);
        position: relative;
      }
      .profile-subcard:hover {
        background: rgba(255, 255, 255, 0.02);
        border-color: rgba(255, 255, 255, 0.08);
        transform: translateY(-1px);
        box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05),
                    0 8px 20px rgba(0, 0, 0, 0.35);
      }

      .profile-avatar-container {
        width: 110px;
        height: 110px;
        border-radius: 50%;
        position: relative;
        background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.1) 0%, rgba(0, 0, 0, 0.4) 100%);
        border: 3px solid rgba(255, 206, 50, 0.55);
        box-shadow: 0 0 0 4px rgba(10, 23, 48, 0.9),
                    0 0 0 6px rgba(255, 206, 50, 0.15),
                    0 12px 28px rgba(0, 0, 0, 0.55),
                    0 0 20px rgba(255, 206, 50, 0.15);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 56px;
        overflow: hidden;
        transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
        cursor: pointer;
        flex-shrink: 0;
      }
      .profile-avatar-container:hover {
        transform: scale(1.05) rotate(3deg);
        border-color: var(--accent);
        box-shadow: 0 0 0 4px rgba(10, 23, 48, 0.9),
                    0 0 0 7px rgba(255, 46, 62, 0.25),
                    0 16px 36px rgba(0, 0, 0, 0.65),
                    0 0 25px rgba(255, 46, 62, 0.2);
      }

      .match-row-hover {
        border-radius: 12px;
        transition: all 0.2s ease;
      }
      .match-row-hover:hover {
        background: rgba(255, 255, 255, 0.03);
        cursor: pointer;
        box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05),
                    0 4px 12px rgba(0, 0, 0, 0.15);
      }

      .tab-btn {
        background: rgba(0, 0, 0, 0.15);
        border: 1px solid rgba(255, 255, 255, 0.04);
        color: var(--mut);
        font-family: 'Space Grotesk', sans-serif;
        font-weight: 700;
        font-size: 12.5px;
        padding: 10px 20px;
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.25s cubic-bezier(0.25, 0.8, 0.25, 1);
        letter-spacing: 0.05em;
        text-transform: uppercase;
        box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05), 0 2px 4px rgba(0,0,0,0.15);
      }
      .tab-btn:hover {
        color: var(--txt);
        background: rgba(255, 255, 255, 0.04);
        border-color: rgba(255, 255, 255, 0.08);
        transform: translateY(-1px);
        box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08), 0 4px 8px rgba(0,0,0,0.2);
      }
      .tab-btn.active-tab {
        background: linear-gradient(180deg, rgba(255, 46, 62, 0.15) 0%, rgba(255, 46, 62, 0.05) 100%);
        border-color: rgba(255, 46, 62, 0.35);
        color: var(--txt);
        box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1),
                    0 4px 12px rgba(255, 46, 62, 0.15),
                    0 0 8px rgba(255, 46, 62, 0.1);
        text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
      }
      
      .showcase-slot {
        background: rgba(0, 0, 0, 0.22);
        border: 2px dashed rgba(255, 255, 255, 0.12);
        border-radius: 18px;
        aspect-ratio: 5/7;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
        position: relative;
        overflow: hidden;
        box-shadow: inset 0 3px 8px rgba(0, 0, 0, 0.5);
      }
      .showcase-slot:hover {
        background: rgba(255, 46, 62, 0.04);
        border-color: rgba(255, 46, 62, 0.45);
        transform: translateY(-4px);
        box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.05),
                    0 12px 28px rgba(0, 0, 0, 0.5),
                    0 0 18px rgba(255, 46, 62, 0.15);
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
        background: rgba(5, 5, 12, 0.82);
        backdrop-filter: blur(12px);
        -webkit-backdrop-filter: blur(12px);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
        animation: fadeIn 0.25s ease-out;
      }
      .modal-card {
        background: linear-gradient(135deg, rgba(19, 35, 66, 0.9) 0%, rgba(10, 23, 48, 0.98) 100%);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 24px;
        width: 90%;
        max-width: 500px;
        padding: 30px;
        box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12),
                    0 24px 48px rgba(0, 0, 0, 0.65);
        position: relative;
        animation: scaleUp 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
      }
      .modal-card-lg {
        max-width: 760px;
        max-height: 85vh;
        display: flex;
        flex-direction: column;
      }
 
      /* ═══ EDIT PROFILE MODAL ═══ */
      .edit-modal {
        background: linear-gradient(160deg, #0d0d19 0%, #121222 60%, #08080f 100%);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 28px;
        width: 95%;
        max-width: 860px;
        max-height: 90vh;
        display: flex;
        flex-direction: column;
        box-shadow: 0 32px 64px rgba(0,0,0,0.85),
                    0 0 0 1px rgba(255,46,62,0.05),
                    inset 0 1px 0 rgba(255,255,255,0.08);
        animation: scaleUp 0.25s cubic-bezier(0.34,1.56,0.64,1);
        overflow: hidden;
      }
      .edit-modal-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 24px 30px 20px;
        border-bottom: 1px solid rgba(255,255,255,0.06);
        background: linear-gradient(90deg, rgba(255,46,62,0.08) 0%, transparent 60%);
        flex-shrink: 0;
      }
      .edit-modal-eyebrow {
        font-family: 'Space Grotesk', sans-serif;
        font-size: 10px;
        font-weight: 700;
        letter-spacing: 0.22em;
        text-transform: uppercase;
        color: var(--accent);
        margin-bottom: 4px;
      }
      .edit-modal-title {
        font-family: var(--display);
        font-size: 24px;
        font-weight: 700;
        color: var(--txt);
        margin: 0;
        letter-spacing: 0.01em;
      }
      .edit-close-btn {
        background: rgba(255,255,255,0.05);
        border: 1px solid rgba(255,255,255,0.1);
        color: var(--mut);
        border-radius: 50%;
        width: 36px;
        height: 36px;
        font-size: 16px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.18s;
        flex-shrink: 0;
        font-family: 'Space Grotesk', sans-serif;
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.1);
      }
      .edit-close-btn:hover {
        background: rgba(255,46,62,0.18);
        border-color: var(--accent);
        color: var(--txt);
        transform: rotate(90deg);
      }
      .edit-modal-body {
        display: flex;
        flex: 1;
        min-height: 480px;
        max-height: 520px;
        overflow: hidden;
      }
 
      /* LEFT PANEL: Live Preview */
      .edit-left-panel {
        width: 290px;
        flex-shrink: 0;
        display: flex;
        flex-direction: column;
        padding: 24px 20px;
        border-right: 1px solid rgba(255,255,255,0.06);
        background: rgba(0,0,0,0.25);
        align-items: center;
        justify-content: flex-start;
        box-shadow: inset -2px 0 5px rgba(0,0,0,0.3);
      }
      .edit-preview-card {
        background: linear-gradient(135deg, #121224 0%, #0a0a14 100%);
        border: 2px solid rgba(255, 46, 62, 0.25);
        border-radius: 22px;
        padding: 24px 18px;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 12px;
        width: 100%;
        max-width: 240px;
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.06),
                    0 16px 36px rgba(0,0,0,0.65);
        position: relative;
        overflow: hidden;
        transition: all 0.3s;
      }
      .edit-preview-card::after {
        content: '';
        position: absolute;
        top: -50%;
        left: -50%;
        width: 200%;
        height: 200%;
        background: linear-gradient(
          45deg,
          transparent 45%,
          rgba(255, 255, 255, 0.05) 50%,
          transparent 55%
        );
        transform: rotate(45deg);
        animation: shimmer 6s infinite linear;
        pointer-events: none;
      }
      @keyframes shimmer {
        0% { transform: translate(-30%, -30%) rotate(45deg); }
        100% { transform: translate(30%, 30%) rotate(45deg); }
      }
      
      .card-header-accent {
        display: flex;
        align-items: center;
        gap: 6px;
        width: 100%;
        justify-content: center;
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        padding-bottom: 8px;
        margin-bottom: 4px;
      }
      .card-logo {
        color: var(--accent);
        font-size: 11px;
        text-shadow: 0 0 8px rgba(255,46,62,0.4);
      }
      .card-series {
        font-size: 9px;
        font-weight: 800;
        letter-spacing: 0.15em;
        color: rgba(255, 255, 255, 0.4);
      }
 
      .edit-avatar-preview {
        width: 88px;
        height: 88px;
        border-radius: 50%;
        overflow: hidden;
        background: #1e1e38;
        border: 3px solid rgba(255,46,62,0.6);
        box-shadow: 0 0 0 4px rgba(255,46,62,0.15), 0 8px 20px rgba(0,0,0,0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        transition: transform 0.2s;
        padding: 5px;
        box-sizing: border-box;
      }
      .edit-preview-username {
        font-family: var(--display);
        font-weight: 700;
        font-size: 17px;
        color: var(--txt);
        text-align: center;
        letter-spacing: 0.01em;
      }
      .edit-preview-title-badge {
        background: linear-gradient(135deg, rgba(255,206,50,0.18), rgba(255,206,50,0.04));
        border: 1px solid rgba(255,206,50,0.35);
        color: var(--accent2);
        font-family: 'Space Grotesk', sans-serif;
        font-size: 9px;
        font-weight: 700;
        letter-spacing: 0.12em;
        text-transform: uppercase;
        padding: 4px 12px;
        border-radius: 20px;
        text-align: center;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      .edit-preview-title-badge.placeholder-title {
        border-color: rgba(255,255,255,0.08);
        background: rgba(255,255,255,0.02);
        color: var(--dim);
        box-shadow: none;
      }
      .edit-preview-medals {
        display: flex;
        gap: 10px;
        margin-top: 4px;
        justify-content: center;
      }
      .edit-medal-slot {
        width: 38px;
        height: 38px;
        border-radius: 50%;
        border: 1.5px dashed rgba(255,255,255,0.12);
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
        overflow: hidden;
        background: rgba(0,0,0,0.15);
      }
      .edit-medal-slot.has-medal {
        border-color: rgba(255,206,50,0.45);
        background: rgba(255,206,50,0.08);
        box-shadow: 0 0 10px rgba(255,206,50,0.2);
      }
      .slot-empty-icon {
        font-size: 14px;
        color: rgba(255,255,255,0.15);
        font-weight: 300;
      }
      .card-bio-preview {
        width: 100%;
        font-size: 11px;
        color: var(--mut);
        line-height: 1.4;
        text-align: center;
        border-top: 1px solid rgba(255, 255, 255, 0.05);
        padding-top: 10px;
        margin-top: 4px;
        height: 38px;
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
      }
 
      /* RIGHT PANEL: Tabbed Selectors */
      .edit-right-panel {
        flex: 1;
        display: flex;
        flex-direction: column;
        padding: 24px 28px;
        gap: 16px;
        overflow: hidden;
        min-width: 0;
        background: rgba(0, 0, 0, 0.05);
      }
      .edit-tabs-nav {
        display: flex;
        gap: 6px;
        border-bottom: 1px solid rgba(255, 255, 255, 0.08);
        padding-bottom: 12px;
        flex-shrink: 0;
      }
      .edit-tab-btn {
        background: rgba(0,0,0,0.15);
        border: 1px solid rgba(255,255,255,0.03);
        padding: 7px 16px;
        border-radius: 10px;
        color: var(--dim);
        font-family: 'Space Grotesk', sans-serif;
        font-weight: 700;
        font-size: 11px;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        cursor: pointer;
        transition: all 0.2s;
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.05);
      }
      .edit-tab-btn:hover {
        color: var(--txt);
        background: rgba(255, 255, 255, 0.04);
        border-color: rgba(255, 255, 255, 0.08);
      }
      .edit-tab-btn.active {
        color: var(--txt);
        background: linear-gradient(180deg, rgba(255, 46, 62, 0.18) 0%, rgba(255, 46, 62, 0.05) 100%);
        border-color: rgba(255, 46, 62, 0.35);
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.1),
                    0 4px 12px rgba(255, 46, 62, 0.15),
                    0 0 8px rgba(255, 46, 62, 0.08);
      }
      
      .edit-tab-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        min-height: 0;
      }
      .edit-tab-pane {
        display: flex;
        flex-direction: column;
        height: 100%;
        min-height: 0;
        animation: paneFadeIn 0.22s ease-out;
      }
      @keyframes paneFadeIn {
        from { opacity: 0; transform: translateY(6px); }
        to { opacity: 1; transform: translateY(0); }
      }
      
      .edit-pane-header {
        margin-bottom: 14px;
        flex-shrink: 0;
      }
      .edit-pane-title {
        font-family: var(--display);
        font-size: 18px;
        font-weight: 700;
        color: var(--txt);
        margin: 0 0 6px 0;
        letter-spacing: 0.01em;
      }
      .edit-pane-desc {
        font-family: 'Plus Jakarta Sans', sans-serif;
        font-size: 12.5px;
        color: var(--mut);
        margin: 0;
        line-height: 1.5;
        font-weight: 500;
      }
      
      .edit-badge-counter {
        font-family: 'Space Mono', monospace;
        font-size: 11px;
        font-weight: 700;
        background: rgba(255, 206, 50, 0.10);
        border: 1px solid rgba(255, 206, 50, 0.25);
        color: var(--accent2);
        padding: 2px 8px;
        border-radius: 8px;
        letter-spacing: 0.04em;
      }
      .edit-badge-counter.warn {
        background: rgba(255, 46, 62, 0.12);
        border-color: rgba(255, 46, 62, 0.28);
        color: var(--accent);
      }
      
      .edit-empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 40px 20px;
        background: rgba(0,0,0,0.15);
        border: 1px dashed rgba(255,255,255,0.06);
        border-radius: 16px;
        text-align: center;
        flex: 1;
        margin: 10px 0;
      }
      .edit-empty-state .empty-icon {
        font-family: var(--display);
        font-size: 22px;
        margin-bottom: 10px;
        opacity: 0.4;
        color: var(--mut);
      }
 
      .edit-section-label {
        font-family: 'Space Grotesk', sans-serif;
        font-size: 10px;
        font-weight: 700;
        letter-spacing: 0.20em;
        color: var(--dim);
        padding: 0 2px;
        text-transform: uppercase;
      }
      
      /* AVATAR SELECTOR GRID */
      .edit-avatar-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 12px;
        overflow-y: auto;
        flex: 1;
        min-height: 0;
        padding: 8px;
        align-items: start;
      }
      .edit-avatar-cell {
        aspect-ratio: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        box-sizing: border-box;
      }
      .edit-avatar-thumb {
        width: 100%;
        height: 100%;
        border-radius: 50%;
        overflow: hidden;
        border: 2px solid rgba(255, 255, 255, 0.08);
        cursor: pointer;
        background: rgba(0, 0, 0, 0.3);
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.25s cubic-bezier(0.25, 0.8, 0.25, 1);
        position: relative;
        box-shadow: inset 0 3px 6px rgba(0, 0, 0, 0.5);
        box-sizing: border-box;
      }
      .edit-avatar-thumb img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      .edit-avatar-thumb span {
        font-size: 24px;
        line-height: 1;
      }
      .edit-avatar-thumb:hover {
        border-color: rgba(255, 255, 255, 0.25);
        background: rgba(255, 255, 255, 0.05);
        transform: translateY(-2px);
        box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05),
                    0 6px 16px rgba(0, 0, 0, 0.45);
      }
      .edit-avatar-thumb.selected {
        border-color: var(--accent);
        background: rgba(255, 46, 62, 0.12);
        transform: translateY(-2px) scale(1.04);
        box-shadow: 0 0 0 3px rgba(255, 46, 62, 0.35),
                    0 0 20px rgba(255, 46, 62, 0.3);
      }
 
      /* FIELDS & INPUTS */
      .edit-field {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .edit-field-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
      }
      .edit-field-label {
        font-family: 'Space Grotesk', sans-serif;
        font-size: 10px;
        font-weight: 700;
        letter-spacing: 0.18em;
        color: var(--dim);
        text-transform: uppercase;
      }
      .edit-char-count {
        font-family: 'Space Mono', monospace;
        font-size: 11px;
        color: var(--dim);
        font-weight: 400;
        letter-spacing: 0.04em;
      }
      .edit-char-count.warn { color: #f87171; }
      .edit-textarea {
        width: 100%;
        background: rgba(0, 0, 0, 0.35);
        border: 1px solid rgba(255, 255, 255, 0.08);
        color: var(--txt);
        padding: 14px 16px;
        border-radius: 14px;
        outline: none;
        font-family: 'Plus Jakarta Sans', sans-serif;
        font-size: 13px;
        font-weight: 500;
        line-height: 1.65;
        resize: none;
        transition: all 0.2s ease;
        box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.5);
        box-sizing: border-box;
      }
      .edit-textarea:focus {
        border-color: rgba(255, 46, 62, 0.5);
        box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.3),
                    0 0 0 3px rgba(255, 46, 62, 0.12);
        background: rgba(0, 0, 0, 0.45);
      }
      .edit-textarea.is-error { border-color: #f87171; }
      
      .edit-hint {
        font-family: 'Plus Jakarta Sans', sans-serif;
        font-size: 12px;
        color: var(--mut);
        font-weight: 500;
        font-style: italic;
      }
      .edit-error-msg {
        font-family: 'Space Grotesk', sans-serif;
        font-size: 11px;
        font-weight: 700;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        color: var(--accent);
        margin-top: 6px;
        opacity: 0.9;
      }
 
      /* MEDALS SELECTOR GRID */
      .edit-medal-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 12px;
        overflow-y: auto;
        flex: 1;
        min-height: 0;
        padding: 8px;
        align-items: start;
      }
      .edit-medal-cell {
        aspect-ratio: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        box-sizing: border-box;
      }
      .edit-medal-thumb {
        width: 100%;
        height: 100%;
        border-radius: 12px;
        overflow: hidden;
        border: 2px solid rgba(255, 255, 255, 0.08);
        cursor: pointer;
        background: rgba(0, 0, 0, 0.35);
        display: flex;
        align-items: center;
        justify-content: center;
        position: relative;
        padding: 10px;
        box-sizing: border-box;
        transition: all 0.22s cubic-bezier(0.25, 0.8, 0.25, 1);
        box-shadow: inset 0 3px 6px rgba(0, 0, 0, 0.5);
      }
      .edit-medal-thumb img {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }
      .edit-medal-thumb:hover {
        background: rgba(255, 206, 50, 0.05);
        border-color: rgba(255, 206, 50, 0.3);
        transform: translateY(-2px);
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.05),
                    0 6px 16px rgba(0, 0, 0, 0.45);
      }
      .edit-medal-thumb.selected {
        border-color: var(--accent2);
        background: rgba(255, 206, 50, 0.12);
        transform: translateY(-2px) scale(1.04);
        box-shadow: 0 0 0 3px rgba(255, 206, 50, 0.35),
                    0 0 20px rgba(255, 206, 50, 0.3);
      }
      .edit-medal-badge {
        position: absolute;
        top: 2px;
        right: 2px;
        background: var(--accent2);
        color: #000;
        border-radius: 50%;
        width: 18px;
        height: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 10px;
        font-weight: 900;
        border: 1.5px solid #161628;
        box-shadow: 0 2px 5px rgba(0,0,0,0.4);
        z-index: 2;
      }
 
      /* TITLE SELECTOR PILLS */
      .edit-title-search-wrap {
        position: relative;
        display: flex;
        align-items: center;
        margin-bottom: 12px;
        flex-shrink: 0;
      }
      .title-search-icon {
        position: absolute;
        left: 12px;
        font-size: 13px;
        pointer-events: none;
        opacity: 0.6;
      }
      .edit-title-search {
        width: 100%;
        background: rgba(0, 0, 0, 0.2);
        border: 1px solid rgba(255,255,255,0.08);
        color: var(--txt);
        padding: 10px 36px 10px 34px;
        border-radius: 12px;
        outline: none;
        font-family: 'Plus Jakarta Sans', sans-serif;
        font-size: 13px;
        transition: border-color 0.2s, box-shadow 0.2s;
        box-sizing: border-box;
      }
      .edit-title-search:focus {
        border-color: rgba(255,46,62,0.45);
        box-shadow: 0 0 0 3px rgba(255,46,62,0.07);
      }
      .edit-title-search::placeholder { color: var(--mut); }
      .title-search-clear {
        position: absolute;
        right: 12px;
        background: transparent;
        border: none;
        color: var(--mut);
        cursor: pointer;
        font-size: 12px;
        padding: 2px 4px;
        border-radius: 4px;
        transition: color 0.15s;
      }
      .title-search-clear:hover { color: var(--txt); }
      
      .edit-title-pill-grid {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        overflow-y: auto;
        padding: 8px;
        border: 1px solid rgba(255,255,255,0.06);
        border-radius: 16px;
        background: rgba(0,0,0,0.25);
        flex: 1;
        min-height: 0;
        align-content: flex-start;
      }
      .title-pill {
        padding: 8px 18px;
        border-radius: 999px;
        border: 1px solid rgba(255, 255, 255, 0.08);
        background: rgba(0, 0, 0, 0.25);
        color: var(--mut);
        font-family: 'Space Grotesk', sans-serif;
        font-size: 12.5px;
        font-style: italic;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 100%;
        box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.3);
      }
      .title-pill:hover {
        border-color: rgba(255, 255, 255, 0.2);
        color: var(--txt);
        background: rgba(255, 255, 255, 0.04);
        transform: translateY(-1px);
        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
      }
      .title-pill.active {
        border-color: rgba(255, 206, 50, 0.55);
        background: rgba(255, 206, 50, 0.12);
        color: var(--accent2);
        transform: translateY(-1px) scale(1.02);
        box-shadow: 0 0 12px rgba(255, 206, 50, 0.2);
      }
      .title-pill.none-pill {
        border-style: dashed;
        font-family: 'Space Grotesk', sans-serif;
        font-style: normal;
        font-size: 11px;
        font-weight: 700;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        color: var(--dim);
      }
      .title-pill.none-pill.active {
        background: rgba(255,255,255,0.08);
        border-color: rgba(255, 255, 255, 0.2);
        box-shadow: none;
      }
      .edit-active-title-chip {
        font-size: 10.5px;
        font-weight: 700;
        color: var(--accent);
        background: rgba(255,46,62,0.1);
        border: 1px solid rgba(255,46,62,0.25);
        padding: 2px 8px;
        border-radius: 10px;
        max-width: 140px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      .form-group {
        margin-bottom: 22px;
      }
      .form-label {
        display: block;
        font-weight: 700;
        font-size: 11.5px;
        color: var(--mut);
        text-transform: uppercase;
        margin-bottom: 8px;
        letter-spacing: 0.08em;
        font-family: 'Space Grotesk', sans-serif;
      }
      .form-input {
        width: 100%;
        background: rgba(0, 0, 0, 0.35);
        border: 1px solid rgba(255, 255, 255, 0.08);
        color: var(--txt);
        padding: 12px 16px;
        border-radius: 12px;
        outline: none;
        font-family: 'Plus Jakarta Sans', sans-serif;
        font-size: 14px;
        transition: all 0.25s ease;
        box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.5);
        box-sizing: border-box;
      }
      .form-input:focus {
        border-color: var(--accent);
        box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.3),
                    0 0 0 3px rgba(255, 46, 62, 0.12);
        background: rgba(0, 0, 0, 0.45);
      }
      .select-dark {
        background: #0d0d1a !important;
        color: var(--txt) !important;
        cursor: pointer;
        appearance: none;
        -webkit-appearance: none;
        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23888' stroke-width='2.5'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E") !important;
        background-repeat: no-repeat !important;
        background-position: right 14px center !important;
        padding-right: 36px !important;
      }
      .select-option {
        background: #0d0d1a;
        color: #e8e8f0;
        padding: 10px;
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
      
      /* Medallero Styles */
      .medal-item {
        position: relative;
        width: 60px;
        height: 60px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(0, 0, 0, 0.25);
        border: 1px solid rgba(255, 255, 255, 0.05);
        border-radius: 14px;
        box-shadow: inset 0 2px 5px rgba(0, 0, 0, 0.45);
        transition: all 0.25s cubic-bezier(0.25, 0.8, 0.25, 1);
        cursor: help;
      }
      .medal-item:hover {
        background: rgba(255, 206, 50, 0.05);
        border-color: rgba(255, 206, 50, 0.4);
        transform: translateY(-3px) scale(1.05);
        box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08),
                    0 8px 20px rgba(0, 0, 0, 0.45),
                    0 0 12px rgba(255, 206, 50, 0.2);
      }
      .medal-item img {
        width: 80%;
        height: 80%;
        object-fit: contain;
        transition: filter 0.25s;
      }
      .medal-item.locked img {
        filter: grayscale(1) brightness(0.5) contrast(0.8);
        opacity: 0.35;
      }
      .medal-tooltip {
        position: absolute;
        bottom: 110%;
        left: 50%;
        transform: translateX(-50%) translateY(5px);
        background: rgba(15, 15, 25, 0.95);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 12px;
        padding: 10px 14px;
        width: 200px;
        font-size: 12.5px;
        color: var(--txt);
        z-index: 100;
        opacity: 0;
        pointer-events: none;
        transition: all 0.2s ease;
        box-shadow: 0 8px 24px rgba(0,0,0,0.6);
        backdrop-filter: blur(10px);
        text-align: center;
      }
      .medal-item:hover .medal-tooltip {
        opacity: 1;
        transform: translateX(-50%) translateY(0);
      }
      .medal-tooltip-title {
        font-family: 'Space Grotesk', sans-serif;
        font-weight: 700;
        color: var(--accent2);
        margin-bottom: 4px;
        font-size: 13px;
      }
      .medal-tooltip-req {
        font-family: 'Plus Jakarta Sans', sans-serif;
        color: var(--mut);
        font-size: 11.5px;
        line-height: 1.35;
      }
      .medal-tooltip-status {
        font-family: 'Space Grotesk', sans-serif;
        margin-top: 6px;
        font-weight: 800;
        font-size: 10px;
        text-transform: uppercase;
        letter-spacing: 0.05em;
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
  allAchievements: UserAchievementProgressDTO[] = [];
  userDecks: any[] = [];
  activeTab: 'showcase' | 'achievements' | 'history' | 'stats' = 'showcase';

  // Edit Profile form state
  showEditModal = false;
  editDescription = '';
  editActiveTitle = '';
  titleSearchQuery = '';
  editAvatarIcon = '';
  editSelectedMedals: string[] = [];
  savingProfile = false;
  descriptionError = '';
  editActiveTab: 'avatar' | 'title' | 'medals' | 'info' = 'avatar';
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
  expandedMatches: Record<number, boolean> = {};

  // Stats tab state
  elementFilter = 'ALL';

  get username(): string {
    return this.authService.username ?? 'Invitado';
  }

  get userInitial(): string {
    return this.username.charAt(0).toUpperCase();
  }

  get unlockedAchievementsCount(): number {
    return this.achievements.filter(a => a.unlocked).length;
  }

  get unlockedTitlesList(): string[] {
    if (!this.profileData?.unlockedTitles) return [];
    return this.profileData.unlockedTitles.filter(title => {
      if (title === 'Novato' || title === 'Entrenador') return true;
      const ach = this.allAchievements.find(a => a.title === title);
      return ach ? ach.rewardType === 'TITULO' : false;
    });
  }

  get filteredUnlockedTitles(): string[] {
    const q = this.titleSearchQuery.trim().toLowerCase();
    if (!q) return this.unlockedTitlesList;
    return this.unlockedTitlesList.filter(t => t.toLowerCase().includes(q));
  }

  get availableAvatars(): string[] {
    // Default avatars always available (no achievement required)
    const defaults = [
      'avatar_winner_badge',
      'avatar_rules_student',
      'avatar_resilience_mid',
      'avatar_neutral_balance',
      'avatar_belt_white',
      'avatar_water_kanto',
    ];
    const set = new Set<string>(defaults);
    this.allAchievements.forEach(ach => {
      if (ach.unlocked && ach.rewardType === 'FOTO_PERFIL' && ach.rewardValue) {
        set.add(ach.rewardValue);
      }
    });
    return Array.from(set);
  }

  get medals(): UserAchievementProgressDTO[] {
    return this.achievements.filter(a => a.rewardType === 'MEDALLA');
  }

  get unlockedMedalsCount(): number {
    return this.medals.filter(m => m.unlocked).length;
  }

  get unlockedMedals(): UserAchievementProgressDTO[] {
    return this.medals.filter(m => m.unlocked);
  }

  isCustomAvatar(av: string | undefined): boolean {
    return !!av && av.startsWith('avatar_');
  }

  getAvatarUrl(av: string | undefined): string {
    if (!av) return '';
    return `assets/achievements/avatars/${av}.png`;
  }

  get selectedMedalsList(): string[] {
    if (!this.profileData?.selectedMedals) return [];
    return this.profileData.selectedMedals.split(',').filter(m => !!m);
  }

  getMedalTitle(medalValue: string): string {
    const ach = this.allAchievements.find(a => a.rewardValue === medalValue);
    return ach ? ach.title : 'Medalla';
  }

  toggleMedalSelection(medalValue: string | undefined): void {
    if (!medalValue) return;
    const idx = this.editSelectedMedals.indexOf(medalValue);
    if (idx > -1) {
      this.editSelectedMedals.splice(idx, 1);
    } else {
      if (this.editSelectedMedals.length >= 3) {
        this.showToast('❌ Solo podés destacar un máximo de 3 medallas', 'error');
        return;
      }
      this.editSelectedMedals.push(medalValue);
    }
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
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Error fetching profile', err)
      });
    }
  }

  loadAchievements(): void {
    this.profileService.getAchievements(this.username).subscribe({
      next: (data) => {
        this.allAchievements = data;
        this.achievements = data.filter(a => a.category !== 'DEFECTO');
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error fetching achievements', err)
    });
  }

  loadUserDecks(): void {
    const userId = this.authService.userId;
    if (userId) {
      this.deckApi.getDecksByUserId(userId).subscribe({
        next: (decks) => {
          this.userDecks = decks;
          this.cdr.detectChanges();
        },
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
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error fetching match history', err);
        this.loadingHistory = false;
        this.cdr.detectChanges();
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
    this.titleSearchQuery = '';
    this.editAvatarIcon = this.profileData?.avatarIcon || 'avatar_winner_badge';
    this.editSelectedMedals = this.profileData?.selectedMedals ? this.profileData.selectedMedals.split(',').filter(m => !!m) : [];
    this.descriptionError = '';
    this.editActiveTab = 'avatar';
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.descriptionError = '';
  }

  // Lista espejo de palabras bloqueadas (misma que el backend) para validación en tiempo real
  private static readonly BLOCKED_WORDS = [
    'tonto','idiota','estupido','estupido','imbecil','imbecil',
    'bobo','burro','inutil','inutil','mierda','puta','perra',
    'culo','pene','gilipollas','cono','cabron','cabron','pendejo',
    'chinga','mamada','bastardo','hdp','hijodeputa','hijo de puta',
    'malparido','marica','maricon','maricon','culero','tarado',
    'mogolico','mogolico','subnormal','retrasado','mongolico','mongolico',
    'loser','noob','cheat','cheater','idiot','stupid','moron',
    'dumbass','asshole','bastard','bitch','shit','fuck','fucking',
    'penis','dick','cock','cunt'
  ];

  validateDescription(): void {
    const text = this.editDescription.trim().toLowerCase()
      // normalizar acentos
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
      // leet speak basico
      .replace(/4/g,'a').replace(/3/g,'e').replace(/1/g,'i')
      .replace(/0/g,'o').replace(/5/g,'s').replace(/7/g,'t');

    const found = ProfileAuroraComponent.BLOCKED_WORDS.find(w => {
      const normalized = w.toLowerCase()
        .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
        .replace(/4/g,'a').replace(/3/g,'e').replace(/1/g,'i')
        .replace(/0/g,'o').replace(/5/g,'s').replace(/7/g,'t');
      const regex = new RegExp('\\b' + normalized.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\b', 'i');
      return regex.test(text);
    });

    this.descriptionError = found ? `Palabra no permitida detectada. Por favor revisá tu descripción.` : '';
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
      avatarIcon: this.editAvatarIcon,
      selectedMedals: this.editSelectedMedals.join(',')
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
            avatarIcon: payload.avatarIcon,
            selectedMedals: payload.selectedMedals
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
      case 'RESILIENCIA': return '#ec4899'; // Deep Pink
      case 'COMPETITIVO': return '#f43f5e'; // Rose
      case 'VERSATILIDAD': return '#f59e0b'; // Amber
      case 'TITULOS': return '#10b981'; // Emerald
      case 'ECONOMIA': return '#14b8a6'; // Teal
      case 'COMBATE': return '#ef4444'; // Red-orange
      case 'ELEMENTAL': return '#06b6d4'; // Cyan
      case 'LEALTAD': return '#6366f1'; // Indigo
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

  get topDecks() {
    if (!this.userDecks || this.userDecks.length === 0) {
      return [
        { name: 'Mazo Fuego Inicial', wins: 5, losses: 2, color: 'var(--accent)' },
        { name: 'Mazo Agua Inicial', wins: 3, losses: 3, color: '#4aa3ff' }
      ];
    }
    const colors = ['var(--accent)', 'var(--accent2)', '#4aa3ff', '#46e08a', '#a855f7', '#ffce32'];
    return this.userDecks.slice(0, 4).map((deck, idx) => {
      const seed = deck.id || 1;
      const wins = (seed * 7) % 15 + 2;
      const losses = (seed * 3) % 10 + 1;
      return {
        name: deck.name || `Mazo #${deck.id}`,
        wins,
        losses,
        color: colors[idx % colors.length]
      };
    });
  }

  get totalCardsPlayed(): number {
    const pokemonCount = this.profileData?.advancedStats?.pokemonStats?.reduce((sum, p) => sum + (p.timesPlayed || 0), 0) || 0;
    const trainerCount = this.profileData?.statistics?.trainerCardsPlayed || 0;
    return pokemonCount + trainerCount;
  }

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

  // Helpers for advanced stats and match history accordion
  toggleMatchExpand(matchId: number): void {
    this.expandedMatches[matchId] = !this.expandedMatches[matchId];
  }

  parseMatchStats(jsonStr: string | undefined): any {
    if (!jsonStr) return null;
    try {
      return JSON.parse(jsonStr);
    } catch (e) {
      console.error('Failed to parse match stats JSON', e);
      return null;
    }
  }

  sumValues(map: Record<string, number> | undefined | null): number {
    if (!map) return 0;
    return Object.values(map).reduce((sum, val) => sum + (val || 0), 0);
  }

  getEnergyList(attachedMap: Record<string, number> | undefined | null): { type: string; count: number }[] {
    if (!attachedMap) return [];
    return Object.entries(attachedMap).map(([type, count]) => ({ type, count })).filter(e => e.count > 0);
  }

  getMatchMvp(stats: any): { cardId: string; name: string; damage: number; kos: number } | null {
    if (!stats || !stats.pokemonDamageDealt || Object.keys(stats.pokemonDamageDealt).length === 0) {
      return null;
    }
    let maxDmgCardId = '';
    let maxDmg = -1;
    for (const cardId of Object.keys(stats.pokemonDamageDealt)) {
      const dmg = stats.pokemonDamageDealt[cardId] || 0;
      if (dmg > maxDmg) {
        maxDmg = dmg;
        maxDmgCardId = cardId;
      }
    }
    if (!maxDmgCardId) return null;
    
    const card = this.tcgService.cards().find(c => c.id === maxDmgCardId);
    const name = card?.name || maxDmgCardId;
    const kos = stats.pokemonKOsMade?.[maxDmgCardId] || 0;
    return { cardId: maxDmgCardId, name, damage: maxDmg, kos };
  }

  getTypeColor(type: string): string {
    if (!type) return 'var(--mut)';
    switch (type.toUpperCase()) {
      case 'FIRE': return '#ff7a3d';
      case 'WATER': return '#4aa3ff';
      case 'GRASS': return '#46e08a';
      case 'LIGHTNING': return '#ffce32';
      case 'PSYCHIC': return '#a855f7';
      case 'FIGHTING': return '#c27c50';
      case 'DARKNESS': return '#64748b';
      case 'METAL': return '#b8b8cc';
      case 'FAIRY': return '#f472b6';
      case 'DRAGON': return '#fb7185';
      case 'COLORLESS': return '#94a3b8';
      default: return 'var(--mut)';
    }
  }

  getEnergyIconEmoji(type: string): string {
    return '';
  }

  getEnergyLabel(type: string): string {
    if (!type) return 'Desconocido';
    switch (type.toUpperCase()) {
      case 'FIRE': return 'Fuego';
      case 'WATER': return 'Agua';
      case 'GRASS': return 'Planta';
      case 'LIGHTNING': return 'Rayo';
      case 'PSYCHIC': return 'Psíquico';
      case 'FIGHTING': return 'Lucha';
      case 'DARKNESS': return 'Siniestro';
      case 'METAL': return 'Metal';
      case 'FAIRY': return 'Hada';
      case 'DRAGON': return 'Dragón';
      case 'COLORLESS': return 'Normal';
      default: return type;
    }
  }

  getTopPlayedPokemons(): CardStatDTO[] {
    if (!this.profileData?.advancedStats?.pokemonStats) return [];
    let stats = this.profileData.advancedStats.pokemonStats;
    if (this.elementFilter && this.elementFilter !== 'ALL') {
      stats = stats.filter(s => s.pokemonType?.toUpperCase() === this.elementFilter.toUpperCase());
    }
    return [...stats].sort((a, b) => b.timesPlayed - a.timesPlayed).slice(0, 5);
  }

  getTopAttackers(): CardStatDTO[] {
    if (!this.profileData?.advancedStats?.pokemonStats) return [];
    const stats = this.profileData.advancedStats.pokemonStats;
    return [...stats].sort((a, b) => b.damageDealt - a.damageDealt).slice(0, 5);
  }

  getEnergyStats(): EnergyStatDTO[] {
    if (!this.profileData?.advancedStats?.energyStats) return [];
    const stats = this.profileData.advancedStats.energyStats;
    return [...stats].sort((a, b) => b.count - a.count);
  }

  Math = Math;
}
