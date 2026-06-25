import {
  Component,
  ViewEncapsulation,
  inject,
  OnInit,
  OnDestroy,
  signal,
  computed,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LogoComponent,
  TrainerChipComponent,
  RankCrestComponent,
  StatComponent,
  IconComponent,
  BallIconComponent,
  SparksComponent,
  AmbientComponent,
  BattleCtaComponent,
} from './ui/aurora-ui.components';
import { HoloCardComponent, AuroraCardComponent } from '../../shared/ui/holo-card/holo-card.component';
import { DeckRailComponent } from './components/deck-rail.component';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService, UserProfileResponseDTO } from '../../core/services/profile.service';
import { LobbyService } from '../../core/services/lobby.service';
import { MatchBackendService } from '../../core/services/match-backend.service';
import { DeckSummaryDTO } from '../../core/models/game-state.models';
import { TutorialService } from '../../core/services/tutorial.service';

type LobbyTab = 'public' | 'private';
type PrivateMode = 'create' | 'join';

@Component({
  selector: 'app-lobby-aurora',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    LogoComponent,
    TrainerChipComponent,
    RankCrestComponent,
    StatComponent,
    IconComponent,
    BallIconComponent,
    SparksComponent,
    AmbientComponent,
    HoloCardComponent,
    AuroraCardComponent,
    DeckRailComponent,
    BattleCtaComponent,
  ],
  encapsulation: ViewEncapsulation.None,
  styles: [`
    /* ── Lobby Match Panel ─────────────────────────────────────────────── */
    .match-panel {
      background: rgba(15, 12, 30, 0.72);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 24px;
      backdrop-filter: blur(18px);
      padding: 28px 32px;
      width: 480px;
      box-shadow: 0 8px 40px rgba(0,0,0,0.4);
    }

    .panel-tabs {
      display: flex;
      gap: 4px;
      background: rgba(255,255,255,0.05);
      border-radius: 12px;
      padding: 4px;
      margin-bottom: 24px;
    }

    .panel-tab {
      flex: 1;
      padding: 8px 0;
      border: none;
      background: transparent;
      color: var(--mut, #7a8090);
      font-size: 12px;
      font-weight: 700;
      letter-spacing: .1em;
      text-transform: uppercase;
      border-radius: 9px;
      cursor: pointer;
      transition: all .2s ease;
    }

    .panel-tab.active {
      background: rgba(255,255,255,0.1);
      color: var(--txt, #e8eaf6);
      box-shadow: 0 2px 8px rgba(0,0,0,0.3);
    }

    .panel-tab:not(.active):hover {
      color: rgba(232,234,246,0.6);
    }

    /* ── Deck selector ─────────────────────────────────────────────────── */
    .deck-label {
      font-size: 10.5px;
      font-weight: 700;
      letter-spacing: .1em;
      text-transform: uppercase;
      color: var(--mut, #7a8090);
      margin-bottom: 8px;
    }

    .deck-select {
      width: 100%;
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 12px;
      color: var(--txt, #e8eaf6);
      font-size: 14px;
      font-weight: 600;
      padding: 10px 14px;
      margin-bottom: 20px;
      cursor: pointer;
      outline: none;
      appearance: none;
      -webkit-appearance: none;
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%237a8090' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
      background-repeat: no-repeat;
      background-position: right 14px center;
      padding-right: 36px;
      transition: border-color .2s;
    }

    .deck-select:focus {
      border-color: rgba(130,100,255,0.5);
    }

    .deck-select option {
      background: #1a1530;
      color: #e8eaf6;
    }

    .no-decks-hint {
      font-size: 12px;
      color: #f59e0b;
      margin-bottom: 16px;
      padding: 10px 14px;
      background: rgba(245,158,11,0.08);
      border: 1px solid rgba(245,158,11,0.2);
      border-radius: 10px;
    }

    /* ── Primary action button ─────────────────────────────────────────── */
    .action-btn {
      width: 100%;
      padding: 14px;
      border: none;
      border-radius: 14px;
      font-size: 14px;
      font-weight: 800;
      letter-spacing: .06em;
      text-transform: uppercase;
      cursor: pointer;
      transition: all .2s ease;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    .action-btn.primary {
      background: linear-gradient(135deg, #7c3aed, #4f46e5);
      color: #fff;
      box-shadow: 0 4px 20px rgba(124,58,237,0.4);
    }

    .action-btn.primary:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 28px rgba(124,58,237,0.55);
    }

    .action-btn.primary:disabled {
      opacity: 0.45;
      cursor: not-allowed;
      transform: none;
    }

    .action-btn.danger {
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.25);
      color: #f87171;
    }

    .action-btn.danger:hover {
      background: rgba(239,68,68,0.18);
    }

    .action-btn.secondary {
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.1);
      color: var(--txt, #e8eaf6);
    }

    .action-btn.secondary:hover:not(:disabled) {
      background: rgba(255,255,255,0.1);
    }

    .action-btn.bot {
      background: linear-gradient(135deg, rgba(20,184,166,0.2), rgba(13,148,136,0.4));
      border: 1px solid rgba(45,212,191,0.3);
      color: #5eead4;
      margin-top: 24px;
      position: relative;
      overflow: hidden;
    }

    .action-btn.bot::before {
      content: '';
      position: absolute;
      top: 0; left: -100%;
      width: 50%; height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent);
      transform: skewX(-20deg);
      transition: all 0.5s ease;
    }

    .action-btn.bot:hover:not(:disabled) {
      background: linear-gradient(135deg, rgba(20,184,166,0.3), rgba(13,148,136,0.5));
      border: 1px solid rgba(45,212,191,0.5);
      transform: translateY(-1px);
      box-shadow: 0 4px 16px rgba(13,148,136,0.4);
    }

    .action-btn.bot:hover:not(:disabled)::before {
      left: 200%;
    }

    .action-btn.bot .bot-icon {
      font-size: 16px;
    }

    /* ── Waiting state ─────────────────────────────────────────────────── */
    .waiting-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 8px 0;
    }

    .spinner-ring {
      width: 48px;
      height: 48px;
      border: 3px solid rgba(124,58,237,0.2);
      border-top-color: #7c3aed;
      border-radius: 50%;
      animation: spin 0.9s linear infinite;
    }

    @keyframes spin { to { transform: rotate(360deg); } }

    .waiting-text {
      font-size: 15px;
      font-weight: 700;
      color: var(--txt, #e8eaf6);
    }

    .waiting-sub {
      font-size: 12px;
      color: var(--mut, #7a8090);
    }

    /* ── Room code display ─────────────────────────────────────────────── */
    .room-code-box {
      background: rgba(124,58,237,0.1);
      border: 1px solid rgba(124,58,237,0.3);
      border-radius: 14px;
      padding: 18px;
      text-align: center;
      margin-bottom: 16px;
    }

    .room-code-label {
      font-size: 10.5px;
      font-weight: 700;
      letter-spacing: .12em;
      text-transform: uppercase;
      color: var(--mut, #7a8090);
      margin-bottom: 8px;
    }

    .room-code-value {
      font-size: 36px;
      font-weight: 900;
      letter-spacing: .25em;
      color: #a78bfa;
      font-family: 'Space Grotesk', monospace;
    }

    .room-code-hint {
      font-size: 12px;
      color: var(--mut, #7a8090);
      margin-top: 6px;
    }

    .copy-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      background: rgba(124,58,237,0.15);
      border: 1px solid rgba(124,58,237,0.3);
      border-radius: 8px;
      color: #a78bfa;
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;
      margin-top: 10px;
      transition: all .2s;
    }

    .copy-btn:hover { background: rgba(124,58,237,0.25); }

    /* ── Join room input ───────────────────────────────────────────────── */
    .private-mode-tabs {
      display: flex;
      gap: 8px;
      margin-bottom: 20px;
    }

    .priv-tab {
      flex: 1;
      padding: 7px;
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 10px;
      background: transparent;
      color: var(--mut, #7a8090);
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;
      transition: all .2s;
    }

    .priv-tab.active {
      border-color: rgba(124,58,237,0.4);
      color: #a78bfa;
      background: rgba(124,58,237,0.1);
    }

    .code-input {
      width: 100%;
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 12px;
      color: var(--txt, #e8eaf6);
      font-size: 20px;
      font-weight: 800;
      letter-spacing: .2em;
      text-transform: uppercase;
      text-align: center;
      padding: 12px 14px;
      margin-bottom: 16px;
      outline: none;
      box-sizing: border-box;
      font-family: 'Space Grotesk', monospace;
      transition: border-color .2s;
    }

    .code-input:focus {
      border-color: rgba(124,58,237,0.5);
    }

    .code-input::placeholder {
      color: rgba(255,255,255,0.2);
      letter-spacing: .1em;
      font-size: 14px;
    }

    /* ── Error message ─────────────────────────────────────────────────── */
    .error-msg {
      font-size: 12px;
      color: #f87171;
      padding: 10px 14px;
      background: rgba(239,68,68,0.08);
      border: 1px solid rgba(239,68,68,0.2);
      border-radius: 10px;
      margin-top: 12px;
    }

    /* ── Divider ───────────────────────────────────────────────────────── */
    .panel-divider {
      border: none;
      border-top: 1px solid rgba(255,255,255,0.07);
      margin: 20px 0;
    }

    /* ── Modal popup ──────────────────────────────────────────────────── */
    .modal-backdrop {
      position: fixed;
      inset: 0;
      z-index: 10000;
      background: rgba(4, 2, 10, 0.65);
      backdrop-filter: blur(12px);
      display: flex;
      align-items: center;
      justify-content: center;
      animation: fadeIn 0.25s ease-out forwards;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .modal-card {
      position: relative;
      background: rgba(15, 12, 30, 0.88);
      border: 1px solid rgba(255,255,255,0.12);
      border-radius: 28px;
      backdrop-filter: blur(24px);
      padding: 32px;
      width: 460px;
      box-shadow: 0 20px 50px rgba(0,0,0,0.6);
      transform: scale(0.92);
      opacity: 0;
      animation: scaleUp 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
    }

    @keyframes scaleUp {
      to {
        transform: scale(1);
        opacity: 1;
      }
    }

    .modal-close-btn {
      position: absolute;
      top: 20px;
      right: 20px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      color: var(--mut, #7a8090);
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      font-size: 13px;
      font-weight: 700;
      transition: all 0.2s;
    }

    .modal-close-btn:hover {
      background: rgba(255, 255, 255, 0.15);
      color: var(--txt, #e8eaf6);
      transform: rotate(90deg);
    }

    .modal-title {
      font-size: 20px;
      font-weight: 800;
      letter-spacing: .02em;
      color: var(--txt, #e8eaf6);
      margin-bottom: 24px;
      text-align: center;
    }

    .help-trigger-btn {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 50%;
      width: 22px;
      height: 22px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: var(--accent2, #fbbf24);
      cursor: pointer;
      transition: all 0.2s ease;
      padding: 0;
      margin-left: 2px;
    }
    .help-trigger-btn:hover {
      background: rgba(255, 255, 255, 0.12);
      border-color: var(--accent2, #fbbf24);
      box-shadow: 0 0 8px rgba(251, 191, 36, 0.35);
    }
  `],
  template: `
    <div class="scene v-aurora" style="position: fixed; inset: 0; z-index: 9999; width: 100vw; height: 100vh;">
      <!-- aurora mesh -->
      <div class="mesh" [style.opacity]="fog">
        <span style="width: 540px; height: 540px; left: -120px; top: -160px; background: var(--m1);"></span>
        <span style="width: 620px; height: 620px; left: 360px; top: -240px; background: var(--m2); animation-delay: -5s;"></span>
        <span style="width: 560px; height: 560px; right: -120px; bottom: -220px; background: var(--m3); animation-delay: -9s;"></span>
        <span style="width: 440px; height: 440px; left: 500px; bottom: -200px; background: var(--m4); animation-delay: -3s;"></span>
      </div>

      <aurora-ambient></aurora-ambient>
      <aurora-sparks [n]="10" color="var(--accent2)"></aurora-sparks>
      <div class="bd-noise"></div><div class="bd-vignette"></div>

      <!-- hero layout -->
      <div style="position: absolute; left: 64px; right: 64px; top: 92px; bottom: 150px; display: flex; align-items: center; gap: 60px;">

        <!-- ── Left copy ─────────────────────────────────────────────── -->
        <div style="width: 560px; flex: 0 0 auto; z-index: 3;">
          <div class="fu" style="display: flex; align-items: center; gap: 12px;">
            <span class="live"></span>
            <span class="eyebrow">Temporada 7 · Liga Oro III</span>
            <button class="help-trigger-btn" (click)="triggerHelp()" title="Ver Tutorial">
              <aurora-icon n="help" [s]="13"></aurora-icon>
            </button>
          </div>
          <h1 class="fu" [style.font-family]="displayFont" [style.font-weight]="fw"
              style="font-size: 76px; line-height: 0.98; letter-spacing: -0.015em; margin: 18px 0 0; animation-delay: .05s;">
            Es tu hora,<br/>
            <span class="name-energy" [style.font-style]="titleFont === 'sans' ? 'normal' : 'italic'">{{ username }}</span>.
          </h1>
          <p class="fu" style="color: var(--mut); font-size: 16px; line-height: 1.55; margin: 20px 0 0; max-width: 380px; animation-delay: .1s;">
            La arena está despierta. Elegí tu mazo y buscá un rival.
          </p>

          <!-- Original CTA Buttons -->
          <div class="fu" style="display: flex; align-items: center; gap: 16px; margin-top: 40px; animation-delay: .16s;">
            <aurora-battle-cta
              id="btn-battle"
              title="BATALLAR"
              sub="Clasificatoria"
              [searching]="lobby.queueStatus() === 'waiting'"
              (startBattle)="openModal('competitive')"
              (cancelBattle)="lobby.leavePublicQueue()">
            </aurora-battle-cta>
            <button class="ghost-btn" (click)="openModal('casual')">
              <aurora-icon n="sword" [s]="18"></aurora-icon> Casual
            </button>
            <div class="bot-section">
              <button class="action-btn bot" style="margin-top: 0; padding: 12px 18px;" [disabled]="lobby.decks().length === 0" (click)="openModal('bot')">
                <span class="bot-icon">🤖</span> Jugar vs Bot
              </button>
            </div>
          </div>

          <!-- Rank strip -->
          <div id="rango-info" class="fu" style="display: flex; align-items: center; gap: 18px; margin-top: 36px; padding: 14px 18px; border: 1px solid var(--line); border-radius: 16px; background: var(--surface); width: fit-content; backdrop-filter: blur(6px); animation-delay: .22s;">
            <aurora-rank-crest [size]="48" tier="III"></aurora-rank-crest>
            <div style="border-right: 1px solid var(--line); padding-right: 18px;">
              <div class="eyebrow" style="font-size: 10.5px;">Rango</div>
              <div style="font-weight: 800; font-size: 15px; margin-top: 3px;">Oro III</div>
            </div>
            <aurora-stat [v]="profileData?.mmr?.toString() ?? '...'" k="MMR"></aurora-stat>
            <aurora-stat [v]="(profileData?.statistics?.winRate ?? 0) + '%'" k="WR"></aurora-stat>
            <div>
              <div class="num" style="display: flex; align-items: center; gap: 6px; font-size: 22px; font-weight: 700; color: var(--accent);">
                {{ streak }}
                @if (streak >= 2) {
                  <aurora-icon n="fire" [s]="streak >= 4 ? 20 : 16" class="streak-fire" [class.hot]="streak >= 4"></aurora-icon>
                }
              </div>
              <div style="font-size: 10.5px; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; color: var(--mut); margin-top: 4px;">Racha</div>
            </div>
          </div>
        </div>

        <!-- ── Floating cards (right) ──────────────────────────────── -->
        <div style="flex: 1; position: relative; height: 100%;">
          <div class="card-aura" style="width: 360px; height: 360px; right: 200px; top: 150px;"></div>
          <aurora-sparks [n]="14" color="var(--accent2)" [area]="{ x: 35, y: 10, w: 60, h: 70 }"></aurora-sparks>
          <div class="bd-glow" style="width: 520px; height: 520px; right: 140px; top: 120px; background: var(--accent); opacity: .18;"></div>

          <div style="position: absolute; right: 432px; top: 250px;">
            <aurora-float-card [card]="cards[1]" [w]="196" [rot]="-13" delay="-2s" [z]="2" [op]="0.92"></aurora-float-card>
          </div>
          <div style="position: absolute; right: 96px; top: 236px;">
            <aurora-float-card [card]="cards[2]" [w]="186" [rot]="13" delay="-4s" [z]="2" [op]="0.92"></aurora-float-card>
          </div>
          <div style="position: absolute; right: 232px; top: 120px; z-index: 3;">
            <aurora-holo-card [card]="cards[0]" [w]="250" [baseRot]="-3"></aurora-holo-card>
          </div>
        </div>

      </div>

      <!-- active-deck dock -->
      <aurora-deck-rail [deck]="deckRail" [display]="displayFont"></aurora-deck-rail>

      <!-- ── Match Modal ──────────────────────────────────────────── -->
      @if (modalOpen()) {
        <div class="modal-backdrop" (click)="closeModal()">
          <div class="modal-card" (click)="$event.stopPropagation()">
            <button class="modal-close-btn" (click)="closeModal()">✕</button>
            
            <h3 class="modal-title">
              @if (modalMode() === 'competitive') {
                🏆 Clasificatoria
              } @else if (modalMode() === 'casual') {
                ⚔️ Partida Casual
              } @else {
                🤖 Jugar vs Bot
              }
            </h3>

            <!-- Tabs: Pública / Privada -->
            @if (modalMode() !== 'bot') {
            <div class="panel-tabs">
              <button class="panel-tab" [class.active]="activeTab() === 'public'" (click)="setTab('public')" id="tab-public">
                🌐 Partida Pública
              </button>
              <button class="panel-tab" [class.active]="activeTab() === 'private'" (click)="setTab('private')" id="tab-private">
                🔒 Sala Privada
              </button>
            </div>
            }

            <!-- ── Deck selector ── -->
            @if (!isAnyMatchPending()) {
              <div class="deck-label">Tu mazo</div>

              @if (lobby.decks().length === 0) {
                <div class="no-decks-hint">
                  ⚠️ No tenés mazos creados.
                  <a routerLink="/deck" (click)="closeModal()" style="color: #f59e0b; font-weight: 700;">Ir al Deck Builder →</a>
                </div>
              } @else {
                <select
                  class="deck-select"
                  id="deck-selector"
                  [value]="lobby.selectedDeckId()"
                  (change)="onDeckChange($event)">
                  @for (deck of lobby.decks(); track deck.id) {
                    <option [value]="deck.id">
                      {{ deck.name }} ({{ deck.totalCards }} cartas)
                    </option>
                  }
                </select>
              }
            }

            <!-- ══════════════ TAB: PÚBLICA ══════════════ -->
            @if (activeTab() === 'public' && modalMode() !== 'bot') {

              @if (lobby.queueStatus() === 'idle') {
                <div style="display: flex; gap: 10px; margin-top: 10px;">
                  <button
                    id="btn-find-match"
                    class="action-btn secondary"
                    [disabled]="lobby.decks().length === 0"
                    (click)="lobby.joinPublicQueue(false)">
                    🙂 Casual
                  </button>
                  <button
                    id="btn-find-match-ranked"
                    class="action-btn primary"
                    [disabled]="lobby.decks().length === 0"
                    (click)="lobby.joinPublicQueue(true)">
                    🏆 Ranked
                  </button>
                </div>
              }

              @if (lobby.queueStatus() === 'waiting') {
                <div class="waiting-state">
                  <div class="spinner-ring"></div>
                  <div class="waiting-text">Buscando rival...</div>
                  <div class="waiting-sub">En cola pública · Mazo seleccionado</div>
                  <button class="action-btn danger" style="width: auto; padding: 8px 20px; font-size: 12px;" (click)="lobby.leavePublicQueue()">
                    ✕ Cancelar búsqueda
                  </button>
                </div>
              }

            }

            <!-- ══════════════ TAB: PRIVADA ══════════════ -->
            @if (activeTab() === 'private' && modalMode() !== 'bot') {

              <!-- Sub-tabs: Crear / Unirse -->
              @if (!isAnyMatchPending()) {
                <div class="private-mode-tabs">
                  <button class="priv-tab" [class.active]="privateMode() === 'create'" (click)="privateMode.set('create')" id="priv-tab-create">
                    ＋ Crear sala
                  </button>
                  <button class="priv-tab" [class.active]="privateMode() === 'join'" (click)="privateMode.set('join')" id="priv-tab-join">
                    → Unirse
                  </button>
                </div>
              }

              <!-- Modo: CREAR -->
              @if (privateMode() === 'create') {

                @if (lobby.roomStatus() === 'idle') {
                  <button
                    id="btn-create-room"
                    class="action-btn primary"
                    [disabled]="lobby.decks().length === 0"
                    (click)="lobby.createPrivateRoom()">
                    ＋ Crear Sala Privada
                  </button>
                }

                @if (lobby.roomStatus() === 'waiting' && lobby.roomCode()) {
                  <div class="room-code-box">
                    <div class="room-code-label">Código de sala</div>
                    <div class="room-code-value">{{ lobby.roomCode() }}</div>
                    <div class="room-code-hint">Compartí este código con tu rival</div>
                    <button class="copy-btn" (click)="copyRoomCode()">
                      {{ copied() ? '✓ Copiado' : '⧉ Copiar código' }}
                    </button>
                  </div>
                  <div class="waiting-state" style="gap: 10px;">
                    <div class="spinner-ring" style="width: 36px; height: 36px; border-width: 2px;"></div>
                    <div class="waiting-sub">Esperando que tu rival ingrese el código...</div>
                    <button class="action-btn danger" style="width: auto; padding: 7px 18px; font-size: 12px;" (click)="cancelRoom()">
                      ✕ Cancelar sala
                    </button>
                  </div>
                }

              }

              <!-- Modo: UNIRSE -->
              @if (privateMode() === 'join') {
                <input
                  id="room-code-input"
                  class="code-input"
                  type="text"
                  placeholder="Ingresá el código"
                  maxlength="6"
                  [(ngModel)]="joinCode"
                />
                <button
                  id="btn-join-room"
                  class="action-btn primary"
                  [disabled]="lobby.decks().length === 0 || joinCode.trim().length < 3"
                  (click)="lobby.joinPrivateRoom(joinCode)">
                  → Unirse a la sala
                </button>
              }

            }

            <!-- ══════════════ MODO BOT ══════════════ -->
            @if (modalMode() === 'bot') {
              <div style="display: flex; gap: 10px; margin-top: 10px;">
                <button
                  class="action-btn bot"
                  style="margin-top: 0; width: 100%;"
                  [disabled]="lobby.decks().length === 0"
                  (click)="startBotMatch()">
                  <span class="bot-icon">🤖</span> Iniciar Partida
                </button>
              </div>
            }

            <!-- Error message -->
            @if (lobby.lobbyError()) {
              <div class="error-msg">⚠️ {{ lobby.lobbyError() }}</div>
            }

          </div>
        </div>
      }

    </div>
  `,
})
export class LobbyAuroraComponent implements OnInit, OnDestroy {
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private authService = inject(AuthService);
  private profileService = inject(ProfileService);
  private matchBackendService = inject(MatchBackendService);
  readonly lobby = inject(LobbyService);
  private tutorialService = inject(TutorialService);

