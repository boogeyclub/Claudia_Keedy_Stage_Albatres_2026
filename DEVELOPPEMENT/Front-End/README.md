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
  "apiBaseUrl": "http://localhost:8080/cacaomarketcm/api"
}
```

That shipped value is the **local development** configuration: the Angular dev server runs on `http://localhost:4200/CacaoMarketCM/` while Spring listens on `http://localhost:8080/cacaomarketcm`, so the API base URL must be absolute. A relative value such as `/cacaomarketcm/api` is resolved against the frontend origin and the request hits the dev server instead of Spring, which answers `404 Not Found` for `POST /cacaomarketcm/api/auth/login`.

There is **no Angular development proxy** and none is needed: the browser calls Spring directly, which is why `apiBaseUrl` is an absolute URL. Two consequences apply to that direct call:

1. Spring CORS must allow the exact frontend origin. The tracked default already does:
   `app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:http://localhost:4200}`, with credentials enabled and `X-Request-Id` exposed.
2. If the frontend is started on a different port or host, add that exact origin to `APP_CORS_ALLOWED_ORIGINS` in `Back-End/service-connectmarket/src/main/resources/.env` and restart the backend.

Because the file is fetched relative to Angular's `/CacaoMarketCM/` base path, the built asset is available as `/CacaoMarketCM/config.json`. It is not content-hashed, so a deployment can replace that one JSON file without rebuilding the JavaScript bundles. A deployment that serves the frontend and Spring behind a single web server can use the origin-relative form instead:

```json
{
  "apiBaseUrl": "/cacaomarketcm/api"
}
```

Invalid or missing runtime configuration stops Angular from bootstrapping rather than silently calling an unintended API. The accepted values are an absolute `http(s)://` URL or a single-slash origin-relative path; a protocol-relative `//host` value is rejected.

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

It returns `200` with `"database":"UP"` only when Spring can read every relation and column created by `gu.sql` — the same verification the API runs when it starts. An incomplete schema is reported as `503` with `"code":"SCHEMA_TABLES_MISSING"` and a `missingElements` list, while an unreachable PostgreSQL is reported as `503` with `"code":"DATA_ACCESS_UNAVAILABLE"`. Repair the database or apply the script before debugging the Angular UI.

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

`/CacaoMarketCM/dashboard/account` lists the current account's active browser sessions and can disconnect an unused browser. Apply the latest [`database/gu.sql`](../../Back-End/database/gu.sql) before using these features, because the login flow writes a session record after each successful sign-in.

### Administrator `gu` table management

The administrator dashboard contains one card for each **dashboard-managed** `gu` table and opens a protected route under `/CacaoMarketCM/dashboard/admin/tables/{table}`. The Angular route guard improves navigation, while the Spring API independently checks the persisted active browser session and `ADMINISTRATEUR` role for every request.

The menu is intentionally limited to the platform's current operating scope:

| Purpose | Tables shown |
| --- | --- |
| Users | `utilisateurs` (create, update, remove), `type_utilisateur` (the `CLIENT`, `VENDEUR`, and `ADMINISTRATEUR` roles referenced by every account) |
| Registrations | `registration_confirmation` (pending sign-ups, with a cancel action), plus `client_particulier` and `client_entreprise`, the buyer profiles a `CLIENT` registration creates |
| Sessions | `sessions_utilisateur` (connected browsers, with a revoke action) |

The password and basic-right tables are deliberately **not** exposed by the dashboard: `gu.password_reset` and `gu.password_history` belong to the self-service password flow, and `gu.basic_rights`/`gu.type_utilisateur_basic_right` are access-configuration tables whose only right (`APP-CONN`) is already granted automatically. The API still supports all ten tables, so re-adding a card means adding its definition to [`admin-table-catalog.ts`](./src/app/core/admin/admin-table-catalog.ts); a URL for a table that is not in the menu redirects back to the dashboard overview.

`client_particulier` is an audit view of the registration-managed private-buyer relationship, while `client_entreprise` allows only controlled updates to the legal company details. `sessions_utilisateur` and `registration_confirmation` remain audit-oriented with only revocation or pending-registration cancellation actions. The UI deliberately has no column or form field for password hashes, session hashes, confirmation hashes, or reset-token hashes. The server enforces the same allow-list and safety rules.

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
