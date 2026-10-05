import { Component, effect, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { TranslationService } from '../../core/i18n/translation.service';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher';

@Component({
  selector: 'app-landing',
  imports: [LanguageSwitcherComponent, RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css'
})
export class LandingComponent {
  protected readonly i18n = inject(TranslationService);
  private readonly title = inject(Title);

  protected readonly journeySteps = [
    {
      number: '01',
      titleKey: 'landing.process.steps.publish.title',
      descriptionKey: 'landing.process.steps.publish.description'
    },
    {
      number: '02',
      titleKey: 'landing.process.steps.demand.title',
      descriptionKey: 'landing.process.steps.demand.description'
    },
    {
      number: '03',
      titleKey: 'landing.process.steps.clarity.title',
      descriptionKey: 'landing.process.steps.clarity.description'
    }
  ];

  protected readonly marketAdvantages = [
    {
      titleKey: 'landing.value.advantages.volume.title',
      descriptionKey: 'landing.value.advantages.volume.description',
      icon: 'layers'
    },
    {
      titleKey: 'landing.value.advantages.signals.title',
      descriptionKey: 'landing.value.advantages.signals.description',
      icon: 'signal'
    },
    {
      titleKey: 'landing.value.advantages.matches.title',
      descriptionKey: 'landing.value.advantages.matches.description',
      icon: 'handshake'
    }
  ];

  constructor() {
    effect(() => this.title.setTitle(this.i18n.t('meta.landingTitle')));
  }
}
