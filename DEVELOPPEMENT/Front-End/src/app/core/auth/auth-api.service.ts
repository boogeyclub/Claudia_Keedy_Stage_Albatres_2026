import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RuntimeConfigurationService } from '../config/runtime-configuration.service';

export type RegistrableUserRole = 'VENDEUR' | 'CLIENT';
export type ClientProfileType = 'PARTICULIER' | 'ENTREPRISE';
export type AuthenticatedUserRole = RegistrableUserRole | 'ADMINISTRATEUR';
export type InterfaceLanguage = 'en' | 'fr';

export interface RegistrationPayload {
  role: RegistrableUserRole;
  /** Required only when role is CLIENT. */
  clientProfileType?: ClientProfileType;
  /** Required only for a CLIENT with clientProfileType ENTREPRISE. */
  raisonSociale?: string;
  niu?: string;
  rccm?: string;
  prenom: string;
  nom: string;
  email: string;
  login: string;
  password: string;
  confirmPassword: string;
  acceptTerms: boolean;
  language: InterfaceLanguage;
}

export interface RegistrationAcceptedResponse {
  email: string;
  expiresAt: string;
}

export interface ConfirmationResponse {
  status: 'CONFIRMED' | 'ALREADY_CONFIRMED';
  message: string;
}

export interface PasswordResetRequestPayload {
  email: string;
  language: InterfaceLanguage;
}

export interface PasswordResetRequestAcceptedResponse {
  message: string;
}

export interface PasswordResetConfirmationPayload {
  token: string;
  password: string;
  confirmPassword: string;
}

export interface PasswordResetConfirmationResponse {
  status: 'RESET';
  message: string;
}

export interface LoginPayload {
  identity: string;
  password: string;
  rememberMe: boolean;
}

export interface AuthenticatedUser {
  id: number;
  email: string;
  login: string;
  prenom: string;
  nom: string;
  role: AuthenticatedUserRole;
}

export interface BrowserSession {
  id: number;
  browserLabel: string;
  rememberMe: boolean;
  createdAt: string;
  lastSeenAt: string;
  expiresAt: string;
  current: boolean;
}

export interface ApiErrorResponse {
  code: string;
  message: string;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly runtimeConfiguration = inject(RuntimeConfigurationService);

  private get apiRoot(): string {
    return this.runtimeConfiguration.apiBaseUrl;
  }

  register(payload: RegistrationPayload): Observable<RegistrationAcceptedResponse> {
    return this.http.post<RegistrationAcceptedResponse>(`${this.apiRoot}/auth/registration`, payload, { withCredentials: true });
  }

  confirmRegistration(token: string): Observable<ConfirmationResponse> {
    return this.http.get<ConfirmationResponse>(`${this.apiRoot}/auth/registration/confirm`, {
      params: { token },
      withCredentials: true
    });
  }

  requestPasswordReset(payload: PasswordResetRequestPayload): Observable<PasswordResetRequestAcceptedResponse> {
    return this.http.post<PasswordResetRequestAcceptedResponse>(`${this.apiRoot}/auth/password-reset/request`, payload, { withCredentials: true });
  }

  confirmPasswordReset(payload: PasswordResetConfirmationPayload): Observable<PasswordResetConfirmationResponse> {
    return this.http.post<PasswordResetConfirmationResponse>(`${this.apiRoot}/auth/password-reset/confirm`, payload, { withCredentials: true });
  }

  login(payload: LoginPayload): Observable<AuthenticatedUser> {
    return this.http.post<AuthenticatedUser>(`${this.apiRoot}/auth/login`, payload, { withCredentials: true });
  }

  currentSession(): Observable<AuthenticatedUser> {
    return this.http.get<AuthenticatedUser>(`${this.apiRoot}/auth/session`, { withCredentials: true });
  }

  browserSessions(): Observable<readonly BrowserSession[]> {
    return this.http.get<readonly BrowserSession[]>(`${this.apiRoot}/auth/sessions`, { withCredentials: true });
  }

  disconnectBrowserSession(sessionId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiRoot}/auth/sessions/${sessionId}`, { withCredentials: true });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiRoot}/auth/logout`, {}, { withCredentials: true });
  }
}
