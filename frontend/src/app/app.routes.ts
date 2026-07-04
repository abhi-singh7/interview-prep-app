import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./auth/register/register.component').then(m => m.RegisterComponent) },
  {
    path: 'interview', canActivate: [AuthGuard], children: [
      { path: '', redirectTo: '/interview/setup', pathMatch: 'full' },
      { path: 'setup', loadComponent: () => import('./interview/setup-page/interview-setup.component').then(m => m.InterviewSetupComponent) },
      { path: 'my-sessions', loadComponent: () => import('./interview/my-sessions/my-sessions.component').then(m => m.MySessionsComponent) },
      { path: ':sessionId', loadComponent: () => import('./interview/interview-page/interview-page.component').then(m => m.InterviewPageComponent) },
      { path: 'detail/:sessionId', loadComponent: () => import('./interview/session-detail/session-detail.component').then(m => m.SessionDetailComponent) },
    ]
  },
  { path: 'results/:sessionId', canActivate: [AuthGuard], loadComponent: () => import('./evaluation/results-page/results-page.component').then(m => m.ResultsPageComponent) },
  { path: 'analytics', canActivate: [AuthGuard], loadComponent: () => import('./analytics/analytics-page/analytics-page.component').then(m => m.AnalyticsPageComponent) },
  { path: '**', redirectTo: '/login' },
];
