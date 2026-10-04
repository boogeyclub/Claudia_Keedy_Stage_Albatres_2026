# CacaoMarketCM localization

Use the built-in `TranslationService` for all user-visible text in current and future Angular components, including page titles, form labels, placeholders, error messages, image alternative text, button labels, language-selector accessibility labels, and branded UI labels.

## Add translated text

1. Add matching English and French values to `translations.ts`.
2. Inject `TranslationService` into a standalone component:

```ts
protected readonly i18n = inject(TranslationService);
```

3. Read text in the template with `i18n.t('feature.section.key')`.

The service uses Angular signals, so visible text updates immediately when the selected language changes. It also saves the selected language in local storage and updates the document's `lang` attribute.

## Reuse the language selector

Add the standalone selector to a component's `imports` and template:

```ts
imports: [LanguageSwitcherComponent]
```

```html
<app-language-switcher />
```

The current site supports `en` and `fr`. Use English as the fallback for a missing key.
