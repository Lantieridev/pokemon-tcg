import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RankingService, RankingDto } from '../../core/services/ranking.service';
import { AuthService } from '../../core/services/auth.service';

const TIER_ORDER = ['Grandmaster', 'Master', 'Diamond', 'Platinum', 'Gold', 'Silver', 'Bronze', 'Unranked'];

@Component({
  selector: 'app-ranking',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ranking.component.html',
  styleUrl: './ranking.component.css'
})
export class RankingComponent implements OnInit {
  private rankingService = inject(RankingService);
  private authService = inject(AuthService);

  rankings = signal<RankingDto[]>([]);
  isLoading = signal(true);
  errorMsg = signal('');
  selectedTier = signal<string | null>(null);

  /** Tiers actually present in this ranking slice, in competitive order —
   *  drives the filter row so it never shows a league nobody is in. */
  availableTiers = computed(() => {
    const present = new Set(this.rankings().map(r => r.tier));
    return TIER_ORDER.filter(t => present.has(t));
  });

  filteredRankings = computed(() => {
    const tier = this.selectedTier();
    if (!tier) return this.rankings();
    return this.rankings().filter(r => r.tier === tier);
  });

  /** Your own row + position, so you don't have to scroll to find yourself —
   *  the whole point of checking a leaderboard is "where do I stand." */
  myRank = computed(() => {
    const username = this.authService.username;
    if (!username) return null;
    const idx = this.rankings().findIndex(r => r.username === username);
    if (idx === -1) return null;
    return { entry: this.rankings()[idx], position: idx + 1 };
  });

  ngOnInit() {
    this.rankingService.getTopGlobal(0, 50).subscribe({
      next: (res) => {
        this.rankings.set(res.content);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMsg.set('No se pudo cargar el ranking global.');
        this.isLoading.set(false);
      }
    });
  }

  setTier(tier: string | null) {
    this.selectedTier.set(tier);
  }

  tierColor(tier: string): string {
    const colors: Record<string, string> = {
      Grandmaster: '#f56565', Master: '#9f7aea', Diamond: '#63b3ed',
      Platinum: '#4fd1c5', Gold: '#ecc94b', Silver: '#a0aec0', Bronze: '#d97d4a'
    };
    return colors[tier] ?? '#a0aec0';
  }
}
