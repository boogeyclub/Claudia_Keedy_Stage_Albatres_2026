import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminApiService, AdminRecord } from '../../../../core/admin/admin-api.service';
import { businessErrorKeyFor } from '../../../../core/admin/admin-error-messages';
import {
  AdminEditorControl,
  AdminRecordContext,
  AdminRowAction,
  AdminTableColumn,
  AdminTableDefinition,
  AdminTableField,
  adminTableForKey
} from '../../../../core/admin/admin-table-catalog';
import { AuthSessionService } from '../../../../core/auth/auth-session.service';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { NotificationMessage, NotificationService } from '../../../../core/notifications/notification.service';

interface EditorOption {
  value: string;
  label: string;
}

type EditorMode = 'create' | 'edit';
type TableOperation = 'load' | 'save' | 'remove';

@Component({
  selector: 'app-admin-table-management',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './admin-table-management.html',
  styleUrl: './admin-table-management.css'
})
export class AdminTableManagementComponent implements OnInit {
  protected readonly i18n = inject(TranslationService);
  protected readonly table = signal<AdminTableDefinition | null>(null);
  protected readonly records = signal<readonly AdminRecord[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly loadFailed = signal(false);
  protected readonly loadError = signal<NotificationMessage>({ key: 'dashboard.admin.management.loadError' });
  protected readonly isLoadingLookups = signal(false);
  protected readonly isSaving = signal(false);
  protected readonly removingRecordId = signal<string | null>(null);
  protected readonly editorMode = signal<EditorMode | null>(null);
  protected readonly selectedRecord = signal<AdminRecord | null>(null);
  protected readonly removalCandidate = signal<AdminRecord | null>(null);
  protected readonly rowActionCandidate = signal<{ record: AdminRecord; action: AdminRowAction } | null>(null);
  protected readonly runningActionRecordId = signal<string | null>(null);
  protected readonly userTypeOptions = signal<readonly EditorOption[]>([]);
  protected readonly basicRightOptions = signal<readonly EditorOption[]>([]);
  protected editorForm: UntypedFormGroup = new UntypedFormGroup({});

  protected readonly recordContext = computed<AdminRecordContext>(() => ({
    records: this.records(),
    currentUserId: this.authSession.user()?.id ?? null
  }));

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly adminApi = inject(AdminApiService);
  private readonly authSession = inject(AuthSessionService);
  private readonly notifications = inject(NotificationService);
  private readonly title = inject(Title);
  private readonly formBuilder = inject(UntypedFormBuilder);

  constructor() {
    effect(() => {
      const definition = this.table();
      this.title.setTitle(definition
        ? `CacaoMarketCM | ${this.i18n.t(definition.titleKey)}`
        : this.i18n.t('meta.administratorDashboardTitle')
      );
    });
  }

  ngOnInit(): void {
    const definition = adminTableForKey(this.route.snapshot.paramMap.get('table'));
    if (!definition) {
      void this.router.navigateByUrl('/dashboard/admin');
      return;
    }

    this.table.set(definition);
    this.loadRecords();
    this.loadEditorLookups(definition);
  }

  protected loadRecords(): void {
    const definition = this.table();
    if (!definition || this.isLoading()) {
      return;
    }

    this.isLoading.set(true);
    this.loadFailed.set(false);
    this.loadError.set({ key: 'dashboard.admin.management.loadError' });
    this.adminApi.tableRows(definition.key).pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: (response) => this.records.set(response.records),
      error: (error: unknown) => {
        const message = this.tableRequestFailureMessage(error, 'load');
        this.records.set([]);
        this.loadFailed.set(true);
        this.loadError.set(message);
        this.notifications.error(message);
      }
    });
  }

  protected openCreate(): void {
    const definition = this.table();
    if (!definition?.createFields || this.isSaving() || this.isLoadingLookups()) {
      return;
    }

    this.configureEditor('create', definition.createFields, null);
  }

  protected openEdit(record: AdminRecord): void {
    const definition = this.table();
    if (!definition?.editFields || this.isSaving() || this.isLoadingLookups()) {
      return;
    }

    this.configureEditor('edit', definition.editFields, record);
  }

  protected closeEditor(): void {
    if (this.isSaving()) {
      return;
    }
    this.editorMode.set(null);
    this.selectedRecord.set(null);
    this.editorForm = this.formBuilder.group({});
  }

  protected saveEditor(): void {
    const definition = this.table();
    const mode = this.editorMode();
    if (!definition || !mode || this.isSaving()) {
      return;
    }

    if (this.editorForm.invalid) {
      this.editorForm.markAllAsTouched();
      this.notifications.warning({ key: 'notifications.forms.invalid' });
      return;
    }

    const fields = mode === 'create' ? definition.createFields : definition.editFields;
    if (!fields) {
      return;
    }

    const values = this.valuesFor(fields);
    const request = mode === 'create'
      ? this.adminApi.createRecord(definition.key, values)
      : this.adminApi.updateRecord(definition.key, this.recordId(this.selectedRecord()), values);

    this.isSaving.set(true);
    request.pipe(
      this.notifications.trackApiCall({
        start: { key: 'notifications.admin.saving' },
        success: { key: 'notifications.admin.saved' },
        error: (error: unknown) => this.tableRequestFailureMessage(error, 'save')
      }),
      finalize(() => this.isSaving.set(false))
    ).subscribe({
      next: () => {
        this.closeEditor();
        this.loadRecords();
      }
    });
  }

  protected askToRemove(record: AdminRecord): void {
    const definition = this.table();
    if (!definition || !this.canRemove(record) || this.removingRecordId() !== null) {
      return;
    }
    this.removalCandidate.set(record);
  }

  /** Row actions the API would accept for this record; the rest stay hidden. */
  protected visibleRowActions(record: AdminRecord): readonly AdminRowAction[] {
    const definition = this.table();
    if (!definition?.rowActions) {
      return [];
    }
    return definition.rowActions.filter(
      (action) => !action.visibleWhen || action.visibleWhen(record, this.recordContext())
    );
  }

  protected askToRunRowAction(record: AdminRecord, action: AdminRowAction): void {
    if (this.runningActionRecordId() !== null) {
      return;
    }
    this.rowActionCandidate.set({ record, action });
  }

  protected cancelRowAction(): void {
    if (!this.runningActionRecordId()) {
      this.rowActionCandidate.set(null);
    }
  }

  /**
   * Confirms a row action. Nothing about the outcome is guessed here: the API is the only place
   * that knows whether credentials were generated and sent.
   */
  protected confirmRowAction(): void {
    const candidate = this.rowActionCandidate();
    if (!candidate || this.runningActionRecordId() !== null) {
      return;
    }

    const recordId = this.recordId(candidate.record);
    this.runningActionRecordId.set(recordId);
    this.adminApi.runRowAction(candidate.action.path(recordId)).pipe(
      this.notifications.trackApiCall({
        start: { key: 'notifications.admin.resettingPassword' },
        success: { key: 'notifications.admin.passwordResetSent' },
        error: (error: unknown) => this.tableRequestFailureMessage(error, 'save')
      }),
      finalize(() => this.runningActionRecordId.set(null))
    ).subscribe({
      next: () => {
        this.rowActionCandidate.set(null);
        this.loadRecords();
      }
    });
  }

  protected isRecordRunningAction(record: AdminRecord): boolean {
    return this.runningActionRecordId() === this.recordId(record);
  }

  protected cancelRemoval(): void {
    if (!this.removingRecordId()) {
      this.removalCandidate.set(null);
    }
  }

  protected confirmRemoval(): void {
    const definition = this.table();
    const candidate = this.removalCandidate();
    if (!definition || !candidate || this.removingRecordId() !== null) {
      return;
    }

    const recordId = this.recordId(candidate);
    this.removingRecordId.set(recordId);
    this.adminApi.removeRecord(definition.key, recordId).pipe(
      this.notifications.trackApiCall({
        start: { key: 'notifications.admin.removing' },
        success: { key: 'notifications.admin.removed' },
        error: (error: unknown) => this.tableRequestFailureMessage(error, 'remove')
      }),
      finalize(() => this.removingRecordId.set(null))
    ).subscribe({
      next: () => {
        this.removalCandidate.set(null);
        this.loadRecords();
      }
    });
  }

  protected recordValue(record: AdminRecord, key: string): unknown {
    return record[key];
  }

  protected formatValue(value: unknown, column: AdminTableColumn): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }

    switch (column.format) {
      case 'date':
        return this.formatDate(value);
      case 'boolean':
        return value === true ? this.i18n.t('dashboard.admin.values.yes') : this.i18n.t('dashboard.admin.values.no');
      case 'status':
        return this.statusLabel(value);
      default:
        return String(value);
    }
  }

  protected statusClass(value: unknown): string {
    const status = typeof value === 'string' ? value : '';
    if (status === 'ACTIF') {
      return 'bg-emerald-50 text-emerald-800';
    }
    if (status === 'SUSPENDU') {
      return 'bg-amber-50 text-amber-800';
    }
    if (status === 'EN_ATTENTE_CONFIRMATION') {
      return 'bg-sky-50 text-sky-800';
    }
    return 'bg-stone-100 text-stone-700';
  }

  protected canRemove(record: AdminRecord): boolean {
    const definition = this.table();
    return Boolean(
      definition?.removeActionKey
      && (!definition.canRemove || definition.canRemove(record, this.recordContext()))
    );
  }

  /**
   * A locked field keeps the record's current value — it is still submitted, because the API
   * expects the complete payload — but the operator cannot change it. It prevents a request the
   * API would always refuse, such as renaming a built-in role code or demoting the last
   * active administrator.
   */
  protected isFieldLocked(field: AdminTableField): boolean {
    const record = this.selectedRecord();
    if (this.editorMode() !== 'edit' || !record || !field.lockedWhen) {
      return false;
    }
    return field.lockedWhen(record, this.recordContext());
  }

  protected isRecordBeingRemoved(record: AdminRecord): boolean {
    return this.removingRecordId() === this.recordId(record);
  }

  protected recordId(record: AdminRecord | null): string {
    const definition = this.table();
    if (!definition || !record) {
      return '';
    }
    const rawId = record[definition.recordIdKey ?? 'id'];
    return typeof rawId === 'string' || typeof rawId === 'number' ? String(rawId) : '';
  }

  protected optionsFor(field: AdminTableField): readonly EditorOption[] {
    if (field.options) {
      return field.options.map((option) => ({ value: option.value, label: this.i18n.t(option.labelKey) }));
    }
    if (field.lookup === 'userTypes') {
      return this.withoutExcludedValues(this.userTypeOptions(), field);
    }
    if (field.lookup === 'basicRights') {
      return this.withoutExcludedValues(this.basicRightOptions(), field);
    }
    return [];
  }

  protected fieldsForCurrentEditor(): readonly AdminTableField[] {
    const definition = this.table();
    if (!definition || this.editorMode() === null) {
      return [];
    }
    return this.editorMode() === 'create' ? definition.createFields ?? [] : definition.editFields ?? [];
  }

  protected controlId(field: AdminTableField): string {
    return `admin-field-${field.key}`;
  }

  protected isSelect(field: AdminTableField): boolean {
    return field.control === 'select';
  }

  protected inputType(field: AdminTableField): AdminEditorControl {
    return field.control;
  }

  private configureEditor(mode: EditorMode, fields: readonly AdminTableField[], record: AdminRecord | null): void {
    const group = this.formBuilder.group({});
    for (const field of fields) {
      group.addControl(field.key, this.formBuilder.control(this.initialValue(field, record), this.validatorsFor(field)));
    }
    this.editorForm = group;
    this.selectedRecord.set(record);
    this.editorMode.set(mode);
  }

  private valuesFor(fields: readonly AdminTableField[]): Record<string, unknown> {
    const values: Record<string, unknown> = {};
    for (const field of fields) {
      values[field.key] = this.editorForm.get(field.key)?.value ?? '';
    }
    return values;
  }

  private initialValue(field: AdminTableField, record: AdminRecord | null): string {
    if (!record) {
      return field.key === 'statut' ? 'ACTIF' : '';
    }
    const value = record[field.key];
    return value === null || value === undefined ? '' : String(value);
  }

  private validatorsFor(field: AdminTableField): ValidatorFn[] {
    const validators: ValidatorFn[] = [];
    if (field.required) {
      validators.push(Validators.required);
    }
    if (field.minLength) {
      validators.push(Validators.minLength(field.minLength));
    }
    if (field.maxLength) {
      validators.push(Validators.maxLength(field.maxLength));
    }
    if (field.control === 'email') {
      validators.push(Validators.email);
    }
    if (field.pattern) {
      validators.push(Validators.pattern(field.pattern));
    }
    return validators;
  }

  private withoutExcludedValues(options: readonly EditorOption[], field: AdminTableField): readonly EditorOption[] {
    if (!field.excludeValues || field.excludeValues.length === 0) {
      return options;
    }
    return options.filter((option) => !field.excludeValues?.includes(option.value));
  }

  private loadEditorLookups(definition: AdminTableDefinition): void {
    const fields = [...(definition.createFields ?? []), ...(definition.editFields ?? [])];
    const needsUserTypes = fields.some((field) => field.lookup === 'userTypes');
    const needsBasicRights = fields.some((field) => field.lookup === 'basicRights');
    if (!needsUserTypes && !needsBasicRights) {
      return;
    }

    this.isLoadingLookups.set(true);
    let pendingRequests = Number(needsUserTypes) + Number(needsBasicRights);
    const completed = (): void => {
      pendingRequests -= 1;
      if (pendingRequests === 0) {
        this.isLoadingLookups.set(false);
      }
    };

    if (needsUserTypes) {
      this.adminApi.tableRows('type_utilisateur').pipe(finalize(completed)).subscribe({
        next: (response) => this.userTypeOptions.set(this.lookupOptions(response.records, 'code', 'name')),
        error: () => this.notifications.error({ key: 'notifications.admin.lookupFailed' })
      });
    }
    if (needsBasicRights) {
      this.adminApi.tableRows('basic_rights').pipe(finalize(completed)).subscribe({
        next: (response) => this.basicRightOptions.set(this.lookupOptions(response.records, 'code', 'name')),
        error: () => this.notifications.error({ key: 'notifications.admin.lookupFailed' })
      });
    }
  }

  private lookupOptions(records: readonly AdminRecord[], codeKey: string, nameKey: string): readonly EditorOption[] {
    return records.flatMap((record) => {
      const id = record['id'];
      const code = record[codeKey];
      const name = record[nameKey];
      if ((typeof id !== 'number' && typeof id !== 'string') || typeof code !== 'string' || typeof name !== 'string') {
        return [];
      }
      return [{ value: String(id), label: `${code} — ${name}` }];
    });
  }

  /**
   * A non-zero HTTP status proves the browser reached Spring. Distinguishing that from a status-0
   * CORS/network failure keeps administrators from treating a server-side schema error as a
   * frontend connectivity problem. The request ID is safe to display and can be matched in logs.
   *
   * A rejected business rule (409/400 answered with an API error code) is translated instead of
   * being reported as a failure: creating a buyer account by hand, renaming a built-in role or
   * demoting the last administrator all come back as an explicit, actionable message.
   */
  private tableRequestFailureMessage(error: unknown, operation: TableOperation): NotificationMessage {
    const fallbackKey = this.fallbackKeyFor(operation);
    if (!(error instanceof HttpErrorResponse)) {
      return { key: fallbackKey };
    }
    if (error.status === 0) {
      return { key: 'notifications.admin.connectionFailed' };
    }
    if (error.status === 401) {
      return { key: 'notifications.admin.sessionExpired' };
    }
    if (error.status === 403) {
      return { key: 'notifications.admin.accessDenied' };
    }
    if (error.status >= 500) {
      const requestId = error.headers.get('X-Request-Id');
      return requestId
        ? { key: 'notifications.admin.backendDataFailedWithRequestId', params: { requestId } }
        : { key: 'notifications.admin.backendDataFailed' };
    }

    const businessKey = businessErrorKeyFor(this.errorCodeOf(error));
    if (businessKey) {
      return { key: businessKey };
    }
    return { key: fallbackKey };
  }

  private errorCodeOf(error: HttpErrorResponse): unknown {
    // An API error body only carries the safe {code, message, timestamp} triple produced by the
    // backend exception handler; anything else is ignored.
    const body: unknown = error.error;
    return typeof body === 'object' && body !== null && 'code' in body
      ? (body as { code?: unknown }).code
      : null;
  }

  private fallbackKeyFor(operation: TableOperation): string {
    switch (operation) {
      case 'save':
        return 'notifications.admin.saveFailed';
      case 'remove':
        return 'notifications.admin.removeFailed';
      default:
        return 'notifications.admin.loadFailed';
    }
  }

  private formatDate(value: unknown): string {
    if (typeof value !== 'string') {
      return '—';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '—';
    }
    return new Intl.DateTimeFormat(this.i18n.language() === 'fr' ? 'fr-FR' : 'en-GB', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(date);
  }

  private statusLabel(value: unknown): string {
    switch (value) {
      case 'ACTIF':
        return this.i18n.t('dashboard.admin.statuses.active');
      case 'SUSPENDU':
        return this.i18n.t('dashboard.admin.statuses.suspended');
      case 'EN_ATTENTE_CONFIRMATION':
        return this.i18n.t('dashboard.admin.statuses.pending');
      default:
        return String(value);
    }
  }
}
