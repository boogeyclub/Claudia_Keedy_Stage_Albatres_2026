# FrontEnd

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.2.21.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/CacaoMarketCM/`. The application will automatically reload whenever you modify any of the source files.

## Authentication API connection

Angular loads [`public/config.json`](./public/config.json) **before** it bootstraps. The file is copied unchanged into the application build output and is the single browser-visible source of the API base URL:

```json
{
  "apiBaseUrl": "/cacaomarketcm/api"
}
```

The default origin-relative value expects the web server to make `/cacaomarketcm/api/...` available from Spring, whose servlet context is lower-case `/cacaomarketcm`. Because the file is fetched relative to Angular's `/CacaoMarketCM/` base path, the built asset is available as `/CacaoMarketCM/config.json`. It is not content-hashed, so a deployment can replace that one JSON file without rebuilding the JavaScript bundles.

There is **no Angular development proxy**. When the frontend and backend have different origins, set `apiBaseUrl` in `public/config.json` to the complete Spring URL before building or deploying, for example:

```json
{
  "apiBaseUrl": "http://localhost:8080/cacaomarketcm/api"
}
```

A direct cross-origin URL requires Spring CORS to allow the exact frontend origin through `APP_CORS_ALLOWED_ORIGINS`. Keep credentials enabled; browser session cookies are used by the authenticated routes. Invalid or missing runtime configuration stops Angular from bootstrapping rather than silently calling an unintended API.

Start the backend service separately before submitting a registration:

```bat
cd ..\Back-End\service-connectmarket
mvnw.cmd spring-boot:run
```

`mvnw.cmd install` only builds the backend; it does not keep the API running.

### Verify the connection

With the backend running, open this URL directly:

```text
http://localhost:8080/cacaomarketcm/api/health
```

It should return:

```json
{"status":"UP","service":"service-connectmarket"}
```

Then verify the schema projection required for administrator user types:

```text
http://localhost:8080/cacaomarketcm/api/health/database
```

It returns `200` with `"database":"UP"` only when Spring can read `gu.type_utilisateur.code` and `gu.type_utilisateur.tu_name`. If either check fails, repair Spring/the PostgreSQL schema before debugging the Angular UI.

The Angular app sends requests to the `apiBaseUrl` loaded from `config.json`. The backend logs each request without logging request bodies, passwords, or registration/reset-token query values.

A browser response with HTTP `500` or `503` means the frontend successfully reached Spring; inspect the safe `X-Request-Id` displayed by the administrator-table error and match it in the backend log. A browser response with status `0` instead indicates a network/CORS/API-base-URL problem.

## Buyer registration profiles

The public registration form asks only a `CLIENT` buyer to select a legal profile. `PARTICULIER` creates a private-individual buyer profile. `ENTREPRISE` additionally requires the company’s `raisonSociale`, `NIU`, and `RCCM`; the existing first and last name fields are labeled as the legal representative or primary contact. `NIU` and `RCCM` conflicts are reported without exposing another company’s data. `VENDEUR` registration remains unchanged and never sends buyer-profile fields.

## Password reset

The sign-in form has a **Forgot password?** action that opens `/CacaoMarketCM/password-reset`. The request page accepts an account email and calls `POST /cacaomarketcm/api/auth/password-reset/request`; it deliberately shows the same success message whether or not a link can be sent, so it does not disclose account existence.

Only confirmed (`ACTIF`) accounts receive a Gmail reset link at their stored email address. The link targets `/CacaoMarketCM/password-reset/confirm?token=...`, where the Angular confirmation page accepts a new password and calls `POST /cacaomarketcm/api/auth/password-reset/confirm`. The page never renders the raw token, directs the user back to sign-in after success, and supports English and French like the rest of the public authentication flow.

The backend defaults the single-use link lifetime to one hour. Set `PASSWORD_RESET_URL` and, if needed, `PASSWORD_RESET_TOKEN_TTL` in the backend's local `src/main/resources/.env`; see the [backend Gmail and reset configuration](../Back-End/service-connectmarket/README.md#google-gmail-smtp-configuration). A successful reset invalidates all persistent browser sessions, so the user must sign in again.

## Role dashboards and browser sessions

A successful login routes each user type to its own protected workspace:

| Database user type | Angular route | Workspace |
| --- | --- | --- |
| `ADMINISTRATEUR` | `/CacaoMarketCM/dashboard/admin` | Administrator dashboard |
| `VENDEUR` | `/CacaoMarketCM/dashboard/vendeur` | Seller dashboard |
| `CLIENT` | `/CacaoMarketCM/dashboard/client` | User/client dashboard |

Dashboard routes first call `GET /cacaomarketcm/api/auth/session`, so refreshing a page verifies both the browser cookie and the persistent `gu.sessions_utilisateur` record. The professional dashboard header contains the CacaoMarketCM logo, account menu, language selector, account settings link, and secure sign-out action.

`/CacaoMarketCM/dashboard/account` lists the current account's active browser sessions and can disconnect an unused browser. Apply the latest [`database/gu.sql`](../database/gu.sql) before using these features, because the login flow writes a session record after each successful sign-in.

### Administrator `gu` table management

The administrator dashboard contains one card for each `gu` table and opens a protected route under `/CacaoMarketCM/dashboard/admin/tables/{table}`. The Angular route guard improves navigation, while the Spring API independently checks the persisted active browser session and `ADMINISTRATEUR` role for every request.

Configuration and account records expose controlled create/update/removal workflows. `client_particulier` is an audit view of the registration-managed private-buyer relationship, while `client_entreprise` allows only controlled updates to the legal company details. `sessions_utilisateur`, `registration_confirmation`, and `password_reset` remain audit-oriented with only revocation or pending-registration cancellation actions; `password_history` is read-only. The UI deliberately has no column or form field for password hashes, session hashes, confirmation hashes, or reset-token hashes. The server enforces the same allow-list and safety rules.

### Dashboard component layout

Role-specific dashboard components are grouped beneath `src/app/pages/dashboard` so future features stay close to the role that owns them:

```text
pages/dashboard/
├── admin/
│   ├── overview/
│   └── table-management/
├── seller/
│   └── overview/
├── client/
│   └── overview/
└── shared/
    ├── account-settings/
    ├── dashboard-redirect.ts
    └── dashboard-shell.*
```

Routes stay unchanged; only the lazy-import locations follow this organisation.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
