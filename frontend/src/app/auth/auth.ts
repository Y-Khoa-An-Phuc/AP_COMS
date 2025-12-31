import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule
  ],
  templateUrl: './auth.html',
  styleUrls: ['./auth.css'],
})
export class Auth implements OnInit {
  loginForm!: FormGroup;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
    });

    this.loginForm.valueChanges.subscribe(() => {
      // Clear error message when user types
      this.errorMessage = '';

      const pc = this.loginForm.get('password');
      if (!pc) return;
      const errors = pc.errors || {};
      if (errors['notFound']) {
        const newErr = { ...errors };
        delete newErr['notFound'];
        pc.setErrors(Object.keys(newErr).length ? newErr : null);
      }
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const { username, password } = this.loginForm.value;
    this.isLoading = true;
    this.errorMessage = '';

    console.log('Submitting login request...');

    try {
      this.authService.login(username, password).subscribe({
        next: (response) => {
          console.log('Login response:', response);
          this.isLoading = false;
          this.errorMessage = '';

          // Store token from response.data.token
          if (response.data?.token) {
            localStorage.setItem('authToken', response.data.token);
            console.log('Token stored successfully');
          } else {
            console.warn('No token found in response.data');
          }

          // Navigate to home on success
          this.router.navigateByUrl('/home');
        },
        error: (error: HttpErrorResponse) => {
          console.log('Login error received:', error);
          console.log('Error status:', error.status);
          console.log('Error object:', error.error);

          this.isLoading = false;

          // Handle different types of errors
          if (error.status === 0) {
            // Network error or CORS issue
            this.errorMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
          } else if (error.status === 401) {
            // Unauthorized - invalid credentials
            this.errorMessage = error.error?.message || 'Tên đăng nhập hoặc mật khẩu không đúng.';
          } else if (error.status === 403) {
            // Forbidden
            this.errorMessage = error.error?.message || 'Bạn không có quyền truy cập.';
          } else if (error.status === 404) {
            // Not found
            this.errorMessage = 'Không tìm thấy dịch vụ đăng nhập. Vui lòng liên hệ quản trị viên.';
          } else if (error.status >= 500) {
            // Server error
            this.errorMessage = error.error?.message || 'Lỗi máy chủ. Vui lòng thử lại sau.';
          } else if (error.error?.message) {
            // Custom server error message
            this.errorMessage = error.error.message;
          } else if (error.error?.error) {
            // Alternative error format
            this.errorMessage = error.error.error;
          } else if (error.message) {
            // Generic HTTP error message
            this.errorMessage = error.message;
          } else {
            // Fallback error message
            this.errorMessage = 'Đã xảy ra lỗi khi đăng nhập. Vui lòng thử lại.';
          }

          console.log('Error message set to:', this.errorMessage);
          console.error('Login failed:', error);

          // Manually trigger change detection for zoneless mode
          this.cdr.detectChanges();
        }
      });
    } catch (error: any) {
      // Catch any synchronous errors
      console.log('Synchronous error caught:', error);
      this.isLoading = false;
      this.errorMessage = error?.message || 'Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.';
      console.error('Unexpected error:', error);

      // Manually trigger change detection for zoneless mode
      this.cdr.detectChanges();
    }
  }
}
