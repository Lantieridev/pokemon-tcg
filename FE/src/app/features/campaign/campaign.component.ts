import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { firstValueFrom, forkJoin } from 'rxjs';

import { CampaignService } from '../../core/services/campaign.service';
import { MatchBackendService } from '../../core/services/match-backend.service';
import { CampaignNode, CampaignProgressResponse } from '../../core/models/campaign.models';
import { DeckSummaryDTO, PokemonTcgCard } from '../../core/models/game-state.models';
import { HudIconComponent, CoinIconComponent } from '../../shared/ui/ui-kit.components';
import { PokemonTcgService } from '../../core/services/pokemon-tcg.service';
import { HoloCardComponent } from '../../shared/ui/holo-card/holo-card.component';
import { ConfettiService } from '../../core/services/confetti.service';

// ── Deck validation helper ──────────────────────────────────

/** Mazo procesado con información de usabilidad */
export interface ValidatedDeck {
  summary: DeckSummaryDTO;
  usable: boolean;
  reason: string | null;
}

/** Cantidad exacta de cartas requerida para jugar */
const REQUIRED_CARDS = 60;

/**
 * Valida si un mazo puede utilizarse para iniciar una partida PvE.
 * Encapsulado como función pura para facilitar extensión con nuevas reglas.
 */
function validateDeck(deck: DeckSummaryDTO): ValidatedDeck {
  // Regla 1: Cantidad de cartas exacta
  if (deck.totalCards < REQUIRED_CARDS) {
    return {
      summary: deck,
      usable: false,
      reason: `Mazo incompleto (${deck.totalCards}/${REQUIRED_CARDS} cartas)`,
    };
  }

  if (deck.totalCards > REQUIRED_CARDS) {
    return {
      summary: deck,
      usable: false,
      reason: `Mazo excede el límite (${deck.totalCards}/${REQUIRED_CARDS} cartas)`,
    };
  }

  // Todas las validaciones pasaron
  return { summary: deck, usable: true, reason: null };
}

// ── Component ───────────────────────────────────────────────

@Component({
  selector: 'app-campaign',
  standalone: true,
  imports: [CommonModule, RouterModule, HudIconComponent, CoinIconComponent, HoloCardComponent],
  templateUrl: './campaign.component.html',
  styleUrl: './campaign.component.css',
})
export class CampaignComponent implements OnInit {
  private readonly campaignService = inject(CampaignService);
  private readonly matchService = inject(MatchBackendService);
  private readonly router = inject(Router);
  private readonly tcgService = inject(PokemonTcgService);
  private readonly confettiService = inject(ConfettiService);
  private static readonly CLEARED_COUNT_KEY = 'campaign_cleared_count';

  // ── State signals ───────────────────────────────────────
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly progress = signal<CampaignProgressResponse | null>(null);

  readonly modalOpen = signal(false);
  readonly selectedNode = signal<CampaignNode | null>(null);
  readonly selectedDeckId = signal<number | null>(null);
  readonly challenging = signal(false);
  readonly challengeError = signal<string | null>(null);

  private allDecks = signal<DeckSummaryDTO[]>([]);

  // ── Computed ────────────────────────────────────────────

  /** Porcentaje de progreso para la barra visual */
  readonly progressPercent = computed(() => {
    const p = this.progress();
    if (!p || p.totalNodesCount === 0) return 0;
    return Math.round((p.clearedNodesCount / p.totalNodesCount) * 100);
  });

  /** Mazos validados, ordenados: usables primero, luego no usables */
  readonly sortedDecks = computed<ValidatedDeck[]>(() => {
    const validated = this.allDecks().map(validateDeck);
    return validated.sort((a, b) => {
      if (a.usable === b.usable) return 0;
      return a.usable ? -1 : 1;
    });
  });

