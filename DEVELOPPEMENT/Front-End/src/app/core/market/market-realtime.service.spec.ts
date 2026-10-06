import { TestBed } from '@angular/core/testing';
import { MarketApiService } from './market-api.service';
import { MarketRealtimeService } from './market-realtime.service';
import { MarketRealtimeEvent } from './market-models';

/** Minimal stand-in for the browser's `EventSource`, with the parts the service relies on. */
class FakeEventSource {
  static readonly instances: FakeEventSource[] = [];

  readonly listeners = new Map<string, ((event: Event) => void)[]>();
  closed = false;

  constructor(readonly url: string, readonly options?: { withCredentials?: boolean }) {
    FakeEventSource.instances.push(this);
  }

  addEventListener(type: string, listener: (event: Event) => void): void {
    this.listeners.set(type, [...(this.listeners.get(type) ?? []), listener]);
  }

  close(): void {
    this.closed = true;
  }

  emit(type: string, payload?: unknown): void {
    const event = { data: payload === undefined ? undefined : JSON.stringify(payload) } as MessageEvent<string>;
    for (const listener of this.listeners.get(type) ?? []) {
      listener(event);
    }
  }

  static get last(): FakeEventSource {
    return FakeEventSource.instances[FakeEventSource.instances.length - 1];
  }

  static reset(): void {
    FakeEventSource.instances.length = 0;
  }
}

const EVENT: MarketRealtimeEvent = {
  type: 'MESSAGE',
  conversationId: 7,
  authorId: 3,
  lotId: 12,
  lotTitre: 'Cacao fermenté',
  authorName: 'Amina',
  preview: 'Bonjour',
  at: '2026-10-06T08:00:00Z'
};

describe('MarketRealtimeService', () => {
  let service: MarketRealtimeService;

  beforeEach(() => {
    FakeEventSource.reset();
    vi.useFakeTimers();
    vi.stubGlobal('EventSource', FakeEventSource);
    TestBed.configureTestingModule({
      providers: [{ provide: MarketApiService, useValue: { eventsUrl: '/cacaomarketcm/api/market/events' } }]
    });
    service = TestBed.inject(MarketRealtimeService);
  });

  afterEach(() => {
    service.disconnect();
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('opens a single credentialed stream even when several screens connect', () => {
    service.connect();
    service.connect();

    expect(FakeEventSource.instances).toHaveLength(1);
    expect(FakeEventSource.last.url).toBe('/cacaomarketcm/api/market/events');
    expect(FakeEventSource.last.options).toEqual({ withCredentials: true });
    expect(service.state()).toBe('CONNECTING');
  });

  it('shows the stream as live once the API is ready', () => {
    service.connect();
    FakeEventSource.last.emit('ready');

    expect(service.state()).toBe('LIVE');
  });

  it('forwards every market event to the screens', () => {
    const received: MarketRealtimeEvent[] = [];
    service.connect();
    service.stream.subscribe((event) => received.push(event));

    FakeEventSource.last.emit('market', EVENT);
    FakeEventSource.last.emit('heartbeat');
    FakeEventSource.last.emit('market', { ...EVENT, conversationId: 8 });

    expect(received.map((event) => event.conversationId)).toEqual([7, 8]);
    expect(received[0]).toEqual(EVENT);
  });

  it('ignores a payload it cannot read instead of breaking the stream', () => {
    const received: MarketRealtimeEvent[] = [];
    service.connect();
    service.stream.subscribe((event) => received.push(event));

    const listeners = FakeEventSource.last.listeners.get('market') ?? [];
    for (const listener of listeners) {
      listener({ data: 'not json' } as MessageEvent<string>);
    }

    expect(received).toEqual([]);
    expect(service.state()).toBe('CONNECTING');
  });

  it('reconnects with a growing backoff while the stream is down', () => {
    service.connect();
    FakeEventSource.last.emit('error');

    expect(service.state()).toBe('RETRYING');
    expect(FakeEventSource.instances).toHaveLength(1);

    vi.advanceTimersByTime(2000);
    expect(FakeEventSource.instances).toHaveLength(2);
    expect(service.state()).toBe('CONNECTING');

    // A second failure waits longer: the API must not be hammered while it restarts.
    FakeEventSource.last.emit('error');
    vi.advanceTimersByTime(2000);
    expect(FakeEventSource.instances).toHaveLength(2);
    vi.advanceTimersByTime(2000);
    expect(FakeEventSource.instances).toHaveLength(3);
  });

  it('closes the stream as soon as the last screen leaves', () => {
    service.connect();
    const opened = FakeEventSource.last;
    service.disconnect();

    expect(opened.closed).toBe(true);
    expect(service.state()).toBe('IDLE');
  });

  it('keeps the stream open while another screen is still subscribed', () => {
    service.connect();
    service.connect();
    const opened = FakeEventSource.last;

    service.disconnect();

    expect(opened.closed).toBe(false);
    expect(service.state()).toBe('CONNECTING');
  });

  it('does not reconnect after the last screen left while the stream was down', () => {
    service.connect();
    FakeEventSource.last.emit('error');
    service.disconnect();

    vi.advanceTimersByTime(60000);

    expect(FakeEventSource.instances).toHaveLength(1);
    expect(service.state()).toBe('IDLE');
  });
});
