import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, of, shareReplay, tap } from 'rxjs';
import { AuthApiService, AuthenticatedUser } from './auth-api.service';

/**
 * Keeps the safe authenticated profile in memory and restores it from the browser's
 * credentialed server session after a page refresh. Passwords and session IDs never enter it.
 */
@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly authApi = inject(AuthApiService);
  private readonly userState = signal<AuthenticatedUser | null>(null);

  private sessionResolved = false;
  private restoration$?: Observable<AuthenticatedUser | null>;

  readonly user = this.userState.asReadonly();

  setAuthenticatedUser(user: AuthenticatedUser): void {
    this.userState.set(user);
    this.sessionResolved = true;
  }

  ensureSession(): Observable<AuthenticatedUser | null> {
    if (this.sessionResolved) {
      return of(this.userState());
    }

    if (this.restoration$) {
      return this.restoration$;
    }

    const restoration = this.authApi.currentSession().pipe(
      tap((user) => this.setAuthenticatedUser(user)),
      catchError(() => {
        this.clearAuthenticatedUser();
        return of(null);
      }),
      finalize(() => {
        this.restoration$ = undefined;
      })
    );

    this.restoration$ = restoration.pipe(shareReplay({ bufferSize: 1, refCount: false }));
    return this.restoration$;
  }

  logout(): Observable<void> {
    return this.authApi.logout().pipe(tap(() => this.clearAuthenticatedUser()));
  }

  clearAuthenticatedUser(): void {
    this.userState.set(null);
    this.sessionResolved = true;
  }
}
