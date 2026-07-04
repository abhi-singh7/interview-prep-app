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
  selector: 'app-register',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSnackBarModule],
  template: `
    <div class="register-container">
      <mat-card>
        <mat-card-header><mat-card-title>Register</mat-card-title></mat-card-header>
        <mat-card-content>
          <form [formGroup]="registerForm" (ngSubmit)="onRegister()">
            <mat-form-field appearance="fill" class="full-width">
              <mat-label>Name</mat-label>
              <input matInput formControlName="name" required>
              <mat-error *ngIf="registerForm.get('name')?.hasError('required')">Name is required</mat-error>
            </mat-form-field>

            <mat-form-field appearance="fill" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" required>
              <mat-error *ngIf="registerForm.get('email')?.hasError('required')">Email is required</mat-error>
            </mat-form-field>

            <mat-form-field appearance="fill" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput formControlName="password" type="password" required>
              <mat-error *ngIf="registerForm.get('password')?.hasError('required')">Password is required</mat-error>
            </mat-form-field>

            <mat-form-field appearance="fill" class="full-width">
              <mat-label>Confirm Password</mat-label>
              <input matInput formControlName="confirmPassword" type="password" required>
              <mat-error *ngIf="registerForm.get('confirmPassword')?.hasError('required')">Please confirm password</mat-error>
            </mat-form-field>

            <button mat-raised-button color="primary" type="submit" [disabled]="loading">
              {{ loading ? 'Registering...' : 'Register' }}
            </button>
          </form>
        </mat-card-content>
        <mat-card-actions class="centered">
          Already have an account? <a routerLink="/login">Login</a>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .register-container { display: flex; justify-content: center; align-items: center; min-height: calc(100vh - 64px); }
    mat-card { max-width: 400px; width: 100%; padding: 20px; }
    .full-width { width: 100%; margin-bottom: 16px; }
    .centered { justify-content: center; display: flex; }
    button[type="submit"] { width: 100%; }
  `]
})
export class RegisterComponent {
  registerForm = this.fb.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    confirmPassword: ['', Validators.required],
  }, { validators: PasswordMatchValidator });

  loading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar,
  ) {}

  onRegister(): void {
    if (this.registerForm.invalid) return;
    const { name, email, password } = this.registerForm.value;
    this.loading = true;
    this.authService.register(name!, email!, password!).subscribe({
      next: () => {
        this.snackBar.open('Registration successful! Please login.', 'Close', { duration: 3000 });
        this.router.navigate(['/login']);
      },
      error: (err) => {
        const msg = err.error?.error || 'Registration failed';
        this.snackBar.open(msg, 'Close', { duration: 5000 });
        this.loading = false;
      }
    });
  }
}

function PasswordMatchValidator(formGroup: FormGroup): void | any {
  const password = formGroup.get('password')?.value;
  const confirmPassword = formGroup.get('confirmPassword')?.value;
  if (password !== confirmPassword) {
    formGroup.get('confirmPassword')?.setErrors({ passwordsMismatch: true });
    return { passwordsMismatch: true };
  }
  return null;
}
