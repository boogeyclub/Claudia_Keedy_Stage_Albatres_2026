package cm.odigital.serviceconnectmarket.market.domain;

import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;

/**
 * Why the market sends an email.
 *
 * <p>The rule is deliberately narrow: a participant is emailed when something <em>starts</em> — a
 * thread is opened on an article, a negotiation is proposed, a visit is proposed, an exact position
 * is requested, or a GPS pin of a visit needs a decision. Ordinary messages inside an existing
 * thread never produce one email per message, they rely on the real-time stream.
 */
public enum MarketEventType {

    /** First message of a buyer about one article: it opens the thread. */
    MESSAGE_INITIAL(
        "Nouveau message sur une annonce CacaoMarketCM",
        "New message about a CacaoMarketCM listing"
    ),

    /** A price/volume proposal was submitted. */
    NEGOCIATION_PROPOSEE(
        "Nouvelle négociation à étudier",
        "A negotiation is waiting for your answer"
    ),

    /** A visit was proposed. */
    RENDEZ_VOUS_PROPOSE(
        "Nouvelle proposition de visite",
        "A visit has been proposed"
    ),

    /** The buyer asked for the exact position of the lot. */
    POSITION_DEMANDEE(
        "Demande de partage de position",
        "A buyer is asking for the exact position"
    ),

    /** The position was shared, or a visit pin is waiting for a validation. */
    POSITION_A_VALIDER(
        "Un point GPS attend votre validation",
        "A GPS pin is waiting for your approval"
    ),

    /** A participant refused the pin of a visit: a new one is needed. */
    POINT_REFUSE(
        "Le point GPS de la visite a été refusé",
        "The GPS pin of the visit was refused"
    ),

    /** Both participants approved the pin: the visit is now confirmed. */
    VISITE_CONFIRMEE(
        "Votre visite est confirmée",
        "Your visit is confirmed"
    );

    private final String frenchSubject;
    private final String englishSubject;

    MarketEventType(String frenchSubject, String englishSubject) {
        this.frenchSubject = frenchSubject;
        this.englishSubject = englishSubject;
    }

    public String subject(RegistrationLanguage language) {
        return language == RegistrationLanguage.FR ? frenchSubject : englishSubject;
    }
}
