# Frontend notifications

`NotificationService` provides global, localized toast notifications for all Angular components.

## Direct notifications

```ts
private readonly notifications = inject(NotificationService);

this.notifications.success({ key: 'feature.saved' });
this.notifications.warning({ key: 'notifications.forms.invalid' });
this.notifications.error({ key: 'feature.failed' });
```

Messages use translation keys rather than pre-rendered text, so an active notification follows the selected English/French language.

## API lifecycle notifications

Wrap any observable API call with `trackApiCall`. It creates a loading notification when the request is subscribed to, then replaces it with success or error feedback.

```ts
this.api.save(payload).pipe(
  this.notifications.trackApiCall({
    start: { key: 'feature.saving' },
    success: { key: 'feature.saved' },
    error: () => ({ key: 'feature.failed' })
  })
).subscribe();
```

The global `<app-notification-outlet />` is mounted in `App`, so no page-specific toast markup is necessary.
