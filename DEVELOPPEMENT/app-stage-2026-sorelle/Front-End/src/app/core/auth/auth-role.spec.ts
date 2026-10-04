import { dashboardPathForRole, roleTranslationKeyFor } from './auth-role';

describe('authentication role routing', () => {
  it('maps every supported authenticated role to its own dashboard', () => {
    expect(dashboardPathForRole('ADMINISTRATEUR')).toBe('/dashboard/admin');
    expect(dashboardPathForRole('VENDEUR')).toBe('/dashboard/vendeur');
    expect(dashboardPathForRole('CLIENT')).toBe('/dashboard/client');
  });

  it('uses a user-facing translation role for the client dashboard', () => {
    expect(roleTranslationKeyFor('CLIENT')).toBe('dashboard.roles.user');
  });
});
