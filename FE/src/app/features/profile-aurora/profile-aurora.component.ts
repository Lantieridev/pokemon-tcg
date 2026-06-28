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
import { HoloCardComponent } from '../../shared/ui/holo-card/holo-card.component';

@Component({
  selector: 'app-profile-aurora',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, StatComponent, IconComponent, AmbientComponent, HoloCardComponent],
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
            position: fixed; top: 100px; left: 50%; transform: translateX(-50%);
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
          <div class="avatar" style="width: 100px; height: 100px; font-size: 44px; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 0 6px var(--bg), 0 0 0 10px var(--accent); background: var(--surface); border-radius: 50%; overflow: hidden; padding: 0;">
            @if (isCustomAvatar(profileData?.avatarIcon)) {
              <img [src]="getAvatarUrl(profileData?.avatarIcon)" style="width: 100%; height: 100%; object-fit: contain; transform: scale(1.15);" />
            } @else {
              {{ getAvatarEmoji(profileData?.avatarIcon) }}
            }
          </div>
          <div>
            <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
              <h1 class="display" style="font-size: 54px; font-weight: 700; margin: 0; color: var(--txt); line-height: 1;">{{ username }}</h1>
              @if (profileData?.activeTitle) {
                <span style="background: linear-gradient(135deg, var(--accent2) 0%, rgba(255,255,255,0.05) 100%); border: 1px solid var(--line); color: var(--txt); padding: 4px 10px; border-radius: 8px; font-size: 11.5px; font-weight: 800; letter-spacing: 0.05em; text-transform: uppercase;">
                  🏅 {{ profileData?.activeTitle }}
                </span>
              }
              <!-- Selected Medals Showcase -->
              @if (selectedMedalsList.length > 0) {
                <div style="display: flex; gap: 8px; align-items: center; margin-left: 4px;">
                  @for (medal of selectedMedalsList; track medal) {
                    <img [src]="'assets/achievements/medals/' + medal + '.png'" style="width: 32px; height: 32px; object-fit: contain; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));" [title]="getMedalTitle(medal)" />
                  }
                </div>
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
              <button (click)="activeTab = 'collection'" [class.active-tab]="activeTab === 'collection'" class="tab-btn">Mi Colección</button>
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

                <!-- Showcase Deck Section -->
                <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px);">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <div>
                      <div class="eyebrow" style="color: var(--accent2);">Mazo Destacado</div>
                      <div style="font-size: 12px; color: var(--mut); margin-top: 4px;">Elegí el mazo que querés mostrar en tu perfil público.</div>
                    </div>
                  </div>

                  @if (profileData?.showcasedDeck) {
                    <div style="display: flex; align-items: center; gap: 20px; padding: 16px; background: rgba(255,255,255,0.02); border: 1px solid var(--line); border-radius: 14px; margin-bottom: 20px;">
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
                          <option [value]="d.id">🎴 {{ d.name }}</option>
                        }
                      </select>
                    </div>
                  </div>
                </div>
              </div>
            }

            <!-- Tab content: Collection -->
            @if (activeTab === 'collection') {
              <div style="display: flex; flex-direction: column; gap: 30px;">
                <div class="eyebrow" style="color: var(--accent2);">Mi Colección</div>
                
                @if (!profileData?.packCollection || profileData!.packCollection.length === 0) {
                  <div style="background: var(--surface); border: 1px dashed var(--line); border-radius: 20px; padding: 40px; text-align: center; color: var(--mut); backdrop-filter: blur(10px);">
                    Aún no tienes cartas obtenidas de sobres. ¡Abre sobres en la tienda para empezar tu colección!
                  </div>
                } @else {
                  <div>
                    <div style="display: flex; gap: 12px; margin-bottom: 24px; border-bottom: 1px solid var(--line); padding-bottom: 12px; overflow-x: auto;">
                       <button (click)="setCollectionFilter('LEGENDARIA')" [class.active-tab]="collectionFilter === 'LEGENDARIA'" class="tab-btn" style="color: #ffce32;">Legendarias ({{ getCollectionCountByRarity('LEGENDARIA') }})</button>
                       <button (click)="setCollectionFilter('EPICA')" [class.active-tab]="collectionFilter === 'EPICA'" class="tab-btn" style="color: #a855f7;">Épicas ({{ getCollectionCountByRarity('EPICA') }})</button>
                       <button (click)="setCollectionFilter('RARA')" [class.active-tab]="collectionFilter === 'RARA'" class="tab-btn" style="color: #4aa3ff;">Raras ({{ getCollectionCountByRarity('RARA') }})</button>
                       <button (click)="setCollectionFilter('COMUN')" [class.active-tab]="collectionFilter === 'COMUN'" class="tab-btn" style="color: #a1a1aa;">Comunes ({{ getCollectionCountByRarity('COMUN') }})</button>
                    </div>

                    <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 16px;">
                      @for (card of getPaginatedCollection(); track card.cardId + (card.isFoil || card.foil) + $index) {
                        <div class="collection-card" [class.is-foil]="card.isFoil || card.foil" [attr.data-rarity]="card.rarity" style="cursor: pointer;" (click)="zoomCard(card)">
                           <div class="collection-card-inner">
                              <img [src]="getCardImageById(card.cardId)" [alt]="card.cardName" style="width: 100%; display: block; border-radius: 6px;" />
                              @if (card.isFoil || card.foil) {
                                <div class="foil-overlay" style="position: absolute; inset: 0; background: linear-gradient(125deg, transparent 20%, rgba(255,255,255,0.4) 40%, rgba(255,255,255,0.8) 50%, rgba(255,255,255,0.4) 60%, transparent 80%); background-size: 200% 200%; mix-blend-mode: color-dodge; animation: foilShine 3s infinite linear; pointer-events: none; border-radius: 6px;"></div>
                                <div style="position: absolute; top: 4px; right: 4px; background: linear-gradient(135deg, #ffce32, #ff9800); color: #fff; font-size: 9px; font-weight: 800; padding: 2px 6px; border-radius: 4px; border: 1px solid #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.5);">✨ FOIL</div>
                              }
                           </div>
                           <div style="text-align: center; margin-top: 8px; font-size: 11.5px; font-weight: 700; color: var(--txt); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                              {{ card.cardName }}
                           </div>
                           <div style="text-align: center; font-size: 10px; margin-top: 2px; font-weight: 800; letter-spacing: 0.05em;"
                                [style.color]="getCollectionRarityColor(card.rarity)">
                              {{ card.rarity }}
                           </div>
                        </div>
                      }
                    </div>

                    @if (getTotalCollectionPages() > 1) {
                      <div style="display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px;">
                        <button class="ghost-btn sm" [disabled]="collectionPage === 0" (click)="prevCollectionPage()" [style.opacity]="collectionPage === 0 ? '0.5' : '1'" style="border-radius: 50%; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; padding: 0;">←</button>
                        <div style="font-family: var(--num); font-size: 13px; color: var(--mut);">Página {{ collectionPage + 1 }} de {{ getTotalCollectionPages() }}</div>
                        <button class="ghost-btn sm" [disabled]="collectionPage >= getTotalCollectionPages() - 1" (click)="nextCollectionPage()" [style.opacity]="collectionPage >= getTotalCollectionPages() - 1 ? '0.5' : '1'" style="border-radius: 50%; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; padding: 0;">→</button>
                      </div>
                    }
                  </div>
                }
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
                <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px); margin-bottom: 24px;">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                    <div style="font-family: var(--display); font-size: 16px; font-weight: 700; letter-spacing: 0.02em; display: flex; align-items: center; gap: 8px; color: var(--txt);">
                      🛡️ Medallero de Logros 
                      <span style="font-size: 12.5px; color: var(--mut); font-weight: 600;">({{ unlockedMedalsCount }} / 25)</span>
                    </div>
                  </div>
                  <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(60px, 1fr)); gap: 12px;">
                    @for (medal of medals; track medal.title) {
                      <div class="medal-item" [class.locked]="!medal.unlocked">
                        <img [src]="'assets/achievements/medals/' + medal.rewardValue + '.png'" [alt]="medal.title" loading="lazy" />
                        @if (!medal.unlocked) {
                          <div style="position: absolute; bottom: 3px; right: 3px; font-size: 8px; background: rgba(0,0,0,0.6); border-radius: 50%; width: 14px; height: 14px; display: flex; align-items: center; justify-content: center; color: #ffb8b8; border: 0.5px solid rgba(255,255,255,0.15);">🔒</div>
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
                            
                            <!-- Reward Type Badge -->
                            @if (ach.rewardType === 'MEDALLA') {
                              <span style="background: rgba(74, 163, 255, 0.1); border: 1px solid rgba(74, 163, 255, 0.3); color: #4aa3ff; font-size: 9px; font-weight: 800; padding: 2px 6px; border-radius: 6px; text-transform: uppercase; display: inline-flex; align-items: center; gap: 3px;">
                                🛡️ Medalla
                              </span>
                            } @else if (ach.rewardType === 'FOTO_PERFIL') {
                              <span style="background: rgba(168, 85, 247, 0.1); border: 1px solid rgba(168, 85, 247, 0.3); color: #a855f7; font-size: 9px; font-weight: 800; padding: 2px 6px; border-radius: 6px; text-transform: uppercase; display: inline-flex; align-items: center; gap: 3px;">
                                👤 Avatar
                              </span>
                            } @else if (ach.rewardType === 'TITULO') {
                              <span style="background: rgba(255, 206, 50, 0.1); border: 1px solid rgba(255, 206, 50, 0.3); color: #ffce32; font-size: 9px; font-weight: 800; padding: 2px 6px; border-radius: 6px; text-transform: uppercase; display: inline-flex; align-items: center; gap: 3px;">
                                🏅 Título
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
                        <div style="background: rgba(255,255,255,0.015); border-bottom: 1px solid var(--line); padding: 20px 24px; display: flex; flex-direction: column; gap: 20px; animation: fadeIn 0.2s ease-out;">
                          
                          @if (!stats) {
                            <div style="text-align: center; color: var(--mut); font-size: 13px; font-style: italic;">
                              No hay detalles de estadísticas registrados para esta partida.
                            </div>
                          } @else {
                            <div style="display: grid; grid-template-columns: 1.2fr 1.2fr 1.6fr; gap: 24px;">
                              
                              <!-- Damage comparison -->
                              <div style="background: rgba(255,255,255,0.01); border: 1px solid var(--line); border-radius: 12px; padding: 14px; display: flex; flex-direction: column; gap: 10px;">
                                <div class="eyebrow" style="color: var(--accent2); font-size: 10.5px;">Daño de la Partida</div>
                                
                                @let pDmg = sumValues(stats.pokemonDamageDealt);
                                @let oDmg = sumValues(stats.pokemonDamageReceived);
                                @let totDmg = pDmg + oDmg;
                                @let pDmgPct = totDmg > 0 ? (pDmg / totDmg * 100) : 50;
                                
                                <div style="display: flex; justify-content: space-between; font-size: 13px; font-weight: 600;">
                                  <span style="color: #4aa3ff;">💥 {{ pDmg }} Hecho</span>
                                  <span style="color: #ff7a3d;">🛡️ {{ oDmg }} Recibido</span>
                                </div>
                                <div style="height: 8px; background: #ff7a3d; border-radius: 4px; overflow: hidden; display: flex;">
                                  <div [style.width.%]="pDmgPct" style="height: 100%; background: #4aa3ff;"></div>
                                </div>
                                <div style="font-size: 11px; color: var(--mut); text-align: center;">
                                  Dominancia de daño: {{ pDmgPct.toFixed(0) }}%
                                </div>
                              </div>

                              <!-- KOs and Energies -->
                              <div style="background: rgba(255,255,255,0.01); border: 1px solid var(--line); border-radius: 12px; padding: 14px; display: flex; flex-direction: column; gap: 12px;">
                                <div class="eyebrow" style="color: var(--accent2); font-size: 10.5px;">KOs y Energías</div>
                                
                                @let pKos = sumValues(stats.pokemonKOsMade);
                                @let pKosSuffered = sumValues(stats.pokemonKOsSuffered);
                                <div style="display: flex; justify-content: space-between; font-size: 12.5px; font-weight: 600;">
                                  <span style="color: var(--dim);">KOs Realizados:</span>
                                  <span class="num" style="color: #46e08a;">⚡ {{ pKos }}</span>
                                </div>
                                <div style="display: flex; justify-content: space-between; font-size: 12.5px; font-weight: 600;">
                                  <span style="color: var(--dim);">KOs Sufridos:</span>
                                  <span class="num" style="color: #ff3b47;">💀 {{ pKosSuffered }}</span>
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
                              <div style="background: rgba(255,255,255,0.01); border: 1px solid var(--line); border-radius: 12px; padding: 14px; display: flex; align-items: center; gap: 14px;">
                                @if (mvp) {
                                  @let mvpImgUrl = getCardImageById(mvp.cardId);
                                  <div style="width: 50px; height: 70px; flex-shrink: 0; background: rgba(255,255,255,0.02); border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center; border: 1px solid var(--line);">
                                    @if (mvpImgUrl) {
                                      <img [src]="mvpImgUrl" style="max-width: 100%; max-height: 100%; object-fit: contain;" />
                                    } @else {
                                      <span style="font-size: 24px;">🃏</span>
                                    }
                                  </div>
                                  <div style="flex: 1; display: flex; flex-direction: column; gap: 4px;">
                                    <div class="eyebrow" style="color: #ffce32; font-size: 10.5px; letter-spacing: 0.1em; display: flex; align-items: center; gap: 4px;">
                                      👑 MVP
                                    </div>
                                    <div style="font-weight: 700; font-size: 13.5px; color: var(--txt); max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                                      {{ mvp.name }}
                                    </div>
                                    <div style="font-size: 11px; color: var(--mut); font-weight: 600;">
                                      🔥 {{ mvp.damage }} Daño | ⚡ {{ mvp.kos }} KOs
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
                  <!-- Daño Panel -->
                  <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px);">
                    <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Balance de Daño</div>
                    <div style="display: flex; justify-content: space-between; margin-bottom: 12px;">
                      <div>
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">Daño Realizado</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #4aa3ff;">
                          💥 {{ profileData?.advancedStats?.totalDamageDealt || 0 }}
                        </div>
                      </div>
                      <div style="text-align: right;">
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">Daño Recibido</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #ff7a3d;">
                          🛡️ {{ profileData?.advancedStats?.totalDamageReceived || 0 }}
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
                  <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px);">
                    <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Derribos (KOs)</div>
                    <div style="display: flex; justify-content: space-between; margin-bottom: 12px;">
                      <div>
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">KOs Realizados</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #46e08a;">
                          ⚡ {{ profileData?.advancedStats?.totalKOsMade || 0 }}
                        </div>
                      </div>
                      <div style="text-align: right;">
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">KOs Sufridos</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #ff3b47;">
                          💀 {{ profileData?.advancedStats?.totalKOsSuffered || 0 }}
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
                <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px);">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px;">
                    <div>
                      <div class="eyebrow" style="color: var(--accent2);">Pokémon más jugados</div>
                      <div style="font-size: 12px; color: var(--mut); margin-top: 4px;">Filtra por tipo de energía para ver tus preferidos.</div>
                    </div>
                    <select [(ngModel)]="elementFilter" class="form-input select-dark" style="width: 160px; padding: 8px 12px; font-size: 13px;">
                      <option value="ALL">🔍 Todos los tipos</option>
                      <option value="FIRE">🔥 Fuego</option>
                      <option value="WATER">💧 Agua</option>
                      <option value="GRASS">🌿 Planta</option>
                      <option value="LIGHTNING">⚡ Rayo</option>
                      <option value="PSYCHIC">🔮 Psíquico</option>
                      <option value="FIGHTING">👊 Lucha</option>
                      <option value="DARKNESS">🌙 Siniestro</option>
                      <option value="METAL">🔩 Metal</option>
                      <option value="FAIRY">🎀 Hada</option>
                      <option value="DRAGON">🐉 Dragón</option>
                      <option value="COLORLESS">⚪ Normal</option>
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
                        <div style="display: flex; align-items: center; gap: 16px; background: rgba(255,255,255,0.01); border: 1px solid var(--line); border-radius: 12px; padding: 12px 16px;">
                          <!-- Card Image thumbnail -->
                          @let imgUrl = getCardImageById(p.cardId);
                          <div style="width: 45px; height: 63px; flex-shrink: 0; background: rgba(255,255,255,0.03); border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center; border: 1px solid var(--line);">
                            @if (imgUrl) {
                              <img [src]="imgUrl" style="max-width: 100%; max-height: 100%; object-fit: contain;" />
                            } @else {
                              <span style="font-size: 20px;">🃏</span>
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
                <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px);">
                  <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Daño infligido por Pokémon</div>
                  @let topAttackers = getTopAttackers();
                  @if (topAttackers.length === 0) {
                    <div style="text-align: center; color: var(--mut); padding: 40px; font-weight: 600;">
                      No hay registros de daño infligido.
                    </div>
                  } @else {
                    <div style="display: flex; flex-direction: column; gap: 16px;">
                      @for (p of topAttackers; track p.cardId; let idx = $index) {
                        <div style="display: flex; align-items: center; gap: 16px; background: rgba(255,255,255,0.01); border: 1px solid var(--line); border-radius: 12px; padding: 12px 16px;">
                          <!-- Card Image thumbnail -->
                          @let imgUrl = getCardImageById(p.cardId);
                          <div style="width: 45px; height: 63px; flex-shrink: 0; background: rgba(255,255,255,0.03); border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center; border: 1px solid var(--line);">
                            @if (imgUrl) {
                              <img [src]="imgUrl" style="max-width: 100%; max-height: 100%; object-fit: contain;" />
                            } @else {
                              <span style="font-size: 20px;">🃏</span>
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
                <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px);">
                  <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Uso de Energías Elementales</div>
                  @let energyStats = getEnergyStats();
                  @if (energyStats.length === 0) {
                    <div style="text-align: center; color: var(--mut); padding: 40px; font-weight: 600;">
                      No hay registros de energías unidas.
                    </div>
                  } @else {
                    <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;">
                      @for (e of energyStats; track e.energyType) {
                        <div style="background: rgba(255,255,255,0.01); border: 1px solid var(--line); border-radius: 12px; padding: 12px 16px; display: flex; flex-direction: column; gap: 8px;">
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
            <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px); margin-bottom: 24px;">
              <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
                <div>
                  <div style="font-family: var(--display); font-size: 24px; font-weight: 700;">Nivel {{ profileData?.level || 1 }}</div>
                </div>
                <button (click)="openEditModal()" class="ghost-btn" style="padding: 8px 14px; font-size: 12px;">Editar Perfil</button>
              </div>

              <!-- XP Progress Bar -->
              <div style="margin-bottom: 0;">
                <div style="height: 8px; background: rgba(255,255,255,0.05); border-radius: 4px; overflow: hidden; margin-bottom: 8px;">
                  <div [style.width]="Math.min(100, ((profileData?.xp || 0) / (profileData?.xpToNextLevel || 100) * 100)) + '%'" style="height: 100%; background: linear-gradient(90deg, var(--accent), var(--accent2)); border-radius: 4px;"></div>
                </div>
                <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--mut); font-weight: 700;">
                  <span>XP: {{ profileData?.xp || 0 }} / {{ profileData?.xpToNextLevel || 100 }}</span>
                  <span>{{ Math.min(100, Math.round(((profileData?.xp || 0) / (profileData?.xpToNextLevel || 100) * 100))) }}%</span>
                </div>
              </div>
            </div>

            <!-- Custom Stats Box -->
            <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 24px; backdrop-filter: blur(10px); margin-bottom: 24px;">
              <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Hitos de Combate</div>
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                <div style="background: rgba(255,255,255,0.01); border: 1px solid var(--line); border-radius: 12px; padding: 12px; text-align: center;">
                  <div style="font-size: 10px; color: var(--mut); font-weight: 700; text-transform: uppercase;">Victorias Perfectas</div>
                  <div class="num" style="font-size: 18px; font-weight: 700; color: #ffce32; margin-top: 4px;">🏆 {{ profileData?.statistics?.perfectWins || 0 }}</div>
                </div>
                <div style="background: rgba(255,255,255,0.01); border: 1px solid var(--line); border-radius: 12px; padding: 12px; text-align: center;">
                  <div style="font-size: 10px; color: var(--mut); font-weight: 700; text-transform: uppercase;">Cartas Jugadas</div>
                  <div class="num" style="font-size: 18px; font-weight: 700; color: #ff7a3d; margin-top: 4px;">🃏 {{ totalCardsPlayed }}</div>
                </div>
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
              <div class="eyebrow" style="margin-bottom: 20px; color: var(--accent2);">Mazos más usados</div>
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
            <button class="edit-close-btn" (click)="closeEditModal()">&#x2715;</button>
          </div>

          <!-- ── BODY: two columns ── -->
          <div class="edit-modal-body">

            <!-- LEFT: preview + avatar picker -->
            <div class="edit-left-panel">

              <!-- Live preview card -->
              <div class="edit-preview-card">
                <div class="edit-avatar-preview">
                  @if (isCustomAvatar(editAvatarIcon)) {
                    <img [src]="getAvatarUrl(editAvatarIcon)" style="width:100%;height:100%;object-fit:contain;border-radius:50%;transform:scale(1.15);" />
                  } @else {
                    <span style="font-size:36px;">{{ getAvatarEmoji(editAvatarIcon) }}</span>
                  }
                </div>
                <div class="edit-preview-username">{{ username }}</div>
                @if (editActiveTitle && editActiveTitle !== 'Ninguno') {
                  <div class="edit-preview-title-badge">🏅 {{ editActiveTitle }}</div>
                }
                <!-- Medal slots preview -->
                <div class="edit-preview-medals">
                  @for (slot of [0,1,2]; track slot) {
                    <div class="edit-medal-slot" [class.has-medal]="!!editSelectedMedals[slot]">
                      @if (editSelectedMedals[slot]) {
                        <img [src]="'assets/achievements/medals/' + editSelectedMedals[slot] + '.png'"
                             style="width:100%;height:100%;object-fit:contain;" />
                      } @else {
                        <span class="slot-empty-icon">+</span>
                      }
                    </div>
                  }
                </div>
              </div>

              <!-- Avatar picker -->
              <div class="edit-section-label">FOTO DE PERFIL</div>
              <div class="edit-avatar-grid scroll">
                @for (av of availableAvatars; track av) {
                  <div class="edit-avatar-thumb"
                       [class.selected]="editAvatarIcon === av"
                       (click)="editAvatarIcon = av">
                    @if (isCustomAvatar(av)) {
                      <img [src]="getAvatarUrl(av)" style="width:100%;height:100%;object-fit:contain;border-radius:50%;transform:scale(1.15);" />
                    } @else {
                      <span>{{ getAvatarEmoji(av) }}</span>
                    }
                  </div>
                }
              </div>

            </div>

            <!-- RIGHT: form fields -->
            <div class="edit-right-panel">

              <!-- Description -->
              <div class="edit-field">
                <div class="edit-field-header">
                  <span class="edit-field-label">📝 Descripción</span>
                  <span class="edit-char-count" [class.warn]="editDescription.length >= 140">{{ editDescription.length }}/150</span>
                </div>
                <textarea
                  [(ngModel)]="editDescription"
                  (ngModelChange)="validateDescription()"
                  class="edit-textarea"
                  [class.is-error]="!!descriptionError"
                  rows="4" maxlength="150"
                  placeholder="Contale al mundo tu historia como entrenador..."></textarea>
                @if (descriptionError) {
                  <div class="edit-error-msg">⚠️ {{ descriptionError }}</div>
                }
              </div>

              <!-- Title picker: pill grid with optional search -->
              <div class="edit-field">
                <div class="edit-field-header">
                  <span class="edit-field-label">🏅 Título Activo</span>
                  @if (editActiveTitle && editActiveTitle !== 'Ninguno') {
                    <span class="edit-active-title-chip">{{ editActiveTitle }}</span>
                  }
                </div>
                @if (unlockedTitlesList.length === 0) {
                  <div class="edit-hint">Completá logros para desbloquear títulos.</div>
                } @else {
                  @if (unlockedTitlesList.length > 5) {
                    <div class="edit-title-search-wrap">
                      <span class="title-search-icon">🔍</span>
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
                    <!-- «Sin título» always first -->
                    <div class="title-pill none-pill"
                         [class.active]="editActiveTitle === 'Ninguno'"
                         (click)="editActiveTitle = 'Ninguno'">
                      — Sin título —
                    </div>
                    @for (title of filteredUnlockedTitles; track title) {
                      <div class="title-pill"
                           [class.active]="editActiveTitle === title"
                           (click)="editActiveTitle = title"
                           [title]="title">
                        🏅 {{ title }}
                      </div>
                    }
                    @if (filteredUnlockedTitles.length === 0 && titleSearchQuery) {
                      <div class="edit-hint" style="grid-column: 1/-1; padding: 8px 4px;">Sin resultados para "{{ titleSearchQuery }}"</div>
                    }
                  </div>
                }
              </div>

              <!-- Medals -->
              <div class="edit-field" style="flex:1;display:flex;flex-direction:column;min-height:0;">
                <div class="edit-field-header">
                  <span class="edit-field-label">🎖️ Medallas Destacadas</span>
                  <span class="edit-char-count" [class.warn]="editSelectedMedals.length >= 3">{{ editSelectedMedals.length }}/3</span>
                </div>
                @if (unlockedMedals.length === 0) {
                  <div class="edit-hint">Completá logros para desbloquear medallas.</div>
                } @else {
                  <div class="edit-medal-grid scroll">
                    @for (medal of unlockedMedals; track medal.rewardValue) {
                      @let isSel = editSelectedMedals.includes(medal.rewardValue || '');
                      <div class="edit-medal-thumb"
                           [class.selected]="isSel"
                           (click)="toggleMedalSelection(medal.rewardValue)"
                           [title]="medal.title || ''">
                        <img [src]="'assets/achievements/medals/' + medal.rewardValue + '.png'"
                             style="width:100%;height:100%;object-fit:contain;" />
                        @if (isSel) {
                          <div class="edit-medal-badge">{{ editSelectedMedals.indexOf(medal.rewardValue || '') + 1 }}</div>
                        }
                      </div>
                    }
                  </div>
                }
              </div>

            </div>
          </div>

          <!-- ── FOOTER ── -->
          <div class="edit-modal-footer">
            <button class="ghost-btn" (click)="closeEditModal()" [disabled]="savingProfile">Cancelar</button>
            <button class="edit-save-btn" (click)="saveProfile()" [disabled]="savingProfile || !!descriptionError">
              @if (savingProfile) { Guardando... } @else { 💾 Guardar Cambios }
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

    <!-- Fullscreen Card Zoom Modal Overlay -->
    @if (zoomedCard; as card) {
      <div class="fixed inset-0 z-[10000] flex items-center justify-center bg-black/85 backdrop-blur-sm cursor-pointer select-none"
           (click)="zoomedCard = null" style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.85); backdrop-filter: blur(4px);">
        <div class="relative max-w-[90vw] max-h-[90vh] flex items-center justify-center p-4 transition-all duration-300 transform scale-100"
             (click)="$event.stopPropagation()">
          <aurora-holo-card [card]="{ img: card.img, name: card.name, type: card.type, rarity: card.rarity, subtypes: card.subtypes }" [w]="400" [idleFloat]="false"></aurora-holo-card>
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

      /* ═══ EDIT PROFILE MODAL ═══ */
      .edit-modal {
        background: linear-gradient(160deg, #161628 0%, #1a1a30 60%, #14141f 100%);
        border: 1px solid rgba(255,255,255,0.09);
        border-radius: 28px;
        width: 95%;
        max-width: 720px;
        max-height: 90vh;
        display: flex;
        flex-direction: column;
        box-shadow: 0 32px 64px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,46,62,0.08), inset 0 1px 0 rgba(255,255,255,0.06);
        animation: scaleUp 0.22s cubic-bezier(0.34,1.56,0.64,1);
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
        font-size: 10px;
        font-weight: 800;
        letter-spacing: 0.14em;
        color: var(--accent);
        margin-bottom: 4px;
      }
      .edit-modal-title {
        font-family: var(--display);
        font-size: 22px;
        font-weight: 700;
        color: var(--txt);
        margin: 0;
      }
      .edit-close-btn {
        background: rgba(255,255,255,0.05);
        border: 1px solid rgba(255,255,255,0.1);
        color: var(--mut);
        border-radius: 50%;
        width: 36px;
        height: 36px;
        font-size: 18px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.18s;
        flex-shrink: 0;
      }
      .edit-close-btn:hover {
        background: rgba(255,46,62,0.15);
        border-color: var(--accent);
        color: var(--txt);
      }
      .edit-modal-body {
        display: flex;
        flex: 1;
        min-height: 0;
        overflow: hidden;
      }
      /* LEFT PANEL */
      .edit-left-panel {
        width: 210px;
        flex-shrink: 0;
        display: flex;
        flex-direction: column;
        padding: 20px 16px;
        border-right: 1px solid rgba(255,255,255,0.06);
        gap: 14px;
        overflow-y: auto;
        background: rgba(0,0,0,0.15);
      }
      .edit-preview-card {
        background: rgba(255,255,255,0.03);
        border: 1px solid rgba(255,255,255,0.07);
        border-radius: 18px;
        padding: 16px 12px 14px;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8px;
      }
      .edit-avatar-preview {
        width: 78px;
        height: 78px;
        border-radius: 50%;
        overflow: hidden;
        background: var(--surface);
        border: 3px solid rgba(255,46,62,0.4);
        box-shadow: 0 0 0 4px rgba(255,46,62,0.1), 0 8px 20px rgba(0,0,0,0.4);
        display: flex;
        align-items: center;
        justify-content: center;
        transition: border-color 0.2s;
      }
      .edit-preview-username {
        font-weight: 700;
        font-size: 13.5px;
        color: var(--txt);
        text-align: center;
      }
      .edit-preview-title-badge {
        background: linear-gradient(135deg, rgba(255,206,50,0.18), rgba(255,206,50,0.05));
        border: 1px solid rgba(255,206,50,0.3);
        color: var(--accent2);
        font-size: 10px;
        font-weight: 700;
        padding: 3px 10px;
        border-radius: 20px;
        text-align: center;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      .edit-preview-medals {
        display: flex;
        gap: 6px;
        margin-top: 2px;
      }
      .edit-medal-slot {
        width: 34px;
        height: 34px;
        border-radius: 8px;
        border: 1.5px dashed rgba(255,255,255,0.15);
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
        overflow: hidden;
        position: relative;
      }
      .edit-medal-slot.has-medal {
        border-color: rgba(255,206,50,0.4);
        background: rgba(255,206,50,0.06);
        box-shadow: 0 0 8px rgba(255,206,50,0.15);
      }
      .slot-empty-icon {
        font-size: 14px;
        color: rgba(255,255,255,0.2);
        font-weight: 300;
      }
      .edit-section-label {
        font-size: 10px;
        font-weight: 800;
        letter-spacing: 0.12em;
        color: var(--mut);
        padding: 0 2px;
      }
      .edit-avatar-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 8px;
        overflow-y: auto;
        flex: 1;
        min-height: 0;
        padding-right: 2px;
      }
      .edit-avatar-thumb {
        width: 52px;
        height: 52px;
        border-radius: 50%;
        overflow: hidden;
        border: 2.5px solid transparent;
        cursor: pointer;
        background: rgba(255,255,255,0.05);
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.18s;
        position: relative;
      }
      .edit-avatar-thumb:hover {
        transform: scale(1.08);
        border-color: rgba(255,255,255,0.25);
        box-shadow: 0 4px 12px rgba(0,0,0,0.4);
      }
      .edit-avatar-thumb.selected {
        border-color: var(--accent);
        box-shadow: 0 0 0 3px rgba(255,46,62,0.2), 0 4px 14px rgba(255,46,62,0.3);
      }
      /* RIGHT PANEL */
      .edit-right-panel {
        flex: 1;
        display: flex;
        flex-direction: column;
        padding: 22px 24px;
        gap: 18px;
        overflow-y: auto;
        min-width: 0;
      }
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
        font-size: 12px;
        font-weight: 800;
        letter-spacing: 0.07em;
        color: var(--mut);
        text-transform: uppercase;
      }
      .edit-char-count {
        font-size: 11px;
        color: var(--mut);
        font-weight: 600;
      }
      .edit-char-count.warn { color: #f87171; }
      .edit-textarea {
        width: 100%;
        background: rgba(255,255,255,0.04);
        border: 1px solid rgba(255,255,255,0.1);
        color: var(--txt);
        padding: 12px 14px;
        border-radius: 14px;
        outline: none;
        font-family: 'Manrope', sans-serif;
        font-size: 14px;
        line-height: 1.55;
        resize: none;
        transition: border-color 0.2s, box-shadow 0.2s;
        box-sizing: border-box;
      }
      .edit-textarea:focus {
        border-color: rgba(255,46,62,0.5);
        box-shadow: 0 0 0 3px rgba(255,46,62,0.08);
      }
      .edit-textarea.is-error { border-color: #f87171; }
      .edit-select {
        width: 100%;
        background: rgba(255,255,255,0.04);
        border: 1px solid rgba(255,255,255,0.1);
        color: var(--txt);
        padding: 11px 36px 11px 14px;
        border-radius: 14px;
        outline: none;
        font-family: 'Manrope', sans-serif;
        font-size: 14px;
        cursor: pointer;
        appearance: none;
        -webkit-appearance: none;
        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23888' stroke-width='2.5'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
        background-repeat: no-repeat;
        background-position: right 14px center;
        transition: border-color 0.2s;
        box-sizing: border-box;
      }
      .edit-select option { background: #1a1a2e; color: #e8e8f0; }
      .edit-select:focus {
        border-color: rgba(255,46,62,0.5);
        box-shadow: 0 0 0 3px rgba(255,46,62,0.08);
      }
      .edit-hint {
        font-size: 12px;
        color: var(--mut);
        font-style: italic;
      }
      .edit-error-msg {
        font-size: 12px;
        color: #f87171;
        font-weight: 600;
      }
      .edit-medal-grid {
        display: grid;
        grid-template-columns: repeat(5, 1fr);
        gap: 8px;
        overflow-y: auto;
        flex: 1;
        min-height: 80px;
        max-height: 160px;
        padding: 10px;
        border: 1px solid rgba(255,255,255,0.07);
        border-radius: 14px;
        background: rgba(0,0,0,0.15);
      }
      .edit-medal-thumb {
        width: 44px;
        height: 44px;
        border-radius: 10px;
        border: 2px solid transparent;
        cursor: pointer;
        background: rgba(255,255,255,0.03);
        display: flex;
        align-items: center;
        justify-content: center;
        position: relative;
        padding: 5px;
        box-sizing: border-box;
        transition: all 0.15s;
      }
      .edit-medal-thumb:hover {
        background: rgba(255,255,255,0.08);
        transform: scale(1.08);
      }
      .edit-medal-thumb.selected {
        border-color: var(--accent2);
        background: rgba(255,206,50,0.1);
        box-shadow: 0 0 10px rgba(255,206,50,0.2);
      }
      .edit-medal-badge {
        position: absolute;
        top: -6px;
        right: -6px;
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
      }
      .edit-modal-footer {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 12px;
        padding: 16px 24px;
        border-top: 1px solid rgba(255,255,255,0.06);
        flex-shrink: 0;
        background: rgba(0,0,0,0.1);
      }
      .edit-save-btn {
        background: linear-gradient(135deg, var(--accent) 0%, #c0152a 100%);
        color: #fff;
        border: none;
        border-radius: 12px;
        padding: 10px 24px;
        font-size: 13.5px;
        font-weight: 700;
        font-family: 'Manrope', sans-serif;
        cursor: pointer;
        transition: all 0.18s;
        box-shadow: 0 4px 14px rgba(255,46,62,0.3);
      }
      .edit-save-btn:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 6px 20px rgba(255,46,62,0.45);
      }
      .edit-save-btn:disabled {
        opacity: 0.45;
        cursor: not-allowed;
        transform: none;
      }

      /* ── TITLE PILL PICKER ── */
      .edit-title-search-wrap {
        position: relative;
        display: flex;
        align-items: center;
        margin-bottom: 8px;
      }
      .title-search-icon {
        position: absolute;
        left: 11px;
        font-size: 13px;
        pointer-events: none;
        opacity: 0.6;
      }
      .edit-title-search {
        width: 100%;
        background: rgba(255,255,255,0.04);
        border: 1px solid rgba(255,255,255,0.1);
        color: var(--txt);
        padding: 9px 36px 9px 32px;
        border-radius: 10px;
        outline: none;
        font-family: 'Manrope', sans-serif;
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
        right: 10px;
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
        gap: 7px;
        max-height: 130px;
        overflow-y: auto;
        padding: 10px;
        border: 1px solid rgba(255,255,255,0.07);
        border-radius: 14px;
        background: rgba(0,0,0,0.15);
      }
      .title-pill {
        display: inline-flex;
        align-items: center;
        padding: 5px 12px;
        border-radius: 20px;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        border: 1.5px solid rgba(255,255,255,0.1);
        background: rgba(255,255,255,0.04);
        color: var(--dim);
        transition: all 0.16s;
        white-space: nowrap;
        user-select: none;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .title-pill:hover {
        border-color: rgba(255,255,255,0.22);
        background: rgba(255,255,255,0.08);
        color: var(--txt);
        transform: translateY(-1px);
      }
      .title-pill.active {
        border-color: var(--accent);
        background: linear-gradient(135deg, rgba(255,46,62,0.2), rgba(255,46,62,0.08));
        color: var(--txt);
        box-shadow: 0 0 10px rgba(255,46,62,0.2), inset 0 0 0 1px rgba(255,46,62,0.15);
      }
      .title-pill.none-pill.active {
        border-color: rgba(255,255,255,0.3);
        background: rgba(255,255,255,0.08);
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
        box-sizing: border-box;
      }
      .form-input:focus {
        border-color: var(--accent);
      }
      /* Fix select dropdown options - force dark background */
      .select-dark {
        background: #1a1a2e !important;
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
        background: #1a1a2e;
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
        background: rgba(255, 255, 255, 0.02);
        border: 1px solid var(--line);
        border-radius: 12px;
        transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
        cursor: help;
      }
      .medal-item:hover {
        background: rgba(255, 255, 255, 0.08);
        border-color: var(--accent2);
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3), 0 0 10px rgba(255, 206, 50, 0.2);
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
        border: 1px solid var(--line);
        border-radius: 8px;
        padding: 10px 14px;
        width: 200px;
        font-size: 12px;
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
        font-weight: 700;
        color: var(--accent2);
        margin-bottom: 4px;
        font-size: 13px;
      }
      .medal-tooltip-req {
        color: var(--mut);
        font-size: 11px;
        line-height: 1.3;
      }
      .medal-tooltip-status {
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

  zoomedCard: any = null;

  profileData: UserProfileResponseDTO | null = null;
  achievements: UserAchievementProgressDTO[] = [];
  allAchievements: UserAchievementProgressDTO[] = [];
  userDecks: any[] = [];
  activeTab: 'showcase' | 'collection' | 'achievements' | 'history' | 'stats' = 'showcase';
  collectionFilter: string = 'COMUN';
  collectionPage: number = 0;

  // Edit Profile form state
  showEditModal = false;
  editDescription = '';
  editActiveTitle = '';
  titleSearchQuery = '';
  editAvatarIcon = '';
  editSelectedMedals: string[] = [];
  savingProfile = false;
  descriptionError = '';
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
    if (!this.profileData?.unlockedTitles) return ['Novato', 'Entrenador'];
    const titles = [...this.profileData.unlockedTitles];
    if (!titles.includes('Novato')) titles.push('Novato');
    if (!titles.includes('Entrenador')) titles.push('Entrenador');
    return titles;
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
    if (this.profileData?.unlockedAvatars) {
      this.profileData.unlockedAvatars.forEach(av => set.add(av));
    }
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
    if (!av) return false;
    const emojis = ['ash', 'misty', 'brock', 'gary', 'serena', 'red', 'default_trainer'];
    return !emojis.includes(av);
  }

  getAvatarUrl(av: string | undefined): string {
    if (!av) return '';

    // Nombres guardados en BD (por item.getName()) y por imageUrl
    if (av === 'Bulbasaur Clásico' || av === 'bulbasaur_classic') return 'assets/store/avatar_bulbasaur.png';
    if (av === 'Charmander Fuego' || av === 'charmander_fire') return 'assets/store/avatar_charmander.png';
    if (av === 'Squirtle Agua' || av === 'squirtle_water') return 'assets/store/avatar_squirtle.png';
    if (av === 'Ash Ketchum' || av === 'ash_avatar') return 'assets/store/avatar_ash.png';
    if (av === 'Misty' || av === 'misty_avatar') return 'assets/store/avatar_misty.png';
    if (av === 'Brock' || av === 'brock_avatar') return 'assets/store/avatar_brock.png';
    if (av === 'Charizard 3D Premium' || av === 'charizard_3d') return 'assets/store/avatar_charizard_3d.png';
    if (av === 'Mewtwo Legendario' || av === 'mewtwo_3d') return 'assets/store/avatar_mewtwo_3d.png';
    if (av === 'Pikachu Chibi' || av === 'pikachu_cute') return 'assets/store/avatar_pikachu_cute.png';
    if (av === 'collector_legend') return 'assets/store/avatar_collector.png';

    // Normalizamos el nombre para que coincida con los archivos guardados
    const normalizedValue = av.toLowerCase()
      .normalize("NFD").replace(/[\u0300-\u036f]/g, "") // Eliminar tildes
      .replace(/\s+/g, '_')
      .replace(/[^a-z0-9_]/g, '');

    const prefix = normalizedValue.startsWith('avatar_') ? '' : 'avatar_';

    return `assets/achievements/avatars/${prefix}${normalizedValue}.png`;
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

  getCollectionCountByRarity(rarity: string): number {
    if (!this.profileData?.packCollection) return 0;
    return this.profileData.packCollection.filter(c => c.rarity === rarity).length;
  }

  setCollectionFilter(rarity: string) {
    this.collectionFilter = rarity;
    this.collectionPage = 0;
  }

  getFilteredCollection() {
    if (!this.profileData?.packCollection) return [];
    let filtered = this.profileData.packCollection.filter(c => c.rarity === this.collectionFilter);
    return filtered.sort((a, b) => {
      const aFoil = a.isFoil || a.foil;
      const bFoil = b.isFoil || b.foil;
      if (aFoil && !bFoil) return -1;
      if (!aFoil && bFoil) return 1;
      return 0;
    });
  }

  getPaginatedCollection() {
    const filtered = this.getFilteredCollection();
    const start = this.collectionPage * 8;
    return filtered.slice(start, start + 8);
  }

  getTotalCollectionPages(): number {
    return Math.ceil(this.getFilteredCollection().length / 8);
  }

  prevCollectionPage() {
    if (this.collectionPage > 0) this.collectionPage--;
  }

  nextCollectionPage() {
    if (this.collectionPage < this.getTotalCollectionPages() - 1) this.collectionPage++;
  }

  getCollectionRarityColor(rarity: string): string {
    switch (rarity) {
      case 'LEGENDARIA': return '#ffce32';
      case 'EPICA': return '#a855f7';
      case 'RARA': return '#4aa3ff';
      default: return '#a1a1aa';
    }
  }

  zoomCard(card: any) {
    const fullCard = this.tcgService.cards().find(c => c.id === card.cardId);
    this.zoomedCard = {
      img: fullCard?.images?.large ?? fullCard?.images?.small ?? this.getCardImageById(card.cardId),
      name: card.cardName || fullCard?.name || 'Pokémon',
      type: (fullCard?.types && fullCard?.types[0]?.toLowerCase()) || 'colorless',
      rarity: (fullCard as any)?.rarity || card.rarity || 'Common',
      subtypes: fullCard?.subtypes || []
    };
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
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.descriptionError = '';
  }

  // Lista espejo de palabras bloqueadas (misma que el backend) para validación en tiempo real
  private static readonly BLOCKED_WORDS = [
    'tonto', 'idiota', 'estupido', 'estupido', 'imbecil', 'imbecil',
    'bobo', 'burro', 'inutil', 'inutil', 'mierda', 'puta', 'perra',
    'culo', 'pene', 'gilipollas', 'cono', 'cabron', 'cabron', 'pendejo',
    'chinga', 'mamada', 'bastardo', 'hdp', 'hijodeputa', 'hijo de puta',
    'malparido', 'marica', 'maricon', 'maricon', 'culero', 'tarado',
    'mogolico', 'mogolico', 'subnormal', 'retrasado', 'mongolico', 'mongolico',
    'loser', 'noob', 'cheat', 'cheater', 'idiot', 'stupid', 'moron',
    'dumbass', 'asshole', 'bastard', 'bitch', 'shit', 'fuck', 'fucking',
    'penis', 'dick', 'cock', 'cunt'
  ];

  validateDescription(): void {
    const text = this.editDescription.trim().toLowerCase()
      // normalizar acentos
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
      // leet speak basico
      .replace(/4/g, 'a').replace(/3/g, 'e').replace(/1/g, 'i')
      .replace(/0/g, 'o').replace(/5/g, 's').replace(/7/g, 't');

    const found = ProfileAuroraComponent.BLOCKED_WORDS.find(w => {
      const normalized = w.toLowerCase()
        .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
        .replace(/4/g, 'a').replace(/3/g, 'e').replace(/1/g, 'i')
        .replace(/0/g, 'o').replace(/5/g, 's').replace(/7/g, 't');
      const regex = new RegExp('\\b' + normalized.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\b', 'i');
      return regex.test(text);
    });

    this.descriptionError = found ? `Palabra no permitida detectada. Por favor revisá tu descripción.` : '';
  }

  showToast(message: string, type: 'success' | 'error' = 'success'): void {
    this.toastMessage = message;
    this.toastType = type;
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
    this.toastTimeout = setTimeout(() => {
      this.toastMessage = '';
      this.cdr.detectChanges();
    }, 3000);
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
          this.profileService.profile$.next(this.profileData);
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
    if (!type) return '⚪';
    switch (type.toUpperCase()) {
      case 'FIRE': return '🔥';
      case 'WATER': return '💧';
      case 'GRASS': return '🌿';
      case 'LIGHTNING': return '⚡';
      case 'PSYCHIC': return '🔮';
      case 'FIGHTING': return '👊';
      case 'DARKNESS': return '🌙';
      case 'METAL': return '🔩';
      case 'FAIRY': return '🎀';
      case 'DRAGON': return '🐉';
      case 'COLORLESS': return '⚪';
      default: return '⚪';
    }
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

