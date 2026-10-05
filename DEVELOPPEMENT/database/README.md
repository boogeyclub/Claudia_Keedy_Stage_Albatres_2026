# Database SQL scripts

This directory keeps database SQL under version control before it is executed.

## Convention

- Store one schema definition per file.
- Name the file after the PostgreSQL schema: `<schema-name>.sql`.
- Scripts in this directory are intentionally **not** executed automatically by Spring Boot.

## Current schemas

| Schema | Script | Tables |
| --- | --- | --- |
| `gu` | [`gu.sql`](./gu.sql) | `type_utilisateur`, `utilisateurs`, `client_particulier`, `client_entreprise`, `sessions_utilisateur`, `registration_confirmation`, `password_reset`, `basic_rights`, `type_utilisateur_basic_right`, `password_history` |

## User types

`gu.type_utilisateur` is the user-type lookup table. Its seeded values determine whether an `utilisateurs` row is a `CLIENT`, `VENDEUR`, or `ADMINISTRATEUR`.

| Column | Purpose |
| --- | --- |
| `id` | Identity primary key |
| `code` | Unique role code: `CLIENT`, `VENDEUR`, or `ADMINISTRATEUR` |
| `tu_name` | Human-readable user-type name |

## Utilisateurs

`gu.utilisateurs` replaces the separate `client`, `vendeur`, and `administrateur` tables. Each row references one user type through `type_utilisateur_id`.

| Column | Purpose |
| --- | --- |
| `id` | Identity primary key |
| `type_utilisateur_id` | Required reference to `type_utilisateur` |
| `nom` | Last name |
| `prenom` | First name |
| `email` | Globally unique email address |
| `login` | Globally unique login name |
| `statut` | Account status, defaulting to `ACTIF`; unconfirmed registrations use `EN_ATTENTE_CONFIRMATION` |
| `dateCreation` | Timestamp set when the row is created |

Email and login identities also have case-insensitive unique indexes so that `buyer@example.com` and `BUYER@example.com` cannot become separate accounts.

## Buyer legal profiles

Only `CLIENT` accounts have a buyer legal profile. `VENDEUR` accounts continue to use only the common `gu.utilisateurs` record.

- `gu.client_particulier` has one column, `utilisateur_id`, which is both its primary key and a cascading reference to a `CLIENT` account.
- `gu.client_entreprise` has `utilisateur_id`, `raison_sociale`, `niu`, and `rccm`. The first is the primary key/cascading account reference; the other three values are required.
- `prenom` and `nom` remain on `gu.utilisateurs`. For an enterprise they identify its legal representative or primary contact, rather than replacing the legal company name.
- Case-insensitive unique indexes enforce that an NIU and an RCCM can each belong to only one enterprise profile. The application normalizes both identifiers to upper case before persistence.

Database triggers ensure that profile rows belong only to `CLIENT` accounts and that an account cannot have both profile types. A deferred database constraint also prevents a `CLIENT` transaction from committing with zero or two profiles, while still allowing the account and its profile to be created atomically. When the latest script is applied to an existing deployment, every existing `CLIENT` without an enterprise profile is safely backfilled into `client_particulier`; it is not guessed to be an enterprise. The migration stops with a clear error instead of guessing if it finds an existing dual profile or a profile attached to a non-`CLIENT` account. Delete or reclassify a profile deliberately before changing an account away from `CLIENT`.

## Connected browser sessions

`gu.sessions_utilisateur` records every successful browser login for a user. One user can have several active rows at the same time, allowing CacaoMarketCM to manage each browser/device connection independently.

| Column | Purpose |
| --- | --- |
| `utilisateur_id` | Required owner of the browser session; it cascades when the account is removed |
| `session_hash` | Unique SHA-256 hash of the servlet session ID; the raw `JSESSIONID` is never stored |
| `browser_label` | Safe, compact browser/device label such as `Google Chrome on Windows`; no full User-Agent is retained |
| `remember_me` | Whether the login was created with the extended seven-day session option |
| `date_creation` / `last_seen_at` | Login and most recent authenticated activity timestamps |
| `expires_at` | Current idle-session expiration timestamp |
| `invalidated_at` | Timestamp set on logout, manual disconnect, new login in the same browser, or expiration |

