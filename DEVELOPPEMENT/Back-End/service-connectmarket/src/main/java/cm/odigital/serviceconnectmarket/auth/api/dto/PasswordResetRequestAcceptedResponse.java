package cm.odigital.serviceconnectmarket.auth.api.dto;

/**
 * Deliberately generic to avoid revealing whether an email has a confirmed CacaoMarketCM account.
 */
public record PasswordResetRequestAcceptedResponse(String message) {
}
