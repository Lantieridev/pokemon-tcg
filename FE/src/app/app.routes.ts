import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'lobby', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/login/login').then(m => m.Login) },
  { path: 'lobby', canActivate: [authGuard], loadComponent: () => import('./features/lobby/lobby.component').then(m => m.LobbyComponent) },
  { path: 'battle', canActivate: [authGuard], loadComponent: () => import('./features/battle/battle.component').then(m => m.BattleComponent) },
  { path: 'deck', canActivate: [authGuard], loadComponent: () => import('./features/deck/deck').then(m => m.Deck) },
  { path: 'social', canActivate: [authGuard], loadComponent: () => import('./features/social/social').then(m => m.Social) },
  { path: 'profile', canActivate: [authGuard], loadComponent: () => import('./features/profile/profile').then(m => m.Profile) },
  { path: 'simulator', canActivate: [authGuard], loadComponent: () => import('./features/simulator/simulator').then(m => m.Simulator) },
  {
    path: '**',
    redirectTo: ''
  }
];
