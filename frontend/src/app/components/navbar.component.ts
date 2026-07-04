import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService } from '../services/auth.service';
import { ThemeToggleComponent } from '../shared/theme/theme-toggle.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, MatButtonModule, MatToolbarModule, ThemeToggleComponent],
  template: `
    <mat-toolbar color="primary">
      <!-- App branding and navigation links -->
      <span class="brand-name" (click)="goToInterview()">AI Interview Prep</span>

      <div class="spacer"></div>

      <!-- Page navigation links — show based on current route -->
      <a [routerLink]="interviewRoute" mat-button *ngIf="authService.isAuthenticated$()" routerLinkActive="active-link">Interview</a>
      <a routerLink="/interview/my-sessions" mat-button *ngIf="authService.isAuthenticated$()" routerLinkActive="active-link">My Sessions</a>
      <a routerLink="/analytics" mat-button *ngIf="authService.isAuthenticated$()" routerLinkActive="active-link" [routerLinkActiveOptions]="{ exact: true }">Analytics</a>

      <!-- User info and logout -->
      <span class="user-name" *ngIf="authService.userName$()">{{ authService.userName$() }}</span>
      <app-theme-toggle></app-theme-toggle>
      <button mat-raised-button color="warn" *ngIf="authService.isAuthenticated$()" (click)="logout()">Logout</button>
    </mat-toolbar>
  `,
  styles: [`
    .spacer { flex: 1 1 auto; }
    .brand-name { font-size: 20px; font-weight: bold; margin-right: 32px; cursor: pointer; user-select: none; }
    .user-name { padding: 0 8px; color: white; margin-right: 8px; }
    .active-link { background-color: rgba(255, 255, 255, 0.15); font-weight: bold; }
  `]
})
export class NavbarComponent implements OnInit, OnDestroy {
  // Route to go to when clicking brand name — uses active session if available
  interviewRoute: any[] = ['/interview', 'setup'];

  private routeSub!: Subscription;

  constructor(
    public authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    // Subscribe to route events so navbar updates when user logs in/out or navigates between routes
    this.routeSub = this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        const activeSessionId = this.authService.getActiveSessionId?.() || null;
        if (activeSessionId && activeSessionId !== 'null' && activeSessionId !== '') {
          this.interviewRoute = ['/interview', Number(activeSessionId)];
        } else {
          this.interviewRoute = ['/interview', 'setup'];
        }
      }
    });
  }

  ngOnDestroy(): void {
    // Clean up subscription to prevent memory leaks
    if (this.routeSub) {
      this.routeSub.unsubscribe();
    }
  }

  goToInterview(): void {
    // Navigate to the active interview session or setup page based on current state
    if (this.interviewRoute.length > 1 && typeof this.interviewRoute[1] === 'number') {
      this.router.navigate(['/interview', this.interviewRoute[1]]);
    } else {
      this.router.navigate(['/interview', 'setup']);
    }
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        sessionStorage.removeItem('isAuthenticated');
        sessionStorage.removeItem('user_name');
        this.authService.isAuthenticated$.set(false);
        this.authService.userName$.set(null);
        this.router.navigate(['/login']);
      },
      error: (err) => {
        // Even if backend logout fails, clear local state and navigate away
        sessionStorage.removeItem('isAuthenticated');
        sessionStorage.removeItem('user_name');
        this.authService.isAuthenticated$.set(false);
        this.authService.userName$.set(null);
        this.router.navigate(['/login']);
      }
    });
  }
}
