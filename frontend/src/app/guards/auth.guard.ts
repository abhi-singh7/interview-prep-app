import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const AuthGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);

  // The HttpOnly jwt_token cookie cannot be read via document.cookie.
  // We track auth state in sessionStorage after login/register succeeds.
  if (isAuthenticated()) {
    return true;
  } else {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }
};

function isAuthenticated(): boolean {
  // Check sessionStorage for auth flag set after successful login/register.
  return sessionStorage.getItem('isAuthenticated') === 'true';
}
