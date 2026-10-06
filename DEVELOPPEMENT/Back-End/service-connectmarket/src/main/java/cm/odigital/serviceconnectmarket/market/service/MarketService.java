package cm.odigital.serviceconnectmarket.market.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.market.api.dto.AppointmentRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.GeoPointRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.LotRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.NegotiationRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.PositionDecisionRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.PositionRequest;
import cm.odigital.serviceconnectmarket.market.domain.AppointmentStatus;
import cm.odigital.serviceconnectmarket.market.domain.LotStatus;
import cm.odigital.serviceconnectmarket.market.domain.MarketEventType;
import cm.odigital.serviceconnectmarket.market.domain.MarketStatuses;
import cm.odigital.serviceconnectmarket.market.domain.NegotiationStatus;
import cm.odigital.serviceconnectmarket.market.messaging.MarketMessagingService;
import cm.odigital.serviceconnectmarket.market.messaging.MarketNotification;
import cm.odigital.serviceconnectmarket.market.persistence.MarketRepository;
import cm.odigital.serviceconnectmarket.market.realtime.MarketRealtimeEvent;
import cm.odigital.serviceconnectmarket.market.realtime.MarketRealtimeHub;

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
    private static final String CLIENT_ROLE = "CLIENT";
    private static final String SELLER_MESSAGES_PATH = "/dashboard/vendeur/messages";
    private static final String BUYER_MESSAGES_PATH = "/dashboard/client/messages";
    private static final int PREVIEW_LENGTH = 140;
    private static final String DEFAULT_CURRENCY = "XAF";
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final Duration NEGOTIATION_TTL = Duration.ofHours(72L);
    private static final Set<LotStatus> BUYER_VISIBLE_STATUSES = Set.of(LotStatus.PUBLIE, LotStatus.RESERVE);

    private final MarketRepository repository;
    private final Clock clock;
    private final MarketMessagingService messagingService;
    private final MarketRealtimeHub realtimeHub;
    private final String frontendBaseUrl;

    public MarketService(
        MarketRepository repository,
        Clock authenticationClock,
        MarketMessagingService messagingService,
        MarketRealtimeHub realtimeHub,
        @Value("${app.market.frontend-base-url:http://localhost:4200/CacaoMarketCM}") String frontendBaseUrl
    ) {
        this.repository = repository;
        this.clock = authenticationClock;
        this.messagingService = messagingService;
        this.realtimeHub = realtimeHub;
        this.frontendBaseUrl = frontendBaseUrl;
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

    /**
     * Negotiations and visits of the caller, whichever side of the conversation it is on.
     *
     * Not read-only: reading this page also applies the negotiation expiry rule, exactly like
     * opening a thread does.
     */
    @Transactional
    public Map<String, Object> deals(long utilisateurId) {
        expireOverdueNegotiations();

        Map<String, Object> deals = new LinkedHashMap<>();
        deals.put("negociations", repository.findNegociationsFor(utilisateurId));
        deals.put("rendezVous", repository.findRendezVousFor(utilisateurId));
        return deals;
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
        Optional<Map<String, Object>> existingThread = repository.findConversationIdByLotAndClient(lotId, clientId);
        boolean openingThread = existingThread.isEmpty();
        long conversationId = existingThread
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
            // The very first message of a thread is the only message that also sends an email: it
            // tells the seller someone is interested in the article. The following ones ride on the
            // real-time stream.
            if (openingThread) {
                notifyOpeningMessage(conversationId, clientId, firstMessage);
            } else {
                publishThreadChange(conversationId, "MESSAGE", clientId, firstMessage);
            }
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
        // Only the participants reach this method, so the exact position of the lot can travel here.
        detail.put("position", repository.findActivePositionPartage(conversationId).orElse(null));
        return detail;
    }

    @Transactional
    public void postMessage(long conversationId, String contenu, long utilisateurId) {
        Map<String, Object> conversation = requireParticipantConversation(conversationId, utilisateurId);
        requireThreadOpen(conversation);
        String message = validMessage(contenu);
        repository.insertMessage(conversationId, utilisateurId, message, "TEXTE", clock.instant());
        publishThreadChange(conversationId, "MESSAGE", utilisateurId, message);
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
        notifyActionStarted(
            conversationId,
            MarketEventType.NEGOCIATION_PROPOSEE,
            utilisateurId,
            "Proposition : "
                + request.prixKg().stripTrailingZeros().toPlainString()
                + " "
                + String.valueOf(conversation.get("lotDevise"))
                + "/kg pour "
                + request.quantiteKg().stripTrailingZeros().toPlainString()
                + " kg.",
            "NEGOCIATION"
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

        if (!request.hasPoint()) {
            throw AuthException.badRequest(
                "MARKET_APPOINTMENT_POINT_REQUIRED",
                "Pin the meeting point on the map before proposing the visit."
            );
        }

        long rendezVousId = repository.insertRendezVous(
            conversationId,
            utilisateurId,
            request.dateProposee(),
            optionalText(request.lieu()),
            optionalText(request.lieuLibelle()),
            request.latitude(),
            request.longitude(),
            optionalText(request.note()),
            clock.instant()
        );
        LOGGER.info(
            "event=market.appointment.proposed utilisateurId={} conversationId={} rendezVousId={}",
            utilisateurId,
            conversationId,
            rendezVousId
        );
        notifyActionStarted(
            conversationId,
            MarketEventType.RENDEZ_VOUS_PROPOSE,
            utilisateurId,
            "Visite proposée le " + instantLabel(request.dateProposee()) + pointLabel(request.lieuLibelle()) + ".",
            "RENDEZ_VOUS"
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

        MarketRepository.ConversationRouting routing = routing(conversationId);
        if (accepted) {
            // Accepting the slot is not enough: the pinned point still has to be approved by the
            // invited participant before the visit is acted upon.
            publish(routing, "RENDEZ_VOUS", utilisateurId, "Créneau accepté : le point GPS reste à valider.");
            if (rendezVous.get("latitude") != null) {
                sendToBoth(
                    routing,
                    MarketEventType.POSITION_A_VALIDER,
                    "Le créneau a été accepté. Le point GPS de la visite attend votre validation.",
                    conversationUrl(routing, routing.clientId)
                );
            }
            return;
        }

        publish(routing, "RENDEZ_VOUS", utilisateurId, "Visite refusée.");
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
        if (!repository.cancelRendezVous(rendezVousId, clock.instant())) {
            throw notFound("rendez-vous", "MARKET_APPOINTMENT_NOT_OPEN");
        }
        publish(
            routing(number(rendezVous, "conversationId")),
            "RENDEZ_VOUS",
            utilisateurId,
            "Visite annulée."
        );
    }

    // ------------------------------------------------------------------ position partagée

    /**
     * The buyer asks the seller to share the exact spot of the lot.
     *
     * <p>The request is always made by the buyer about their own thread: the seller answers it, which
     * is why the roles are checked here and not only in the browser.
     */
    @Transactional
    public Map<String, Object> requestPositionShare(long conversationId, PositionRequest request, long utilisateurId) {
        Map<String, Object> conversation = requireParticipantConversation(conversationId, utilisateurId);
        requireThreadOpen(conversation);
        if (number(conversation, "clientId") != utilisateurId) {
            throw AuthException.forbidden(
                "MARKET_POSITION_BUYER_ONLY",
                "Only the buyer can ask for the exact position of the article."
            );
        }
        if (repository.findActivePositionPartage(conversationId).isPresent()) {
            throw AuthException.conflict(
                "MARKET_POSITION_REQUEST_OPEN",
                "A position request is already active in this conversation."
            );
        }

        long destinataireId = number(conversation, "vendeurId");
        long partageId = repository.insertPositionDemande(
            conversationId,
            utilisateurId,
            destinataireId,
            optionalText(request == null ? null : request.message()),
            clock.instant()
        );
        LOGGER.info(
            "event=market.position.requested utilisateurId={} conversationId={} partageId={}",
            utilisateurId,
            conversationId,
            partageId
        );
        notifyActionStarted(
            conversationId,
            MarketEventType.POSITION_DEMANDEE,
            utilisateurId,
            "L'acheteur demande la position exacte du lot : vous pouvez la partager ou refuser.",
            "POSITION"
        );
        return conversationDetail(conversationId, utilisateurId);
    }

    /** The seller answers a position request: share the exact spot, or refuse it. */
    @Transactional
    public Map<String, Object> decidePositionShare(long partageId, PositionDecisionRequest request, long utilisateurId) {
        Map<String, Object> partage = requirePositionPartage(partageId);
        long conversationId = number(partage, "conversationId");
        Map<String, Object> conversation = requireParticipantConversation(conversationId, utilisateurId);
        if (number(partage, "destinataireId") != utilisateurId) {
            throw AuthException.forbidden(
                "MARKET_POSITION_NOT_RECIPIENT",
                "Only the participant who received the request can answer it."
            );
        }
        if (!"DEMANDE".equals(partage.get("statut"))) {
            throw AuthException.conflict(
                "MARKET_POSITION_NOT_PENDING",
                "This position request has already been answered."
            );
        }

        Instant now = clock.instant();
        if (request.accepts()) {
            if (request.latitude() == null || request.longitude() == null) {
                throw AuthException.badRequest(
                    "MARKET_POSITION_POINT_REQUIRED",
                    "Sharing a position requires the GPS point."
                );
            }
            if (!repository.updatePositionPartageDecision(
                partageId,
                "ACCEPTEE",
                request.latitude(),
                request.longitude(),
                optionalText(request.libelle()),
                now
            )) {
                throw notFound("position", "MARKET_POSITION_NOT_PENDING");
            }
        } else if (!repository.updatePositionPartageDecision(partageId, "REFUSEE", null, null, null, now)) {
            throw notFound("position", "MARKET_POSITION_NOT_PENDING");
        }

        LOGGER.info(
            "event=market.position.answered utilisateurId={} conversationId={} partageId={} statut={}",
            utilisateurId,
            conversationId,
            partageId,
            request.accepts() ? "ACCEPTEE" : "REFUSEE"
        );
        publish(
            routing(conversationId),
            "POSITION",
            utilisateurId,
            request.accepts() ? "Position exacte partagée." : "Demande de position refusée."
        );
        return conversationDetail(conversationId, utilisateurId);
    }

    /** The seller withdraws a shared position; the pin disappears from the thread. */
    @Transactional
    public Map<String, Object> revokePositionShare(long partageId, long utilisateurId) {
        Map<String, Object> partage = requirePositionPartage(partageId);
        long conversationId = number(partage, "conversationId");
        requireParticipantConversation(conversationId, utilisateurId);
        if (number(partage, "destinataireId") != utilisateurId) {
            throw AuthException.forbidden(
                "MARKET_POSITION_NOT_RECIPIENT",
                "Only the participant who shared the position can withdraw it."
            );
        }
        if (!repository.revokePositionPartage(partageId, clock.instant())) {
            throw AuthException.conflict(
                "MARKET_POSITION_NOT_SHARED",
                "This position is not currently shared."
            );
        }
        LOGGER.info(
            "event=market.position.revoked utilisateurId={} conversationId={} partageId={}",
            utilisateurId,
            conversationId,
            partageId
        );
        publish(routing(conversationId), "POSITION", utilisateurId, "Partage de position retiré.");
        return conversationDetail(conversationId, utilisateurId);
    }

    // ------------------------------------------------------------------ point GPS de la visite

    /** The author of a visit moves its GPS pin; the invited participant must approve it again. */
    @Transactional
    public Map<String, Object> updateRendezVousPoint(long rendezVousId, GeoPointRequest request, long utilisateurId) {
        Map<String, Object> rendezVous = requireRendezVous(rendezVousId);
        long conversationId = number(rendezVous, "conversationId");
        requireParticipantConversation(conversationId, utilisateurId);
        if (number(rendezVous, "proposeurId") != utilisateurId) {
            throw AuthException.forbidden(
                "MARKET_APPOINTMENT_NOT_PROPOSER",
                "Only the author of the visit can move its meeting point."
            );
        }

        Instant now = clock.instant();
        if (!repository.updateRendezVousPoint(
            rendezVousId,
            request.latitude(),
            request.longitude(),
            optionalText(request.libelle()),
            now
        )) {
            throw AuthException.conflict(
                "MARKET_APPOINTMENT_POINT_LOCKED",
                "The point of this visit can no longer be changed."
            );
        }
        LOGGER.info(
            "event=market.appointment.point.moved utilisateurId={} rendezVousId={} conversationId={}",
            utilisateurId,
            rendezVousId,
            conversationId
        );

        MarketRepository.ConversationRouting routing = routing(conversationId);
        publish(routing, "RENDEZ_VOUS", utilisateurId, "Nouveau point GPS proposé pour la visite.");
        sendToOthers(
            routing,
            utilisateurId,
            MarketEventType.POSITION_A_VALIDER,
            "Un nouveau point GPS est proposé pour la visite : votre validation est nécessaire.",
            conversationUrl(routing, otherParticipant(routing, utilisateurId))
        );
        return conversationDetail(conversationId, utilisateurId);
    }

    /**
     * One participant approves or refuses the pin of a visit.
     *
     * <p>Approving records the decision of that side; when both sides have approved, the visit becomes
     * CONFIRME. Refusing clears both approvals, so the pin has to be proposed again — nothing is acted
     * upon while one side disagrees with the place.
     */
    @Transactional
    public Map<String, Object> decideRendezVousPoint(long rendezVousId, boolean approved, long utilisateurId) {
        Map<String, Object> rendezVous = requireRendezVous(rendezVousId);
        long conversationId = number(rendezVous, "conversationId");
        requireParticipantConversation(conversationId, utilisateurId);
        String statut = String.valueOf(rendezVous.get("statut"));
        if (!AppointmentStatus.ACCEPTE.databaseValue().equals(statut)) {
            throw AuthException.conflict(
                "MARKET_APPOINTMENT_NOT_ACCEPTED",
                "The slot must be accepted before its meeting point is approved."
            );
        }
        if (rendezVous.get("latitude") == null) {
            throw AuthException.conflict(
                "MARKET_APPOINTMENT_POINT_MISSING",
                "This visit has no meeting point yet."
            );
        }

        Instant now = clock.instant();
        long proposeurId = number(rendezVous, "proposeurId");
        if (!approved) {
            repository.clearRendezVousPointValidations(rendezVousId);
            LOGGER.info(
                "event=market.appointment.point.refused utilisateurId={} rendezVousId={} conversationId={}",
                utilisateurId,
                rendezVousId,
                conversationId
            );
            MarketRepository.ConversationRouting routing = routing(conversationId);
            publish(routing, "RENDEZ_VOUS", utilisateurId, "Point GPS refusé : une nouvelle proposition est attendue.");
            sendToOthers(
                routing,
                utilisateurId,
                MarketEventType.POINT_REFUSE,
                "Le point GPS de la visite a été refusé : proposez un autre point pour pouvoir acter le rendez-vous.",
                conversationUrl(routing, otherParticipant(routing, utilisateurId))
            );
            return conversationDetail(conversationId, utilisateurId);
        }

        if (!repository.markRendezVousPointValidated(rendezVousId, proposeurId == utilisateurId, now)) {
            throw AuthException.conflict(
                "MARKET_APPOINTMENT_POINT_LOCKED",
                "This meeting point can no longer be approved."
            );
        }

        boolean confirmed = repository.confirmRendezVous(rendezVousId, now);
        MarketRepository.ConversationRouting routing = routing(conversationId);
        if (confirmed) {
            LOGGER.info(
                "event=market.appointment.confirmed utilisateurId={} rendezVousId={} conversationId={}",
                utilisateurId,
                rendezVousId,
                conversationId
            );
            publish(routing, "RENDEZ_VOUS", utilisateurId, "Point GPS validé par les deux parties : visite actée.");
            sendToBoth(
                routing,
                MarketEventType.VISITE_CONFIRMEE,
                "Les deux parties ont validé le point GPS : la visite est confirmée.",
                conversationUrl(routing, routing.clientId)
            );
            return conversationDetail(conversationId, utilisateurId);
        }

        publish(routing, "RENDEZ_VOUS", utilisateurId, "Point GPS validé : en attente de l'autre partie.");
        return conversationDetail(conversationId, utilisateurId);
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

    // ------------------------------------------------------------------ notifications

    /** Email + real-time signal for the opening message of a thread. */
    private void notifyOpeningMessage(long conversationId, long clientId, String message) {
        MarketRepository.ConversationRouting routing = routing(conversationId);
        long vendeurId = routing.vendeurId();

        messagingService.send(new MarketNotification(
            MarketEventType.MESSAGE_INITIAL,
            routing.participantEmail(vendeurId),
            routing.participantFirstName(vendeurId),
            routing.participantName(clientId),
            routing.lotTitre(),
            "Message : " + preview(message),
            conversationUrl(routing, vendeurId),
            clock.instant()
        ));
        publish(routing, "MESSAGE", clientId, message);
    }

    /**
     * Email + real-time signal for the beginning of an action (a negotiation, a visit, a position
     * request). The counterpart is the recipient; both sides get the live signal so their screens
     * update together.
     */
    private void notifyActionStarted(
        long conversationId,
        MarketEventType type,
        long authorId,
        String detail,
        String realtimeType
    ) {
        MarketRepository.ConversationRouting routing = routing(conversationId);
        long recipientId = otherParticipant(routing, authorId);

        messagingService.send(new MarketNotification(
            type,
            routing.participantEmail(recipientId),
            routing.participantFirstName(recipientId),
            routing.participantName(authorId),
            routing.lotTitre(),
            detail,
            conversationUrl(routing, recipientId),
            clock.instant()
        ));
        publish(routing, realtimeType, authorId, preview(detail));
    }

    /** Emails both participants, each with the link of their own workspace. */
    private void sendToBoth(
        MarketRepository.ConversationRouting routing,
        MarketEventType type,
        String detail,
        String clientUrl
    ) {
        messagingService.send(new MarketNotification(
            type,
            routing.clientEmail(),
            routing.clientPrenom(),
            routing.vendeurNom(),
            routing.lotTitre(),
            detail,
            clientUrl,
            clock.instant()
        ));
        messagingService.send(new MarketNotification(
            type,
            routing.vendeurEmail(),
            routing.vendeurPrenom(),
            routing.clientNom(),
            routing.lotTitre(),
            detail,
            conversationUrl(routing, routing.vendeurId()),
            clock.instant()
        ));
    }

    /** Emails the participant who did not act. */
    private void sendToOthers(
        MarketRepository.ConversationRouting routing,
        long authorId,
        MarketEventType type,
        String detail,
        String url
    ) {
        long recipientId = otherParticipant(routing, authorId);
        messagingService.send(new MarketNotification(
            type,
            routing.participantEmail(recipientId),
            routing.participantFirstName(recipientId),
            routing.participantName(authorId),
            routing.lotTitre(),
            detail,
            url,
            clock.instant()
        ));
    }

    /** Pushes one real-time event to both participants of a thread. */
    private void publish(
        MarketRepository.ConversationRouting routing,
        String type,
        long authorId,
        String preview
    ) {
        realtimeHub.publish(
            routing.participantIds(),
            new MarketRealtimeEvent(
                type,
                routing.conversationId(),
                authorId,
                routing.lotId(),
                routing.lotTitre(),
                routing.participantName(authorId),
                preview(preview),
                clock.instant()
            )
        );
    }

    private void publishThreadChange(long conversationId, String type, long authorId, String content) {
        publish(routing(conversationId), type, authorId, content);
    }

    private MarketRepository.ConversationRouting routing(long conversationId) {
        return repository.findConversationRouting(conversationId)
            .orElseThrow(() -> notFound("conversation", "MARKET_CONVERSATION_NOT_FOUND"));
    }

    private long otherParticipant(MarketRepository.ConversationRouting routing, long utilisateurId) {
        return utilisateurId == routing.vendeurId() ? routing.clientId() : routing.vendeurId();
    }

    /** Deep link of the messages screen of the recipient's workspace. */
    private String conversationUrl(MarketRepository.ConversationRouting routing, long utilisateurId) {
        String path = utilisateurId == routing.vendeurId() ? SELLER_MESSAGES_PATH : BUYER_MESSAGES_PATH;
        return frontendBaseUrl + path + "?conversationId=" + routing.conversationId();
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= PREVIEW_LENGTH ? normalized : normalized.substring(0, PREVIEW_LENGTH) + "…";
    }

    private String pointLabel(String lieuLibelle) {
        String label = optionalText(lieuLibelle);
        return label == null ? "" : " — " + label;
    }

    private String instantLabel(Instant value) {
        return DateTimeFormatter.ofPattern("d MMMM uuuu 'à' HH:mm 'UTC'", Locale.FRENCH)
            .withZone(ZoneOffset.UTC)
            .format(value);
    }

    private Map<String, Object> requirePositionPartage(long partageId) {
        return repository.findPositionPartage(partageId)
            .orElseThrow(() -> notFound("position", "MARKET_POSITION_NOT_FOUND"));
    }

    private AuthException notFound(String resource, String code) {
        return AuthException.badRequest(code, "The requested " + resource + " was not found.");
    }
}
