import { ApplicationConfig } from '@angular/core';
import { provideRouter, withPreloading } from '@angular/router';
import { routes } from './app.routes';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { CsrfTokenInterceptor } from './interceptors/csrf.interceptor';
import { IdlePreloadingStrategy } from './guards/idle-preloading.strategy';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withPreloading(IdlePreloadingStrategy)),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptorsFromDi()),
    provideCharts(withDefaultRegisterables()),
    // Register the CSRF interceptor as a multi-provider so it is applied to all HTTP requests
    { provide: HTTP_INTERCEPTORS, useClass: CsrfTokenInterceptor, multi: true },
  ],
};
