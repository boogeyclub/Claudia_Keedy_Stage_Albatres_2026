import { TestBed } from '@angular/core/testing';
import { TranslationDictionary, TRANSLATIONS } from './translations';
import { TranslationService } from './translation.service';

function leafKeys(dictionary: TranslationDictionary, prefix = ''): string[] {
  return Object.entries(dictionary).flatMap(([key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key;

    return typeof value === 'string'
      ? [path]
      : leafKeys(value as TranslationDictionary, path);
  });
}

describe('TranslationService', () => {
  let service: TranslationService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(TranslationService);
  });

  it('uses English translations by default', () => {
    service.setLanguage('en');

    expect(service.t('landing.hero.titleAccent')).toBe('serious demand.');
  });

  it('switches to French and updates translated values', () => {
    service.setLanguage('fr');

    expect(service.t('landing.hero.titleAccent')).toBe('une demande sérieuse.');
    expect(service.language()).toBe('fr');
  });

  it('returns the key when no translation exists', () => {
    service.setLanguage('fr');

    expect(service.t('missing.translation.key')).toBe('missing.translation.key');
  });

  it('keeps the English and French translation keys aligned', () => {
    expect(leafKeys(TRANSLATIONS.en).sort()).toEqual(leafKeys(TRANSLATIONS.fr).sort());
  });
});
