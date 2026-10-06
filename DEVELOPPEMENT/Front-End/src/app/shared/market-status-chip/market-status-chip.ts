import { Component, computed, inject, input } from '@angular/core';
import { TranslationService } from '../../core/i18n/translation.service';
import { MarketStatusTone } from '../../core/market/market-status';

const TONE_CLASSES: Readonly<Record<MarketStatusTone, string>> = {
  neutral: 'bg-cacao-pale text-cacao-roast ring-cacao-gold/30',
  info: 'bg-sky-50 text-sky-800 ring-sky-200',
  pending: 'bg-amber-50 text-amber-800 ring-amber-200',
  success: 'bg-emerald-50 text-emerald-800 ring-emerald-200',
  danger: 'bg-rose-50 text-rose-800 ring-rose-200',
  muted: 'bg-stone-100 text-stone-600 ring-stone-200'
};

/** Translated status badge shared by the catalogue and the conversation pages. */
@Component({
  selector: 'app-market-status-chip',
  templateUrl: './market-status-chip.html',
  styleUrl: './market-status-chip.css'
})
export class MarketStatusChipComponent {
  readonly labelKey = input.required<string>();
  readonly tone = input<MarketStatusTone>('neutral');

  protected readonly i18n = inject(TranslationService);
  protected readonly toneClass = computed(() => TONE_CLASSES[this.tone()]);
}
