import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { dashboardPathForRole } from '../../../core/auth/auth-role';
import { AuthSessionService } from '../../../core/auth/auth-session.service';

@Component({
  selector: 'app-dashboard-redirect',
  template: ''
})
export class DashboardRedirectComponent implements OnInit {
  private readonly authSession = inject(AuthSessionService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    const user = this.authSession.user();
    if (user) {
      void this.router.navigateByUrl(dashboardPathForRole(user.role), { replaceUrl: true });
    }
  }
}
