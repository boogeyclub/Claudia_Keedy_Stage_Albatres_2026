package cm.odigital.serviceconnectmarket.market.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.market.api.dto.AppointmentRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.LotRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.NegotiationRequest;
import cm.odigital.serviceconnectmarket.market.domain.AppointmentStatus;
import cm.odigital.serviceconnectmarket.market.domain.LotStatus;
import cm.odigital.serviceconnectmarket.market.domain.MarketStatuses;
import cm.odigital.serviceconnectmarket.market.domain.NegotiationStatus;
import cm.odigital.serviceconnectmarket.market.persistence.MarketRepository;

/**
 * Business rules of the cocoa catalogue and of the buyer/seller exchange.
 *
 * <p>Life cycles enforced here (the database repeats the important ones as CHECK constraints and
 * guard triggers):
 *
 * <ul>
 *   <li><b>Lot</b> — BROUILLON → PUBLIE → RESERVE (set when a negotiation is accepted) → VENDU, or
 *       ARCHIVE at any time by the seller. Only a published or reserved lot is visible to buyers.
 *   <li><b>Conversation</b> — OUVERTE → EN_NEGOCIATION → ACCORD (accepted proposal) or back to
 *       OUVERTE when the proposal is refused, cancelled or expires.
 *   <li><b>Négociation</b> — one open proposal per thread, PROPOSEE → ACCEPTEE, REFUSEE, ANNULEE or
 *       EXPIREE 72 hours after it was sent. Only the counterpart can decide.
 *   <li><b>Rendez-vous</b> — one open visit per thread, PROPOSE → ACCEPTE, REFUSE or ANNULE. Only
 *       the counterpart can decide; the proposer can cancel.
 * </ul>
 */
