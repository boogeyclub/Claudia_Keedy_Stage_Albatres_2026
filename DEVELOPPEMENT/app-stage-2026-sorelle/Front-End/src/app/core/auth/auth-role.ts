import { AuthenticatedUserRole } from './auth-api.service';

const DASHBOARD_PATHS: Record<AuthenticatedUserRole, string> = {
  ADMINISTRATEUR: '/dashboard/admin',
  VENDEUR: '/dashboard/vendeur',
  CLIENT: '/dashboard/client'
};

const ROLE_TRANSLATION_KEYS: Record<AuthenticatedUserRole, string> = {
  ADMINISTRATEUR: 'dashboard.roles.administrator',
  VENDEUR: 'dashboard.roles.seller',
  CLIENT: 'dashboard.roles.user'
};

export function dashboardPathForRole(role: AuthenticatedUserRole): string {
  return DASHBOARD_PATHS[role];
}

export function roleTranslationKeyFor(role: AuthenticatedUserRole): string {
  return ROLE_TRANSLATION_KEYS[role];
}
