# CacaoMarketCM authentication service

This Spring service owns registration, authentication, browser sessions, and password recovery for `CLIENT`, `VENDEUR`, and `ADMINISTRATEUR` rows in the PostgreSQL `gu` schema.

The service runs beneath the lower-case `/cacaomarketcm` servlet context by default, so its API base is `/cacaomarketcm/api`. Override the context only when a deployment requires it through `SERVER_SERVLET_CONTEXT_PATH`, and keep the Angular API base URL aligned.

## Registration and confirmation flow

1. `POST /cacaomarketcm/api/auth/registration` validates a `CLIENT` or `VENDEUR` registration.
2. The service writes an `EN_ATTENTE_CONFIRMATION` row in `gu.utilisateurs`. Every `CLIENT` registration also writes exactly one buyer profile in the same transaction: `gu.client_particulier` for `PARTICULIER`, or `gu.client_entreprise` for `ENTREPRISE`. It stores a BCrypt password hash in `gu.password_history`, and stores only the SHA-256 hash of a cryptographically random confirmation token in `gu.registration_confirmation`.
3. The messaging module sends a single-use URL to the Angular confirmation page through Google Gmail SMTP. The URL is valid for exactly **3 hours**.
4. The Angular page captures the token, calls `GET /cacaomarketcm/api/auth/registration/confirm?token=...`, shows the localized result, and redirects the user to sign in after confirmation. The backend activates the user (`ACTIF`) when the token is valid.
5. A scheduler runs every minute, and registration/authentication requests also perform cleanup. Any unconfirmed expired registration is denied and its `utilisateurs`, `password_history`, and confirmation rows are removed transactionally.

Raw confirmation tokens are never persisted or returned by the registration API.

## Password-reset flow

1. The Angular sign-in page links to `/CacaoMarketCM/password-reset`, where the user enters their account email address.
2. `POST /cacaomarketcm/api/auth/password-reset/request` sends a reset message only when that email belongs to an `ACTIF` account. Pending/unconfirmed, inactive, and unknown accounts receive the same generic accepted response and never receive a link.
3. The backend stores only a SHA-256 hash of a fresh single-use token in `gu.password_reset`. A later request replaces the older unused token. Links are valid for **one hour** by default.
4. The Gmail messaging service sends the link to the Angular reset page at `/CacaoMarketCM/password-reset/confirm?token=...`; the raw token is never logged or persisted.
5. That page posts the token and a new password to `POST /cacaomarketcm/api/auth/password-reset/confirm`. A valid completion writes a new BCrypt `password_history` row, consumes the reset token, and invalidates every active browser session for that account. The user must then sign in again.

## API

### Start registration

```http
POST /cacaomarketcm/api/auth/registration
Content-Type: application/json
```

```json
{
  "role": "VENDEUR",
  "prenom": "Amina",
  "nom": "Ngono",
  "email": "amina@example.com",
  "login": "amina-cocoa",
  "password": "a secure password",
  "confirmPassword": "a secure password",
  "acceptTerms": true,
  "language": "fr"
}
```

A successful request returns `202 Accepted` with the destination email and expiration timestamp. It does not return a token or password.

`VENDEUR` registrations must not include buyer-profile fields. A `CLIENT` registration must include `clientProfileType` as either `PARTICULIER` or `ENTREPRISE`. A private-individual buyer has no additional legal fields. An enterprise buyer must include nonblank `raisonSociale`, `niu`, and `rccm`; `niu` and `rccm` are normalized to uppercase and are each unique across all enterprise buyer profiles. For an enterprise, `prenom` and `nom` identify the legal representative or primary contact.

For example, an enterprise buyer sends:

```json
{
  "role": "CLIENT",
  "clientProfileType": "ENTREPRISE",
  "raisonSociale": "Cacao Source Cameroun SARL",
  "niu": "M012345678901A",
  "rccm": "RC/YAO/2026/B/123",
  "prenom": "Amina",
  "nom": "Ngono",
  "email": "amina@example.com",
  "login": "cacao-source",
  "password": "a secure password",
  "confirmPassword": "a secure password",
  "acceptTerms": true,
  "language": "fr"
}
```

