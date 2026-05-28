import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'lobby', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/login/login').then(m => m.Login) },
  { path: 'lobby', loadComponent: () => import('./features/lobby/lobby.component').then(m => m.LobbyComponent) },
  { path: 'battle', loadComponent: () => import('./features/battle/battle.component').then(m => m.BattleComponent) },
  { path: 'deck', loadComponent: () => import('./features/deck/deck').then(m => m.Deck) },
  { path: 'social', loadComponent: () => import('./features/social/social').then(m => m.Social) },
  { path: 'profile', loadComponent: () => import('./features/profile/profile').then(m => m.Profile) },
  { path: 'simulator', loadComponent: () => import('./features/simulator/simulator').then(m => m.Simulator) },
  {
    path: '**',
    redirectTo: ''
  }
];
