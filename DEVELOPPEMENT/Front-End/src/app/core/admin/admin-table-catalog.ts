import { AdminRecord } from './admin-api.service';

/**
 * Tables exposed by the administrator dashboard menu.
 *
 * The API allow-list is wider (the backend `AdminTable` enum): it still supports the password
 * and basic-right tables. The dashboard deliberately shows only what the platform currently needs —
 * registration, user, buyer-profile, and session management — so the menu stays focused on the
 * CacaoMarketCM operating scope. Re-adding one of the other tables means adding its definition
 * below again.
 */
export type AdminTableKey =
  | 'utilisateurs'
  | 'type_utilisateur'
  | 'client_particulier'
  | 'client_entreprise'
  | 'registration_confirmation'
  | 'sessions_utilisateur';

export type AdminTableIcon = 'users' | 'person' | 'monitor' | 'mail';
export type AdminEditorControl = 'text' | 'email' | 'password' | 'select';
export type AdminLookupSource = 'userTypes' | 'basicRights';

export interface AdminTableColumn {
  key: string;
  labelKey: string;
  format?: 'date' | 'boolean' | 'status';
  compact?: boolean;
}

export interface AdminSelectOption {
  value: string;
  labelKey: string;
}

export interface AdminTableField {
  key: string;
  labelKey: string;
  control: AdminEditorControl;
  required?: boolean;
  maxLength?: number;
  minLength?: number;
  autocomplete?: string;
  options?: readonly AdminSelectOption[];
  lookup?: AdminLookupSource;
}

export interface AdminTableDefinition {
  key: AdminTableKey;
  schemaName: string;
  titleKey: string;
  descriptionKey: string;
  securityNoteKey: string;
  icon: AdminTableIcon;
  columns: readonly AdminTableColumn[];
  createFields?: readonly AdminTableField[];
  editFields?: readonly AdminTableField[];
  removeActionKey?: string;
  isAuditOnly?: boolean;
  recordIdKey?: string;
  canRemove?: (record: AdminRecord) => boolean;
}

const STATUS_OPTIONS: readonly AdminSelectOption[] = [
  { value: 'ACTIF', labelKey: 'dashboard.admin.statuses.active' },
  { value: 'SUSPENDU', labelKey: 'dashboard.admin.statuses.suspended' }
];

const TYPE_FIELDS: readonly AdminTableField[] = [
  { key: 'code', labelKey: 'dashboard.admin.fields.code', control: 'text', required: true, maxLength: 50 },
  { key: 'name', labelKey: 'dashboard.admin.fields.name', control: 'text', required: true, maxLength: 100 }
];

const USER_CREATE_FIELDS: readonly AdminTableField[] = [
  { key: 'typeUtilisateurId', labelKey: 'dashboard.admin.fields.userType', control: 'select', required: true, lookup: 'userTypes' },
  { key: 'prenom', labelKey: 'dashboard.admin.fields.firstName', control: 'text', required: true, maxLength: 100, autocomplete: 'given-name' },
  { key: 'nom', labelKey: 'dashboard.admin.fields.lastName', control: 'text', required: true, maxLength: 100, autocomplete: 'family-name' },
  { key: 'email', labelKey: 'dashboard.admin.fields.email', control: 'email', required: true, maxLength: 255, autocomplete: 'email' },
  { key: 'login', labelKey: 'dashboard.admin.fields.login', control: 'text', required: true, minLength: 3, maxLength: 100, autocomplete: 'username' },
  { key: 'statut', labelKey: 'dashboard.admin.fields.status', control: 'select', required: true, options: STATUS_OPTIONS },
  { key: 'password', labelKey: 'dashboard.admin.fields.temporaryPassword', control: 'password', required: true, minLength: 8, maxLength: 72, autocomplete: 'new-password' }
];

const USER_EDIT_FIELDS: readonly AdminTableField[] = USER_CREATE_FIELDS.filter((field) => field.key !== 'password');

