import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { KeycloakProfile } from 'keycloak-js';

@Injectable({ providedIn: 'root' })
export class AuthService {

  constructor(private keycloak: KeycloakService) {}

  isLoggedIn(): boolean {
    return this.keycloak.isLoggedIn();
  }

  getUsername(): string {
    const token = this.keycloak.getKeycloakInstance().tokenParsed as any;
    return token?.preferred_username ?? '';
  }

  async getToken(): Promise<string> {
    return this.keycloak.getToken();
  }

  async loadProfile(): Promise<KeycloakProfile> {
    return this.keycloak.loadUserProfile();
  }

  hasRole(role: string): boolean {
    return this.keycloak.isUserInRole(role);
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  isViewer(): boolean {
    return this.hasRole('VIEWER');
  }

  login(): void {
    this.keycloak.login({ redirectUri: window.location.origin });
  }

  logout(): void {
    this.keycloak.logout(window.location.origin);
  }
}
