import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatSelectModule, MatProgressSpinnerModule, MatSnackBarModule,
  ],
  template: `
    <div style="display:flex;align-items:center;justify-content:center;height:100vh;background:#f5f5f5">
      <mat-card style="width:420px;padding:16px">
        <mat-card-header>
          <mat-card-title>PLM Platform</mat-card-title>
          <mat-card-subtitle>Create an account</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="submit()" style="display:flex;flex-direction:column;gap:12px;margin-top:16px">
            <mat-form-field appearance="outline">
              <mat-label>Username</mat-label>
              <input matInput formControlName="username" autocomplete="username">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Email</mat-label>
              <input matInput type="email" formControlName="email" autocomplete="email">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Password</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="new-password">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Role</mat-label>
              <mat-select formControlName="role">
                <mat-option value="ENGINEER">Engineer</mat-option>
                <mat-option value="VIEWER">Viewer</mat-option>
                <mat-option value="ADMIN">Admin</mat-option>
              </mat-select>
            </mat-form-field>
            <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">
              @if (loading) {
                <mat-spinner diameter="20" style="margin:auto"></mat-spinner>
              } @else {
                Register
              }
            </button>
            <a mat-button routerLink="/login" style="text-align:center">Already have an account? Sign in</a>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
})
export class RegisterComponent {
  form = this.fb.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['ENGINEER', Validators.required],
  });
  loading = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private snack: MatSnackBar,
  ) {}

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    const { username, email, password, role } = this.form.value;
    this.auth.register(username!, email!, password!, role!).subscribe({
      next: () => this.router.navigate(['/']),
      error: (e) => {
        this.snack.open(e.error?.message ?? 'Registration failed', 'Close', { duration: 3000 });
        this.loading = false;
      },
    });
  }
}
