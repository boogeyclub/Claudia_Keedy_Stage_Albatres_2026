import { Component, inject } from '@angular/core';
import { NotificationKind, NotificationService } from '../../core/notifications/notification.service';
import { TranslationService } from '../../core/i18n/translation.service';

@Component({
  selector: 'app-notification-outlet',
  templateUrl: './notification-outlet.html',
  styleUrl: './notification-outlet.css'
})
export class NotificationOutletComponent {
  protected readonly i18n = inject(TranslationService);
  protected readonly notificationService = inject(NotificationService);

  protected roleFor(kind: NotificationKind): 'alert' | 'status' {
    return kind === 'error' || kind === 'warning' ? 'alert' : 'status';
  }
}
