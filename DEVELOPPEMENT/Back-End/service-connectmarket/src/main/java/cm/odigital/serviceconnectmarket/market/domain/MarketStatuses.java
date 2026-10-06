package cm.odigital.serviceconnectmarket.market.domain;

import java.util.Locale;
import java.util.Optional;

/** Case-insensitive lookup of a submitted status value. */
public final class MarketStatuses {

    public static Optional<LotStatus> lotStatus(String value) {
        return lookup(value, LotStatus.values());
    }

    public static Optional<NegotiationStatus> negotiationStatus(String value) {
        return lookup(value, NegotiationStatus.values());
    }

    public static Optional<AppointmentStatus> appointmentStatus(String value) {
        return lookup(value, AppointmentStatus.values());
    }

    private static <T extends Enum<T>> Optional<T> lookup(String value, T[] candidates) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (T candidate : candidates) {
            if (candidate.name().equals(normalized) || candidate.name().replace('_', ' ').equals(normalized)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private MarketStatuses() {
    }
}
