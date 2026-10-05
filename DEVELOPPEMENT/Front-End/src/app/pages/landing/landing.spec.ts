import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { TranslationService } from '../../core/i18n/translation.service';
import { LandingComponent } from './landing';

describe('LandingComponent', () => {
  async function createComponent() {
    await TestBed.configureTestingModule({
      imports: [LandingComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    const i18n = TestBed.inject(TranslationService);
    i18n.setLanguage('en');

    const fixture = TestBed.createComponent(LandingComponent);
    fixture.detectChanges();

    return { fixture, i18n };
  }

  it('renders the CacaoMarketCM landing heading', async () => {
    const { fixture } = await createComponent();
    const heading = fixture.nativeElement.querySelector('[data-testid="landing-title"]') as HTMLElement;

    expect(heading.textContent).toContain('Where cocoa harvests meet');

    const volumeVisibilityCard = fixture.nativeElement.querySelector(
      '[data-testid="hero-volume-visibility-card"]'
    ) as HTMLElement;
    expect(volumeVisibilityCard.textContent).toContain('Trade begins with');
    expect(volumeVisibilityCard.classList).not.toContain('absolute');
  });

  it('switches visible landing content to French from the language selector', async () => {
    const { fixture } = await createComponent();
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const frenchButton = buttons.find((button) => button.textContent?.trim() === 'FR');

    frenchButton?.click();
    fixture.detectChanges();

    const heading = fixture.nativeElement.querySelector('[data-testid="landing-title"]') as HTMLElement;
    expect(heading.textContent).toContain('Là où les récoltes de cacao rencontrent');
    expect(TestBed.inject(Title).getTitle()).toBe('CacaoMarketCM | Le commerce du cacao en toute confiance');
  });
});
