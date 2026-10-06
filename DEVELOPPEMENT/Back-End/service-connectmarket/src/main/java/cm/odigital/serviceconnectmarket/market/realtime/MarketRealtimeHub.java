package cm.odigital.serviceconnectmarket.market.realtime;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;

/**
 * Keeps the live connections of the market open and pushes events to their owner.
 *
 * <p>Design notes:
 *
 * <ul>
 *   <li>one {@link SseEmitter} per open browser tab, keyed by account id, so a participant can keep
 *       several workspaces open at once;</li>
 *   <li>a slow or dead browser only loses its own connection: failed sends are dropped and the
 *       emitter is closed instead of blocking the request that produced the event;</li>
 *   <li>a heartbeat is sent every 25 seconds so proxies and browsers do not silently close an idle
 *       stream. It also removes connections a browser left without a proper close.</li>
 * </ul>
 */
@Component
public class MarketRealtimeHub {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketRealtimeHub.class);
    private static final Duration CONNECTION_TIMEOUT = Duration.ofMinutes(30);
    private static final long HEARTBEAT_SECONDS = 25L;
    private static final String EVENT_NAME = "market";

    private final Map<Long, List<SseEmitter>> connectionsByUtilisateur = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "market-realtime-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public MarketRealtimeHub() {
        heartbeat.scheduleAtFixedRate(this::beat, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
    }

    /** Opens the stream of one signed-in account. */
    public SseEmitter register(long utilisateurId) {
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT.toMillis());
        connectionsByUtilisateur
            .computeIfAbsent(utilisateurId, key -> new CopyOnWriteArrayList<>())
            .add(emitter);

        emitter.onCompletion(() -> remove(utilisateurId, emitter));
        emitter.onTimeout(() -> remove(utilisateurId, emitter));
        emitter.onError(throwable -> remove(utilisateurId, emitter));

        try {
            emitter.send(SseEmitter.event().name("ready").data("{\"status\":\"CONNECTED\"}"));
        } catch (IOException | IllegalStateException exception) {
            remove(utilisateurId, emitter);
        }

        LOGGER.info(
            "event=market.realtime.connected utilisateurId={} openStreams={}",
            utilisateurId,
            openStreamCount()
        );
        return emitter;
    }

    /**
     * Pushes one event to every open stream of the given accounts. Accounts without an open stream
     * are simply skipped: the event is already stored, and the browser loads it on its next request.
     */
    public void publish(Collection<Long> utilisateurIds, MarketRealtimeEvent event) {
        for (Long utilisateurId : utilisateurIds) {
            if (utilisateurId == null) {
                continue;
            }
            for (SseEmitter emitter : connectionsByUtilisateur.getOrDefault(utilisateurId, List.of())) {
                send(utilisateurId, emitter, event);
            }
        }
    }

    /** Number of open streams, exposed in the connection log line. */
    public int openStreamCount() {
        return connectionsByUtilisateur.values().stream().mapToInt(List::size).sum();
    }

    private void send(long utilisateurId, SseEmitter emitter, MarketRealtimeEvent event) {
        try {
            // SseEmitter is not safe for concurrent writes; a conversation produces few events, so a
            // per-connection lock is cheaper than a queue.
            synchronized (emitter) {
                emitter.send(SseEmitter.event().name(EVENT_NAME).data(event));
            }
        } catch (IOException | IllegalStateException exception) {
            LOGGER.info(
                "event=market.realtime.dropped utilisateurId={} reason={}",
                utilisateurId,
                exception.getClass().getSimpleName()
            );
            remove(utilisateurId, emitter);
            try {
                emitter.complete();
            } catch (RuntimeException ignored) {
                // The browser is already gone; nothing else to clean up.
            }
        }
    }

    private void beat() {
        connectionsByUtilisateur.forEach((utilisateurId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    synchronized (emitter) {
                        emitter.send(SseEmitter.event().name("heartbeat").data("{}"));
                    }
                } catch (IOException | IllegalStateException exception) {
                    remove(utilisateurId, emitter);
                }
            }
        });
    }

    private void remove(long utilisateurId, SseEmitter emitter) {
        List<SseEmitter> emitters = connectionsByUtilisateur.get(utilisateurId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            connectionsByUtilisateur.remove(utilisateurId);
        }
    }

    @PreDestroy
    void shutdown() {
        heartbeat.shutdownNow();
        connectionsByUtilisateur.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));
        connectionsByUtilisateur.clear();
    }
}
