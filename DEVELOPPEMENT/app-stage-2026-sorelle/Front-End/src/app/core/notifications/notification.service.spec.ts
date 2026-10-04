import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationService);
    service.clear();
  });

  afterEach(() => {
    service.clear();
  });

  it('adds and dismisses a warning notification', () => {
    const id = service.warning({ key: 'notifications.forms.invalid' }, { durationMs: 0 });

    expect(service.notifications()).toEqual([
      expect.objectContaining({ id, kind: 'warning', key: 'notifications.forms.invalid' })
    ]);

    service.dismiss(id);
    expect(service.notifications()).toEqual([]);
  });

  it('replaces an API loading notification with a success notification', () => {
    const request = new Subject<string>();
    request.pipe(service.trackApiCall({
      start: { key: 'notifications.registration.starting' },
      success: { key: 'notifications.registration.success' }
    })).subscribe();

    expect(service.notifications()[0]).toEqual(expect.objectContaining({ kind: 'loading' }));

    request.next('accepted');
    request.complete();

    expect(service.notifications()[0]).toEqual(expect.objectContaining({
      kind: 'success',
      key: 'notifications.registration.success'
    }));
  });

  it('replaces an API loading notification with the configured error notification', () => {
    const request = new Subject<string>();
    request.pipe(service.trackApiCall({
      start: { key: 'notifications.login.starting' },
      success: { key: 'notifications.login.success' },
      error: () => ({ key: 'auth.login.errors.invalidCredentials' })
    })).subscribe({ error: () => undefined });

    request.error(new Error('invalid credentials'));

    expect(service.notifications()[0]).toEqual(expect.objectContaining({
      kind: 'error',
      key: 'auth.login.errors.invalidCredentials'
    }));
  });

  it('removes an in-progress API notification when the request is cancelled', () => {
    const request = new Subject<string>();
    const subscription = request.pipe(service.trackApiCall({
      start: { key: 'notifications.login.starting' },
      success: { key: 'notifications.login.success' }
    })).subscribe();

    subscription.unsubscribe();

    expect(service.notifications()).toEqual([]);
  });
});
