import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'lobby', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: 'lobby',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/lobby-aurora/lobby-aurora.component').then((m) => m.LobbyAuroraComponent),
  },
  {
    path: 'battle/:matchId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/battle/battle.component').then(
        (m) => m.BattleComponent
      ),
  },
  // Ruta legacy sin matchId (compatibilidad)
  {
    path: 'battle',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/battle/battle.component').then(
        (m) => m.BattleComponent
      ),
  },
  {
    path: 'deck',
    canActivate: [authGuard],
    loadComponent: () => import('./features/deck-aurora/deck-aurora.component').then((m) => m.DeckAuroraComponent),
  },
  {
    path: 'ranking',
    canActivate: [authGuard],
    loadComponent: () => import('./features/ranking/ranking.component').then((m) => m.RankingComponent),
  },
  {
    path: 'social',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/social/social').then((m) => m.Social),
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile-aurora/profile-aurora.component').then((m) => m.ProfileAuroraComponent),
  },
  {
    path: 'simulator',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/simulator/simulator').then((m) => m.Simulator),
  },
  {
    path: 'store',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/store/store.component').then((m) => m.StoreComponent),
  },
  {
    path: 'battle-pass',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/battle-pass/battle-pass.component').then((m) => m.BattlePassComponent),
  },
  {
    path: 'campaign',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/campaign/campaign.component').then((m) => m.CampaignComponent),
  },
  { path: '**', redirectTo: '' },
];
