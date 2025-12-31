import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-first-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './first-login.html',
  styleUrls: ['./first-login.css'],
})
export class FirstLogin implements OnInit {
  passwordForm!: FormGroup;
  isLoading = false;
  isValidatingToken = true;
  errorMessage = '';
  successMessage = '';
  tokenValid = false;
  token = '';
  username = '';
  email = '';

  constructor(
    private fb: FormBuilder,
    public router: Router,
    private route: ActivatedRoute,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // Initialize form
    this.passwordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8), this.passwordStrengthValidator]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: this.passwordMatchValidator
    });

    this.passwordForm.valueChanges.subscribe(() => {
      this.errorMessage = '';
    });

    // Extract token from query parameters
    this.route.queryParams.subscribe(params => {
      this.token = params['token'];

      if (!this.token) {
        // No token provided - redirect to login
        this.errorMessage = 'Không có mã xác thực. Vui lòng kiểm tra email của bạn.';
        this.isValidatingToken = false;
        setTimeout(() => {
          this.router.navigateByUrl('/');
        }, 3000);
        return;
      }

      // Validate token
      this.validateToken();
    });
  }

  // Password strength validator
  passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) {
      return null;
    }

    const hasUpperCase = /[A-Z]/.test(value);
    const hasLowerCase = /[a-z]/.test(value);
    const hasNumeric = /[0-9]/.test(value);
    const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(value);

    const passwordValid = hasUpperCase && hasLowerCase && hasNumeric && hasSpecialChar;

    return !passwordValid ? { passwordStrength: true } : null;
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

  validateToken(): void {
    this.isValidatingToken = true;
    this.errorMessage = '';

    this.authService.validateFirstLoginToken(this.token).subscribe({
      next: (response) => {
        console.log('Token validation successful:', response);
        this.isValidatingToken = false;

        if (response.data?.valid) {
          this.tokenValid = true;
          this.username = response.data.username;
          this.email = response.data.email;
        } else {
          this.tokenValid = false;
          this.errorMessage = 'Liên kết này không hợp lệ hoặc đã được sử dụng. Vui lòng liên hệ quản trị viên.';
        }

        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        console.log('Token validation error:', error);
        this.isValidatingToken = false;
        this.tokenValid = false;

        if (error.status === 400) {
          this.errorMessage = error.error?.message || 'Liên kết này không hợp lệ hoặc đã được sử dụng. Vui lòng liên hệ quản trị viên.';
        } else if (error.status === 0) {
          this.errorMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
        } else {
          this.errorMessage = error.error?.message || 'Đã xảy ra lỗi. Vui lòng thử lại.';
        }

        this.cdr.detectChanges();
      }
    });
  }

  onSubmit(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const { newPassword, confirmPassword } = this.passwordForm.value;
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.setFirstLoginPassword(this.token, newPassword, confirmPassword).subscribe({
      next: (response) => {
        console.log('Password set successful:', response);
        this.isLoading = false;

        if (response.data?.token) {
          // Store JWT token and user info
          localStorage.setItem('authToken', response.data.token);

          if (response.data.id) {
            localStorage.setItem('userId', response.data.id.toString());
          }
          if (response.data.username) {
            localStorage.setItem('username', response.data.username);
          }
          if (response.data.email) {
            localStorage.setItem('email', response.data.email);
          }
          if (response.data.role) {
            localStorage.setItem('role', response.data.role);
          }

          // Show success message briefly
          this.successMessage = 'Mật khẩu đã được thiết lập thành công! Đang chuyển hướng...';
          this.cdr.detectChanges();

          // Auto-redirect to home/dashboard
          setTimeout(() => {
            this.router.navigateByUrl('/home');
          }, 1500);
        } else {
          this.errorMessage = 'Thiếu thông tin xác thực. Vui lòng thử lại.';
          this.cdr.detectChanges();
        }
      },
      error: (error: HttpErrorResponse) => {
        console.log('Password set error:', error);
        this.isLoading = false;

        if (error.status === 400) {
          this.errorMessage = error.error?.message || 'Mật khẩu không hợp lệ. Vui lòng thử lại.';
        } else if (error.status === 0) {
          this.errorMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
        } else if (error.status >= 500) {
          this.errorMessage = error.error?.message || 'Lỗi máy chủ. Vui lòng thử lại sau.';
        } else {
          this.errorMessage = error.error?.message || 'Đã xảy ra lỗi. Vui lòng thử lại.';
        }

        this.cdr.detectChanges();
      }
    });
  }
}
