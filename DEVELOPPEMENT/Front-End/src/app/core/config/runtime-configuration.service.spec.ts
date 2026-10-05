import { RuntimeConfigurationService } from './runtime-configuration.service';

describe('RuntimeConfigurationService', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('loads and normalizes the public API base URL', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ apiBaseUrl: 'https://api.example.test/cacaomarketcm/api/' }),
    });
    vi.stubGlobal('fetch', fetchMock);
    const service = new RuntimeConfigurationService();

    await service.load();

    expect(fetchMock).toHaveBeenCalledWith(new URL('config.json', document.baseURI).toString(), {
      cache: 'no-store',
    });
    expect(service.apiBaseUrl).toBe('https://api.example.test/cacaomarketcm/api');
  });

  it('rejects an unsafe or unsupported API base URL', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({ apiBaseUrl: '//untrusted.example.test/api' }),
      }),
    );
    const service = new RuntimeConfigurationService();

    await expect(service.load()).rejects.toThrow(
      'apiBaseUrl must be an absolute HTTP(S) URL or an origin-relative path',
    );
  });
});
