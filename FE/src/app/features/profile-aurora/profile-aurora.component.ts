import { Component, inject, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CARDS } from '../../shared/data/cards.mock';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService, UserProfileResponseDTO } from '../../core/services/profile.service';
import { StatComponent, IconComponent, TrainerChipComponent, AmbientComponent, LogoComponent } from '../lobby-aurora/ui/aurora-ui.components';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-profile-aurora',
  standalone: true,
  imports: [CommonModule, RouterModule, StatComponent, IconComponent, TrainerChipComponent, AmbientComponent, LogoComponent],
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
          <div class="avatar" style="width: 100px; height: 100px; font-size: 40px; box-shadow: 0 0 0 6px var(--bg), 0 0 0 10px var(--accent);">{{ userInitial }}</div>
          <div>
            <h1 class="display" style="font-size: 54px; font-weight: 700; margin: 0; color: var(--txt); line-height: 1;">{{ username }}</h1>
            <div style="color: var(--mut); font-weight: 600; letter-spacing: 0.05em; font-size: 14px; margin-top: 8px;">Entrenador Pokémon</div>
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
          
          <!-- Match History -->
          <div>
            <div class="eyebrow" style="margin-bottom: 20px; color: var(--accent2);">Historial Reciente</div>
            <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 20px; overflow: hidden; backdrop-filter: blur(10px);">
              @for (m of matches; track m.id) {
                <div style="display: grid; grid-template-columns: 60px 1fr 1fr 80px 100px; align-items: center; padding: 16px 20px; border-bottom: 1px solid var(--line); transition: background 0.2s;" class="match-row-hover">
                  <div [style.color]="m.w ? 'var(--accent)' : 'var(--mut)'" style="font-family: var(--display); font-size: 24px; font-weight: 700;">{{ m.w ? 'W' : 'L' }}</div>
                  <div>
                    <div style="font-weight: 700; font-size: 15px;">{{ m.deck }}</div>
                    <div style="font-size: 11px; color: var(--mut); text-transform: uppercase; letter-spacing: 0.05em; margin-top: 4px;">Tú</div>
                  </div>
                  <div>
                    <div style="font-weight: 600; font-size: 14px; color: var(--dim);">vs {{ m.opp }}</div>
                    <div style="font-size: 11px; color: var(--mut); margin-top: 4px;">Oponente: {{ m.vs }}</div>
                  </div>
                  <div class="num" [style.color]="m.w ? '#46e08a' : '#ff3b47'" style="font-size: 15px; font-weight: 700;">{{ m.lp > 0 ? '+' : '' }}{{ m.lp }} LP</div>
                  <div style="text-align: right; color: var(--dim); font-size: 12px; font-weight: 600;">{{ m.ago }}</div>
                </div>
              }
            </div>
          </div>

          <!-- Archetypes / Donut -->
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
    <style>
      .match-row-hover:hover { background: rgba(255,255,255,0.03); cursor: pointer; }
    </style>
  `
})
export class ProfileAuroraComponent implements OnInit {
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
        next: (data) => this.profileData = data,
        error: (err) => console.error('Error fetching profile', err)
      });
    }
  }

  cards = CARDS;

  matches = [
    { id:1, w:true,  deck:'Charizard Rush',  vs:'BrockSteel',  opp:'Alakazam Mill', turns:14, lp:+22, ago:'hace 2 h' },
    { id:2, w:false, deck:'Charizard Rush',  vs:'GaryOak',     opp:'Blastoise Pivot', turns:21, lp:-18, ago:'hace 4 h' },
    { id:3, w:true,  deck:'Pikachu Toolbox', vs:'MistyW',      opp:'Gyarados Burn',  turns:11, lp:+26, ago:'hace 5 h' },
    { id:4, w:true,  deck:'Charizard Rush',  vs:'JessieR',     opp:'Meowth Greed',   turns:9,  lp:+24, ago:'hace 1 d' },
    { id:5, w:false, deck:'Charizard Rush',  vs:'JamesK',      opp:'Weezing Toxic',  turns:17, lp:-22, ago:'hace 1 d' },
    { id:6, w:true,  deck:'Pikachu Toolbox', vs:'BluKaz',      opp:'Charizard Rush', turns:13, lp:+25, ago:'hace 2 d' },
  ];

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
