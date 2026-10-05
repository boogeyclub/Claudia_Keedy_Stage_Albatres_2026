import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthApiService, BrowserSession } from '../../../../core/auth/auth-api.service';
import { roleTranslationKeyFor } from '../../../../core/auth/auth-role';
import { AuthSessionService } from '../../../../core/auth/auth-session.service';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { NotificationService } from '../../../../core/notifications/notification.service';

@Component({
  selector: 'app-account-settings',
  templateUrl: './account-settings.html',
  styleUrl: './account-settings.css'
})
export class AccountSettingsComponent implements OnInit {
  protected readonly i18n = inject(TranslationService);
  protected readonly user = inject(AuthSessionService).user;
  protected readonly roleTranslationKeyFor = roleTranslationKeyFor;
  protected readonly sessions = signal<readonly BrowserSession[]>([]);
  protected readonly isLoadingSessions = signal(false);
  protected readonly disconnectingSessionId = signal<number | null>(null);

  private readonly authApi = inject(AuthApiService);
  private readonly authSession = inject(AuthSessionService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly title = inject(Title);

  constructor() {
    effect(() => this.title.setTitle(this.i18n.t('meta.accountTitle')));
  }

  ngOnInit(): void {
    this.loadSessions();
  }

  protected loadSessions(): void {
    if (this.isLoadingSessions()) {
      return;
    }

    this.isLoadingSessions.set(true);
    this.authApi.browserSessions().pipe(
      finalize(() => this.isLoadingSessions.set(false))
    ).subscribe({
      next: (sessions) => this.sessions.set(sessions),
      error: () => this.notifications.error({ key: 'notifications.session.sessionsLoadFailed' })
    });
  }

  protected disconnect(session: BrowserSession): void {
    if (this.disconnectingSessionId() !== null) {
      return;
    }

    this.disconnectingSessionId.set(session.id);
    if (session.current) {
      this.authSession.logout().pipe(
        this.notifications.trackApiCall({
          start: { key: 'notifications.session.signingOut' },
          success: { key: 'notifications.session.signedOut' },
          error: { key: 'notifications.session.signOutFailed' }
        }),
        finalize(() => this.disconnectingSessionId.set(null))
      ).subscribe({
        next: () => void this.router.navigateByUrl('/login')
      });
      return;
    }

    this.authApi.disconnectBrowserSession(session.id).pipe(
      this.notifications.trackApiCall({
        start: { key: 'notifications.session.disconnecting' },
        success: { key: 'notifications.session.disconnected' },
        error: { key: 'notifications.session.disconnectFailed' }
      }),
      finalize(() => this.disconnectingSessionId.set(null))
    ).subscribe({
      next: () => this.sessions.update((items) => items.filter((item) => item.id !== session.id))
    });
  }

  protected formattedDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '—';
    }

    return new Intl.DateTimeFormat(this.i18n.language() === 'fr' ? 'fr-FR' : 'en-GB', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(date);
  }
}