Active rows have `invalidated_at IS NULL` and an `expires_at` in the future. The backend checks this persistent row in addition to the servlet cookie for every protected session endpoint. Expired sessions are marked invalid by a scheduled cleanup; their safe history remains available for account-level session management.

## Default development administrator

`gu.sql` creates an active bootstrap account when it is first applied:

| Field | Seeded value |
| --- | --- |
| User type | `ADMINISTRATEUR` |
| Login | `root` |
| Email | `root@cacaomarket.local` |
| Initial password | `root1234` |
| Status | `ACTIF` |

The password is stored only as a BCrypt hash in `gu.password_history`; plaintext `root1234` is never stored in PostgreSQL. The seed is idempotent: if `root` already exists, it is made an active administrator, but an existing current password is **not** overwritten.

> `root` / `root1234` is a development bootstrap credential only. Change it immediately after the first login and never use it in a production deployment.

## Registration confirmations

`gu.registration_confirmation` holds the one-time confirmation state for a pending registration.

| Column | Purpose |
| --- | --- |
| `utilisateur_id` | Unique reference to the pending `utilisateurs` row |
| `token_hash` | SHA-256 hash of the email token; the raw token is never persisted |
| `expires_at` | Exact confirmation deadline, set to three hours after registration |
| `confirmed_at` | Timestamp written only after a successful confirmation |
| `date_creation` | Timestamp when the confirmation record was created |

The backend creates `CLIENT` and `VENDEUR` registrations with `statut = EN_ATTENTE_CONFIRMATION`. A scheduled cleanup and every registration/authentication request remove any unconfirmed expired account together with its password history. A valid confirmation sets the status to `ACTIF`; only then can the account authenticate through the `APP-CONN` basic right.

## Password-reset requests

`gu.password_reset` holds the current single-use reset token for an account. Only an `ACTIF` account—meaning its registration has already been confirmed—can receive a row and reset link.

| Column | Purpose |
| --- | --- |
| `utilisateur_id` | Unique owner of the reset request; it cascades when the account is removed |
| `token_hash` | Unique SHA-256 hash of the reset token; the raw token is never stored |
| `expires_at` | Exact reset-link deadline; the backend configures this as one hour by default |
| `used_at` | Timestamp written when a valid token has changed the password |
| `date_creation` | Creation time; a newer request replaces the old unused token and resets this value |

A reset completion inserts a new BCrypt password history row, automatically archives the former current password through the existing database trigger, marks the reset token used, and invalidates every active browser session for that account. The user must then sign in with the new password.

## Basic rights and type associations

`gu.basic_rights` stores the rights catalog.

| Column | Purpose |
| --- | --- |
| `id` | Identity primary key |
| `code` | Unique, stable right code |
| `br_name` | Human-readable basic-right name |

`gu.type_utilisateur_basic_right` manages the many-to-many association between a user type and its basic rights. Its composite primary key is `(type_utilisateur_id, basic_right_id)`.

The initial right is seeded as follows:

```text
APP-CONN — Connexion a l'application CacaoMARKETCM
```

`APP-CONN` is associated with every user type so all users can connect to the application. `ADMINISTRATEUR` is automatically associated with every basic right, including any added in the future. A database trigger prevents an administrator right from being removed or reassigned.

## Password history

Passwords are stored only in `gu.password_history`, not in `utilisateurs`.

- Each row references one account through `utilisateur_id`.
- `password` must contain a secure password hash, never a plaintext password.
- The quoted `"current"` boolean identifies the active password for that user.
- `date_insertion` records when the password was added.
- `date_changement` records when a current password was replaced and became an old password.
- A database trigger automatically archives the previous current password when a new current password row is inserted.
- A unique partial index prevents more than one current password for the same user.

To apply a script manually to the local database, run it from the repository root:

```bash
psql -v ON_ERROR_STOP=1 -h localhost -p 6000 -U sorelle -d cacaomarketcm -f database/gu.sql
```
