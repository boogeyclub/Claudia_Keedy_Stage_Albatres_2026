# Stage 2026 — ALBATRES / CacaoMarketCM

Dépôt de stage de **KEEDY SORELLE CLAUDIA** (IAI-Cameroun, 3ᵉ année Génie Logiciel), effectué du
**01 juillet au 30 septembre 2026** chez **ALBATRES Ingénierie S.A**.

Thème : **« Conception et réalisation d'une plateforme collaborative entre le client et le vendeur de
cacao : cas de Néo Industry S.A »**.

Le livrable logiciel s'appelle **CacaoMarketCM** : une application web 3-tiers où des vendeurs
(CLIENT / VENDEUR / ADMINISTRATEUR) publient et consultent des offres de cacao, avec inscription
confirmée par e-mail, sessions navigateur suivies en base, réinitialisation de mot de passe et
espace d'administration des tables PostgreSQL du schéma `gu`.

## Organisation du dépôt

```text
.
├── ANALYSE/                     Modélisation PowerAMC/PowerDesigner (phase analyse)
│   ├── Stage 2026.sws           Espace de travail PowerDesigner
│   └── PROJET_ANALYSE_POWERAMC_STAGE/
│       ├── *.prj / *.bpj        Projet d'analyse
│       ├── Diagramme de cas d utilisation global.{moo,boo}   Diagramme de cas d'usage
│       ├── Utilisation global.jpg                             Export image du diagramme
│       └── Analyse d'impact ... 'Gérer son profil'.mai        Analyse d'impact
│
├── DEVELOPPEMENT/               Phase réalisation
│   ├── Back-End/
│   │   ├── service-connectmarket/        API Spring Boot (Java 17+, Maven, WAR)
│   │   └── database/                     Schéma PostgreSQL versionné (gu.sql + README)
│   └── Front-End/                        Application Angular 21 + Tailwind CSS 4
│
├── RAPPORT/                     Rapport de fin de stage académique (.docx)
└── Stage 2026 ALBATRES NIV 3.rar   Archive de remise (niveau 3)
```

## Pile technique

| Couche | Technologie |
| --- | --- |
| Front-end | Angular 21 (composants standalone, routes *lazy*), Tailwind CSS 4, TypeScript 5.9, Vitest |
| Back-end | Spring Boot 4.1 (Web MVC, Validation, JDBC, Mail), Java 17, packaging WAR, SpringDoc/OpenAPI |
| Base de données | PostgreSQL, schéma `gu` (19 tables : comptes et marché), scripts SQL exécutés manuellement |
| E-mail | Gmail SMTP + mot de passe d'application Google (confirmation d'inscription, reset) |
| Sécurité | BCrypt, jetons aléatoires stockés **uniquement** en SHA-256, sessions serveur persistées |

### Back-end — `DEVELOPPEMENT/Back-End/service-connectmarket`

Contexte servlet par défaut : `/cacaomarketcm`, donc l'API est exposée sous
`/cacaomarketcm/api/**`. Code organisé en paquets *api / domain / service / persistence /
messaging / session / config* autour de `auth`, plus un module `admin` (whitelist de tables), un
module `market` (catalogue, messagerie, négociations, rendez-vous) et un paquet `observability`.

