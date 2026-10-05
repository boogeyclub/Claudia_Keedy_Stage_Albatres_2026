import { SupportedLanguage } from '../i18n/translation.service';

/**
 * Locale-aware formatting of the values coming from the API.
 *
 * Amounts arrive as numbers, dates as ISO strings; the pages never print a raw ISO value.
 */

const LOCALES: Readonly<Record<SupportedLanguage, string>> = {
  en: 'en-GB',
  fr: 'fr-FR'
};

function locale(language: SupportedLanguage): string {
  return LOCALES[language];
}

export function formatAmount(value: number | null | undefined, language: SupportedLanguage): string {
  if (value === null || value === undefined) {
    return '—';
  }

  return new Intl.NumberFormat(locale(language), { maximumFractionDigits: 2 }).format(value);
}

export function formatPrice(
  value: number | null | undefined,
  devise: string | null | undefined,
  language: SupportedLanguage
): string {
  const amount = formatAmount(value, language);
  return devise ? `${amount} ${devise}` : amount;
}

export function formatKilograms(value: number | null | undefined, language: SupportedLanguage): string {
  if (value === null || value === undefined) {
    return '—';
  }
  return `${new Intl.NumberFormat(locale(language), { maximumFractionDigits: 2 }).format(value)} kg`;
}

export function formatDate(value: string | null | undefined, language: SupportedLanguage): string {
  const date = toDate(value);
  if (!date) {
    return '—';
  }
  return new Intl.DateTimeFormat(locale(language), { dateStyle: 'medium' }).format(date);
}

export function formatDateTime(value: string | null | undefined, language: SupportedLanguage): string {
  const date = toDate(value);
  if (!date) {
    return '—';
  }
  return new Intl.DateTimeFormat(locale(language), { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

/** Value for an `<input type="datetime-local">`, expressed in the browser's time zone. */
export function toDateTimeLocalValue(date: Date): string {
  const pad = (part: number): string => String(part).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** Suggests a visit two days from now, at 10:00 local time, for the appointment form. */
export function suggestedVisitSlot(): string {
  const suggestion = new Date();
  suggestion.setDate(suggestion.getDate() + 2);
  suggestion.setHours(10, 0, 0, 0);
  return toDateTimeLocalValue(suggestion);
}

function toDate(value: string | null | undefined): Date | null {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}