  /** Retrato real del líder si existe, o el badge de tipo Kanto como emblema
   *  para los gimnasios que no tienen un retrato con nombre dibujado todavía. */
  private static readonly GYM_ART: Record<number, string> = {
    1: 'assets/store/avatar_brock.png',
    2: 'assets/store/avatar_misty.png',
    3: 'assets/achievements/avatars/avatar_lightning_kanto.png',
    4: 'assets/achievements/avatars/avatar_grass_kanto.png',
    5: 'assets/achievements/avatars/avatar_fire_kanto.png',
    6: 'assets/achievements/avatars/avatar_psychic_kanto.png',
    7: 'assets/achievements/avatars/avatar_fire_kanto.png',
    8: 'assets/achievements/avatars/avatar_colorless_kanto.png',
  };

  gymArt(nodeId: number): string {
    return CampaignComponent.GYM_ART[nodeId] ?? 'assets/achievements/avatars/avatar_gym_leader.png';
  }

  /** Pokémon TCG type each gym leader battles with, so the hero can feature
   *  real cards from the catalog instead of just a badge icon. */
  private static readonly GYM_TCG_TYPE: Record<number, string> = {
    1: 'Fighting', 2: 'Water', 3: 'Lightning', 4: 'Grass',
    5: 'Fire', 6: 'Psychic', 7: 'Fire', 8: 'Colorless',
  };

  /** Up to 3 real cards of the current gym's type, for the hero's card fan. */
  gymCards(nodeId: number): PokemonTcgCard[] {
    const type = CampaignComponent.GYM_TCG_TYPE[nodeId];
    if (!type) return [];
    return this.tcgService.cards()
      .filter(c => c.supertype === 'Pokémon' && c.types?.includes(type))
      .slice(0, 3);
  }

  /** The card that bleeds off the leader panel's edge — deliberately a
   *  DIFFERENT card than the fan below it, so the same art doesn't repeat
   *  and read as a duplication mistake. */
  heroBleedCard(nodeId: number): PokemonTcgCard | null {
    const type = CampaignComponent.GYM_TCG_TYPE[nodeId];
    if (!type) return null;
    const pool = this.tcgService.cards().filter(c => c.supertype === 'Pokémon' && c.types?.includes(type));
    return pool[3] ?? pool[0] ?? null;
  }

  getCardImage(card: PokemonTcgCard): string {
    return card.images?.large ?? card.images?.small ?? '';
  }

  /** The gym leader panel is tinted by the actual Pokémon type he battles
   *  with — the accent comes from the content, not a fixed global theme color. */
  private static readonly GYM_TYPE_COLOR: Record<number, string> = {
    1: '#d97d4a', 2: '#4aa3ff', 3: '#ffcc33', 4: '#5ad27a',
    5: '#ff7a3d', 6: '#c87bff', 7: '#ff7a3d', 8: '#cfd6e4',
  };
  gymTypeColor(nodeId: number): string {
    return CampaignComponent.GYM_TYPE_COLOR[nodeId] ?? 'var(--accent)';
  }

  /** Clicking a cleared gym on the path previews it in the hero panel without
   *  navigating away; the active (next-to-fight) gym is still the default. */
  readonly previewNodeId = signal<number | null>(null);

  readonly heroNode = computed<CampaignNode | null>(() => {
    const nodes = this.progress()?.nodes ?? [];
    const previewId = this.previewNodeId();
    if (previewId != null) {
      const found = nodes.find(n => n.id === previewId);
      if (found) return found;
    }
    return this.activeNode();
  });

  previewNode(node: CampaignNode): void {
    if (node.status === 'LOCKED') return;
    this.previewNodeId.set(node.id);
  }

  /** The gym the player is on right now — the next UNLOCKED node, or the final
   *  node once every gym is cleared (so there's still something to feature). */
  readonly activeNode = computed<CampaignNode | null>(() => {
    const nodes = this.progress()?.nodes ?? [];
    return nodes.find(n => n.status === 'UNLOCKED') ?? nodes[nodes.length - 1] ?? null;
  });

