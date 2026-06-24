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
import { TutorialService } from '../../core/services/tutorial.service';

@Component({
  selector: 'app-profile-aurora',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, StatComponent, IconComponent, TrainerChipComponent, AmbientComponent, LogoComponent],
  encapsulation: ViewEncapsulation.None,
  template: `
    <div class="scene v-aurora prf-root">

      <!-- ░░ DYNAMIC BACKGROUND ░░ -->
      <div class="prf-bg-layer">
        <div class="prf-orb prf-orb-1"></div>
        <div class="prf-orb prf-orb-2"></div>
        <div class="prf-orb prf-orb-3"></div>
        <div class="prf-orb prf-orb-4"></div>
        <div class="prf-scan-line"></div>
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

      <!-- Topbar spacer -->
      <div style="height: 92px; width: 100%; flex: 0 0 auto;"></div>

      <!-- ░░ MAIN CONTENT ░░ -->
      <div class="prf-content">

        <!-- ═══ HERO HEADER BANNER ═══ -->
        <div class="prf-hero fu">
          <!-- Left: Avatar + Identity -->
          <div class="prf-hero-identity">
            <div class="prf-avatar-wrap">
              <div class="prf-avatar-ring"></div>
              <div class="profile-avatar-container">
                @if (isCustomAvatar(profileData?.avatarIcon)) {
                  <img [src]="getAvatarUrl(profileData?.avatarIcon)" style="width: 100%; height: 100%; object-fit: cover;" />
                } @else {
                  {{ getAvatarEmoji(profileData?.avatarIcon) }}
                }
              </div>
            </div>

            <div class="prf-identity-info">
              <div class="prf-identity-row" style="display: flex; align-items: center; gap: 10px;">
                <h1 class="display prf-username name-energy" style="margin: 0;">{{ username }}</h1>
                @if (profileData?.activeTitle) {
                  <span class="prf-title-badge">{{ profileData?.activeTitle }}</span>
                }
                <button class="help-trigger-btn" (click)="triggerHelp()" title="Ver Tutorial">
                  <aurora-icon n="help" [s]="13"></aurora-icon>
                </button>
              </div>

              <div class="prf-medals-row">
                @if (selectedMedalsList.length > 0) {
                  @for (medal of selectedMedalsList; track medal) {
                    <div class="prf-medal-chip" [title]="getMedalTitle(medal)">
                      <img [src]="'assets/achievements/medals/' + medal + '.png'" style="width: 22px; height: 22px; object-fit: contain;" />
                    </div>
                  }
                }
              </div>

              <p class="prf-bio">{{ profileData?.description || 'Sin descripción de entrenador.' }}</p>
            </div>
          </div>

          <!-- Right: Stats Cluster -->
          <div id="perfil-stats" class="prf-stats-cluster">
            <div class="prf-stat-chip prf-stat-wins">
              <span class="prf-stat-value">{{ totalWins }}</span>
              <span class="prf-stat-label">Victorias</span>
            </div>
            <div class="prf-stat-divider"></div>
            <div class="prf-stat-chip prf-stat-losses">
              <span class="prf-stat-value">{{ totalLosses }}</span>
              <span class="prf-stat-label">Derrotas</span>
            </div>
            <div class="prf-stat-divider"></div>
            <div class="prf-stat-chip prf-stat-wr">
              <span class="prf-stat-value">{{ overallWinRate }}%</span>
              <span class="prf-stat-label">Win Rate</span>
            </div>
          </div>
        </div>

        <!-- ═══ MAIN 2-COLUMN GRID ═══ -->
        <div class="prf-grid fu" style="animation-delay: 0.1s;">

          <!-- LEFT: Tabbed Content -->
          <div class="prf-col-main">

            <!-- Tab Nav -->
            <div id="perfil-tabs" class="prf-tabs-nav">
              <button class="prf-tab" [class.active]="activeTab === 'showcase'" (click)="activeTab = 'showcase'">
                <span class="prf-tab-icon">✦</span> Vitrina y Mazo
              </button>
              <button class="prf-tab" [class.active]="activeTab === 'achievements'" (click)="activeTab = 'achievements'">
                <span class="prf-tab-icon">◈</span> Logros y Títulos
              </button>
              <button class="prf-tab" [class.active]="activeTab === 'stats'" (click)="activeTab = 'stats'">
                <span class="prf-tab-icon">▲</span> Estadísticas
              </button>
              <button class="prf-tab" [class.active]="activeTab === 'history'" (click)="activeTab = 'history'">
                <span class="prf-tab-icon">⊟</span> Historial
              </button>
            </div>

            <!-- ── Tab: SHOWCASE ── -->
            @if (activeTab === 'showcase') {
              <div class="prf-tab-body" style="display: flex; flex-direction: column; gap: 30px;">

                <!-- Card Showcase Slots -->
                <div id="perfil-vitrinas" class="prf-panel">
                  <div class="prf-panel-header">
                    <div class="eyebrow">Vitrina de Cartas Destacadas</div>
                    <span class="prf-panel-hint">Clic o arrastra para colocar cartas</span>
                  </div>
                  <div class="prf-showcase-grid">
                    @for (pos of [1, 2, 3]; track pos) {
                      @let slot = getShowcaseSlot(pos);
                      <div class="showcase-slot"
                           (click)="openCardSelector(pos)"
                           (dragover)="allowDrop($event)"
                           (drop)="handleDropOnSlot($event, pos)">
                        <div class="showcase-slot-glow"></div>
                        @if (slot) {
                          <img [src]="getCardImageById(slot.cardId)" [alt]="slot.cardName" style="max-height: 100%; max-width: 100%; pointer-events: none; position: relative; z-index: 1;" />
                          <button class="remove-btn" (click)="removeCardFromShowcase(pos, $event)">×</button>
                        } @else {
                          <div class="showcase-slot-empty">
                            <div class="showcase-empty-icon">＋</div>
                            <div class="showcase-empty-label">Vacío</div>
                          </div>
                        }
                      </div>
                    }
                  </div>
                </div>

                <!-- Showcase Deck -->
                <div class="prf-panel">
                  <div class="prf-panel-header">
                    <div class="eyebrow">Mazo Destacado</div>
                    <span class="prf-panel-hint">Se muestra en tu perfil público</span>
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

            <!-- ── Tab: ACHIEVEMENTS ── -->
            @if (activeTab === 'achievements') {
              <div class="prf-tab-body">
                <div class="eyebrow" style="margin-bottom: 20px; color: var(--accent2);">
                  Logros y Títulos de Entrenador
                  <div style="font-size: 14.5px; color: var(--mut); margin-top: 6px; font-family: Space Grotesk, sans-serif; text-transform: none; letter-spacing: normal;">
                    {{ unlockedAchievementsCount }}/{{ achievements.length }}
                  </div>
                </div>

                <!-- Medallero -->
                <div class="prf-panel" style="margin-bottom: 24px;">
                  <div class="prf-panel-header">
                    <div style="font-family: var(--display); font-size: 16px; font-weight: 700; display: flex; align-items: center; gap: 8px;">
                      Medallero de Logros
                      <span style="font-size: 12.5px; color: var(--mut); font-weight: 600;">({{ unlockedMedalsCount }} / 25)</span>
                    </div>
                  </div>
                  <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(60px, 1fr)); gap: 12px;">
                    @for (medal of medals; track medal.title) {
                      <div class="medal-item" [class.locked]="!medal.unlocked">
                        <img [src]="'assets/achievements/medals/' + medal.rewardValue + '.png'" [alt]="medal.title" loading="lazy" />
                        @if (!medal.unlocked) {
                          <div style="position: absolute; bottom: 3px; right: 3px; font-size: 7px; background: rgba(0,0,0,0.7); border-radius: 4px; padding: 2px 4px; color: #ffb8b8; font-family: 'Space Grotesk', sans-serif; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em;">Bloq</div>
                        }
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

                <!-- Achievement List -->
                <div class="prf-panel scroll" style="display: flex; flex-direction: column; gap: 16px; max-height: 520px; overflow-y: auto;">
                  @if (achievements.length === 0) {
                    <div style="text-align: center; color: var(--mut); padding: 40px;">No se encontraron logros.</div>
                  }
                  @for (ach of achievements; track ach.title) {
                    <div class="prf-achievement-row" [style.opacity]="ach.unlocked ? '1' : '0.65'" [class.unlocked]="ach.unlocked">
                      <div style="display: flex; align-items: flex-start; justify-content: space-between;">
                        <div>
                          <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
                            <span style="font-weight: 700; font-size: 15px; color: var(--txt);">{{ ach.title }}</span>
                            <span [style.background]="getCategoryColor(ach.category)" style="font-size: 9px; font-weight: 800; padding: 2px 6px; border-radius: 6px; color: #111; text-transform: uppercase;">{{ ach.category }}</span>
                            @if (ach.rewardType === 'MEDALLA') {
                              <span class="prf-reward-badge prf-reward-medal">Medalla</span>
                            } @else if (ach.rewardType === 'FOTO_PERFIL') {
                              <span class="prf-reward-badge prf-reward-avatar">Avatar</span>
                            } @else if (ach.rewardType === 'TITULO') {
                              <span class="prf-reward-badge prf-reward-title">Título</span>
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
                        <div class="prf-ach-status" [style.color]="ach.unlocked ? '#46e08a' : 'var(--mut)'">
                          {{ ach.unlocked ? '✓' : '✗' }}
                        </div>
                      </div>
                      <div style="display: flex; align-items: center; gap: 12px; margin-top: 10px;">
                        <div class="prf-progress-track">
                          <div class="prf-progress-fill"
                               [style.width]="(Math.min(ach.progress, ach.target) / ach.target * 100) + '%'"
                               [style.background]="ach.unlocked ? 'linear-gradient(90deg, #46e08a, #27ae60)' : 'linear-gradient(90deg, var(--accent), var(--accent2))'">
                          </div>
                        </div>
                        <span class="num" style="font-size: 12px; color: var(--dim); width: 65px; text-align: right;">{{ ach.progress }} / {{ ach.target }}</span>
                      </div>
                    </div>
                  }
                </div>
              </div>
            }

            <!-- ── Tab: MATCH HISTORY ── -->
            @if (activeTab === 'history') {
              <div class="prf-tab-body">
                <div class="eyebrow" style="margin-bottom: 20px; color: var(--accent2);">Historial Reciente</div>
                <div class="prf-panel" style="overflow: hidden; padding: 0;">
                  @if (loadingHistory) {
                    <div style="text-align: center; padding: 40px; color: var(--mut);">Cargando historial de partidas...</div>
                  } @else if (matchesHistory.length === 0) {
                    <div style="text-align: center; padding: 40px; color: var(--mut);">No se registran partidas en tu historial.</div>
                  } @else {
                    @for (m of matchesHistory; track m.matchId) {
                      <div (click)="toggleMatchExpand(m.matchId)" class="prf-match-row" [class.win]="m.result === 'WIN' || m.result === 'VICTORY'" [class.expanded]="expandedMatches[m.matchId]">
                        <div class="prf-match-result" [class.win]="m.result === 'WIN' || m.result === 'VICTORY'">
                          {{ m.result === 'WIN' || m.result === 'VICTORY' ? 'W' : 'L' }}
                        </div>
                        <div class="prf-match-info">
                          <div class="prf-match-id">Partida #{{ m.matchId }}</div>
                          <div class="prf-match-status">{{ m.status }}</div>
                        </div>
                        <div class="prf-match-opponent">vs {{ m.opponent }}</div>
                        <div class="prf-match-date">
                          <span>{{ m.date | date:'dd/MM/yy HH:mm' }}</span>
                          <span class="prf-match-chevron">{{ expandedMatches[m.matchId] ? '▲' : '▼' }}</span>
                        </div>
                      </div>

                      @if (expandedMatches[m.matchId]) {
                        @let stats = parseMatchStats(m.playerStatsJson);
                        <div class="prf-match-detail">
                          @if (!stats) {
                            <div style="text-align: center; color: var(--mut); font-size: 13px; font-style: italic;">
                              No hay detalles de estadísticas registrados para esta partida.
                            </div>
                          } @else {
                            <div style="display: grid; grid-template-columns: 1.2fr 1.2fr 1.6fr; gap: 24px;">
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
                                <div style="font-size: 11px; color: var(--mut); text-align: center;">Dominancia: {{ pDmgPct.toFixed(0) }}%</div>
                              </div>

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
                                        <span [style.background]="getTypeColor(e.type)" style="font-size: 10.5px; font-weight: 700; color: #111; padding: 2px 6px; border-radius: 6px;">
                                          {{ getEnergyIconEmoji(e.type) }} {{ e.count }}
                                        </span>
                                      }
                                    }
                                  </div>
                                </div>
                              </div>

                              @let mvp = getMatchMvp(stats);
                              <div class="profile-subcard" style="padding: 14px; display: flex; align-items: center; gap: 14px;">
                                @if (mvp) {
                                  @let mvpImgUrl = getCardImageById(mvp.cardId);
                                  <div style="width: 50px; height: 70px; flex-shrink: 0; border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center; border: 1px solid var(--line); background: rgba(255,255,255,0.02);">
                                    @if (mvpImgUrl) {
                                      <img [src]="mvpImgUrl" style="max-width: 100%; max-height: 100%; object-fit: contain;" />
                                    } @else {
                                      <span style="font-size: 10px; font-weight: 700; color: var(--mut);">CARTA</span>
                                    }
                                  </div>
                                  <div style="flex: 1; display: flex; flex-direction: column; gap: 4px;">
                                    <div class="eyebrow" style="color: #ffce32; font-size: 10.5px;">MVP</div>
                                    <div style="font-weight: 700; font-size: 13.5px; color: var(--txt); overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">{{ mvp.name }}</div>
                                    <div style="font-size: 11px; color: var(--mut); font-weight: 600;">{{ mvp.damage }} Daño | {{ mvp.kos }} KOs</div>
                                  </div>
                                } @else {
                                  <div style="flex: 1; text-align: center; color: var(--mut); font-size: 12px; font-style: italic; padding: 10px;">Sin MVP destacado</div>
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

            <!-- ── Tab: STATS ── -->
            @if (activeTab === 'stats') {
              <div class="prf-tab-body" style="display: flex; flex-direction: column; gap: 30px;">

                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px;">
                  <div class="prf-panel">
                    <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Balance de Daño</div>
                    <div style="display: flex; justify-content: space-between; margin-bottom: 12px;">
                      <div>
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">Daño Realizado</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #4aa3ff;">{{ profileData?.advancedStats?.totalDamageDealt || 0 }}</div>
                      </div>
                      <div style="text-align: right;">
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">Daño Recibido</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #ff7a3d;">{{ profileData?.advancedStats?.totalDamageReceived || 0 }}</div>
                      </div>
                    </div>
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

                  <div class="prf-panel">
                    <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Derribos (KOs)</div>
                    <div style="display: flex; justify-content: space-between; margin-bottom: 12px;">
                      <div>
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">KOs Realizados</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #46e08a;">{{ profileData?.advancedStats?.totalKOsMade || 0 }}</div>
                      </div>
                      <div style="text-align: right;">
                        <div style="font-size: 11px; color: var(--mut); font-weight: 700; text-transform: uppercase;">KOs Sufridos</div>
                        <div style="font-family: var(--display); font-size: 28px; font-weight: 700; color: #ff3b47;">{{ profileData?.advancedStats?.totalKOsSuffered || 0 }}</div>
                      </div>
                    </div>
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

                <div class="prf-panel">
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
                    <div style="text-align: center; color: var(--mut); padding: 40px; font-weight: 600;">No hay registros de Pokémon jugados para este tipo.</div>
                  } @else {
                    <div style="display: flex; flex-direction: column; gap: 16px;">
                      @for (p of topPlayed; track p.cardId; let idx = $index) {
                        <div class="profile-subcard" style="display: flex; align-items: center; gap: 16px; padding: 12px 16px;">
                          @let imgUrl = getCardImageById(p.cardId);
                          <div style="width: 45px; height: 63px; flex-shrink: 0; border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center; border: 1px solid var(--line); background: rgba(255,255,255,0.03);">
                            @if (imgUrl) { <img [src]="imgUrl" style="max-width: 100%; max-height: 100%; object-fit: contain;" /> }
                            @else { <span style="font-size: 10px; font-weight: 700; color: var(--mut);">CARTA</span> }
                          </div>
                          <div style="flex: 1;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                              <span style="font-weight: 700; font-size: 14.5px; color: var(--txt);">#{{ idx + 1 }} {{ p.cardName }}</span>
                              <span class="num" style="font-size: 13.5px; font-weight: 800; color: var(--accent2);">{{ p.timesPlayed }} partidas</span>
                            </div>
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

                <div class="prf-panel">
                  <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Daño infligido por Pokémon</div>
                  @let topAttackers = getTopAttackers();
                  @if (topAttackers.length === 0) {
                    <div style="text-align: center; color: var(--mut); padding: 40px; font-weight: 600;">No hay registros de daño infligido.</div>
                  } @else {
                    <div style="display: flex; flex-direction: column; gap: 16px;">
                      @for (p of topAttackers; track p.cardId; let idx = $index) {
                        <div class="profile-subcard" style="display: flex; align-items: center; gap: 16px; padding: 12px 16px;">
                          @let imgUrl = getCardImageById(p.cardId);
                          <div style="width: 45px; height: 63px; flex-shrink: 0; border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center; border: 1px solid var(--line); background: rgba(255,255,255,0.03);">
                            @if (imgUrl) { <img [src]="imgUrl" style="max-width: 100%; max-height: 100%; object-fit: contain;" /> }
                            @else { <span style="font-size: 10px; font-weight: 700; color: var(--mut);">CARTA</span> }
                          </div>
                          <div style="flex: 1;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                              <span style="font-weight: 700; font-size: 14.5px; color: var(--txt);">#{{ idx + 1 }} {{ p.cardName }}</span>
                              <span class="num" style="font-size: 13.5px; font-weight: 800; color: #4aa3ff;">{{ p.damageDealt }} daño</span>
                            </div>
                            @let maxDmg = topAttackers[0].damageDealt || 1;
                            @let dmgPct2 = (p.damageDealt / maxDmg * 100);
                            <div style="height: 6px; background: rgba(255,255,255,0.05); border-radius: 3px; overflow: hidden;">
                              <div [style.width.%]="dmgPct2" style="height: 100%; background: linear-gradient(90deg, #4aa3ff, #00c6ff); border-radius: 3px; transition: width 0.3s;"></div>
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

                <div class="prf-panel">
                  <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Uso de Energías Elementales</div>
                  @let energyStats = getEnergyStats();
                  @if (energyStats.length === 0) {
                    <div style="text-align: center; color: var(--mut); padding: 40px; font-weight: 600;">No hay registros de energías unidas.</div>
                  } @else {
                    <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;">
                      @for (e of energyStats; track e.energyType) {
                        <div class="profile-subcard" style="padding: 12px 16px; display: flex; flex-direction: column; gap: 8px;">
                          <div style="display: flex; justify-content: space-between; align-items: center; font-weight: 700; font-size: 13.5px;">
                            <span>{{ getEnergyIconEmoji(e.energyType) }} {{ getEnergyLabel(e.energyType) }}</span>
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

          <!-- RIGHT: Sidebar -->
          <div class="prf-col-side">

            <!-- Level Card -->
            <div id="perfil-nivel" class="prf-panel prf-level-card" style="margin-bottom: 24px;">
              <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
                <div>
                  <div class="eyebrow" style="color: var(--accent); margin-bottom: 4px;">Entrenador</div>
                  <div style="font-family: var(--display); font-size: 26px; font-weight: 700; letter-spacing: -0.02em;">Nivel {{ profileData?.level || 1 }}</div>
                </div>
                <button (click)="openEditModal()" class="prf-edit-btn">
                  Editar Perfil
                </button>
              </div>
              <div class="prf-xp-track">
                <div class="prf-xp-fill" [style.width]="((profileData?.xp || 0) / (profileData?.xpToNextLevel || 100) * 100) + '%'">
                  <div class="prf-xp-glow"></div>
                </div>
              </div>
              <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--mut); font-weight: 700; margin-top: 8px;">
                <span>XP: {{ profileData?.xp || 0 }} / {{ profileData?.xpToNextLevel || 100 }}</span>
                <span>{{ Math.round(((profileData?.xp || 0) / (profileData?.xpToNextLevel || 100) * 100)) }}%</span>
              </div>
            </div>

            <!-- Combat Milestones -->
            <div class="prf-panel" style="margin-bottom: 24px;">
              <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Hitos de Combate</div>
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                <div class="prf-milestone-chip">
                  <div class="prf-milestone-label">Victorias Perfectas</div>
                  <div class="prf-milestone-value" style="color: #ffce32;">{{ profileData?.statistics?.perfectWins || 0 }}</div>
                </div>
                <div class="prf-milestone-chip">
                  <div class="prf-milestone-label">Cartas Jugadas</div>
                  <div class="prf-milestone-value" style="color: #ff7a3d;">{{ totalCardsPlayed }}</div>
                </div>
              </div>
            </div>

            <!-- Honors -->
            <div class="prf-panel" style="margin-bottom: 24px;">
              <div class="eyebrow" style="color: var(--accent2); margin-bottom: 16px;">Honores Recibidos</div>
              <div style="display: flex; flex-direction: column; gap: 10px;">
                <div class="prf-honor-row">
                  <span class="prf-honor-label">Buen Deportista</span>
                  <span class="prf-honor-count" style="color: #46e08a;">{{ profileData?.honors?.['GOOD_SPORTSMAN'] || 0 }}</span>
                </div>
                <div class="prf-honor-row">
                  <span class="prf-honor-label">Amigable</span>
                  <span class="prf-honor-count" style="color: #ffce32;">{{ profileData?.honors?.['FRIENDLY'] || 0 }}</span>
                </div>
                <div class="prf-honor-row">
                  <span class="prf-honor-label">Gran Estratega</span>
                  <span class="prf-honor-count" style="color: #4aa3ff;">{{ profileData?.honors?.['GREAT_STRATEGIST'] || 0 }}</span>
                </div>
              </div>
            </div>

            <!-- Donut Chart: Decks -->
            <div class="prf-panel">
              <div class="eyebrow" style="color: var(--accent2); margin-bottom: 20px;">Mazos más usados</div>
              <div style="display: flex; justify-content: center; margin-bottom: 28px;">
                <div class="prf-donut-wrap">
                  <div [style.background]="'conic-gradient(' + donutStops + ')'" class="prf-donut">
                    <div class="prf-donut-hole">
                      <div class="num prf-donut-value">{{ overallWinRate }}%</div>
                      <div class="prf-donut-sub">Win Rate</div>
                    </div>
                  </div>
                </div>
              </div>
              <div style="display: flex; flex-direction: column; gap: 12px;">
                @for (a of topDecks; track a.name) {
                  <div class="prf-deck-row">
                    <div class="prf-deck-dot" [style.background]="a.color" [style.box-shadow]="'0 0 6px ' + a.color"></div>
                    <div class="prf-deck-name">{{ a.name }}</div>
                    <div class="num prf-deck-record">{{ a.wins }}W - {{ a.losses }}L</div>
                  </div>
                }
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
                    <img [src]="getAvatarUrl(editAvatarIcon)" style="width:100%;height:100%;object-fit:contain;border-radius:50%;transform:scale(1.15);" />
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
          <div style="flex: 1; min-height: 0; overflow-y: auto; padding-right: 6px;">
            @if (filteredShowcaseCards.length === 0) {
              <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 20px; color: var(--mut); text-align: center;">
                <div style="font-size: 40px; margin-bottom: 10px;">🃏</div>
                <div style="font-weight: bold; margin-bottom: 6px;">No se encontraron cartas</div>
                <div>Intenta buscar con otro nombre.</div>
              </div>
            } @else {
              <div class="card-select-grid">
                @for (card of filteredShowcaseCards; track card.id) {
                  <div class="card-select-item"
                       (click)="selectCardForShowcase(card.id)"
                       draggable="true"
                       (dragstart)="handleDragStart($event, card.id)">
                    <img [src]="card.images.small || card.images.large" [alt]="card.name" />
                  </div>
                }
              </div>
            }
          </div>
        </div>
      </div>
    }

    <style>
      /* ═══════════════════════════════════════════════════
         PROFILE AURORA — PREMIUM REDESIGN CSS
      ═══════════════════════════════════════════════════ */

      .help-trigger-btn {
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 50%;
        width: 24px;
        height: 24px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        color: var(--accent2, #fbbf24);
        cursor: pointer;
        transition: all 0.2s ease;
        padding: 0;
        margin-left: 10px;
        flex-shrink: 0;
      }
      .help-trigger-btn:hover {
        background: rgba(255, 255, 255, 0.12);
        border-color: var(--accent2, #fbbf24);
        box-shadow: 0 0 8px rgba(251, 191, 36, 0.35);
      }

      /* ── Root container ── */
      .prf-root {
        position: fixed;
        inset: 0;
        z-index: 9999;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
      }

      /* ── Animated Background ── */
      .prf-bg-layer {
        position: fixed;
        inset: 0;
        pointer-events: none;
        z-index: 0;
        overflow: hidden;
      }
      .prf-orb {
        position: absolute;
        border-radius: 50%;
        filter: blur(90px);
        opacity: 0.18;
        animation: prf-drift 20s ease-in-out infinite alternate;
      }
      .prf-orb-1 { width: 500px; height: 500px; background: radial-gradient(circle, #ff2e3e, transparent 70%); top: -120px; left: -80px; animation-duration: 22s; }
      .prf-orb-2 { width: 600px; height: 600px; background: radial-gradient(circle, #1a6bd6, transparent 70%); top: 40%; right: -150px; animation-duration: 28s; animation-delay: -8s; }
      .prf-orb-3 { width: 400px; height: 400px; background: radial-gradient(circle, #7c3aed, transparent 70%); bottom: 10%; left: 20%; animation-duration: 18s; animation-delay: -4s; }
      .prf-orb-4 { width: 350px; height: 350px; background: radial-gradient(circle, #ffce32, transparent 70%); top: 55%; left: 45%; animation-duration: 25s; animation-delay: -12s; opacity: 0.1; }
      @keyframes prf-drift {
        0% { transform: translate(0, 0) scale(1); }
        33% { transform: translate(30px, -20px) scale(1.05); }
        66% { transform: translate(-20px, 25px) scale(0.97); }
        100% { transform: translate(15px, -15px) scale(1.03); }
      }
      .prf-scan-line {
        position: absolute;
        left: 0; right: 0;
        height: 2px;
        background: linear-gradient(90deg, transparent, rgba(255,46,62,0.15) 30%, rgba(74,163,255,0.1) 70%, transparent);
        animation: prf-scan 8s linear infinite;
        opacity: 0.6;
      }
      @keyframes prf-scan {
        0% { top: -10px; }
        100% { top: calc(100vh + 10px); }
      }

      /* ── Page layout ── */
      .prf-content {
        position: relative;
        z-index: 5;
        max-width: 1080px;
        margin: 0 auto;
        padding: 32px 24px 80px;
        display: flex;
        flex-direction: column;
        gap: 36px;
        width: 100%;
        box-sizing: border-box;
      }

      /* ── HERO HEADER ── */
      .prf-hero {
        display: flex;
        align-items: center;
        gap: 28px;
        background: linear-gradient(135deg, rgba(19,35,66,0.78) 0%, rgba(10,23,48,0.92) 100%);
        border: 1px solid rgba(255,255,255,0.09);
        border-radius: 24px;
        padding: 28px 32px;
        backdrop-filter: blur(24px);
        -webkit-backdrop-filter: blur(24px);
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.12),
                    0 4px 6px rgba(0,0,0,0.2),
                    0 20px 48px rgba(0,0,0,0.55);
        position: relative;
        overflow: hidden;
        flex-wrap: wrap;
      }
      .prf-hero::before {
        content: '';
        position: absolute;
        top: 0; left: 0; right: 0;
        height: 1px;
        background: linear-gradient(90deg, transparent, rgba(255,46,62,0.5), rgba(74,163,255,0.3), transparent);
      }
      .prf-hero-identity {
        display: flex;
        align-items: center;
        gap: 22px;
        flex: 1;
        min-width: 0;
      }

      /* Avatar with animated ring */
      .prf-avatar-wrap {
        position: relative;
        flex-shrink: 0;
      }
      .prf-avatar-ring {
        position: absolute;
        inset: -6px;
        border-radius: 50%;
        background: conic-gradient(from 0deg, #ff2e3e, #ffce32, #4aa3ff, #7c3aed, #ff2e3e);
        animation: prf-ring-spin 8s linear infinite;
        opacity: 0.6;
      }
      .prf-avatar-ring::after {
        content: '';
        position: absolute;
        inset: 3px;
        border-radius: 50%;
        background: var(--bg2, #0a1730);
      }
      @keyframes prf-ring-spin {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
      }
      .prf-identity-info { flex: 1; min-width: 0; }
      .prf-identity-row {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
        margin-bottom: 8px;
      }
      .prf-username {
        font-size: 40px;
        font-weight: 800;
        margin: 0;
        line-height: 1.05;
        letter-spacing: -0.02em;
        background: linear-gradient(135deg, #fff 0%, rgba(255,255,255,0.75) 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }
      .prf-title-badge {
        background: linear-gradient(135deg, rgba(255,206,50,0.2), rgba(255,206,50,0.05));
        border: 1px solid rgba(255,206,50,0.4);
        color: #ffce32;
        font-family: 'Space Grotesk', sans-serif;
        font-size: 11px;
        font-weight: 800;
        letter-spacing: 0.1em;
        text-transform: uppercase;
        padding: 4px 12px;
        border-radius: 999px;
        box-shadow: 0 0 12px rgba(255,206,50,0.15), inset 0 1px 0 rgba(255,255,255,0.1);
        transition: all 0.25s;
        flex-shrink: 0;
      }
      .prf-title-badge:hover {
        box-shadow: 0 0 20px rgba(255,206,50,0.3), inset 0 1px 0 rgba(255,255,255,0.15);
        transform: translateY(-1px);
      }
      .prf-medals-row {
        display: flex;
        gap: 8px;
        margin-bottom: 10px;
      }
      .prf-medal-chip {
        width: 38px;
        height: 38px;
        border-radius: 10px;
        background: rgba(0,0,0,0.25);
        border: 1px solid rgba(255,206,50,0.22);
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.25s;
        cursor: default;
        box-shadow: inset 0 2px 5px rgba(0,0,0,0.45);
      }
      .prf-medal-chip:hover {
        border-color: rgba(255,206,50,0.5);
        transform: translateY(-2px) scale(1.06);
        box-shadow: 0 0 14px rgba(255,206,50,0.25), inset 0 1px 0 rgba(255,255,255,0.05);
      }
      .prf-bio {
        color: var(--mut);
        font-weight: 500;
        font-size: 13.5px;
        margin: 0;
        font-style: italic;
        max-width: 520px;
        line-height: 1.55;
        font-family: 'Plus Jakarta Sans', sans-serif;
      }

      /* Stat cluster on the right of the hero */
      .prf-stats-cluster {
        display: flex;
        align-items: center;
        gap: 0;
        background: rgba(0,0,0,0.3);
        border: 1px solid rgba(255,255,255,0.07);
        border-radius: 18px;
        backdrop-filter: blur(10px);
        overflow: hidden;
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.07), 0 8px 24px rgba(0,0,0,0.4);
        flex-shrink: 0;
      }
      .prf-stat-chip {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 18px 28px;
        transition: all 0.25s;
        cursor: default;
      }
      .prf-stat-chip:hover {
        background: rgba(255,255,255,0.04);
      }
      .prf-stat-value {
        font-family: var(--display);
        font-size: 32px;
        font-weight: 800;
        line-height: 1;
        letter-spacing: -0.02em;
        display: block;
      }
      .prf-stat-label {
        font-family: 'Space Grotesk', sans-serif;
        font-size: 10px;
        font-weight: 700;
        letter-spacing: 0.12em;
        text-transform: uppercase;
        color: var(--mut);
        margin-top: 5px;
        display: block;
      }
      .prf-stat-wins .prf-stat-value { color: #46e08a; }
      .prf-stat-losses .prf-stat-value { color: #ff4560; }
      .prf-stat-wr .prf-stat-value { color: #ffce32; }
      .prf-stat-divider {
        width: 1px;
        height: 40px;
        background: rgba(255,255,255,0.07);
      }

      /* ── GRID ── */
      .prf-grid {
        display: grid;
        grid-template-columns: 1fr 320px;
        gap: 32px;
        align-items: start;
      }
      .prf-col-main { min-width: 0; }
      .prf-col-side { display: flex; flex-direction: column; gap: 0; }

      /* ── PANELS ── */
      .prf-panel {
        background: linear-gradient(135deg, rgba(19,35,66,0.72) 0%, rgba(10,23,48,0.88) 100%);
        border: 1px solid rgba(255,255,255,0.08);
        border-radius: 20px;
        backdrop-filter: blur(20px);
        -webkit-backdrop-filter: blur(20px);
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.1),
                    0 4px 6px rgba(0,0,0,0.25),
                    0 16px 36px rgba(0,0,0,0.5);
        padding: 24px;
        transition: all 0.3s cubic-bezier(0.25,0.8,0.25,1);
        position: relative;
        overflow: hidden;
      }
      .prf-panel::before {
        content: '';
        position: absolute;
        top: 0; left: 10%; right: 10%;
        height: 1px;
        background: linear-gradient(90deg, transparent, rgba(255,255,255,0.12), transparent);
      }
      .prf-panel:hover {
        border-color: rgba(255,46,62,0.2);
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.15),
                    0 20px 48px rgba(0,0,0,0.6),
                    0 0 20px rgba(255,46,62,0.06);
      }
      .prf-panel-header {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        margin-bottom: 20px;
        flex-wrap: wrap;
        gap: 8px;
      }
      .prf-panel-hint {
        font-size: 11.5px;
        color: var(--mut);
        font-weight: 500;
        font-family: 'Plus Jakarta Sans', sans-serif;
      }

      /* ── TABS NAV ── */
      .prf-tabs-nav {
        display: flex;
        gap: 8px;
        margin-bottom: 20px;
        padding: 6px;
        background: rgba(0,0,0,0.25);
        border: 1px solid rgba(255,255,255,0.06);
        border-radius: 16px;
        backdrop-filter: blur(8px);
        flex-wrap: wrap;
      }
      .prf-tab {
        display: flex;
        align-items: center;
        gap: 6px;
        background: transparent;
        border: 1px solid transparent;
        color: var(--mut);
        font-family: 'Space Grotesk', sans-serif;
        font-weight: 700;
        font-size: 12px;
        padding: 9px 16px;
        border-radius: 11px;
        cursor: pointer;
        transition: all 0.22s cubic-bezier(0.25,0.8,0.25,1);
        letter-spacing: 0.05em;
        text-transform: uppercase;
        flex: 1;
        justify-content: center;
        white-space: nowrap;
      }
      .prf-tab-icon {
        font-size: 10px;
        opacity: 0.7;
        transition: opacity 0.2s, transform 0.2s;
      }
      .prf-tab:hover {
        color: var(--txt);
        background: rgba(255,255,255,0.04);
        border-color: rgba(255,255,255,0.07);
      }
      .prf-tab:hover .prf-tab-icon {
        opacity: 1;
        transform: scale(1.2);
      }
      .prf-tab.active {
        background: linear-gradient(180deg, rgba(255,46,62,0.18) 0%, rgba(255,46,62,0.06) 100%);
        border-color: rgba(255,46,62,0.4);
        color: var(--txt);
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.1),
                    0 4px 12px rgba(255,46,62,0.18),
                    0 0 12px rgba(255,46,62,0.12);
        text-shadow: 0 0 10px rgba(255,255,255,0.25);
      }
      .prf-tab.active .prf-tab-icon {
        opacity: 1;
        color: var(--accent);
      }
      .prf-tab-body {
        animation: paneFadeIn 0.22s ease-out;
      }

      /* ── SHOWCASE GRID ── */
      .prf-showcase-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 20px;
      }
      .showcase-slot-glow {
        position: absolute;
        inset: -1px;
        border-radius: 18px;
        background: linear-gradient(135deg, rgba(255,46,62,0.08), rgba(74,163,255,0.08));
        opacity: 0;
        transition: opacity 0.35s;
        pointer-events: none;
      }
      .showcase-slot:hover .showcase-slot-glow { opacity: 1; }
      .showcase-slot-empty {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        pointer-events: none;
        gap: 8px;
      }
      .showcase-empty-icon {
        font-size: 32px;
        font-weight: 300;
        color: rgba(255,255,255,0.2);
        line-height: 1;
        font-family: var(--display);
        transition: color 0.25s, transform 0.25s;
      }
      .showcase-slot:hover .showcase-empty-icon {
        color: rgba(255,46,62,0.6);
        transform: scale(1.1);
      }
      .showcase-empty-label {
        font-size: 10.5px;
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.12em;
        color: rgba(255,255,255,0.2);
        transition: color 0.25s;
      }
      .showcase-slot:hover .showcase-empty-label {
        color: rgba(255,46,62,0.55);
      }

      /* ── MATCH HISTORY ── */
      .prf-match-row {
        display: grid;
        grid-template-columns: 56px 1fr 1fr 140px;
        align-items: center;
        padding: 15px 20px;
        border-bottom: 1px solid rgba(255,255,255,0.04);
        transition: background 0.2s, transform 0.15s;
        cursor: pointer;
        position: relative;
      }
      .prf-match-row::before {
        content: '';
        position: absolute;
        left: 0; top: 0; bottom: 0;
        width: 3px;
        background: transparent;
        transition: background 0.2s;
        border-radius: 0 3px 3px 0;
      }
      .prf-match-row.win::before { background: #46e08a; }
      .prf-match-row:not(.win)::before { background: #ff4560; }
      .prf-match-row:hover { background: rgba(255,255,255,0.025); }
      .prf-match-result {
        font-family: var(--display);
        font-size: 26px;
        font-weight: 800;
        color: #ff4560;
        text-shadow: 0 0 12px rgba(255,69,96,0.4);
      }
      .prf-match-result.win {
        color: #46e08a;
        text-shadow: 0 0 12px rgba(70,224,138,0.4);
      }
      .prf-match-id { font-weight: 700; font-size: 14.5px; color: var(--txt); }
      .prf-match-status { font-size: 11px; color: var(--mut); text-transform: uppercase; letter-spacing: 0.05em; margin-top: 3px; }
      .prf-match-opponent { font-weight: 600; font-size: 13.5px; color: var(--dim); }
      .prf-match-date { text-align: right; color: var(--dim); font-size: 11.5px; font-weight: 600; display: flex; align-items: center; justify-content: flex-end; gap: 8px; }
      .prf-match-chevron { font-size: 10px; opacity: 0.55; transition: transform 0.2s; }
      .prf-match-detail {
        background: rgba(0,0,0,0.2);
        border-bottom: 1px solid rgba(255,255,255,0.05);
        padding: 20px 24px;
        animation: fadeIn 0.2s ease-out;
        box-shadow: inset 0 4px 12px rgba(0,0,0,0.3);
      }

      /* ── ACHIEVEMENT ROWS ── */
      .prf-achievement-row {
        background: rgba(0,0,0,0.18);
        border: 1px solid rgba(255,255,255,0.05);
        border-radius: 14px;
        padding: 16px;
        transition: all 0.22s;
        position: relative;
      }
      .prf-achievement-row:hover {
        border-color: rgba(255,255,255,0.1);
        background: rgba(255,255,255,0.02);
        transform: translateY(-1px);
        box-shadow: 0 8px 20px rgba(0,0,0,0.3);
      }
      .prf-achievement-row.unlocked {
        border-color: rgba(70,224,138,0.12);
      }
      .prf-achievement-row.unlocked:hover {
        border-color: rgba(70,224,138,0.22);
        box-shadow: 0 8px 20px rgba(0,0,0,0.3), 0 0 12px rgba(70,224,138,0.06);
      }
      .prf-ach-status {
        font-family: 'Space Grotesk', sans-serif;
        font-size: 18px;
        font-weight: 700;
        flex-shrink: 0;
      }
      .prf-progress-track {
        flex: 1;
        height: 6px;
        background: rgba(255,255,255,0.05);
        border-radius: 3px;
        overflow: hidden;
      }
      .prf-progress-fill {
        height: 100%;
        border-radius: 3px;
        transition: width 0.5s cubic-bezier(0.25,0.8,0.25,1);
      }
      .prf-reward-badge {
        font-size: 9px;
        font-weight: 800;
        padding: 2px 7px;
        border-radius: 6px;
        text-transform: uppercase;
        letter-spacing: 0.06em;
        border: 1px solid;
      }
      .prf-reward-medal { background: rgba(74,163,255,0.1); border-color: rgba(74,163,255,0.3); color: #4aa3ff; }
      .prf-reward-avatar { background: rgba(168,85,247,0.1); border-color: rgba(168,85,247,0.3); color: #a855f7; }
      .prf-reward-title { background: rgba(255,206,50,0.1); border-color: rgba(255,206,50,0.3); color: #ffce32; }

      /* ── SIDEBAR: Level/XP ── */
      .prf-level-card { position: relative; }
      .prf-level-card::after {
        content: '';
        position: absolute;
        top: -1px; left: 15%; right: 15%;
        height: 2px;
        background: linear-gradient(90deg, transparent, rgba(255,46,62,0.6), transparent);
        border-radius: 0 0 2px 2px;
      }
      .prf-edit-btn {
        background: rgba(255,255,255,0.04);
        border: 1px solid rgba(255,255,255,0.1);
        color: var(--mut);
        font-family: 'Space Grotesk', sans-serif;
        font-size: 11px;
        font-weight: 700;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        padding: 8px 14px;
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.22s;
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.06);
      }
      .prf-edit-btn:hover {
        background: rgba(255,46,62,0.12);
        border-color: rgba(255,46,62,0.35);
        color: var(--txt);
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.1), 0 0 10px rgba(255,46,62,0.15);
      }
      .prf-xp-track {
        height: 10px;
        background: rgba(255,255,255,0.05);
        border-radius: 5px;
        overflow: hidden;
        position: relative;
        box-shadow: inset 0 2px 4px rgba(0,0,0,0.4);
      }
      .prf-xp-fill {
        height: 100%;
        background: linear-gradient(90deg, var(--accent), var(--accent2));
        border-radius: 5px;
        position: relative;
        transition: width 0.6s cubic-bezier(0.25,0.8,0.25,1);
      }
      .prf-xp-glow {
        position: absolute;
        right: 0; top: 0; bottom: 0;
        width: 20px;
        background: rgba(255,255,255,0.4);
        filter: blur(4px);
        border-radius: 5px;
        animation: prf-xp-pulse 2s ease-in-out infinite;
      }
      @keyframes prf-xp-pulse {
        0%, 100% { opacity: 0.8; }
        50% { opacity: 0.3; }
      }

      /* ── SIDEBAR: Milestones ── */
      .prf-milestone-chip {
        background: rgba(0,0,0,0.22);
        border: 1px solid rgba(255,255,255,0.05);
        border-radius: 14px;
        padding: 14px 12px;
        text-align: center;
        transition: all 0.25s;
        box-shadow: inset 0 2px 5px rgba(0,0,0,0.45);
      }
      .prf-milestone-chip:hover {
        border-color: rgba(255,255,255,0.1);
        transform: translateY(-2px);
        box-shadow: inset 0 1px 0 rgba(255,255,255,0.05), 0 8px 16px rgba(0,0,0,0.35);
      }
      .prf-milestone-label {
        font-size: 9.5px;
        color: var(--mut);
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        margin-bottom: 6px;
      }
      .prf-milestone-value {
        font-family: var(--display);
        font-size: 26px;
        font-weight: 800;
        line-height: 1;
        letter-spacing: -0.02em;
      }

      /* ── SIDEBAR: Honors ── */
      .prf-honor-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 10px 14px;
        background: rgba(0,0,0,0.18);
        border: 1px solid rgba(255,255,255,0.04);
        border-radius: 12px;
        transition: all 0.22s;
      }
      .prf-honor-row:hover {
        background: rgba(255,255,255,0.02);
        border-color: rgba(255,255,255,0.08);
        transform: translateX(2px);
      }
      .prf-honor-label { font-size: 13px; font-weight: 600; color: var(--txt); }
      .prf-honor-count { font-family: 'Space Mono', monospace; font-size: 16px; font-weight: 700; letter-spacing: -0.02em; }

      /* ── SIDEBAR: Donut ── */
      .prf-donut-wrap {
        position: relative;
        width: 148px;
        height: 148px;
        filter: drop-shadow(0 8px 20px rgba(0,0,0,0.5));
      }
      .prf-donut {
        width: 148px;
        height: 148px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        position: relative;
        transition: transform 0.3s;
      }
      .prf-donut:hover { transform: scale(1.03); }
      .prf-donut-hole {
        width: 108px;
        height: 108px;
        border-radius: 50%;
        background: var(--bg2, #0a1730);
        position: absolute;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        box-shadow: inset 0 4px 12px rgba(0,0,0,0.6);
      }
      .prf-donut-value { font-size: 22px; font-weight: 800; color: var(--txt); line-height: 1; }
      .prf-donut-sub { font-size: 9px; font-weight: 700; color: var(--mut); letter-spacing: 0.12em; text-transform: uppercase; margin-top: 2px; }

      /* ── SIDEBAR: Deck rows ── */
      .prf-deck-row {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 6px 0;
        transition: transform 0.2s;
      }
      .prf-deck-row:hover { transform: translateX(3px); }
      .prf-deck-dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        flex-shrink: 0;
        transition: transform 0.2s;
      }
      .prf-deck-row:hover .prf-deck-dot { transform: scale(1.3); }
      .prf-deck-name { font-size: 14px; font-weight: 600; color: var(--txt); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      .prf-deck-record { font-size: 12.5px; color: var(--mut); }


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
        max-width: 800px;
        max-height: 85vh;
        display: flex;
        flex-direction: column;
        overflow: hidden;
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
        grid-template-columns: repeat(5, 1fr);
        gap: 14px;
        padding: 4px 2px 14px;
        width: 100%;
      }
      .card-select-item {
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        height: 168px;
        border-radius: 10px;
        overflow: hidden;
        border: 1px solid rgba(255,255,255,0.08);
        box-shadow: 0 4px 10px rgba(0,0,0,0.45);
        background: rgba(0,0,0,0.25);
        transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
      }
      .card-select-item img {
        width: 100%;
        height: 100%;
        object-fit: contain;
        pointer-events: none;
        display: block;
      }
      .card-select-item:hover {
        transform: translateY(-5px) scale(1.04);
        box-shadow: 0 10px 24px rgba(0,0,0,0.7);
        border-color: var(--accent2);
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
  private tutorialService = inject(TutorialService);

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
      'ash', 'misty', 'brock', 'gary', 'serena', 'red',
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
    const emojis = ['ash', 'misty', 'brock', 'gary', 'serena', 'red'];
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

  ngOnInit(): void {
    this.tcgService.loadCards();
    this.loadProfile();
    this.tutorialService.triggerTutorial('profile');
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

  triggerHelp(): void {
    this.tutorialService.triggerTutorial('profile', true);
  }
}