const ENTERPRISE_PROFILE_FIELDS: readonly AdminTableField[] = [
  { key: 'raisonSociale', labelKey: 'dashboard.admin.fields.companyName', control: 'text', required: true, maxLength: 150, autocomplete: 'organization' },
  { key: 'niu', labelKey: 'dashboard.admin.fields.niu', control: 'text', required: true, maxLength: 50, autocomplete: 'off' },
  { key: 'rccm', labelKey: 'dashboard.admin.fields.rccm', control: 'text', required: true, maxLength: 50, autocomplete: 'off' }
];

/**
 * Browser-visible table definitions only list safe fields. The API independently applies the same
 * table allow-list and omits all persisted password/session/token hashes.
 *
 * The menu follows the database logic that matters operationally: an account (`utilisateurs`) has a
 * type (`type_utilisateur`), a `CLIENT` account owns exactly one buyer profile
 * (`client_particulier` or `client_entreprise`) created by its registration, a pending registration
 * is confirmed through `registration_confirmation`, and each sign-in is tracked in
 * `sessions_utilisateur`.
 */
export const ADMIN_TABLE_CATALOG: readonly AdminTableDefinition[] = [
  {
    key: 'utilisateurs',
    schemaName: 'gu.utilisateurs',
    titleKey: 'dashboard.admin.tables.users.title',
    descriptionKey: 'dashboard.admin.tables.users.description',
    securityNoteKey: 'dashboard.admin.tables.users.securityNote',
    icon: 'person',
    columns: [
      { key: 'id', labelKey: 'dashboard.admin.columns.id', compact: true },
      { key: 'prenom', labelKey: 'dashboard.admin.columns.firstName' },
      { key: 'nom', labelKey: 'dashboard.admin.columns.lastName' },
      { key: 'email', labelKey: 'dashboard.admin.columns.email' },
      { key: 'login', labelKey: 'dashboard.admin.columns.login', compact: true },
      { key: 'typeCode', labelKey: 'dashboard.admin.columns.userType', compact: true },
      { key: 'statut', labelKey: 'dashboard.admin.columns.status', format: 'status', compact: true },
      { key: 'dateCreation', labelKey: 'dashboard.admin.columns.createdAt', format: 'date' }
    ],
    createFields: USER_CREATE_FIELDS,
    editFields: USER_EDIT_FIELDS,
    removeActionKey: 'dashboard.admin.actions.delete'
  },
  {
    key: 'type_utilisateur',
    schemaName: 'gu.type_utilisateur',
    titleKey: 'dashboard.admin.tables.userTypes.title',
    descriptionKey: 'dashboard.admin.tables.userTypes.description',
    securityNoteKey: 'dashboard.admin.tables.userTypes.securityNote',
    icon: 'users',
    columns: [
      { key: 'id', labelKey: 'dashboard.admin.columns.id', compact: true },
      { key: 'code', labelKey: 'dashboard.admin.columns.code', compact: true },
      { key: 'name', labelKey: 'dashboard.admin.columns.name' }
    ],
    createFields: TYPE_FIELDS,
    editFields: TYPE_FIELDS,
    removeActionKey: 'dashboard.admin.actions.delete'
  },
  {
    key: 'client_particulier',
    schemaName: 'gu.client_particulier',
    titleKey: 'dashboard.admin.tables.individualClients.title',
    descriptionKey: 'dashboard.admin.tables.individualClients.description',
    securityNoteKey: 'dashboard.admin.tables.individualClients.securityNote',
    icon: 'person',
    isAuditOnly: true,
    columns: [
      { key: 'id', labelKey: 'dashboard.admin.columns.id', compact: true },
      { key: 'prenom', labelKey: 'dashboard.admin.columns.firstName' },
      { key: 'nom', labelKey: 'dashboard.admin.columns.lastName' },
      { key: 'email', labelKey: 'dashboard.admin.columns.email' },
      { key: 'login', labelKey: 'dashboard.admin.columns.login', compact: true },
      { key: 'statut', labelKey: 'dashboard.admin.columns.status', format: 'status', compact: true },
      { key: 'dateCreation', labelKey: 'dashboard.admin.columns.createdAt', format: 'date' }
    ]
  },
  {
    key: 'client_entreprise',
    schemaName: 'gu.client_entreprise',
    titleKey: 'dashboard.admin.tables.enterpriseClients.title',
    descriptionKey: 'dashboard.admin.tables.enterpriseClients.description',
    securityNoteKey: 'dashboard.admin.tables.enterpriseClients.securityNote',
    icon: 'users',
    columns: [
      { key: 'id', labelKey: 'dashboard.admin.columns.id', compact: true },
      { key: 'raisonSociale', labelKey: 'dashboard.admin.columns.companyName' },
      { key: 'niu', labelKey: 'dashboard.admin.columns.niu', compact: true },
      { key: 'rccm', labelKey: 'dashboard.admin.columns.rccm', compact: true },
      { key: 'prenom', labelKey: 'dashboard.admin.columns.representativeFirstName' },
      { key: 'nom', labelKey: 'dashboard.admin.columns.representativeLastName' },
      { key: 'email', labelKey: 'dashboard.admin.columns.email' },
      { key: 'statut', labelKey: 'dashboard.admin.columns.status', format: 'status', compact: true }
    ],
    editFields: ENTERPRISE_PROFILE_FIELDS
  },
  {
    key: 'registration_confirmation',
    schemaName: 'gu.registration_confirmation',
    titleKey: 'dashboard.admin.tables.confirmations.title',
    descriptionKey: 'dashboard.admin.tables.confirmations.description',
    securityNoteKey: 'dashboard.admin.tables.confirmations.securityNote',
    icon: 'mail',
    isAuditOnly: true,
    columns: [
      { key: 'id', labelKey: 'dashboard.admin.columns.id', compact: true },
      { key: 'utilisateurLogin', labelKey: 'dashboard.admin.columns.login', compact: true },
      { key: 'utilisateurEmail', labelKey: 'dashboard.admin.columns.email' },
      { key: 'utilisateurStatus', labelKey: 'dashboard.admin.columns.status', format: 'status', compact: true },
      { key: 'expiresAt', labelKey: 'dashboard.admin.columns.expiresAt', format: 'date' },
      { key: 'confirmedAt', labelKey: 'dashboard.admin.columns.confirmedAt', format: 'date' },
      { key: 'dateCreation', labelKey: 'dashboard.admin.columns.createdAt', format: 'date' }
    ],
    removeActionKey: 'dashboard.admin.actions.cancelPending',
    canRemove: (record) => record['utilisateurStatus'] === 'EN_ATTENTE_CONFIRMATION' && record['confirmedAt'] === null
  },
  {
    key: 'sessions_utilisateur',
    schemaName: 'gu.sessions_utilisateur',
    titleKey: 'dashboard.admin.tables.sessions.title',
    descriptionKey: 'dashboard.admin.tables.sessions.description',
    securityNoteKey: 'dashboard.admin.tables.sessions.securityNote',
    icon: 'monitor',
    isAuditOnly: true,
    columns: [
      { key: 'id', labelKey: 'dashboard.admin.columns.id', compact: true },
      { key: 'utilisateurLogin', labelKey: 'dashboard.admin.columns.login', compact: true },
      { key: 'utilisateurEmail', labelKey: 'dashboard.admin.columns.email' },
      { key: 'browserLabel', labelKey: 'dashboard.admin.columns.browser' },
      { key: 'rememberMe', labelKey: 'dashboard.admin.columns.extended', format: 'boolean', compact: true },
      { key: 'lastSeenAt', labelKey: 'dashboard.admin.columns.lastSeenAt', format: 'date' },
      { key: 'expiresAt', labelKey: 'dashboard.admin.columns.expiresAt', format: 'date' },
      { key: 'invalidatedAt', labelKey: 'dashboard.admin.columns.revokedAt', format: 'date' }
    ],
    removeActionKey: 'dashboard.admin.actions.revoke',
    canRemove: (record) => record['invalidatedAt'] === null
  }
] as const;

export function adminTableForKey(key: string | null): AdminTableDefinition | undefined {
  return ADMIN_TABLE_CATALOG.find((table) => table.key === key);
}
