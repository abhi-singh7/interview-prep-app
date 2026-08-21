import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = 'api/auth';

  readonly isAuthenticated$ = signal<boolean>(sessionStorage.getItem('isAuthenticated') === 'true');
  readonly userName$ = signal<string | null>(sessionStorage.getItem('user_name'));

  /** Get the active interview session ID if one exists */
  getActiveSessionId(): string | null {
    return sessionStorage.getItem('interview_session_id');
  }

  constructor(private http: HttpClient) {}

  register(name: string, email: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, { name, email, password });
  }

  login(email: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, { email, password });
  }

  logout(): Observable<any> {
    sessionStorage.removeItem('isAuthenticated');
    sessionStorage.removeItem('user_name');
    this.isAuthenticated$.set(false);
    this.userName$.set(null);
    return this.http.post(`${this.apiUrl}/logout`, {});
  }

  isLoggedIn(): boolean {
    // The HttpOnly jwt_token cookie cannot be read via document.cookie.
    // Check sessionStorage flag set after successful login/register.
    const isAuth = sessionStorage.getItem('isAuthenticated') === 'true';
    if (!isAuth) {
      // If no local flag, verify with the backend by calling a protected endpoint.
      this.verifyWithBackend().then(verified => {
        if (verified) {
          sessionStorage.setItem('isAuthenticated', 'true');
          this.isAuthenticated$.set(true);
        } else {
          sessionStorage.removeItem('isAuthenticated');
          this.isAuthenticated$.set(false);
        }
      });
    }
    return isAuth;
  }

  /**
   * Verify authentication status by calling a protected endpoint.
   * The HttpOnly cookie is sent automatically with the request.
   */
  private async verifyWithBackend(): Promise<boolean> {
    try {
      await firstValueFrom(this.http.get(`${this.apiUrl}/status`));
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get the current user's name from sessionStorage.
   */
  getUserName(): string | null {
    return sessionStorage.getItem('user_name');
  }

  /**
   * Set auth state after successful login/register.
   */
  setAuthState(name: string): void {
    sessionStorage.setItem('isAuthenticated', 'true');
    sessionStorage.setItem('user_name', name);
    this.isAuthenticated$.set(true);
    this.userName$.set(name);
  }
}