### Confirm registration

```http
GET /cacaomarketcm/api/auth/registration/confirm?token=<token-from-email>
```

A link presented at or after its deadline is denied with `410 Gone` and `REGISTRATION_CONFIRMATION_EXPIRED` when its pending record is still present; after the scheduled purge it is treated as an invalid link. In either case, the pending account is removed and cannot be activated.

### Request a password reset

```http
POST /cacaomarketcm/api/auth/password-reset/request
Content-Type: application/json
```

```json
{
  "email": "amina@example.com",
  "language": "fr"
}
```

After email-format validation, the endpoint returns `202 Accepted` with generic wording. This deliberately does not reveal whether an account exists, is confirmed, or was eligible to receive an email.

### Complete a password reset

```http
POST /cacaomarketcm/api/auth/password-reset/confirm
Content-Type: application/json
```

```json
{
  "token": "token-from-email",
  "password": "a new secure password",
  "confirmPassword": "a new secure password"
}
```

A successful request returns `200 OK` with a `RESET` status message. Invalid, expired, already-used, or ineligible tokens are rejected; password rules and confirmation matching are also enforced by the API.

### Login

```http
POST /cacaomarketcm/api/auth/login
Content-Type: application/json
```

```json
{
  "identity": "amina-cocoa",
  "password": "a secure password",
  "rememberMe": false
}
```

Only active `CLIENT`, `VENDEUR`, and `ADMINISTRATEUR` accounts with a valid current password and the `APP-CONN` basic right can sign in. The service establishes an HTTP session and returns the authenticated profile without a password hash. It also creates one `gu.sessions_utilisateur` record for that browser; the raw `JSESSIONID` is hashed before persistence and is never logged or stored as plaintext. `rememberMe: true` extends the server-side idle-session limit from 30 minutes to 7 days. A pending account returns `403` with `REGISTRATION_PENDING_CONFIRMATION`. The tracked `gu.sql` schema seeds the development administrator login `root` with initial password `root1234`; change that bootstrap credential immediately after first use.

### Browser-session API

