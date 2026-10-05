import { Routes } from '@angular/router';
import { anonymousOnlyGuard, authenticatedGuard, roleGuard } from './core/auth/auth-session.guards';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/landing/landing').then((module) => module.LandingComponent)
  },
  {
    path: 'login',
    canActivate: [anonymousOnlyGuard],
    loadComponent: () => import('./pages/login/login').then((module) => module.LoginComponent)
  },
  {
    path: 'password-reset/confirm',
    loadComponent: () => import('./pages/password-reset-confirmation/password-reset-confirmation').then((module) => module.PasswordResetConfirmationComponent)
  },
  {
    path: 'password-reset',
    loadComponent: () => import('./pages/password-reset-request/password-reset-request').then((module) => module.PasswordResetRequestComponent)
  },
  {
    path: 'registration/confirm',
    loadComponent: () => import('./pages/registration-confirmation/registration-confirmation').then((module) => module.RegistrationConfirmationComponent)
  },
  {
    path: 'registration',
    loadComponent: () => import('./pages/registration/registration').then((module) => module.RegistrationComponent)
  },
  {
    path: 'register',
    pathMatch: 'full',
    redirectTo: 'registration'
  },
  {
    path: 'dashboard',
    canActivate: [authenticatedGuard],
    loadComponent: () => import('./pages/dashboard/shared/dashboard-shell').then((module) => module.DashboardShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./pages/dashboard/shared/dashboard-redirect').then((module) => module.DashboardRedirectComponent)
      },
      {
        path: 'admin/tables/:table',
        canActivate: [roleGuard('ADMINISTRATEUR')],
        loadComponent: () => import('./pages/dashboard/admin/table-management/admin-table-management').then((module) => module.AdminTableManagementComponent)
      },
      {
        path: 'admin',
        canActivate: [roleGuard('ADMINISTRATEUR')],
        loadComponent: () => import('./pages/dashboard/admin/overview/admin-dashboard').then((module) => module.AdminDashboardComponent)
      },
      {
        path: 'vendeur',
        canActivate: [roleGuard('VENDEUR')],
        loadComponent: () => import('./pages/dashboard/seller/overview/seller-dashboard').then((module) => module.SellerDashboardComponent)
      },
      {
        path: 'vendeur/lots',
        canActivate: [roleGuard('VENDEUR')],
        data: { titleKey: 'dashboard.seller.prepareCard.title', descriptionKey: 'dashboard.seller.prepareCard.description' },
        loadComponent: () => import('./pages/dashboard/shared/workspace-section/workspace-section').then((module) => module.WorkspaceSectionComponent)
      },
      {
        path: 'vendeur/profil',
        canActivate: [roleGuard('VENDEUR')],
        data: { titleKey: 'dashboard.seller.profileCard.title', descriptionKey: 'dashboard.seller.profileCard.description' },
        loadComponent: () => import('./pages/dashboard/shared/workspace-section/workspace-section').then((module) => module.WorkspaceSectionComponent)
      },
      {
        path: 'vendeur/messages',
        canActivate: [roleGuard('VENDEUR')],
        data: { titleKey: 'dashboard.seller.conversationsCard.title', descriptionKey: 'dashboard.seller.conversationsCard.description' },
        loadComponent: () => import('./pages/dashboard/shared/workspace-section/workspace-section').then((module) => module.WorkspaceSectionComponent)
      },
      {
        path: 'client',
        canActivate: [roleGuard('CLIENT')],
        loadComponent: () => import('./pages/dashboard/client/overview/client-dashboard').then((module) => module.ClientDashboardComponent)
      },
      {
        path: 'client/catalogue',
        canActivate: [roleGuard('CLIENT')],
        data: { titleKey: 'dashboard.user.discoverCard.title', descriptionKey: 'dashboard.user.discoverCard.description' },
        loadComponent: () => import('./pages/dashboard/shared/workspace-section/workspace-section').then((module) => module.WorkspaceSectionComponent)
      },
      {
        path: 'client/preferences',
        canActivate: [roleGuard('CLIENT')],
        data: { titleKey: 'dashboard.user.preferencesCard.title', descriptionKey: 'dashboard.user.preferencesCard.description' },
        loadComponent: () => import('./pages/dashboard/shared/workspace-section/workspace-section').then((module) => module.WorkspaceSectionComponent)
      },
      {
        path: 'client/messages',
        canActivate: [roleGuard('CLIENT')],
        data: { titleKey: 'dashboard.user.conversationsCard.title', descriptionKey: 'dashboard.user.conversationsCard.description' },
        loadComponent: () => import('./pages/dashboard/shared/workspace-section/workspace-section').then((module) => module.WorkspaceSectionComponent)
      },
      {
        path: 'account',
        loadComponent: () => import('./pages/dashboard/shared/account-settings/account-settings').then((module) => module.AccountSettingsComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
