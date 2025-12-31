import { Injectable } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpHeaders,
  HttpParams,
} from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface ApiRequestOptions {
  headers?:
    | HttpHeaders
    | {
        [header: string]: string | string[];
      };
  params?:
    | HttpParams
    | {
        [param: string]:
          | string
          | number
          | boolean
          | ReadonlyArray<string | number | boolean>;
      };
  withAuth?: boolean;
  skipAuthRedirect?: boolean;
}

export interface NormalizedApiError {
  status: number;
  message: string;
  details?: any;
}

@Injectable({
  providedIn: 'root',
})
export class ApiClient {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  get<T>(path: string, options?: ApiRequestOptions): Observable<T> {
    const url = this.buildUrl(path);
    const httpOptions = this.buildOptions(options);
    return this.http
      .get<T>(url, httpOptions)
      .pipe(catchError((error) => this.handleError(error, options)));
  }

  post<T>(path: string, body?: any, options?: ApiRequestOptions): Observable<T> {
    const url = this.buildUrl(path);
    const httpOptions = this.buildOptions(options);
    return this.http
      .post<T>(url, body, httpOptions)
      .pipe(catchError((error) => this.handleError(error, options)));
  }

  put<T>(path: string, body?: any, options?: ApiRequestOptions): Observable<T> {
    const url = this.buildUrl(path);
    const httpOptions = this.buildOptions(options);
    return this.http
      .put<T>(url, body, httpOptions)
      .pipe(catchError((error) => this.handleError(error, options)));
  }

  delete<T>(path: string, options?: ApiRequestOptions): Observable<T> {
    const url = this.buildUrl(path);
    const httpOptions = this.buildOptions(options);
    return this.http
      .delete<T>(url, httpOptions)
      .pipe(catchError((error) => this.handleError(error, options)));
  }

  private buildUrl(path: string): string {
    if (/^https?:\/\//i.test(path)) {
      return path;
    }
    const normalized = path.startsWith('/') ? path : `/${path}`;
    return `${this.baseUrl}${normalized}`;
  }

  private buildOptions(options?: ApiRequestOptions) {
    const { withAuth = true, headers, ...rest } = options || {};
    let resolvedHeaders: HttpHeaders;

    if (headers instanceof HttpHeaders) {
      resolvedHeaders = headers;
    } else {
      resolvedHeaders = new HttpHeaders(headers || {});
    }

    if (withAuth) {
      const token = localStorage.getItem('authToken');
      if (token) {
        resolvedHeaders = resolvedHeaders.set('Authorization', `Bearer ${token}`);
      }
    }

    return {
      headers: resolvedHeaders,
      ...rest,
    };
  }

  private handleError(error: HttpErrorResponse, options?: ApiRequestOptions) {
    console.log('[ApiClient] Error received:', {
      status: error.status,
      statusText: error.statusText,
      message: error.message,
      error: error.error,
      url: error.url
    });

    const normalizedError: NormalizedApiError & { error?: any } = {
      status: error.status || 0,
      message: this.extractMessage(error),
      details: this.extractDetails(error),
    };
    normalizedError.error = { message: normalizedError.message, details: normalizedError.details };

    console.log('[ApiClient] Normalized error:', normalizedError);
    console.log('[ApiClient] Should skip redirect?', this.shouldSkipRedirect(options));
    console.log('[ApiClient] Current URL:', this.router.url);

    if (normalizedError.status === 401 && !this.shouldSkipRedirect(options)) {
      console.log('[ApiClient] Triggering 401 redirect...');
      this.handleUnauthorizedRedirect();
    }

    // 403 is bubbled as-is; caller shows "Access denied"
    return throwError(() => normalizedError);
  }

  private extractMessage(error: HttpErrorResponse): string {
    if (error.error?.message) return error.error.message;
    if (typeof error.error === 'string' && error.error.trim()) return error.error;
    if (error.message) return error.message;
    return 'An unexpected error occurred. Please try again.';
  }

  private extractDetails(error: HttpErrorResponse): any {
    if (error.error && typeof error.error === 'object') {
      const { message, ...rest } = error.error;
      return Object.keys(rest).length ? rest : undefined;
    }
    return undefined;
  }

  private shouldSkipRedirect(options?: ApiRequestOptions): boolean {
    if (options?.skipAuthRedirect) return true;
    const currentUrl = this.router.url || '';
    // Skip redirect if already on login page (root path) or first-login page
    if (currentUrl === '/' || currentUrl.startsWith('/first-login')) {
      return true;
    }
    return false;
  }

  private handleUnauthorizedRedirect(): void {
    const currentUrl = this.router.url || '/';
    console.log('[ApiClient] handleUnauthorizedRedirect called. Current URL:', currentUrl);

    // Prevent redirect loop on auth pages (root path is login)
    if (currentUrl === '/' || currentUrl.startsWith('/first-login')) {
      console.log('[ApiClient] Skipping redirect - already on auth page');
      return;
    }

    console.log('[ApiClient] Clearing localStorage...');
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');

    const nextParam = encodeURIComponent(currentUrl);
    const redirectUrl = `/?next=${nextParam}`;
    console.log('[ApiClient] Redirecting to:', redirectUrl);
    this.router.navigateByUrl(redirectUrl);
  }
}
