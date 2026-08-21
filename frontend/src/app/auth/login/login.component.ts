import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSnackBarModule],
  template: `
    <div class="login-container">
      <mat-card>
        <mat-card-header><mat-card-title>Login</mat-card-title></mat-card-header>
        <mat-card-content>
          <form [formGroup]="loginForm" (ngSubmit)="onLogin()">
            <mat-form-field appearance="fill" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" required>
              <mat-error *ngIf="loginForm.get('email')?.hasError('required')">Email is required</mat-error>
            </mat-form-field>

            <mat-form-field appearance="fill" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput formControlName="password" type="password" required>
              <mat-error *ngIf="loginForm.get('password')?.hasError('required')">Password is required</mat-error>
            </mat-form-field>

            <button mat-raised-button color="primary" type="submit" [disabled]="loading">
              {{ loading ? 'Logging in...' : 'Login' }}
            </button>
          </form>
        </mat-card-content>
        <mat-card-actions class="centered">
          Don't have an account? <a routerLink="/register">Register</a>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container { display: flex; justify-content: center; align-items: center; min-height: calc(100vh - 64px); }
    mat-card { max-width: 400px; width: 100%; padding: 20px; }
    .full-width { width: 100%; margin-bottom: 16px; }
    .centered { justify-content: center; display: flex; }
    button[type="submit"] { width: 100%; }
  `]
})
export class LoginComponent {
  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  loading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar,
  ) {}

  onLogin(): void {
    if (this.loginForm.invalid) return;
    this.loading = true;
    const { email, password } = this.loginForm.value;
    this.authService.login(email!, password!).subscribe({
      next: (res) => {
        // Set auth state after successful login.
        // The HttpOnly jwt_token cookie is set by the backend and sent automatically with requests.
        const userName = res?.name || res?.user?.name || 'User';
        this.authService.setAuthState(userName);
        this.snackBar.open('Logged in successfully', 'Close', { duration: 3000 });
        this.router.navigate(['/interview']);
      },
      error: (err) => {
        const msg = err.error?.error || 'Invalid credentials';
        this.snackBar.open(msg, 'Close', { duration: 5000 });
        this.loading = false;
      }
    });
  }
}
