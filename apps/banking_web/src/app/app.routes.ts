import { Routes } from '@angular/router';
import { ShellPage } from './core/layout/shell.page';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register.page').then((m) => m.RegisterPage),
  },
  {
    path: '',
    component: ShellPage,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
      },
      {
        path: 'loans',
        loadComponent: () => import('./features/loans/loans.page').then((m) => m.LoansPage),
      },
      {
        path: 'loans/:id',
        loadComponent: () =>
          import('./features/loans/loan-detail.page').then((m) => m.LoanDetailPage),
      },
      {
        path: 'cards',
        loadComponent: () => import('./features/cards/cards.page').then((m) => m.CardsPage),
      },
      {
        path: 'risk',
        loadComponent: () => import('./features/risk/risk.page').then((m) => m.RiskPage),
      },
      {
        path: 'ebanking',
        loadComponent: () =>
          import('./features/ebanking/ebanking.page').then((m) => m.EbankingPage),
      },
      {
        path: 'contact-center',
        loadComponent: () =>
          import('./features/contact-center/contact-center.page').then((m) => m.ContactCenterPage),
      },
      {
        path: 'enforcement',
        loadComponent: () =>
          import('./features/enforcement/enforcement.page').then((m) => m.EnforcementPage),
      },
      {
        path: 'reporting',
        loadComponent: () =>
          import('./features/reporting/reporting.page').then((m) => m.ReportingPage),
      },
      {
        path: 'reporting/dashboard',
        loadComponent: () =>
          import('./features/reporting/reporting-dashboard.page').then(
            (m) => m.ReportingDashboardPage,
          ),
      },
      {
        path: 'cases',
        loadComponent: () => import('./features/cases/cases.page').then((m) => m.CasesPage),
      },
      {
        path: 'collections',
        loadComponent: () =>
          import('./features/collections/collections.page').then((m) => m.CollectionsPage),
      },
      {
        path: 'approvals',
        loadComponent: () =>
          import('./features/approvals/approvals.page').then((m) => m.ApprovalsPage),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
