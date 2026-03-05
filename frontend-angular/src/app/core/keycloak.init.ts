import { KeycloakService } from 'keycloak-angular';

export function initializeKeycloak(keycloak: KeycloakService): () => Promise<boolean> {
  return () =>
    keycloak.init({
      config: {
        url: (window as any).__env?.keycloakUrl ?? 'http://localhost:8081',
        realm: 'plm',
        clientId: 'plm-frontend',
      },
      initOptions: {
        // check-sso: silently checks if already logged in; does NOT force login on app load
        // The AuthGuard triggers login() when a protected route is accessed
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/assets/silent-check-sso.html',
      },
      // Automatically attaches Bearer token to all HTTP requests
      enableBearerInterceptor: true,
      bearerPrefix: 'Bearer',
      // Only attach token to calls going to /api (not external CDNs etc.)
      bearerExcludedUrls: ['/assets'],
    });
}
