import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule
  ],
  templateUrl: './change-password.html',
  styleUrls: ['./change-password.css'],
})
export class ChangePassword implements OnInit {
  changePasswordForm!: FormGroup;
  isLoading = false;
  errorMessage = '';
  username = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    // Get username from navigation state
    const navigation = this.router.getCurrentNavigation();
    this.username = navigation?.extras?.state?.['username'] || '';

    // If no username, redirect back to login
    if (!this.username) {
      this.router.navigateByUrl('/');
    }
  }

  ngOnInit() {
    this.changePasswordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: this.passwordMatchValidator
    });

    this.changePasswordForm.valueChanges.subscribe(() => {
      this.errorMessage = '';
    });
  }

  // Custom validator to check if passwords match
  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword');
    const confirmPassword = control.get('confirmPassword');

    if (!newPassword || !confirmPassword) {
      return null;
    }

    return newPassword.value === confirmPassword.value ? null : { passwordMismatch: true };
  }

  onSubmit(): void {
    if (this.changePasswordForm.invalid) {
      this.changePasswordForm.markAllAsTouched();
      return;
    }

    const { currentPassword, newPassword } = this.changePasswordForm.value;
    this.isLoading = true;
    this.errorMessage = '';

    this.authService.changePassword(this.username, currentPassword, newPassword).subscribe({
      next: (response) => {
        console.log('Password change successful:', response);
        this.isLoading = false;

        // Store token if provided
        if (response.token) {
          localStorage.setItem('authToken', response.token);
        }

        // Navigate to home on success
        this.router.navigateByUrl('/home');
      },
      error: (error: HttpErrorResponse) => {
        console.log('Password change error:', error);
        this.isLoading = false;

        if (error.status === 0) {
          this.errorMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
        } else if (error.status === 401) {
          this.errorMessage = error.error?.message || 'Mật khẩu hiện tại không đúng.';
        } else if (error.status === 400) {
          this.errorMessage = error.error?.message || 'Mật khẩu mới không hợp lệ.';
        } else if (error.status >= 500) {
          this.errorMessage = error.error?.message || 'Lỗi máy chủ. Vui lòng thử lại sau.';
        } else {
          this.errorMessage = error.error?.message || 'Đã xảy ra lỗi. Vui lòng thử lại.';
        }

        this.cdr.detectChanges();
      }
    });
  }

  cancel(): void {
    // Clear any stored data and return to login
    localStorage.removeItem('authToken');
    this.router.navigateByUrl('/');
  }
}
