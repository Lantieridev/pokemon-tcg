import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { firstValueFrom, forkJoin } from 'rxjs';

import { CampaignService } from '../../core/services/campaign.service';
import { MatchBackendService } from '../../core/services/match-backend.service';
import { CampaignNode, CampaignProgressResponse } from '../../core/models/campaign.models';
import { DeckSummaryDTO } from '../../core/models/game-state.models';

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
  imports: [CommonModule, RouterModule],
  templateUrl: './campaign.component.html',
  styleUrl: './campaign.component.css',
})
export class CampaignComponent implements OnInit {
  private readonly campaignService = inject(CampaignService);
  private readonly matchService = inject(MatchBackendService);
  private readonly router = inject(Router);

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

  // ── Lifecycle ──────────────────────────────────────────

  ngOnInit(): void {
    this.loadProgress();
  }

  // ── Data loading ───────────────────────────────────────

  async loadProgress(): Promise<void> {
    this.loading.set(true);
    this.loadError.set(null);

    try {
      const data = await firstValueFrom(this.campaignService.getProgress());
      this.progress.set(data);
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
