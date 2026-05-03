import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';
import { ApiResponse } from '../models/api-response.model';
import { BehaviorSubject, Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<AuthResponse | null>(null);
  
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor() {
    const storedUser = localStorage.getItem('currentUser');
    if (storedUser) {
      try {
        this.currentUserSubject.next(JSON.parse(storedUser));
      } catch (e) {
        // ignore JSON parse error
      }
    }
  }

  login(credentials: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        if (response.success && response.data) {
          localStorage.setItem('currentUser', JSON.stringify(response.data));
          localStorage.setItem('token', response.data.token);
          this.currentUserSubject.next(response.data);
        }
      })
    );
  }

  register(userData: RegisterRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/register`, userData).pipe(
      tap(response => {
        if (response.success && response.data) {
          localStorage.setItem('currentUser', JSON.stringify(response.data));
          localStorage.setItem('token', response.data.token);
          this.currentUserSubject.next(response.data);
        }
      })
    );
  }

  logout(): void {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('token');
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}
