import { ApplicationConfig } from '@angular/core';
import { provideRouter, withPreloading } from '@angular/router';
import { routes } from './app.routes';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { IdlePreloadingStrategy } from './guards/idle-preloading.strategy';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withPreloading(IdlePreloadingStrategy)),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptorsFromDi()),
    provideCharts(withDefaultRegisterables()),
    // REMOVED: the CSRF interceptor never worked — it read the HttpOnly "_csrf" cookie via
    // document.cookie (always null) and the backend never validates X-XSRF-TOKEN. The backend
    // now enforces a same-site Origin check on state-changing requests (JwtAuthenticationFilter),
    // alongside the SameSite=Strict jwt_token cookie, so no client-side token plumbing is needed.
  ],
};
