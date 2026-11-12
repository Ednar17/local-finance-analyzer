import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'presse' },
  {
    path: 'presse',
    title: 'Revue de presse',
    loadComponent: () => import('./pages/news.component').then((m) => m.NewsComponent),
  },
  {
    path: 'rapports',
    title: "Rapports d'entreprises",
    loadComponent: () => import('./pages/reports.component').then((m) => m.ReportsComponent),
  },
  {
    path: 'budget',
    title: 'Budget',
    loadComponent: () => import('./pages/budget.component').then((m) => m.BudgetComponent),
  },
  {
    path: 'operations',
    title: 'Opérations',
    loadComponent: () => import('./pages/transactions.component').then((m) => m.TransactionsComponent),
  },
  { path: '**', redirectTo: 'presse' },
];
