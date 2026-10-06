/**
 * Maps the business error codes returned by the protected administration API to translated
 * messages. The API answers with `{ code, message, timestamp }`; the browser only uses `code`, so
 * the wording stays in the i18n dictionaries and no server text is displayed to an operator.
 *
 * Everything else (connection, authentication, server or database failure) is classified by
 * status code in the component, keeping those messages longer and more diagnostic.
 */
const BUSINESS_ERROR_KEYS: Readonly<Record<string, string>> = {
  ADMIN_CLIENT_CREATION_REQUIRES_REGISTRATION: 'notifications.admin.errors.clientCreationRequiresRegistration',
  ADMIN_CLIENT_ROLE_CHANGE_REQUIRES_PROFILE_WORKFLOW: 'notifications.admin.errors.clientRoleChangeRequiresProfile',
  ADMIN_SYSTEM_ROLE_PROTECTED: 'notifications.admin.errors.systemRoleProtected',
  ADMIN_USER_TYPE_IN_USE: 'notifications.admin.errors.userTypeInUse',
  ADMIN_USER_TYPE_NOT_LOGIN_CAPABLE: 'notifications.admin.errors.userTypeNotLoginCapable',
  ADMIN_USER_TYPE_CODE_ALREADY_EXISTS: 'notifications.admin.errors.userTypeCodeAlreadyExists',
  ADMIN_USER_TYPE_NAME_ALREADY_EXISTS: 'notifications.admin.errors.userTypeNameAlreadyExists',
  ADMIN_IDENTITY_ALREADY_EXISTS: 'notifications.admin.errors.identityAlreadyExists',
  ADMIN_ENTERPRISE_IDENTIFIER_ALREADY_EXISTS: 'notifications.admin.errors.enterpriseIdentifierAlreadyExists',
  ADMIN_SELF_DELETE_FORBIDDEN: 'notifications.admin.errors.selfDeleteForbidden',
  ADMIN_SELF_PRIVILEGE_CHANGE_FORBIDDEN: 'notifications.admin.errors.selfPrivilegeChangeForbidden',
  ADMIN_LAST_ADMINISTRATOR_PROTECTED: 'notifications.admin.errors.lastAdministratorProtected',
  ADMIN_CONFIRMATION_NOT_PENDING: 'notifications.admin.errors.confirmationNotPending',
  ADMIN_RECORD_NOT_FOUND: 'notifications.admin.errors.recordNotFound',
  ADMIN_MUTATION_INVALID: 'notifications.admin.errors.mutationInvalid',
  ADMIN_MUTATION_FIELD_FORBIDDEN: 'notifications.admin.errors.mutationFieldForbidden',
  ADMIN_TABLE_READ_ONLY: 'notifications.admin.errors.tableReadOnly',
  ADMIN_TABLE_IMMUTABLE: 'notifications.admin.errors.tableImmutable',
  ADMIN_TABLE_UNSUPPORTED: 'notifications.admin.errors.tableUnsupported',
  PROTECTED_DATA_CONFLICT: 'notifications.admin.errors.protectedDataConflict',
  ADMINISTRATOR_ACCESS_REQUIRED: 'notifications.admin.errors.administratorRequired',
  ADMIN_PASSWORD_RESET_ACCOUNT_NOT_ACTIVE: 'notifications.admin.errors.passwordResetAccountNotActive',
  ADMIN_PASSWORD_RESET_USER_NOT_FOUND: 'notifications.admin.errors.passwordResetUserNotFound',
  CREDENTIALS_MAIL_DELIVERY_UNAVAILABLE: 'notifications.admin.errors.credentialsMailUnavailable'
};

export function businessErrorKeyFor(code: unknown): string | null {
  return typeof code === 'string' ? BUSINESS_ERROR_KEYS[code] ?? null : null;
}
