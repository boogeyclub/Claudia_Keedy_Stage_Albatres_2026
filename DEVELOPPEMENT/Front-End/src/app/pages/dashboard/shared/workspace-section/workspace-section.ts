import { Component, computed, effect, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { AuthSessionService } from '../../../../core/auth/auth-session.service';
import { workspaceTitleKeyFor } from '../../../../core/auth/role-navigation';
import { TranslationService } from '../../../../core/i18n/translation.service';

/**
 * Placeholder page for a workspace section that is not built yet.
 *
 * The shared dashboard header links every role to its sections; this component keeps those links
 * navigable until the real pages replace it, instead of bouncing the user back to the landing page.
 * The text comes from the route `data`, reusing the wording already shown on the role overview.
 */
@Component({
  selector: 'app-workspace-section',
  templateUrl: './workspace-section.html',
  styleUrl: './workspace-section.css'
})
export class WorkspaceSectionComponent {
  protected readonly i18n = inject(TranslationService);
  private readonly route = inject(ActivatedRoute);
  private readonly authSession = inject(AuthSessionService);
  private readonly title = inject(Title);

  private readonly sectionData = this.route.snapshot.data;
  protected readonly titleKey = String(this.sectionData['titleKey'] ?? '');
  protected readonly descriptionKey = String(this.sectionData['descriptionKey'] ?? '');
  protected readonly workspaceTitle = computed(() => {
    const user = this.authSession.user();
    return user ? this.i18n.t(workspaceTitleKeyFor(user.role)) : this.i18n.t('dashboard.shared.workspace');
  });

  constructor() {
    effect(() => this.title.setTitle(`${this.i18n.t('common.brandName')} | ${this.i18n.t(this.titleKey)}`));
  }
}
