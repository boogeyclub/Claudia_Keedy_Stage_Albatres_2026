import { Injectable, inject, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { MarketApiService } from './market-api.service';
import { MarketRealtimeEvent } from './market-models';

/**
 * Live connection to the market stream (Server-Sent Events).
 *
 * The service is created lazily by the screens that need live updates: opening the connection is a
 * side effect of calling {@link connect}, and closing it happens on {@link disconnect} when the last
 * screen leaves. One connection per tab is enough — the API pushes everything the account may see.
 *
 * A browser that drops the stream (network change, laptop asleep) is reconnected automatically with
 * a short backoff, because the pin is only useful while it is fresh.
 */
@Injectable({ providedIn: 'root' })
export class MarketRealtimeService {
  private readonly marketApi = inject(MarketApiService);
  private readonly events = new Subject<MarketRealtimeEvent>();
  private readonly connectionState = signal<'IDLE' | 'CONNECTING' | 'LIVE' | 'RETRYING'>('IDLE');

  private source: EventSource | null = null;
  private subscribers = 0;
  private retryHandle: ReturnType<typeof setTimeout> | null = null;
  private retryDelayMs = 2000;

  /** Current state, exposed so a screen can show a discreet indicator. */
  readonly state = this.connectionState.asReadonly();

  /** Every event pushed by the API while the connection is open. */
  readonly stream: Observable<MarketRealtimeEvent> = this.events.asObservable();

  /**
   * Opens the stream for the first caller and keeps it open while other screens are subscribed.
   * The session cookie authenticates the request, exactly like the other API calls.
   */
  connect(): void {
    this.subscribers += 1;
    if (this.source || this.retryHandle) {
      return;
    }
    this.open();
  }

  /** Releases one subscription; the stream closes when nobody needs it any more. */
  disconnect(): void {
    this.subscribers = Math.max(0, this.subscribers - 1);
    if (this.subscribers > 0) {
      return;
    }

    if (this.retryHandle) {
      clearTimeout(this.retryHandle);
      this.retryHandle = null;
    }
    this.source?.close();
    this.source = null;
    this.connectionState.set('IDLE');
  }

  private open(): void {
    this.connectionState.set('CONNECTING');
    const source = new EventSource(this.marketApi.eventsUrl, { withCredentials: true });
    this.source = source;

    source.addEventListener('ready', () => {
      this.retryDelayMs = 2000;
      this.connectionState.set('LIVE');
    });

    source.addEventListener('market', (message: MessageEvent<string>) => {
      const event = this.parse(message.data);
      if (event) {
        this.events.next(event);
      }
    });

    // The heartbeat only exists to keep the connection alive; nothing to do with it.
    source.addEventListener('heartbeat', () => undefined);

    source.addEventListener('error', () => {
      source.close();
      this.source = null;
      if (this.subscribers === 0) {
        return;
      }
      this.connectionState.set('RETRYING');
      this.retryHandle = setTimeout(() => {
        this.retryHandle = null;
        if (this.subscribers > 0) {
          this.open();
        }
      }, this.retryDelayMs);
      // Back off gently so a stopped backend is not hammered, and cap it at 30 seconds.
      this.retryDelayMs = Math.min(this.retryDelayMs * 2, 30000);
    });
  }

  private parse(payload: string): MarketRealtimeEvent | null {
    try {
      return JSON.parse(payload) as MarketRealtimeEvent;
    } catch {
      return null;
    }
  }
}