All endpoints below require the credentialed browser session cookie except logout, which is safely idempotent.

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/cacaomarketcm/api/auth/session` | Restores the currently authenticated profile after a browser refresh; checks both the servlet session and its active database row. |
| `GET` | `/cacaomarketcm/api/auth/sessions` | Lists the current user's active browser sessions with safe browser labels, timestamps, and a `current` flag. |
| `DELETE` | `/cacaomarketcm/api/auth/sessions/{sessionId}` | Disconnects one session owned by the current user. Disconnecting the current row immediately signs out that browser. |
| `POST` | `/cacaomarketcm/api/auth/logout` | Invalidates the active servlet session and marks its browser-session record invalid. |

Expired browser-session records are marked invalid every five minutes by default. Set `SESSION_CLEANUP_INTERVAL` to change that schedule (for example `PT1M`). Apply the latest `database/gu.sql` before starting this version of the API, because successful login now writes to `gu.sessions_utilisateur`.

### Administrator `gu` table API

The Angular administrator workspace uses the explicitly whitelisted routes below. Every route requires both a validated, active browser session and the `ADMINISTRATEUR` role; the browser-only route guard is not treated as authorization.

| Method | Route | Safe scope |
| --- | --- | --- |
| `GET` | `/cacaomarketcm/api/admin/tables/{table}` | Returns a safe, table-specific projection for one of the ten approved `gu` tables. |
| `POST` | `/cacaomarketcm/api/admin/tables/{table}` | Creates only supported user-type, user, basic-right, or type/right-assignment records. |
| `PUT` | `/cacaomarketcm/api/admin/tables/{table}/{recordId}` | Updates only supported user-type, user, basic-right, or enterprise-profile records. |
| `DELETE` | `/cacaomarketcm/api/admin/tables/{table}/{recordId}` | Applies the table-specific safe action: controlled removal, session/reset revocation, pending-registration cancellation, or right-assignment removal. |

Approved table keys are `type_utilisateur`, `utilisateurs`, `client_particulier`, `client_entreprise`, `sessions_utilisateur`, `registration_confirmation`, `password_reset`, `basic_rights`, `type_utilisateur_basic_right`, and `password_history`. They are an enum allow-list, not SQL identifiers supplied by a caller.

`client_particulier` is an audit-safe profile relationship; it is created only by the buyer registration flow. `client_entreprise` exposes a tightly scoped update for `raisonSociale`, `niu`, and `rccm`, with identifier uniqueness preserved. Direct administrator creation of `CLIENT` accounts and role changes into or out of `CLIENT` are refused so an account cannot bypass or orphan its required buyer legal profile.

`password_history` is read-only. Browser sessions, registration confirmations, and password resets are audit-safe views with narrowly scoped revocation/cancellation actions. Raw passwords, password hashes, browser session hashes, confirmation token hashes, and password-reset token hashes are never accepted for display or returned by these routes. The service protects built-in roles, the `APP-CONN` capability, administrator assignments, self-removal, and the final active administrator account.

The browser frontend uses `PUT`, so the configured credentialed CORS policy explicitly permits `GET`, `POST`, `PUT`, and `DELETE` from the configured frontend origins.

## Run and verify the API connection

From `Back-End/service-connectmarket`, start the backend process (building with `mvnw.cmd install` alone does not start it):

```bat
mvnw.cmd spring-boot:run
```

Wait for Spring Boot to report that it has started on port `8080`, then open:

```text
http://localhost:8080/cacaomarketcm/api/health
```

A running service returns:

```json
{"status":"UP","service":"service-connectmarket"}
```

Then verify the database projection used by the administrator user-type table without exposing a database row:

```text
http://localhost:8080/cacaomarketcm/api/health/database
```

A healthy schema returns `200` with `{"status":"UP","database":"UP",...}`. A `503` response with `DATA_ACCESS_UNAVAILABLE` means Spring is running but cannot read `gu.type_utilisateur` and its `code`/`tu_name` columns; apply the current [`database/gu.sql`](../../database/gu.sql) or correct the PostgreSQL connection/permissions before testing administrator tables.

## Structured API-call logs

Every `/cacaomarketcm/api/...` call receives an `X-Request-Id` response header. That identifier is included on every ordered log entry made during the request, so one registration, confirmation, login, or logout flow can be followed from start to finish.

The logs are written to both the service console/IntelliJ Run view and, by default, to:

```text
Back-End/service-connectmarket/logs/cacaomarket-api.log
```

A successful registration produces a sequence similar to:

```text
event=api.request.received method=POST path=/cacaomarketcm/api/auth/registration ...
event=registration.request.accepted role=VENDEUR email=d***@example.com login=ro***
event=registration.identity.available
event=registration.pending.persisted utilisateurId=42 expiresAt=...
event=registration.mail.smtp-send.completed
event=registration.workflow.completed expiresAt=...
event=api.request.completed method=POST path=/cacaomarketcm/api/auth/registration status=202 outcome=success durationMs=...
```

Rejected requests also expose the safe API error code, for example `REGISTRATION_MAIL_DELIVERY_UNAVAILABLE`, `REGISTRATION_CONFIRMATION_EXPIRED`, `PASSWORD_RESET_TOKEN_EXPIRED`, or `INVALID_CREDENTIALS`. Request bodies, passwords, password hashes, raw registration or reset tokens, mail credentials, and token query values are intentionally never written to the logs.

A database failure is returned safely as `503 DATA_ACCESS_UNAVAILABLE` and logs only an exception type, safe SQLSTATE, and application origin. For example, SQLSTATE `42P01` points to a missing relation, `42703` to an outdated/missing column, and `42501` to a database-permission problem. Apply the tracked [`database/gu.sql`](../../database/gu.sql) and verify the configured PostgreSQL account when one of these appears.

The optional `APP_LOG_FILE` environment variable can move the log file to a different location.

Angular reads its API base URL from the built public `config.json`; there is no Angular development proxy. When `apiBaseUrl` points to a different origin, direct frontend requests require credentialed CORS. See the [frontend API connection instructions](../../Front-End/README.md#authentication-api-connection) and ensure `APP_CORS_ALLOWED_ORIGINS` includes each exact direct origin.

## Google Gmail SMTP configuration

The reusable account-messaging module is [`SmtpRegistrationMessagingService`](src/main/java/cm/odigital/serviceconnectmarket/auth/messaging/SmtpRegistrationMessagingService.java). It uses Spring's `JavaMailSender` with Gmail's authenticated SMTP server (`smtp.gmail.com`, port `587`, STARTTLS) and sends both registration confirmations and password-reset links through that account.

### Where to set the Google email and App Password

Set them only in this local, ignored file:

```text
Back-End/service-connectmarket/src/main/resources/.env
```

Start from the tracked template:

```bat
cd Back-End\service-connectmarket
copy src\main\resources\.env.example src\main\resources\.env
```

Then edit these values in `src/main/resources/.env`. The Spring configuration imports this file both when launched from the service folder and when IntelliJ's working directory is `Back-End`:

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-google-address@gmail.com
MAIL_PASSWORD=your-16-character-google-app-password
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
MAIL_STARTTLS_REQUIRED=true
REGISTRATION_MAIL_FROM=your-google-address@gmail.com
```

