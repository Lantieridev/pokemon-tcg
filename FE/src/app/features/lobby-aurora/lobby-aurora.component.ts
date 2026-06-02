import { Component, ViewEncapsulation, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { 
  LogoComponent, TrainerChipComponent, RankCrestComponent, 
  StatComponent, BattleCtaComponent, IconComponent, BallIconComponent,
  SparksComponent, AmbientComponent 
} from './ui/aurora-ui.components';
import { HoloCardComponent, AuroraCardComponent } from './components/cards.components';
import { DeckRailComponent } from './components/deck-rail.component';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService, UserProfileResponseDTO } from '../../core/services/profile.service';

@Component({
  selector: 'app-lobby-aurora',
  standalone: true,
  imports: [
    CommonModule, RouterModule, LogoComponent, TrainerChipComponent, RankCrestComponent,
    StatComponent, BattleCtaComponent, IconComponent, BallIconComponent,
    SparksComponent, AmbientComponent, HoloCardComponent, AuroraCardComponent,
    DeckRailComponent
  ],
  encapsulation: ViewEncapsulation.None,
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

      <!-- top bar -->
      <div style="position: absolute; top: 0; left: 0; right: 0; height: 92px; display: flex; align-items: center; justify-content: space-between; padding: 0 44px; z-index: 6;">
        <aurora-logo></aurora-logo>
        <div style="display: flex; align-items: center; gap: 22px;">
          <nav style="display: flex; gap: 26px; font-size: 13.5px; font-weight: 600; color: var(--mut);">
            <a routerLink="/lobby" style="text-decoration: none; color: var(--txt);">Inicio</a>
            <a routerLink="/deck" style="text-decoration: none; color: var(--mut);">Mazos</a>
            <a routerLink="/profile" style="text-decoration: none; color: var(--mut);">Perfil</a>
          </nav>
          <div style="display: flex; align-items: center; gap: 8px;">
            <div class="topcur"><aurora-ball-icon [size]="15"></aurora-ball-icon> {{ profileData?.battlePoints ?? 0 }}</div>
            <div class="topcur"><aurora-icon n="pokecoin" [s]="15"></aurora-icon> {{ profileData?.pokecoins ?? 0 }}</div>
          </div>
          <aurora-trainer-chip [name]="username" [initial]="userInitial" [mmr]="profileData?.mmr?.toString() ?? ''"></aurora-trainer-chip>
        </div>
      </div>

      <!-- hero -->
      <div style="position: absolute; left: 64px; right: 64px; top: 92px; bottom: 150px; display: flex; align-items: center;">
        <!-- left copy -->
        <div style="width: 560px; flex: 0 0 auto; z-index: 3;">
          <div class="fu" style="display: flex; align-items: center; gap: 10px;">
            <span class="live"></span><span class="eyebrow">Temporada 7 · Liga Oro III</span>
          </div>
          <h1 class="fu" [style.font-family]="displayFont" [style.font-weight]="fw" style="font-size: 90px; line-height: 0.98; letter-spacing: -0.015em; margin: 22px 0 0; animation-delay: .05s;">
            Es tu hora,<br /><span class="name-energy" [style.font-style]="titleFont === 'sans' ? 'normal' : 'italic'">{{ username }}</span>.
          </h1>
          <p class="fu" style="color: var(--mut); font-size: 17px; line-height: 1.55; margin: 24px 0 0; max-width: 410px; animation-delay: .1s;">
            La arena está despierta. <b style="color: var(--txt); font-weight: 700;">12 480</b> entrenadores buscan rival ahora mismo.
          </p>
          <div class="fu" style="display: flex; align-items: center; gap: 16px; margin-top: 40px; animation-delay: .16s;">
            <aurora-battle-cta title="BATALLAR" sub="Clasificatoria"></aurora-battle-cta>
            <button class="ghost-btn"><aurora-icon n="sword" [s]="18"></aurora-icon> Casual</button>
          </div>

          <!-- quiet rank strip -->
          <div class="fu" style="display: flex; align-items: center; gap: 18px; margin-top: 44px; padding: 14px 18px; border: 1px solid var(--line); border-radius: 16px; background: var(--surface); width: fit-content; backdrop-filter: blur(6px); animation-delay: .22s;">
            <aurora-rank-crest [size]="52" tier="III"></aurora-rank-crest>
            <div style="border-right: 1px solid var(--line); padding-right: 18px;">
              <div class="eyebrow" style="font-size: 10.5px;">Rango</div>
              <div style="font-weight: 800; font-size: 15px; margin-top: 3px;">Oro III</div>
            </div>
            <aurora-stat [v]="profileData?.mmr?.toString() ?? '...'" k="MMR"></aurora-stat>
            <aurora-stat [v]="(profileData?.statistics?.winRate ?? 0) + '%'" k="WR"></aurora-stat>
            <div>
              <div class="num" style="display: flex; align-items: center; gap: 6px; font-size: 22px; font-weight: 700; color: var(--accent);">
                {{streak}}
                @if (streak >= 2) {
                  <aurora-icon n="fire" [s]="streak >= 4 ? 20 : 16" class="streak-fire" [class.hot]="streak >= 4"></aurora-icon>
                }
              </div>
              <div style="font-size: 10.5px; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; color: var(--mut); margin-top: 4px;">Racha</div>
            </div>
          </div>
        </div>

        <!-- right: floating real cards, fanned -->
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
      <aurora-deck-rail [deck]="deck" [display]="displayFont"></aurora-deck-rail>
    </div>
  `
})
export class LobbyAuroraComponent implements OnInit {
  private cdr = inject(ChangeDetectorRef);
  private authService = inject(AuthService);
  private profileService = inject(ProfileService);

  profileData: UserProfileResponseDTO | null = null;

  get username(): string {
    return this.authService.username ?? 'Invitado';
  }

  get userInitial(): string {
    return this.username.charAt(0).toUpperCase();
  }

  ngOnInit(): void {
    if (this.username !== 'Invitado') {
      this.profileService.getProfile(this.username).subscribe({
        next: (data) => {
          this.profileData = data;
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Error fetching profile', err)
      });
    }
  }

  fog = 0.62;
  titleFont = 'serif';
  streak = 4; // Racha no devuelta por API, queda mockeada por diseño
  
  get displayFont() {
    return this.titleFont === 'sans' ? "'Space Grotesk',sans-serif" : "'Instrument Serif',serif";
  }
  
  get fw() {
    return this.titleFont === 'sans' ? 700 : 400;
  }

  cards = [
    { name: 'Charizard EX', type: 'fire', img: 'https://images.pokemontcg.io/xy1/11_hires.png' },
    { name: 'Greninja EX', type: 'water', img: 'https://images.pokemontcg.io/xy9/40_hires.png' },
    { name: 'Mewtwo EX', type: 'psychic', img: 'https://images.pokemontcg.io/xy8/62_hires.png' }
  ];

  deck = [
    { name: 'Charizard EX', img: 'https://images.pokemontcg.io/xy1/11.png' },
    { name: 'Pikachu', img: 'https://images.pokemontcg.io/xy1/42.png' },
    { name: 'Arcanine', img: 'https://images.pokemontcg.io/sm1/22.png' },
    { name: 'Ninetales', img: 'https://images.pokemontcg.io/sm1/15.png' },
    { name: 'Magmar', img: 'https://images.pokemontcg.io/det1/2.png' },
    { name: 'Charizard', img: 'https://images.pokemontcg.io/base1/4.png' }
  ];
}
