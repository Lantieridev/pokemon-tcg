import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CARDS } from '../../shared/data/cards.mock';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService, UserProfileResponseDTO } from '../../core/services/profile.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, IconComponent],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profile implements OnInit {
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
    { name:'Charizard Rush',  wins: 22, losses: 8,  color:'#ff7a3d' },
    { name:'Pikachu Toolbox', wins: 14, losses: 9,  color:'#ffcc33' },
    { name:'Blastoise Pivot', wins: 6,  losses: 4,  color:'#4aa3ff' },
    { name:'Venusaur Stall',  wins: 5,  losses: 7,  color:'#5ad27a' },
  ];

  mastery = [
    { id:'charizard', stars: 5, plays: 124 },
    { id:'pikachu',   stars: 4, plays: 87  },
    { id:'blastoise', stars: 3, plays: 56  },
    { id:'mewtwo',    stars: 3, plays: 41  },
    { id:'venusaur',  stars: 2, plays: 22  },
    { id:'alakazam',  stars: 1, plays:  9  },
  ];

  get overallWinRate(): number {
    const totalWins = this.archetypes.reduce((s, a) => s + a.wins, 0);
    const totalLoss = this.archetypes.reduce((s, a) => s + a.losses, 0);
    return Math.round((totalWins / (totalWins + totalLoss)) * 100);
  }

  get totalWins(): number {
    return this.archetypes.reduce((s, a) => s + a.wins, 0);
  }

  get totalLosses(): number {
    return this.archetypes.reduce((s, a) => s + a.losses, 0);
  }

  get donutStops(): string {
    const total = this.archetypes.reduce((s, a) => s + a.wins + a.losses, 0);
    let acc = 0;
    return this.archetypes.map((a) => {
      const start = acc;
      const span = ((a.wins + a.losses) / total) * 100;
      acc += span;
      return `${a.color} ${start}% ${acc}%`;
    }).join(', ');
  }

  Math = Math;
}
