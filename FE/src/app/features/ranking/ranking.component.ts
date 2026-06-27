import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RankingService, RankingDto } from '../../core/services/ranking.service';
import { AmbientComponent, SparksComponent, IconComponent } from '../lobby-aurora/ui/aurora-ui.components';

@Component({
  selector: 'app-ranking',
  standalone: true,
  imports: [CommonModule, AmbientComponent, SparksComponent, IconComponent],
  templateUrl: './ranking.component.html',
  styleUrl: './ranking.component.css'
})
export class RankingComponent implements OnInit {
  private rankingService = inject(RankingService);
  
  rankings = signal<RankingDto[]>([]);
  isLoading = signal(true);
  errorMsg = signal('');

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
}
