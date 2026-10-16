import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../services/auth.service';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  constructor(
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();

    if (token && !request.url.includes('/api/login') && this.authService.isAuthenticated()) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {

        if (request.url.includes('/api/login')) {
          return throwError(() => error);
        }

        if (error.status === 401) {
          this.authService.logout();
          this.snackBar.open('Sesión expirada o no autorizada. Inicia sesión nuevamente.', 'Cerrar', { duration: 3500 });
        } else if (error.status === 403) {
          this.snackBar.open('No cuentas con permisos para realizar esta acción.', 'Cerrar', { duration: 3000 });
        }

        return throwError(() => error);
      })
    );
  }
}