  /** Every other gym, rendered as the compact path strip rather than full cards. */
  readonly otherNodes = computed<CampaignNode[]>(() => {
    const nodes = this.progress()?.nodes ?? [];
    const active = this.activeNode();
    return nodes.filter(n => n.id !== active?.id);
  });

  // ── Lifecycle ──────────────────────────────────────────

  ngOnInit(): void {
    this.loadProgress();
    this.tcgService.loadCards();
  }

  // ── Data loading ───────────────────────────────────────

  async loadProgress(): Promise<void> {
    this.loading.set(true);
    this.loadError.set(null);

    try {
      const data = await firstValueFrom(this.campaignService.getProgress());
      this.progress.set(data);
      this.celebrateIfNewlyCleared(data.clearedNodesCount);
    } catch (err: any) {
      const message =
        err?.error?.message ??
        err?.message ??
        'Error al cargar el progreso de la campaña.';
      this.loadError.set(message);
      console.error('[Campaign] Error loading progress:', err);
    } finally {
      this.loading.set(false);
    }
  }

  /** Fires a confetti burst the moment you come back to Campaign having
   *  cleared a gym since your last visit — sessionStorage is enough since
   *  this only needs to survive the round trip to /battle and back. */
  private celebrateIfNewlyCleared(clearedCount: number): void {
    const key = CampaignComponent.CLEARED_COUNT_KEY;
    const previous = Number(sessionStorage.getItem(key) ?? '0');
    if (clearedCount > previous) {
      this.confettiService.celebrate();
    }
    sessionStorage.setItem(key, String(clearedCount));
  }

  private async loadDecks(): Promise<void> {
    try {
      const [userDecks, templateDecks] = await firstValueFrom(
        forkJoin([
          this.matchService.getDecks(),
          this.matchService.getTemplates()
        ])
      );
      
      const all = [...(userDecks ?? []), ...(templateDecks ?? [])];
      this.allDecks.set(all);
    } catch (err) {
      console.warn('[Campaign] Error loading decks:', err);
      this.allDecks.set([]);
    }
  }

  // ── User interactions ─────────────────────────────────

  onNodeClick(node: CampaignNode): void {
    if (node.status === 'LOCKED') return;

    this.selectedNode.set(node);
    this.selectedDeckId.set(null);
    this.challengeError.set(null);
    this.modalOpen.set(true);

    // Load decks when modal opens
    this.loadDecks();
  }

  onDeckSelect(deck: ValidatedDeck): void {
    if (!deck.usable) return;
    this.selectedDeckId.set(deck.summary.id);
    this.challengeError.set(null);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.selectedNode.set(null);
    this.selectedDeckId.set(null);
    this.challengeError.set(null);
    this.challenging.set(false);
  }

  async startChallenge(): Promise<void> {
    const node = this.selectedNode();
    const deckId = this.selectedDeckId();
    if (!node || !deckId) return;

    this.challenging.set(true);
    this.challengeError.set(null);

    try {
      const res = await firstValueFrom(
        this.campaignService.challengeNode(node.id, deckId)
      );
      // Navigate to battle
      this.closeModal();
      this.router.navigate(['/battle', res.matchId]);
    } catch (err: any) {
      const message = this.extractErrorMessage(err);
      this.challengeError.set(message);
      console.error('[Campaign] Error starting challenge:', err);
    } finally {
      this.challenging.set(false);
    }
  }

  // ── Error handling ────────────────────────────────────

  private extractErrorMessage(err: any): string {
    // Spring Boot error body
    if (err?.error?.message) return err.error.message;
    // Plain text body
    if (typeof err?.error === 'string') return err.error;
    // HTTP status-based messages
    if (err?.status === 400) return 'El mazo seleccionado no es válido para este desafío.';
    if (err?.status === 403) return 'No tenés permiso para realizar esta acción.';
    if (err?.status === 404) return 'Nodo de campaña no encontrado.';
    // Fallback
    return 'Error inesperado al iniciar el desafío. Intentá de nuevo.';
  }
}