| Méthode | Route | Rôle |
| --- | --- | --- |
| `POST` | `/api/auth/registration` | Inscription `CLIENT` ou `VENDEUR` (statut `EN_ATTENTE_CONFIRMATION`) |
| `GET` | `/api/auth/registration/confirm?token=…` | Activation du compte via le lien e-mail (3 h) |
| `POST` | `/api/auth/password-reset/request` | Demande de réinitialisation (réponse générique, e-mail seulement si compte `ACTIF`) |
| `POST` | `/api/auth/password-reset/confirm` | Nouveau mot de passe (lien 1 h, invalide toutes les sessions) |
| `POST` | `/api/auth/login` | Authentification, session HTTP + ligne `gu.sessions_utilisateur` |
| `GET` | `/api/auth/session` | Restaure le profil après rafraîchissement du navigateur |
| `GET` / `DELETE` | `/api/auth/sessions[/{id}]` | Liste / déconnecte un navigateur |
| `POST` | `/api/auth/logout` | Déconnexion idempotente |
| `GET` | `/api/health`, `/api/health/database` | État du service et de l'accès au schéma |
| `GET`/`POST`/`PUT`/`DELETE` | `/api/admin/tables/{table}[/{recordId}]` | Gestion admin des 10 tables de comptes du schéma `gu` (allow-list, réservée `ADMINISTRATEUR`) — le menu du dashboard en expose 6 : inscriptions, utilisateurs, profils acheteur et sessions |
| `GET` | `/api/market/reference` | Régions (avec leurs villes) et types de cacao pour les filtres |
| `GET` | `/api/market/lots` | Catalogue public filtrable : région, ville, type de cacao, dates de récolte, date de disponibilité, prix, quantité, recherche |
| `GET` | `/api/market/lots/{lotId}` | Détail d'un lot et de ses photos (un lot non publié reste réservé à son vendeur) |
| `GET`/`POST`/`PUT` | `/api/market/vendeur/lots[/{lotId}]` | Catalogue du vendeur connecté : liste, création, modification (rôle `VENDEUR`) |
| `PUT` | `/api/market/vendeur/lots/{lotId}/statut` | Cycle de vie du lot : `BROUILLON` → `PUBLIE` → `RESERVE` → `VENDU`, ou `ARCHIVE` |
| `POST`/`GET` | `/api/market/conversations[/{id}]` | Messagerie par article : ouvrir ou reprendre le fil d'un lot (rôle `CLIENT`), lister sa boîte, lire un fil |
| `GET` | `/api/market/deals` | Négociations et rendez-vous de l'utilisateur connecté, toutes conversations confondues |
| `POST` | `/api/market/conversations/{id}/messages` | Envoyer un message dans un fil dont on est participant |
| `POST` | `/api/market/conversations/{id}/negociations` | Proposer un prix et un volume (une seule proposition ouverte par fil, valable 72 h) |
| `POST` | `/api/market/negociations/{id}/decision` | `ACCEPTER` ou `REFUSER` — l'acceptation passe le lot en `RESERVE` et diminue le volume disponible |
| `POST` | `/api/market/negociations/{id}/annulation` | Annuler sa propre proposition |
| `POST` | `/api/market/conversations/{id}/rendez-vous` | Demander une visite du site (un seul rendez-vous en attente par fil) |
| `POST` | `/api/market/rendez-vous/{id}/decision` | `ACCEPTER` ou `REFUSER` un rendez-vous proposé par l'autre participant |
| `POST` | `/api/market/rendez-vous/{id}/annulation` | Annuler sa propre demande de visite |

Points de conception notables :

- un `CLIENT` possède **exactement un** profil acheteur (`client_particulier` **ou**
  `client_entreprise`), garanti par triggers et contraintes différées côté base ;
- aucun mot de passe en clair ni jeton brut n'est journalisé ou renvoyé par l'API ;
- chaque appel `/api/**` porte un en-tête `X-Request-Id` repris dans les logs
  (`logs/cacaomarket-api.log`) pour tracer un flux de bout en bout ;
