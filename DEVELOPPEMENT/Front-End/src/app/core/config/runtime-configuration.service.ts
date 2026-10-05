import { Injectable } from '@angular/core';

export interface RuntimeConfiguration {
  apiBaseUrl: string;
}

const DEFAULT_CONFIGURATION: RuntimeConfiguration = {
  apiBaseUrl: '/cacaomarketcm/api',
};

/**
 * Loads browser-visible deployment settings before Angular bootstraps. Keeping this value in the
 * public asset makes the API location configurable without rebuilding the JavaScript bundles.
 */
@Injectable({ providedIn: 'root' })
export class RuntimeConfigurationService {
  private configuration: RuntimeConfiguration = DEFAULT_CONFIGURATION;

  get apiBaseUrl(): string {
    return this.configuration.apiBaseUrl;
  }

  async load(): Promise<void> {
    const configurationUrl = new URL('config.json', document.baseURI).toString();
    let response: Response;

    try {
      response = await fetch(configurationUrl, { cache: 'no-store' });
    } catch {
      throw new Error(`Unable to load the runtime configuration at ${configurationUrl}.`);
    }

    if (!response.ok) {
      throw new Error(
        `Unable to load the runtime configuration at ${configurationUrl} (HTTP ${response.status}).`,
      );
    }

    let payload: unknown;
    try {
      payload = await response.json();
    } catch {
      throw new Error('The runtime configuration is not valid JSON.');
    }

    this.configuration = this.parseConfiguration(payload);
  }

  private parseConfiguration(payload: unknown): RuntimeConfiguration {
    if (typeof payload !== 'object' || payload === null || Array.isArray(payload)) {
      throw new Error('The runtime configuration must be a JSON object.');
    }

    const values = payload as Record<string, unknown>;
    const apiBaseUrl = values['apiBaseUrl'];
    if (typeof apiBaseUrl !== 'string') {
      throw new Error('The runtime configuration must define apiBaseUrl as a string.');
    }

    const normalizedApiBaseUrl = apiBaseUrl.trim().replace(/\/+$/, '');
    if (!normalizedApiBaseUrl || !this.isSupportedApiBaseUrl(normalizedApiBaseUrl)) {
      throw new Error(
        'The runtime configuration apiBaseUrl must be an absolute HTTP(S) URL or an origin-relative path.',
      );
    }

    return { apiBaseUrl: normalizedApiBaseUrl };
  }

  private isSupportedApiBaseUrl(apiBaseUrl: string): boolean {
    return (
      (!apiBaseUrl.startsWith('//') && apiBaseUrl.startsWith('/')) ||
      /^https?:\/\//i.test(apiBaseUrl)
    );
  }
}