  get username(): string {
    return this.authService.username ?? 'Invitado';
  }

  // ── UI State ────────────────────────────────────────────────────────────
  readonly activeTab = signal<LobbyTab>('public');
  readonly privateMode = signal<PrivateMode>('create');
  readonly copied = signal(false);
  readonly modalOpen = signal(false);
  readonly modalMode = signal<'competitive' | 'casual' | 'bot'>('competitive');
  joinCode = '';

  profileData: UserProfileResponseDTO | null = null;

  readonly isAnyMatchPending = computed(
    () =>
      this.lobby.queueStatus() === 'waiting' ||
      this.lobby.roomStatus() === 'waiting'
  );

  // ── Visual config ────────────────────────────────────────────────────────
  fog = 0.62;
  titleFont = 'serif';
  get streak(): number {
    return this.profileData?.statistics?.winStreak ?? 0;
  }

  get displayFont() {
    return this.titleFont === 'sans'
      ? "'Space Grotesk',sans-serif"
      : "'Instrument Serif',serif";
  }

  get fw() {
    return this.titleFont === 'sans' ? 700 : 400;
  }

  cards = [
    { name: 'Charizard EX', type: 'fire', img: 'https://images.pokemontcg.io/xy1/11_hires.png' },
    { name: 'Greninja EX', type: 'water', img: 'https://images.pokemontcg.io/xy9/40_hires.png' },
    { name: 'Mewtwo EX', type: 'psychic', img: 'https://images.pokemontcg.io/xy8/62_hires.png' },
  ];

