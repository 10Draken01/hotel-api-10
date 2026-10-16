import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthRequest, AuthResponse, JwtPayload } from '../models/auth.model';
import { Rol, ROLES } from '../models/usuario.model';
import { JwtHelper } from '../utils/jwt.helper';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly loginUrl = `${environment.authUrl}/api/login`;
  private readonly tokenKey = 'auth_token';

  constructor(
    private http: HttpClient,
    private router: Router
  ) { }

  login(credentials: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.loginUrl, credentials).pipe(
      tap((res) => {
        if (res?.token) {
          localStorage.setItem(this.tokenKey, res.token);
        }
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    return !!token && !JwtHelper.isTokenExpired(token);
  }

  getPayload(): JwtPayload | null {
    const token = this.getToken();
    if (!token || JwtHelper.isTokenExpired(token)) {
      return null;
    }
    return JwtHelper.decodeToken(token);
  }

  getUsername(): string | null {
    return this.getPayload()?.sub ?? null;
  }

  getRoles(): Rol[] {
    return this.getPayload()?.roles ?? [];
  }

  hasRole(role: Rol): boolean {
    return this.getRoles().includes(role);
  }

  hasAnyRole(roles: Rol[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  isAdmin(): boolean {
    return this.hasRole(ROLES[0]);
  }
}