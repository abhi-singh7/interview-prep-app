import { Injectable } from '@angular/core';
import { PreloadingStrategy, Route } from '@angular/router';
import { Observable, of } from 'rxjs';

/**
 * Preload all lazy-loaded route modules when the app becomes idle (after the
 * initial render stabilises). This gives the same UX benefit as "PreloadAllModules"
 * but without blocking the first paint — users see the current page instantly and
 * subsequent navigations feel instant because the bundles are already in memory.
 */
@Injectable({ providedIn: 'root' })
export class IdlePreloadingStrategy implements PreloadingStrategy {
  preload(route: Route, load: () => Observable<unknown>): Observable<unknown> {
    return new Observable(observer => {
      const timer = setTimeout(() => {
        load().subscribe({
          next: (value) => observer.next(value),
          error: err => observer.error(err),
          complete: () => observer.complete(),
        });
      }, 0);

      return () => clearTimeout(timer);
    });
  }
}
