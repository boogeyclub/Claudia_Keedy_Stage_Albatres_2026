import { AuthenticatedUserRole } from './auth-api.service';

/**
 * One entry of the workspace navigation shown in the shared dashboard header.
 *
 * `exact` marks the workspace landing page so it stops being highlighted as soon as the user opens
 * a sub-section (all the other entries use the default prefix matching).
 */
export interface WorkspaceNavigationItem {
  readonly path: string;
  readonly labelKey: string;
  readonly exact?: boolean;
}

/**
 * Role-specific navigation displayed in the shared dashboard header.
 *
 * The sections follow each role's workspace: the administrator navigates the three dashboard
 * pillars (users, registrations, sessions, which map to the protected `gu` tables), the seller
 * prepares lots and follows buyers, and the client browses the catalogue before contacting sellers.
 */
const WORKSPACE_NAVIGATION: Record<AuthenticatedUserRole, readonly WorkspaceNavigationItem[]> = {
  ADMINISTRATEUR: [
    { path: '/dashboard/admin', labelKey: 'dashboard.header.overview', exact: true },
    { path: '/dashboard/admin/tables/utilisateurs', labelKey: 'dashboard.header.nav.users' },
    { path: '/dashboard/admin/tables/registration_confirmation', labelKey: 'dashboard.header.nav.registrations' },
    { path: '/dashboard/admin/tables/sessions_utilisateur', labelKey: 'dashboard.header.nav.sessions' }
  ],
  VENDEUR: [
    { path: '/dashboard/vendeur', labelKey: 'dashboard.header.overview', exact: true },
    { path: '/dashboard/vendeur/lots', labelKey: 'dashboard.header.nav.lots' },
    { path: '/dashboard/vendeur/profil', labelKey: 'dashboard.header.nav.profile' },
    { path: '/dashboard/vendeur/messages', labelKey: 'dashboard.header.nav.messages' }
  ],
  CLIENT: [
    { path: '/dashboard/client', labelKey: 'dashboard.header.overview', exact: true },
    { path: '/dashboard/client/catalogue', labelKey: 'dashboard.header.nav.catalog' },
    { path: '/dashboard/client/deals', labelKey: 'dashboard.header.nav.deals' },
    { path: '/dashboard/client/messages', labelKey: 'dashboard.header.nav.messages' }
  ]
};

const WORKSPACE_TITLE_KEYS: Record<AuthenticatedUserRole, string> = {
  ADMINISTRATEUR: 'dashboard.header.workspaces.administrator',
  VENDEUR: 'dashboard.header.workspaces.seller',
  CLIENT: 'dashboard.header.workspaces.client'
};

export function workspaceNavigationFor(role: AuthenticatedUserRole): readonly WorkspaceNavigationItem[] {
  return WORKSPACE_NAVIGATION[role];
}

export function workspaceTitleKeyFor(role: AuthenticatedUserRole): string {
  return WORKSPACE_TITLE_KEYS[role];
}
