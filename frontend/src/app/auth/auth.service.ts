import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ApiClient } from '../core/api/api-client.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private api: ApiClient) {}

  login(username: string, password: string): Observable<any> {
    return this.api
      .post('/auth/login', { username, password }, { withAuth: false, skipAuthRedirect: true })
      .pipe(
        tap((response: any) => {
          const data = response?.data ?? response;

          if (data?.firstLogin || data?.mustChangePassword) {
            // Let caller handle first-login flow; do not store token
            return;
          }

          const token = data?.token;
          if (token) {
            localStorage.setItem('authToken', token);
          }

          if (data?.user) {
            localStorage.setItem('user', JSON.stringify(data.user));
          }
        })
      );
  }

  validateFirstLoginToken(token: string): Observable<any> {
    return this.api.get('/auth/first-login/validate', {
      params: { token },
      withAuth: false,
    });
  }

  setFirstLoginPassword(token: string, newPassword: string, confirmPassword: string): Observable<any> {
    return this.api.post(
      '/auth/first-login/set-password',
      { token, newPassword, confirmPassword },
      { withAuth: false }
    );
  }

  changePassword(username: string, currentPassword: string, newPassword: string): Observable<any> {
    return this.api.post('/auth/change-password', { username, currentPassword, newPassword });
  }
}