- les erreurs base de données sont traduites en `503 DATA_ACCESS_UNAVAILABLE` sans fuite SQL ;
- **au démarrage, le service vérifie le schéma** : chaque table et chaque colonne attendue de
  `gu.sql` est testée par une sonde `LIMIT 0` (aucune donnée n'est lue). Si un élément manque, le
  démarrage s'arrête avec la liste exacte des tables/colonnes absentes ; `SCHEMA_VERIFICATION_FAIL_FAST=false`
  permet de démarrer malgré tout, et `/api/health/database` renvoie le même diagnostic à chaud.

### Front-end — `DEVELOPPEMENT/Front-End`

Base d'URL `/CacaoMarketCM/`. L'API n'est pas appelée en dur : `public/config.json`
(`apiBaseUrl`) est chargé **avant le bootstrap** par `RuntimeConfigurationService` ; le fichier est
copié tel quel dans le build et modifiable sans reconstruire les bundles. Il n'y a **pas de proxy
de développement Angular** : en cross-origin, il faut renseigner l'URL complète et autoriser
l'origine via `APP_CORS_ALLOWED_ORIGINS`.

| Route Angular | Écran |
| --- | --- |
| `/` | Vitrine publique (présentation de la plateforme) |
| `/login` | Connexion |
| `/registration` (+ `/register`) | Inscription (profil acheteur particulier/entreprise) |
| `/registration/confirm` | Confirmation de compte |
| `/password-reset`, `/password-reset/confirm` | Demande et choix d'un nouveau mot de passe |
| `/dashboard/admin[/tables/:table]` | Espace administrateur (vue d'ensemble + gestion des tables) |
| `/dashboard/vendeur/lots` | Catalogue du vendeur : création, modification, publication, archivage |
| `/dashboard/vendeur/messages` | Messagerie par article (messages, négociation, rendez-vous) |
| `/dashboard/vendeur/profil` | Profil vendeur : compte et résumé de l'activité catalogue |
| `/dashboard/client/catalogue` | Catalogue des lots publiés : filtres (région, ville, type, dates, prix, volume) et contact du vendeur |
| `/dashboard/client/messages` | Messagerie par article (messages, négociation, rendez-vous) |
| `/dashboard/client/deals` | Négociations et rendez-vous en cours, avec réponse directe |
| `/dashboard/account` | Paramètres du compte et sessions navigateur actives |

L'interface est **bilingue français / anglais** (`core/i18n`) et dispose d'un système de
notifications globales (`core/notifications`). Les gardes de route (`authenticatedGuard`,
`roleGuard`, `anonymousOnlyGuard`) améliorent la navigation mais l'autorisation réelle est
toujours revérifiée côté Spring.

### Base de données — `DEVELOPPEMENT/Back-End/database`

`gu.sql` crée le schéma `gu` et ses 19 tables : les dix tables de comptes
(`type_utilisateur`, `utilisateurs`, `client_particulier`, `client_entreprise`,
`sessions_utilisateur`, `registration_confirmation`, `password_reset`, `basic_rights`,
`type_utilisateur_basic_right`, `password_history`) et les neuf tables du marché
(`region`, `ville`, `type_cacao`, `lots`, `lot_medias`, `conversations`, `messages`,
`negociations`, `rendez_vous`).
Le script est **idempotent** et contient une migration de reprise avec arrêt explicite en cas de
données contradictoires. Il amorce le compte d'administration de développement `root` /
`root1234` (à changer immédiatement), le droit `APP-CONN`, les dix régions et leurs villes, les
trois types de cacao, ainsi qu'un **jeu de démonstration** : deux vendeurs (`vendeur.cacao`,
`vendeur.littoral`), deux clients (`client.yaounde`, `client.douala`), quatre lots, trois
conversations avec messages, deux négociations (une acceptée, une en attente) et une demande de
visite — tous avec le mot de passe de développement `root1234`, à changer ou à supprimer avant
tout déploiement réel.

## Démarrage local

```bash
# 1. Base de données (PostgreSQL démarré sur le port 6000)
psql -v ON_ERROR_STOP=1 -h localhost -p 6000 -U sorelle -d cacaomarketcm \
     -f DEVELOPPEMENT/Back-End/database/gu.sql

# 2. Back-end (port 8080, contexte /cacaomarketcm)
cd DEVELOPPEMENT/Back-End/service-connectmarket
cp src/main/resources/.env.example src/main/resources/.env   # puis renseigner les identifiants Gmail
./mvnw spring-boot:run
# vérifier : http://localhost:8080/cacaomarketcm/api/health

# 3. Front-end (port 4200)
cd DEVELOPPEMENT/Front-End
npm install
npm start        # http://localhost:4200/CacaoMarketCM/
```

Le front lit l'URL de l'API dans `DEVELOPPEMENT/Front-End/public/config.json`, chargé **avant le
démarrage d'Angular** et modifiable sans rebuild. La valeur versionnée pointe déjà sur le backend
local :

```json
{ "apiBaseUrl": "http://localhost:8080/cacaomarketcm/api" }
```

Il n'y a **aucun proxy Angular** : le navigateur appelle Spring directement, ce qui exige que
l'origine du front soit autorisée par CORS — c'est déjà le cas par défaut
(`APP_CORS_ALLOWED_ORIGINS=http://localhost:4200`). Si vous changez le port du front, ajoutez la
nouvelle origine exacte dans le `.env` du backend puis redémarrez-le. Une valeur *relative*
(`/cacaomarketcm/api`) ne fonctionne que lorsque front et API sont servis par le même serveur web.

Le fichier `src/main/resources/.env` (identifiants PostgreSQL et Gmail) est ignoré par Git et
exclu du WAR : ne jamais le committer, jamais y mettre le mot de passe Google normal.

## Documentation détaillée

- API, flux d'inscription/reset, Gmail SMTP, logs : `DEVELOPPEMENT/Back-End/service-connectmarket/README.md`
- Configuration Angular, CORS, dashboards : `DEVELOPPEMENT/Front-End/README.md`
- Schéma, triggers, comptes de test : `DEVELOPPEMENT/Back-End/database/README.md`
