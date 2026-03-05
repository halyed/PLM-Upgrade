import { APP_INITIALIZER, ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { KeycloakAngularModule, KeycloakService } from 'keycloak-angular';
import { routes } from './app.routes';
import { initializeKeycloak } from './core/keycloak.init';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    // KeycloakAngularModule registers KeycloakService + bearer interceptor
    importProvidersFrom(KeycloakAngularModule),
    // provideHttpClient without custom interceptors — Keycloak handles bearer tokens
    provideHttpClient(),
    provideAnimations(),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService],
    },
  ],
};
