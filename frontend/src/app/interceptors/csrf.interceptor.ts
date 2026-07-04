import { Injectable } from '@angular/core';
import { HttpEvent, HttpInterceptor, HttpRequest, HttpResponse, HttpHandler } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable()
export class CsrfTokenInterceptor implements HttpInterceptor {
  private csrfToken: string | null = null;

  constructor() {
    // Try to read the CSRF token cookie on first request
    this.readCsrfFromCookie();
  }

  private readCsrfFromCookie(): void {
    const value = `; ${document.cookie}`;
    const parts = value.split('; _csrf=');
    if (parts.length === 2) {
      this.csrfToken = parts.pop()?.split(';').shift() ?? null;
    }
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // For GET requests, the CSRF token is sent as a cookie — no header needed
    if (req.method === 'GET') {
      return next.handle(req);
    }

    // Add the X-CSRF-TOKEN header for non-GET requests
    const csrfReq = req.clone({
      headers: req.headers.set('X-XSRF-TOKEN', this.csrfToken || ''),
    });

    return next.handle(csrfReq).pipe(
      tap((event) => {
        // If we get a 403 CSRF error, try reading the token again and retry
        if (event instanceof HttpResponse && event.status === 403) {
          this.readCsrfFromCookie();
          const updatedReq = req.clone({
            headers: req.headers.set('X-XSRF-TOKEN', this.csrfToken || ''),
          });
          // Note: In production, you'd want to retry the request here
        }
      })
    );
  }
}
