import { Injectable, signal } from '@angular/core';
import { MonoTypeOperatorFunction, defer, catchError, finalize, tap, throwError } from 'rxjs';
import { TranslationParams } from '../i18n/translation.service';

export type NotificationKind = 'loading' | 'success' | 'error' | 'warning' | 'info';

export interface NotificationMessage {
  key: string;
  params?: TranslationParams;
}

export interface NotificationOptions {
  durationMs?: number;
  dismissible?: boolean;
}

export interface AppNotification extends NotificationMessage {
  id: number;
  kind: NotificationKind;
  dismissible: boolean;
}

export interface ApiCallNotifications<T> {
  start: NotificationMessage;
  success: NotificationMessage | ((response: T) => NotificationMessage);
  error?: NotificationMessage | ((error: unknown) => NotificationMessage);
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly notifications = signal<readonly AppNotification[]>([]);

  private nextId = 0;
  private readonly dismissalTimers = new Map<number, ReturnType<typeof setTimeout>>();

  loading(message: NotificationMessage, options: NotificationOptions = {}): number {
    return this.show('loading', message, { dismissible: false, ...options });
  }

  success(message: NotificationMessage, options: NotificationOptions = {}): number {
    return this.show('success', message, options);
  }

  error(message: NotificationMessage, options: NotificationOptions = {}): number {
    return this.show('error', message, options);
  }

  warning(message: NotificationMessage, options: NotificationOptions = {}): number {
    return this.show('warning', message, options);
  }

  info(message: NotificationMessage, options: NotificationOptions = {}): number {
    return this.show('info', message, options);
  }

  show(kind: NotificationKind, message: NotificationMessage, options: NotificationOptions = {}): number {
    const id = ++this.nextId;
    const notification: AppNotification = {
      id,
      kind,
      key: message.key,
      params: message.params,
      dismissible: options.dismissible ?? kind !== 'loading'
    };

    this.notifications.update((items) => [...items, notification].slice(-5));
    this.scheduleDismissal(notification, options.durationMs);
    return id;
  }

  replace(id: number, kind: NotificationKind, message: NotificationMessage, options: NotificationOptions = {}): void {
    const existing = this.notifications().find((notification) => notification.id === id);
    if (!existing) {
      this.show(kind, message, options);
      return;
    }

    const notification: AppNotification = {
      id,
      kind,
      key: message.key,
      params: message.params,
      dismissible: options.dismissible ?? kind !== 'loading'
    };

    this.clearDismissal(id);
    this.notifications.update((items) => items.map((item) => item.id === id ? notification : item));
    this.scheduleDismissal(notification, options.durationMs);
  }

  dismiss(id: number): void {
    this.clearDismissal(id);
    this.notifications.update((items) => items.filter((notification) => notification.id !== id));
  }

  clear(): void {
    this.dismissalTimers.forEach((timer) => clearTimeout(timer));
    this.dismissalTimers.clear();
    this.notifications.set([]);
  }

  trackApiCall<T>(lifecycle: ApiCallNotifications<T>): MonoTypeOperatorFunction<T> {
    return (source) => defer(() => {
      const notificationId = this.loading(lifecycle.start);
      let completedWithOutcome = false;

      return source.pipe(
        tap((response) => {
          completedWithOutcome = true;
          this.replace(
            notificationId,
            'success',
            this.resolveMessage(lifecycle.success, response)
          );
        }),
        catchError((error: unknown) => {
          completedWithOutcome = true;
          this.replace(
            notificationId,
            'error',
            lifecycle.error
              ? this.resolveMessage(lifecycle.error, error)
              : { key: 'notifications.api.requestFailed' }
          );
          return throwError(() => error);
        }),
        finalize(() => {
          if (!completedWithOutcome) {
            this.dismiss(notificationId);
          }
        })
      );
    });
  }

  private resolveMessage<T>(
    message: NotificationMessage | ((value: T) => NotificationMessage),
    value: T
  ): NotificationMessage {
    return typeof message === 'function' ? message(value) : message;
  }

  private scheduleDismissal(notification: AppNotification, durationMs?: number): void {
    const duration = durationMs ?? this.defaultDuration(notification.kind);
    if (duration <= 0) {
      return;
    }

    const timer = setTimeout(() => this.dismiss(notification.id), duration);
    this.dismissalTimers.set(notification.id, timer);
  }

  private clearDismissal(id: number): void {
    const timer = this.dismissalTimers.get(id);
    if (timer !== undefined) {
      clearTimeout(timer);
      this.dismissalTimers.delete(id);
    }
  }

  private defaultDuration(kind: NotificationKind): number {
    switch (kind) {
      case 'loading':
        return 0;
      case 'error':
        return 8000;
      case 'warning':
        return 6500;
      case 'success':
        return 5000;
      default:
        return 4500;
    }
  }
}