@Service
public class MarketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketService.class);
    private static final String ADMINISTRATEUR_ROLE = "ADMINISTRATEUR";
    private static final String DEFAULT_CURRENCY = "XAF";
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final Duration NEGOTIATION_TTL = Duration.ofHours(72L);
    private static final Set<LotStatus> BUYER_VISIBLE_STATUSES = Set.of(LotStatus.PUBLIE, LotStatus.RESERVE);

    private final MarketRepository repository;
    private final Clock clock;

    public MarketService(MarketRepository repository, Clock authenticationClock) {
        this.repository = repository;
        this.clock = authenticationClock;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reference() {
        List<Map<String, Object>> villes = repository.findVilles();
        List<Map<String, Object>> regions = new ArrayList<>();
        for (Map<String, Object> region : repository.findRegions()) {
            long regionId = number(region, "id");
            Map<String, Object> enriched = new LinkedHashMap<>(region);
            enriched.put(
                "villes",
                villes.stream().filter(ville -> number(ville, "regionId") == regionId).toList()
            );
            regions.add(enriched);
        }

        Map<String, Object> reference = new LinkedHashMap<>();
        reference.put("regions", regions);
        reference.put("cacaoTypes", repository.findCacaoTypes());
        return reference;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> catalogue(MarketRepository.LotFilter filter) {
        return repository.findCatalogue(filter);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myLots(long vendeurId) {
        return repository.findLotsForVendeur(vendeurId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> lotDetail(long lotId, long utilisateurId, String role) {
        Map<String, Object> lot = requireLot(lotId);
        if (!BUYER_VISIBLE_STATUSES.contains(lotStatus(lot))
            && !ADMINISTRATEUR_ROLE.equals(role)
            && number(lot, "vendeurId") != utilisateurId) {
            throw AuthException.forbidden("MARKET_LOT_NOT_VISIBLE", "This lot is not published yet.");
        }

        Map<String, Object> detail = new LinkedHashMap<>(lot);
        detail.put("medias", repository.findLotMedias(lotId));
        return detail;
    }

    @Transactional
    public long createLot(LotRequest request, long vendeurId) {
        requireVendeurAccount(vendeurId);
        validateReferences(request);
        Instant now = clock.instant();
        boolean publish = Boolean.TRUE.equals(request.publier());

        long lotId = repository.insertLot(
            vendeurId,
            request.typeCacaoId(),
            request.titre().trim(),
            optionalText(request.description()),
            request.quantiteKg(),
            request.prixKg(),
            currency(request.devise()),
            request.regionId(),
            request.villeId(),
            optionalText(request.localisation()),
            request.latitude(),
            request.longitude(),
            request.dateRecolte(),
            request.dateDisponibilite(),
            (publish ? LotStatus.PUBLIE : LotStatus.BROUILLON).databaseValue(),
            publish ? now : null,
            now
        );
        savePhoto(lotId, request.photoUrl());
        LOGGER.info(
            "event=market.lot.created vendeurId={} lotId={} statut={}",
            vendeurId,
            lotId,
            publish ? LotStatus.PUBLIE.databaseValue() : LotStatus.BROUILLON.databaseValue()
        );
        return lotId;
    }

    @Transactional
    public void updateLot(long lotId, LotRequest request, long vendeurId) {
        Map<String, Object> lot = requireOwnedLot(lotId, vendeurId);
        if (lotStatus(lot) == LotStatus.VENDU) {
            throw AuthException.conflict(
                "MARKET_LOT_STATUS_TRANSITION",
                "A sold lot can no longer be modified."
            );
        }
        validateReferences(request);

        boolean updated = repository.updateLot(
            lotId,
            vendeurId,
            request.typeCacaoId(),
            request.titre().trim(),
            optionalText(request.description()),
            request.quantiteKg(),
            request.prixKg(),
            currency(request.devise()),
            request.regionId(),
            request.villeId(),
            optionalText(request.localisation()),
            request.latitude(),
            request.longitude(),
            request.dateRecolte(),
            request.dateDisponibilite(),
            clock.instant()
        );
        if (!updated) {
            throw notFound("lot", "MARKET_LOT_NOT_FOUND");
        }

        if (request.photoUrl() != null) {
            repository.deleteLotMedias(lotId);
            savePhoto(lotId, request.photoUrl());
        }
    }

    @Transactional
    public void changeLotStatus(long lotId, String requestedStatus, long vendeurId) {
        Map<String, Object> lot = requireOwnedLot(lotId, vendeurId);
        LotStatus target = MarketStatuses.lotStatus(requestedStatus)
            .orElseThrow(() -> AuthException.badRequest(
                "MARKET_LOT_STATUS_INVALID",
                "The requested lot status is not supported."
            ));
        LotStatus current = lotStatus(lot);
        if (!isAllowedTransition(current, target)) {
            throw AuthException.conflict(
                "MARKET_LOT_STATUS_TRANSITION",
                "A lot cannot move from " + current.databaseValue() + " to " + target.databaseValue() + "."
            );
        }

        Instant publicationAt = target == LotStatus.PUBLIE && lot.get("datePublication") == null
            ? clock.instant()
            : null;
        if (!repository.updateLotStatus(lotId, vendeurId, target.databaseValue(), publicationAt)) {
            throw notFound("lot", "MARKET_LOT_NOT_FOUND");
        }
        LOGGER.info(
            "event=market.lot.status-changed vendeurId={} lotId={} from={} to={}",
            vendeurId,
            lotId,
            current.databaseValue(),
            target.databaseValue()
        );
    }

    /** BROUILLON → PUBLIE, PUBLIE/RESERVE → VENDU or ARCHIVE, ARCHIVE → PUBLIE. */
    private boolean isAllowedTransition(LotStatus current, LotStatus target) {
        return switch (current) {
            case BROUILLON -> target == LotStatus.PUBLIE || target == LotStatus.ARCHIVE;
            case PUBLIE -> target == LotStatus.VENDU || target == LotStatus.ARCHIVE;
            case RESERVE -> target == LotStatus.VENDU || target == LotStatus.ARCHIVE;
            case ARCHIVE -> target == LotStatus.PUBLIE;
            case VENDU -> false;
        };
    }

    @Transactional
    public Map<String, Object> openConversation(long lotId, String message, long clientId) {
        Map<String, Object> lot = requireLot(lotId);
        if (!BUYER_VISIBLE_STATUSES.contains(lotStatus(lot))) {
            throw AuthException.conflict(
                "MARKET_LOT_NOT_VISIBLE",
                "This lot is not open for buyer conversations."
            );
        }
        if (number(lot, "vendeurId") == clientId) {
            throw AuthException.conflict(
                "MARKET_CONVERSATION_OWN_LOT",
                "You cannot open a buyer conversation about your own lot."
            );
        }

        String firstMessage = optionalText(message);
        long conversationId = repository.findConversationIdByLotAndClient(lotId, clientId)
            .map(existing -> number(existing, "id"))
            .orElseGet(() -> {
                long created = repository.insertConversation(lotId, clientId, number(lot, "vendeurId"));
                LOGGER.info(
                    "event=market.conversation.opened clientId={} lotId={} conversationId={}",
                    clientId,
                    lotId,
                    created
                );
                return created;
            });

        if (firstMessage != null) {
            repository.insertMessage(conversationId, clientId, firstMessage, "TEXTE", clock.instant());
        }
        return conversationDetail(conversationId, clientId);
    }

    @Transactional
    public List<Map<String, Object>> conversations(long utilisateurId, String role) {
        expireOverdueNegotiations();

        if ("CLIENT".equals(role)) {
            return repository.findConversationsForClient(utilisateurId);
        }
        if ("VENDEUR".equals(role)) {
            return repository.findConversationsForVendeur(utilisateurId);
        }
        return List.of();
    }

    @Transactional
    public Map<String, Object> conversationDetail(long conversationId, long utilisateurId) {
        Map<String, Object> conversation = requireParticipantConversation(conversationId, utilisateurId);
        expireOverdueNegotiations();
        repository.markMessagesRead(conversationId, utilisateurId, clock.instant());

        Map<String, Object> detail = new LinkedHashMap<>(conversation);
        detail.put("messages", repository.findMessages(conversationId));
        detail.put("negociations", repository.findNegociations(conversationId));
        detail.put("rendezVous", repository.findRendezVousList(conversationId));
        return detail;
    }

    @Transactional
    public void postMessage(long conversationId, String contenu, long utilisateurId) {
        Map<String, Object> conversation = requireParticipantConversation(conversationId, utilisateurId);
        requireThreadOpen(conversation);
        String message = validMessage(contenu);
        repository.insertMessage(conversationId, utilisateurId, message, "TEXTE", clock.instant());
    }

    @Transactional
    public long proposeNegociation(long conversationId, NegotiationRequest request, long utilisateurId) {
        Map<String, Object> conversation = requireParticipantConversation(conversationId, utilisateurId);
        if ("ACCORD".equals(conversation.get("statut"))) {
            throw AuthException.conflict(
                "MARKET_CONVERSATION_AGREED",
                "An agreement is already recorded for this conversation."
            );
        }
        requireThreadOpen(conversation);
        if (repository.hasOpenNegociation(conversationId)) {
            throw AuthException.conflict(
                "MARKET_NEGOTIATION_OPEN",
                "A proposal is already waiting for an answer in this conversation."
            );
        }

        BigDecimal available = decimal(conversation, "lotQuantiteDisponibleKg");
        if (request.quantiteKg().compareTo(available) > 0) {
            throw AuthException.conflict(
                "MARKET_LOT_QUANTITY_INSUFFICIENT",
                "The requested volume is larger than the quantity still available on this lot."
            );
        }

        Instant now = clock.instant();
        long negociationId = repository.insertNegociation(
            conversationId,
            utilisateurId,
            request.prixKg(),
            request.quantiteKg(),
            optionalText(request.message()),
            now.plus(NEGOTIATION_TTL),
            now
        );
        repository.updateConversationStatus(conversationId, "EN_NEGOCIATION");
        LOGGER.info(
            "event=market.negotiation.proposed utilisateurId={} conversationId={} negociationId={}",
            utilisateurId,
            conversationId,
            negociationId
        );
        return negociationId;
    }

    @Transactional
    public void decideNegociation(long negociationId, boolean accepted, long utilisateurId) {
        Map<String, Object> negociation = requireNegociation(negociationId);
        long conversationId = number(negociation, "conversationId");
        Map<String, Object> conversation = requireParticipantConversation(conversationId, utilisateurId);
        if (number(negociation, "proposeurId") == utilisateurId) {
            throw AuthException.conflict(
                "MARKET_NEGOTIATION_SELF_DECISION",
                "The proposal must be answered by the other participant."
            );
        }
        requireOpenNegociation(negociation);

        Instant now = clock.instant();
        if (accepted) {
            Map<String, Object> lot = requireLot(number(conversation, "lotId"));
            if (!repository.updateNegociationStatus(negociationId, NegotiationStatus.ACCEPTEE.databaseValue(), now)) {
                throw notFound("negotiation", "MARKET_NEGOTIATION_NOT_OPEN");
            }
            if (lotStatus(lot) == LotStatus.PUBLIE) {
                repository.reserveLot(number(lot, "id"));
            }
            if (!repository.reduceAvailableQuantity(
                number(lot, "id"),
                decimal(negociation, "quantiteKg")
            )) {
                throw AuthException.conflict(
                    "MARKET_LOT_QUANTITY_INSUFFICIENT",
                    "The requested volume is no longer available on this lot."
                );
            }
            repository.updateConversationStatus(conversationId, "ACCORD");
            LOGGER.info(
                "event=market.negotiation.accepted utilisateurId={} conversationId={} negociationId={}",
                utilisateurId,
                conversationId,
                negociationId
            );
            return;
        }

        if (!repository.updateNegociationStatus(negociationId, NegotiationStatus.REFUSEE.databaseValue(), now)) {
            throw notFound("negotiation", "MARKET_NEGOTIATION_NOT_OPEN");
        }
        repository.updateConversationStatus(conversationId, "OUVERTE");
        LOGGER.info(
            "event=market.negotiation.refused utilisateurId={} conversationId={} negociationId={}",
            utilisateurId,
            conversationId,
            negociationId
        );
    }

    @Transactional
    public void cancelNegociation(long negociationId, long utilisateurId) {
        Map<String, Object> negociation = requireNegociation(negociationId);
        long conversationId = number(negociation, "conversationId");
        requireParticipantConversation(conversationId, utilisateurId);
        if (number(negociation, "proposeurId") != utilisateurId) {
            throw AuthException.forbidden(
                "MARKET_NEGOTIATION_NOT_PROPOSER",
                "Only the author of the proposal can cancel it."
            );
        }
        requireOpenNegociation(negociation);

        if (!repository.updateNegociationStatus(
            negociationId,
            NegotiationStatus.ANNULEE.databaseValue(),
            clock.instant()
        )) {
            throw notFound("negotiation", "MARKET_NEGOTIATION_NOT_OPEN");
        }
        repository.updateConversationStatus(conversationId, "OUVERTE");
    }

    @Transactional
    public long proposeRendezVous(long conversationId, AppointmentRequest request, long utilisateurId) {
        Map<String, Object> conversation = requireParticipantConversation(conversationId, utilisateurId);
        requireThreadOpen(conversation);
        if (repository.hasOpenRendezVous(conversationId)) {
            throw AuthException.conflict(
                "MARKET_APPOINTMENT_OPEN",
                "A visit is already waiting for an answer in this conversation."
            );
        }
        if (!request.dateProposee().isAfter(clock.instant())) {
            throw AuthException.badRequest(
                "MARKET_APPOINTMENT_DATE_INVALID",
                "A visit must be proposed for a future date."
            );
        }

        long rendezVousId = repository.insertRendezVous(
            conversationId,
            utilisateurId,
            request.dateProposee(),
            optionalText(request.lieu()),
            optionalText(request.note()),
            clock.instant()
        );
        LOGGER.info(
            "event=market.appointment.proposed utilisateurId={} conversationId={} rendezVousId={}",
            utilisateurId,
            conversationId,
            rendezVousId
        );
        return rendezVousId;
    }

    @Transactional
    public void decideRendezVous(long rendezVousId, boolean accepted, long utilisateurId) {
        Map<String, Object> rendezVous = requireRendezVous(rendezVousId);
        long conversationId = number(rendezVous, "conversationId");
        requireParticipantConversation(conversationId, utilisateurId);
        if (number(rendezVous, "proposeurId") == utilisateurId) {
            throw AuthException.conflict(
                "MARKET_APPOINTMENT_SELF_DECISION",
                "The visit must be answered by the other participant."
            );
        }
        requireOpenRendezVous(rendezVous);

        AppointmentStatus status = accepted ? AppointmentStatus.ACCEPTE : AppointmentStatus.REFUSE;
        if (!repository.updateRendezVousStatus(rendezVousId, status.databaseValue(), clock.instant())) {
            throw notFound("rendez-vous", "MARKET_APPOINTMENT_NOT_OPEN");
        }
    }

    @Transactional
    public void cancelRendezVous(long rendezVousId, long utilisateurId) {
        Map<String, Object> rendezVous = requireRendezVous(rendezVousId);
        requireParticipantConversation(number(rendezVous, "conversationId"), utilisateurId);
        if (number(rendezVous, "proposeurId") != utilisateurId) {
            throw AuthException.forbidden(
                "MARKET_APPOINTMENT_NOT_PROPOSER",
                "Only the author of the visit proposal can cancel it."
            );
        }
        requireOpenRendezVous(rendezVous);
        if (!repository.updateRendezVousStatus(
            rendezVousId,
            AppointmentStatus.ANNULE.databaseValue(),
            clock.instant()
        )) {
            throw notFound("rendez-vous", "MARKET_APPOINTMENT_NOT_OPEN");
        }
    }

    // ------------------------------------------------------------------ internals

    /**
     * Lazily applies the negotiation expiry rule: an overdue proposal becomes EXPIREE and the
     * thread returns to OUVERTE. Keeping it lazy avoids a background scheduler for a rule that only
     * matters when a participant looks at the thread.
     */
    private void expireOverdueNegotiations() {
        List<Long> affected = repository.expireOverdueNegociations(clock.instant());
        for (Long conversationId : affected) {
            repository.updateConversationStatus(conversationId, "OUVERTE");
        }
    }

    private void requireThreadOpen(Map<String, Object> conversation) {
        if ("CLOTUREE".equals(conversation.get("statut"))) {
            throw AuthException.conflict(
                "MARKET_CONVERSATION_CLOSED",
                "This conversation is closed and can no longer receive messages."
            );
        }
    }

    private void requireOpenNegociation(Map<String, Object> negociation) {
        if (!NegotiationStatus.PROPOSEE.databaseValue().equals(negociation.get("statut"))) {
            throw AuthException.conflict(
                "MARKET_NEGOTIATION_NOT_OPEN",
                "This proposal is no longer waiting for an answer."
            );
        }
        Object expiresAt = negociation.get("expiresAt");
        if (expiresAt instanceof Instant expiry && !clock.instant().isBefore(expiry)) {
            repository.updateNegociationStatus(
                number(negociation, "id"),
                NegotiationStatus.EXPIREE.databaseValue(),
                clock.instant()
            );
            throw AuthException.conflict(
                "MARKET_NEGOTIATION_EXPIRED",
                "This proposal expired before it was answered."
            );
        }
    }

    private void requireOpenRendezVous(Map<String, Object> rendezVous) {
        if (!AppointmentStatus.PROPOSE.databaseValue().equals(rendezVous.get("statut"))) {
            throw AuthException.conflict(
                "MARKET_APPOINTMENT_NOT_OPEN",
                "This visit proposal is no longer waiting for an answer."
            );
        }
    }

    private void requireVendeurAccount(long utilisateurId) {
        String role = repository.findUtilisateurRole(utilisateurId).orElse(null);
        if (!"VENDEUR".equals(role)) {
            throw AuthException.forbidden(
                "MARKET_VENDEUR_REQUIRED",
                "Only a seller account can manage a catalogue."
            );
        }
    }

    private void validateReferences(LotRequest request) {
        if (!repository.regionExists(request.regionId())) {
            throw AuthException.badRequest("MARKET_REFERENCE_INVALID", "The selected region does not exist.");
        }
        if (request.villeId() != null && !repository.villeBelongsToRegion(request.villeId(), request.regionId())) {
            throw AuthException.badRequest(
                "MARKET_REFERENCE_INVALID",
                "The selected city does not belong to the selected region."
            );
        }
        if (!repository.cacaoTypeExists(request.typeCacaoId())) {
            throw AuthException.badRequest("MARKET_REFERENCE_INVALID", "The selected cocoa type does not exist.");
        }
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw AuthException.badRequest(
                "MARKET_REFERENCE_INVALID",
                "Latitude and longitude must be provided together."
            );
        }
    }

    private void savePhoto(long lotId, String photoUrl) {
        String url = optionalText(photoUrl);
        if (url != null) {
            repository.insertLotMedia(lotId, url, null, 0);
        }
    }

    private Map<String, Object> requireLot(long lotId) {
        return repository.findLot(lotId).orElseThrow(() -> notFound("lot", "MARKET_LOT_NOT_FOUND"));
    }

    private Map<String, Object> requireOwnedLot(long lotId, long vendeurId) {
        Map<String, Object> lot = requireLot(lotId);
        if (number(lot, "vendeurId") != vendeurId) {
            throw AuthException.forbidden("MARKET_LOT_NOT_OWNED", "This lot belongs to another seller.");
        }
        return lot;
    }

    private Map<String, Object> requireParticipantConversation(long conversationId, long utilisateurId) {
        Map<String, Object> conversation = repository.findConversation(conversationId, utilisateurId)
            .orElseThrow(() -> notFound("conversation", "MARKET_CONVERSATION_NOT_FOUND"));
        if (number(conversation, "clientId") != utilisateurId && number(conversation, "vendeurId") != utilisateurId) {
            throw AuthException.forbidden(
                "MARKET_CONVERSATION_FORBIDDEN",
                "This conversation belongs to other participants."
            );
        }
        return conversation;
    }

    private Map<String, Object> requireNegociation(long negociationId) {
        return repository.findNegociation(negociationId)
            .orElseThrow(() -> notFound("negotiation", "MARKET_NEGOTIATION_NOT_FOUND"));
    }

    private Map<String, Object> requireRendezVous(long rendezVousId) {
        return repository.findRendezVous(rendezVousId)
            .orElseThrow(() -> notFound("rendez-vous", "MARKET_APPOINTMENT_NOT_FOUND"));
    }

    private String validMessage(String contenu) {
        String message = optionalText(contenu);
        if (message == null || message.length() > MAX_MESSAGE_LENGTH) {
            throw AuthException.badRequest(
                "MARKET_MESSAGE_INVALID",
                "A message must contain between 1 and " + MAX_MESSAGE_LENGTH + " characters."
            );
        }
        return message;
    }

    private String currency(String value) {
        String devise = value == null || value.isBlank()
            ? DEFAULT_CURRENCY
            : value.trim().toUpperCase(Locale.ROOT);
        if (devise.length() != 3) {
            throw AuthException.badRequest("MARKET_REFERENCE_INVALID", "The currency code must have three letters.");
        }
        return devise;
    }

    private String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private LotStatus lotStatus(Map<String, Object> lot) {
        return MarketStatuses.lotStatus(String.valueOf(lot.get("statut")))
            .orElseThrow(() -> new IllegalStateException("Unexpected lot status in the database."));
    }

    private long number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Missing numeric value '" + key + "' in the market repository result.");
    }

    private BigDecimal decimal(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        throw new IllegalStateException("Missing decimal value '" + key + "' in the market repository result.");
    }

    private AuthException notFound(String resource, String code) {
        return AuthException.badRequest(code, "The requested " + resource + " was not found.");
    }
}
