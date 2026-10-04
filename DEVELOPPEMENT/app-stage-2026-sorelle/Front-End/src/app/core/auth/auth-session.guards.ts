import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthenticatedUserRole } from './auth-api.service';
import { dashboardPathForRole } from './auth-role';
import { AuthSessionService } from './auth-session.service';

/** Redirects unauthenticated navigation to sign-in after verifying the server session once. */
export const authenticatedGuard: CanActivateFn = (_route, state) => {
  const session = inject(AuthSessionService);
  const router = inject(Router);

  return session.ensureSession().pipe(
    map((user) => user
      ? true
      : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })
    )
  );
};

/** Keeps each dashboard available only to its matching user type. */
export function roleGuard(role: AuthenticatedUserRole): CanActivateFn {
  return (_route, state) => {
    const session = inject(AuthSessionService);
    const router = inject(Router);

    return session.ensureSession().pipe(
      map((user) => {
        if (!user) {
          return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
        }

        return user.role === role ? true : router.parseUrl(dashboardPathForRole(user.role));
      })
    );
  };
}

/** Prevents an authenticated browser from returning to the sign-in form. */
export const anonymousOnlyGuard: CanActivateFn = () => {
  const session = inject(AuthSessionService);
  const router = inject(Router);

  return session.ensureSession().pipe(
    map((user) => user ? router.parseUrl(dashboardPathForRole(user.role)) : true)
  );
};
