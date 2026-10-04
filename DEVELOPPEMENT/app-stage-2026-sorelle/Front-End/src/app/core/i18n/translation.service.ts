import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, computed, effect, inject, signal } from '@angular/core';
import { TRANSLATIONS, TranslationDictionary, TranslationNode } from './translations';

export type SupportedLanguage = 'en' | 'fr';
export type TranslationParams = Record<string, string | number>;

export interface LanguageOption {
  code: SupportedLanguage;
  labelKey: string;
  shortLabelKey: string;
}

const LANGUAGE_STORAGE_KEY = 'cacaomarket.language';
const FALLBACK_LANGUAGE: SupportedLanguage = 'en';

const LANGUAGE_OPTIONS: readonly LanguageOption[] = [
  { code: 'en', labelKey: 'common.languageNames.english', shortLabelKey: 'common.languageCodes.english' },
  { code: 'fr', labelKey: 'common.languageNames.french', shortLabelKey: 'common.languageCodes.french' }
];

@Injectable({ providedIn: 'root' })
export class TranslationService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly document = inject(DOCUMENT);

  readonly languages = LANGUAGE_OPTIONS;
  readonly language = signal<SupportedLanguage>(this.getInitialLanguage());
  readonly isFrench = computed(() => this.language() === 'fr');

  constructor() {
    effect(() => {
      const language = this.language();

      this.document.documentElement.lang = language;
      this.document.documentElement.dir = 'ltr';

      if (isPlatformBrowser(this.platformId)) {
        localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
      }
    });
  }

  setLanguage(language: SupportedLanguage): void {
    this.language.set(language);
  }

  toggleLanguage(): void {
    this.setLanguage(this.language() === 'en' ? 'fr' : 'en');
  }

  t(key: string, params: TranslationParams = {}): string {
    const language = this.language();
    const value = this.getValue(TRANSLATIONS[language], key)
      ?? this.getValue(TRANSLATIONS[FALLBACK_LANGUAGE], key)
      ?? key;

    return this.interpolate(value, params);
  }

  private getInitialLanguage(): SupportedLanguage {
    if (!isPlatformBrowser(this.platformId)) {
      return FALLBACK_LANGUAGE;
    }

    const storedLanguage = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (storedLanguage === 'en' || storedLanguage === 'fr') {
      return storedLanguage;
    }

    return navigator.language.toLowerCase().startsWith('fr') ? 'fr' : FALLBACK_LANGUAGE;
  }

  private getValue(dictionary: TranslationDictionary, key: string): string | undefined {
    const value = key.split('.').reduce<TranslationNode | undefined>((node, segment) => {
      if (typeof node !== 'object' || node === null) {
        return undefined;
      }

      return node[segment];
    }, dictionary);

    return typeof value === 'string' ? value : undefined;
  }

  private interpolate(value: string, params: TranslationParams): string {
    return value.replace(/{{\s*(\w+)\s*}}/g, (match, parameter: string) => String(params[parameter] ?? match));
  }
}