  deckRail = [
    { name: 'Charizard EX', img: 'https://images.pokemontcg.io/xy1/11.png' },
    { name: 'Pikachu', img: 'https://images.pokemontcg.io/xy1/42.png' },
    { name: 'Arcanine', img: 'https://images.pokemontcg.io/sm1/22.png' },
    { name: 'Ninetales', img: 'https://images.pokemontcg.io/sm1/15.png' },
    { name: 'Magmar', img: 'https://images.pokemontcg.io/det1/2.png' },
    { name: 'Charizard', img: 'https://images.pokemontcg.io/base1/4.png' },
  ];

  // ── Lifecycle ────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.lobby.reset();
    this.lobby.loadDecks();
    this.lobby.connectLobbyWebSocket();
    this.tutorialService.triggerTutorial('lobby');

    const username = this.authService.username;
    if (username) {
      this.profileService.getProfile(username).subscribe({
        next: (data) => {
          this.profileData = data;
          this.cdr.detectChanges();
        },
        error: (err) => console.warn('Error fetching profile', err),
      });
    }
  }

  ngOnDestroy(): void {
    // Si el usuario navega fuera sin haber encontrado partida, limpiamos
    if (this.lobby.queueStatus() === 'waiting') {
      this.lobby.leavePublicQueue();
    }
    this.lobby.disconnectLobbyWebSocket();
  }

  // ── Handlers ─────────────────────────────────────────────────────────────

  startBotMatch(): void {
    const decks = this.lobby.decks();
    if (decks.length === 0) {
      alert('Debes tener al menos un mazo para jugar.');
      return;
    }
    const selectedDeckId = this.lobby.selectedDeckId();
    const deckId = selectedDeckId ? selectedDeckId : decks[0].id;
    const username = this.authService.username;
    
    if (username) {
      this.matchBackendService.createBotMatch(username, deckId).subscribe({
        next: (res) => {
          this.router.navigate(['/battle', res.matchId]);
        },
        error: (err) => {
          console.error('Error al crear partida contra bot', err);
          alert('Hubo un problema al crear la partida contra el bot.');
        }
      });
    }
  }

  openModal(mode: 'competitive' | 'casual' | 'bot'): void {
    this.modalMode.set(mode);
    this.modalOpen.set(true);
  }

  closeModal(): void {
    if (this.isAnyMatchPending()) {
      if (this.lobby.queueStatus() === 'waiting') {
        this.lobby.leavePublicQueue();
      }
      if (this.lobby.roomStatus() === 'waiting') {
        this.cancelRoom();
      }
    }
    this.modalOpen.set(false);
  }

  setTab(tab: LobbyTab): void {
    if (this.isAnyMatchPending()) return; // No cambiar tabs mientras busca
    this.activeTab.set(tab);
    this.lobby.lobbyError.set(null);
  }

  onDeckChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.lobby.selectedDeckId.set(Number(value));
  }

  async copyRoomCode(): Promise<void> {
    const code = this.lobby.roomCode();
    if (code) {
      await navigator.clipboard.writeText(code);
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    }
  }

  cancelRoom(): void {
    this.lobby.roomStatus.set('idle');
    this.lobby.roomCode.set(null);
    this.lobby.lobbyError.set(null);
  }

  triggerHelp(): void {
    this.tutorialService.triggerTutorial('lobby', true);
  }
}
