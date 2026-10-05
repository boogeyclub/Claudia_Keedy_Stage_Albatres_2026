package cm.odigital.serviceconnectmarket.market.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cm.odigital.serviceconnectmarket.auth.session.AuthenticatedSession;
import cm.odigital.serviceconnectmarket.market.api.dto.AppointmentRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.CatalogueResponse;
import cm.odigital.serviceconnectmarket.market.api.dto.ConversationDetailResponse;
import cm.odigital.serviceconnectmarket.market.api.dto.ConversationListResponse;
import cm.odigital.serviceconnectmarket.market.api.dto.ConversationRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.DecisionRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.LotDetailResponse;
import cm.odigital.serviceconnectmarket.market.api.dto.LotRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.LotStatusRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.MarketDealsResponse;
import cm.odigital.serviceconnectmarket.market.api.dto.MarketMutationResponse;
import cm.odigital.serviceconnectmarket.market.api.dto.MarketReferenceResponse;
import cm.odigital.serviceconnectmarket.market.api.dto.MessageRequest;
import cm.odigital.serviceconnectmarket.market.api.dto.NegotiationRequest;
import cm.odigital.serviceconnectmarket.market.persistence.MarketRepository;
import cm.odigital.serviceconnectmarket.market.service.MarketService;

/**
 * Catalogue, messaging, negotiation and visit endpoints.
 *
 * <p>Access model: every endpoint re-checks the servlet session. Reading the catalogue needs any
 * active account; publishing lots needs a VENDEUR account; opening a buyer thread needs a CLIENT
 * account and is refused on one's own lot. Decisions (accept/refuse) are reserved to the
 * counterpart, cancellations to the author, and both are verified again by the service.
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketController.class);

    private final MarketSessionGuard sessionGuard;
    private final MarketService marketService;

    public MarketController(MarketSessionGuard sessionGuard, MarketService marketService) {
        this.sessionGuard = sessionGuard;
        this.marketService = marketService;
    }

    @GetMapping("/reference")
    public MarketReferenceResponse reference(HttpServletRequest servletRequest) {
        sessionGuard.requireSession(servletRequest);
        Map<String, Object> reference = marketService.reference();
        return new MarketReferenceResponse(
            cast(reference.get("regions")),
            cast(reference.get("cacaoTypes"))
        );
    }

    @GetMapping("/lots")
    public CatalogueResponse catalogue(
        @RequestParam(required = false) Long regionId,
        @RequestParam(required = false) Long villeId,
        @RequestParam(required = false) Long typeCacaoId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recolteFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recolteTo,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate disponibiliteFrom,
        @RequestParam(required = false) BigDecimal prixMin,
        @RequestParam(required = false) BigDecimal prixMax,
        @RequestParam(required = false) BigDecimal quantiteMin,
        @RequestParam(required = false) String recherche,
        HttpServletRequest servletRequest
    ) {
        sessionGuard.requireSession(servletRequest);
        MarketRepository.LotFilter filter = new MarketRepository.LotFilter(
            regionId,
            villeId,
            typeCacaoId,
            recolteFrom,
            recolteTo,
            disponibiliteFrom,
            prixMin,
            prixMax,
            quantiteMin,
            recherche
        );
        return new CatalogueResponse("CATALOGUE", marketService.catalogue(filter));
    }

    @GetMapping("/lots/{lotId}")
    public LotDetailResponse lot(
        @PathVariable long lotId,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession session = sessionGuard.requireSession(servletRequest);
        Map<String, Object> detail = marketService.lotDetail(
            lotId,
            session.utilisateur().id(),
            session.utilisateur().role()
        );
        List<Map<String, Object>> medias = cast(detail.remove("medias"));
        return new LotDetailResponse(detail, medias);
    }

    @GetMapping("/vendeur/lots")
    public CatalogueResponse myLots(HttpServletRequest servletRequest) {
        AuthenticatedSession vendeur = sessionGuard.requireVendeur(servletRequest);
        return new CatalogueResponse("MES_LOTS", marketService.myLots(vendeur.utilisateur().id()));
    }

    @PostMapping("/vendeur/lots")
    public ResponseEntity<MarketMutationResponse> createLot(
        @Valid @RequestBody LotRequest request,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession vendeur = sessionGuard.requireVendeur(servletRequest);
        long lotId = marketService.createLot(request, vendeur.utilisateur().id());
        LOGGER.info("event=market.lot.requested.action=created vendeurId={} lotId={}", vendeur.utilisateur().id(), lotId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new MarketMutationResponse(lotId, null, "Lot created."));
    }

    @PutMapping("/vendeur/lots/{lotId}")
    public MarketMutationResponse updateLot(
        @PathVariable long lotId,
        @Valid @RequestBody LotRequest request,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession vendeur = sessionGuard.requireVendeur(servletRequest);
        marketService.updateLot(lotId, request, vendeur.utilisateur().id());
        return new MarketMutationResponse(lotId, null, "Lot updated.");
    }

    @PutMapping("/vendeur/lots/{lotId}/statut")
    public MarketMutationResponse changeLotStatus(
        @PathVariable long lotId,
        @Valid @RequestBody LotStatusRequest request,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession vendeur = sessionGuard.requireVendeur(servletRequest);
        marketService.changeLotStatus(lotId, request.statut(), vendeur.utilisateur().id());
        return new MarketMutationResponse(lotId, request.statut(), "Lot status updated.");
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationDetailResponse> openConversation(
        @Valid @RequestBody ConversationRequest request,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession client = sessionGuard.requireClient(servletRequest);
        Map<String, Object> conversation = marketService.openConversation(
            request.lotId(),
            request.message(),
            client.utilisateur().id()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toConversationDetail(conversation));
    }

    @GetMapping("/deals")
    public MarketDealsResponse deals(HttpServletRequest servletRequest) {
        AuthenticatedSession session = sessionGuard.requireSession(servletRequest);
        Map<String, Object> deals = marketService.deals(session.utilisateur().id());
        return new MarketDealsResponse(cast(deals.get("negociations")), cast(deals.get("rendezVous")));
    }

    @GetMapping("/conversations")
    public ConversationListResponse conversations(HttpServletRequest servletRequest) {
        AuthenticatedSession session = sessionGuard.requireSession(servletRequest);
        List<Map<String, Object>> conversations = marketService.conversations(
            session.utilisateur().id(),
            session.utilisateur().role()
        );
        return new ConversationListResponse(conversations);
    }

    @GetMapping("/conversations/{conversationId}")
    public ConversationDetailResponse conversation(
        @PathVariable long conversationId,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession session = sessionGuard.requireSession(servletRequest);
        Map<String, Object> conversation = marketService.conversationDetail(
            conversationId,
            session.utilisateur().id()
        );
        return toConversationDetail(conversation);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MarketMutationResponse> postMessage(
        @PathVariable long conversationId,
        @Valid @RequestBody MessageRequest request,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession session = sessionGuard.requireSession(servletRequest);
        marketService.postMessage(conversationId, request.contenu(), session.utilisateur().id());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new MarketMutationResponse(conversationId, null, "Message sent."));
    }

    @PostMapping("/conversations/{conversationId}/negociations")
    public ResponseEntity<MarketMutationResponse> proposeNegociation(
        @PathVariable long conversationId,
        @Valid @RequestBody NegotiationRequest request,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession session = sessionGuard.requireSession(servletRequest);
        long negociationId = marketService.proposeNegociation(
            conversationId,
            request,
            session.utilisateur().id()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new MarketMutationResponse(negociationId, "PROPOSEE", "Proposal sent."));
    }

    @PostMapping("/negociations/{negociationId}/decision")
    public MarketMutationResponse decideNegociation(
        @PathVariable long negociationId,
        @Valid @RequestBody DecisionRequest request,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession session = sessionGuard.requireSession(servletRequest);
        boolean accepted = request.accepts();
        marketService.decideNegociation(negociationId, accepted, session.utilisateur().id());
        return new MarketMutationResponse(
            negociationId,
            accepted ? "ACCEPTEE" : "REFUSEE",
            accepted ? "Proposal accepted." : "Proposal refused."
        );
    }

    @PostMapping("/negociations/{negociationId}/annulation")
    public MarketMutationResponse cancelNegociation(
        @PathVariable long negociationId,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession session = sessionGuard.requireSession(servletRequest);
        marketService.cancelNegociation(negociationId, session.utilisateur().id());
        return new MarketMutationResponse(negociationId, "ANNULEE", "Proposal cancelled.");
    }

    @PostMapping("/conversations/{conversationId}/rendez-vous")
    public ResponseEntity<MarketMutationResponse> proposeRendezVous(
        @PathVariable long conversationId,
        @Valid @RequestBody AppointmentRequest request,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession session = sessionGuard.requireSession(servletRequest);
        long rendezVousId = marketService.proposeRendezVous(
            conversationId,
            request,
            session.utilisateur().id()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new MarketMutationResponse(rendezVousId, "PROPOSE", "Visit proposed."));
    }

    @PostMapping("/rendez-vous/{rendezVousId}/decision")
    public MarketMutationResponse decideRendezVous(
        @PathVariable long rendezVousId,
        @Valid @RequestBody DecisionRequest request,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession session = sessionGuard.requireSession(servletRequest);
        boolean accepted = request.accepts();
        marketService.decideRendezVous(rendezVousId, accepted, session.utilisateur().id());
        return new MarketMutationResponse(
            rendezVousId,
            accepted ? "ACCEPTE" : "REFUSE",
            accepted ? "Visit accepted." : "Visit refused."
        );
    }

    @PostMapping("/rendez-vous/{rendezVousId}/annulation")
    public MarketMutationResponse cancelRendezVous(
        @PathVariable long rendezVousId,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession session = sessionGuard.requireSession(servletRequest);
        marketService.cancelRendezVous(rendezVousId, session.utilisateur().id());
        return new MarketMutationResponse(rendezVousId, "ANNULE", "Visit cancelled.");
    }

    private ConversationDetailResponse toConversationDetail(Map<String, Object> conversation) {
        return new ConversationDetailResponse(
            conversation,
            cast(conversation.get("messages")),
            cast(conversation.get("negociations")),
            cast(conversation.get("rendezVous"))
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> cast(Object value) {
        return value == null ? List.of() : (List<Map<String, Object>>) value;
    }
}