Do **not** put the App Password in `application.properties`, commit `.env`, or use the normal Google account password. The repository ignores this `.env` intentionally. It is also excluded from the packaged WAR, so production deployments should use environment variables or an external `.env` file.

### Create the Google App Password

1. Enable **2-Step Verification** for the Google account that will send CacaoMarketCM mail.
2. Open [Google App Passwords](https://myaccount.google.com/apppasswords), create an App Password, and copy the generated 16-character value.
3. Paste that generated value into `MAIL_PASSWORD` in `Back-End/service-connectmarket/src/main/resources/.env`. If Google displays it in groups, paste it without spaces. For the IntelliJ run configuration shown in the logs, restart the backend after saving the file so Spring reloads it.
4. Set `REGISTRATION_MAIL_FROM` to the same Gmail/Google Workspace mailbox or to an alias verified by that account. You may leave it blank to use `MAIL_USERNAME` automatically.

For Google Workspace accounts, App Password availability can be disabled by the organization administrator. If it is unavailable, ask the administrator to permit it or use an approved SMTP relay/OAuth configuration instead.

Set `REGISTRATION_CONFIRMATION_URL` in the same `.env` file to the public **Angular confirmation page**, for example:

```properties
REGISTRATION_CONFIRMATION_URL=http://localhost:4200/CacaoMarketCM/registration/confirm
```

For production, replace `localhost:4200` with the public frontend host/domain. Do not include a token in that environment variable; the service appends a fresh secure token, and the Angular page sends it safely to the backend confirmation endpoint.

Configure the password-reset destination and optional lifetime in the same file:

```properties
PASSWORD_RESET_URL=http://localhost:4200/CacaoMarketCM/password-reset/confirm
PASSWORD_RESET_TOKEN_TTL=PT1H
```

Use the public frontend host in production and omit the token from `PASSWORD_RESET_URL`; the service appends a new token to each eligible email. `PASSWORD_RESET_TOKEN_TTL` uses ISO-8601 duration syntax (`PT1H` is one hour). Restart the backend after changing either setting.

Registration deliberately fails with `503 REGISTRATION_MAIL_DELIVERY_UNAVAILABLE` when Gmail SMTP is absent or cannot deliver. The transaction rolls back so the application never leaves an unconfirmable pending account in the database.

## Database setup

The tracked schema is [`../../database/gu.sql`](../../database/gu.sql). Apply it manually before starting the service:

```bash
psql -v ON_ERROR_STOP=1 -h localhost -p 6000 -U sorelle -d cacaomarketcm -f ../../database/gu.sql
```
