import { Injectable, ChangeDetectorRef } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ViewRestorerService {
  /**
   * Apply a value to a child component and trigger change detection on the
   * host. Used as a drop-in replacement for `setTimeout(() => ..., 0)` which
   * was previously used to force Angular's CD cycle after setting ViewChild
   * properties directly (which bypasses its dirty-checking).
   */
  applyAndRefresh<T>(component: T, cdr: ChangeDetectorRef, setter: (c: T) => void): void {
    setter(component);
    Promise.resolve().then(() => {
      cdr.markForCheck();
      cdr.detectChanges();
    });
  }

  /**
   * Synchronously apply a value and immediately trigger CD on the host. Use
   * this when you want the change reflected in the DOM without waiting for
   * Angular's next tick (e.g., during view restoration on route entry).
   */
  applyAndDetect<T>(component: T, cdr: ChangeDetectorRef, setter: (c: T) => void): void {
    setter(component);
    cdr.markForCheck();
  }
}
