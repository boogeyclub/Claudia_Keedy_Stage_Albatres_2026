import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { AdminDashboardComponent } from './admin-dashboard';

describe('AdminDashboardComponent', () => {
  it('renders one protected management card for each gu schema table', async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    const i18n = TestBed.inject(TranslationService);
    i18n.setLanguage('en');
    const fixture = TestBed.createComponent(AdminDashboardComponent);
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    const cards = nativeElement.querySelectorAll('[data-testid="admin-table-card"]');
    const links = nativeElement.querySelectorAll('a[href*="/dashboard/admin/tables/"]');

    expect(cards.length).toBe(10);
    expect(links.length).toBe(10);
    expect(nativeElement.textContent).toContain('Manage the protected gu data tables');
    expect(TestBed.inject(Title).getTitle()).toBe('CacaoMarketCM | Administrator workspace');
  });
});
