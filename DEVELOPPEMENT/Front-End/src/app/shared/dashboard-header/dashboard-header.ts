import { ElementRef, HostListener, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { finalize } from 'rxjs';
import { dashboardPathForRole, roleTranslationKeyFor } from '../../core/auth/auth-role';
import { workspaceNavigationFor, workspaceTitleKeyFor } from '../../core/auth/role-navigation';
import { AuthSessionService } from '../../core/auth/auth-session.service';
import { TranslationService } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { LanguageSwitcherComponent } from '../language-switcher/language-switcher';

@Component({
  selector: 'app-dashboard-header',
  imports: [RouterLink, RouterLinkActive, LanguageSwitcherComponent],
  templateUrl: './dashboard-header.html',
  styleUrl: './dashboard-header.css'
})
export class DashboardHeaderComponent {
  protected readonly i18n = inject(TranslationService);
  private readonly authSession = inject(AuthSessionService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  protected readonly user = this.authSession.user;
  protected readonly menuOpen = signal(false);
  protected readonly navigationOpen = signal(false);
  protected readonly isSigningOut = signal(false);
  protected readonly dashboardPath = computed(() => {
    const user = this.user();
    return user ? dashboardPathForRole(user.role) : '/';
  });
  protected readonly navigation = computed(() => {
    const user = this.user();
    return user ? workspaceNavigationFor(user.role) : [];
  });
  protected readonly workspaceTitle = computed(() => {
    const user = this.user();
    return user ? this.i18n.t(workspaceTitleKeyFor(user.role)) : this.i18n.t('dashboard.shared.workspace');
  });
  protected readonly displayName = computed(() => {
    const user = this.user();
    if (!user) {
      return '';
    }

    return [user.prenom, user.nom].filter((name) => name.trim().length > 0).join(' ') || user.login;
  });
  protected readonly initials = computed(() => {
    const user = this.user();
    if (!user) {
      return '?';
    }

    const initial = `${user.prenom?.trim().charAt(0) ?? ''}${user.nom?.trim().charAt(0) ?? ''}`.toUpperCase();
    return initial || user.login.trim().slice(0, 2).toUpperCase();
  });
  protected readonly roleLabel = computed(() => {
    const user = this.user();
    return user ? this.i18n.t(roleTranslationKeyFor(user.role)) : '';
  });
  protected readonly greeting = computed(() => {
    const user = this.user();
    if (!user) {
      return '';
    }

    const name = user.prenom?.trim() || this.displayName();
    return this.i18n.t('dashboard.header.greeting', { name });
  });

  @HostListener('document:keydown.escape')
  protected closeMenusOnEscape(): void {
    this.menuOpen.set(false);
    this.navigationOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  protected closeMenusWhenClickingOutside(event: MouseEvent): void {
    const target = event.target;
    if ((this.menuOpen() || this.navigationOpen()) && target instanceof Node && !this.elementRef.nativeElement.contains(target)) {
      this.menuOpen.set(false);
      this.navigationOpen.set(false);
    }
  }

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
    this.navigationOpen.set(false);
  }

  protected toggleNavigation(): void {
    this.navigationOpen.update((open) => !open);
    this.menuOpen.set(false);
  }

  protected closeMenus(): void {
    this.menuOpen.set(false);
    this.navigationOpen.set(false);
  }

  protected signOut(): void {
    if (this.isSigningOut()) {
      return;
    }

    this.isSigningOut.set(true);
    this.authSession.logout().pipe(
      this.notifications.trackApiCall({
        start: { key: 'notifications.session.signingOut' },
        success: { key: 'notifications.session.signedOut' },
        error: { key: 'notifications.session.signOutFailed' }
      }),
      finalize(() => this.isSigningOut.set(false))
    ).subscribe({
      next: () => {
        this.closeMenus();
        void this.router.navigateByUrl('/login');
      }
    });
  }
}
