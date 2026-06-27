import { Component, Input, Output, EventEmitter, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PublicProfileDTO } from '../../../core/models/friends.models';
import { PokemonTcgService } from '../../../core/services/pokemon-tcg.service';
import { StatComponent, IconComponent, TrainerChipComponent } from '../../../features/lobby-aurora/ui/aurora-ui.components';

@Component({
  selector: 'app-public-profile-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, StatComponent, IconComponent, TrainerChipComponent],
  templateUrl: './public-profile-modal.component.html',
  styleUrls: ['./public-profile-modal.component.css']
})
export class PublicProfileModalComponent implements OnInit {
  private tcgService = inject(PokemonTcgService);

  @Input() profile!: PublicProfileDTO;
  @Output() close = new EventEmitter<void>();

  activeTab: 'showcase' | 'stats' = 'showcase';
  elementFilter = 'ALL';
  Math = Math;

  ngOnInit() {
    this.tcgService.loadCards();
  }

  onClose() {
    this.close.emit();
  }

  isCustomAvatar(av: string | undefined): boolean {
    if (!av) return false;
    const emojis = ['ash', 'misty', 'brock', 'gary', 'serena', 'red', 'default_trainer'];
    return !emojis.includes(av);
  }

  getAvatarUrl(av: string | undefined): string {
    if (!av) return '';
    
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

    const normalizedValue = av.toLowerCase()
      .normalize("NFD").replace(/[\u0300-\u036f]/g, "")
      .replace(/\s+/g, '_')
      .replace(/[^a-z0-9_]/g, '');
      
    const prefix = normalizedValue.startsWith('avatar_') ? '' : 'avatar_';
    return `assets/achievements/avatars/${prefix}${normalizedValue}.png`;
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

  get selectedMedalsList(): string[] {
    if (!this.profile?.selectedMedals) return [];
    return this.profile.selectedMedals.split(',').filter(m => !!m);
  }

  get totalWins(): number {
    return this.profile?.statistics?.matchesWon ?? 0;
  }

  get totalLosses(): number {
    return this.profile?.statistics?.matchesLost ?? 0;
  }

  get overallWinRate(): number {
    return this.profile?.statistics?.winRate ?? 0;
  }

  getShowcaseSlot(position: number) {
    return this.profile?.showcase?.find(s => s.slotPosition === position);
  }

  getCardImageById(cardId: string): string {
    const card = this.tcgService.cards().find(c => c.id === cardId);
    return card?.images?.small ?? card?.images?.large ?? '';
  }

  getTopPlayedPokemons(): any[] {
    if (!this.profile?.advancedStats?.pokemonStats) return [];
    let stats = this.profile.advancedStats.pokemonStats;
    if (this.elementFilter && this.elementFilter !== 'ALL') {
      stats = stats.filter((s: any) => s.pokemonType?.toUpperCase() === this.elementFilter.toUpperCase());
    }
    return [...stats].sort((a: any, b: any) => b.timesPlayed - a.timesPlayed).slice(0, 5);
  }

  getTopAttackers(): any[] {
    if (!this.profile?.advancedStats?.pokemonStats) return [];
    const stats = this.profile.advancedStats.pokemonStats;
    return [...stats].sort((a: any, b: any) => b.damageDealt - a.damageDealt).slice(0, 5);
  }

  getEnergyStats(): any[] {
    if (!this.profile?.advancedStats?.energyStats) return [];
    const stats = this.profile.advancedStats.energyStats;
    return [...stats].sort((a: any, b: any) => b.count - a.count);
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
}
