import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminApiService, AdminRecord } from '../../../../core/admin/admin-api.service';
import {
  AdminEditorControl,
  AdminTableColumn,
  AdminTableDefinition,
  AdminTableField,
  adminTableForKey
} from '../../../../core/admin/admin-table-catalog';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { NotificationMessage, NotificationService } from '../../../../core/notifications/notification.service';

interface EditorOption {
  value: string;
  label: string;
}

type EditorMode = 'create' | 'edit';

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
  protected readonly userTypeOptions = signal<readonly EditorOption[]>([]);
  protected readonly basicRightOptions = signal<readonly EditorOption[]>([]);
  protected editorForm: UntypedFormGroup = new UntypedFormGroup({});

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly adminApi = inject(AdminApiService);
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
        const message = this.tableRequestFailureMessage(error);
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
        error: (error: unknown) => this.tableRequestFailureMessage(error)
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
        error: (error: unknown) => this.tableRequestFailureMessage(error)
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
    return Boolean(definition?.removeActionKey && (!definition.canRemove || definition.canRemove(record)));
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
      return this.userTypeOptions();
    }
    if (field.lookup === 'basicRights') {
      return this.basicRightOptions();
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
    return validators;
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
   * The same classification is used for reads and controlled administrator mutations.
   */
  private tableRequestFailureMessage(error: unknown): NotificationMessage {
    if (!(error instanceof HttpErrorResponse)) {
      return { key: 'notifications.admin.loadFailed' };
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
    return { key: 'notifications.admin.loadFailed' };
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
