import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../services/auth.service';
import { Rol } from '../models/usuario.model';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);

  if (!authService.isAuthenticated()) {
    authService.logout();
    snackBar.open('Tu sesión ha expirado. Ingresa nuevamente.', 'Cerrar', { duration: 3000 });
    return false;
  }

  const rolesPermitidos = route.data['roles'] as Rol[] | undefined;

  if (!rolesPermitidos || rolesPermitidos.length === 0) {
    return true;
  }

  if (authService.hasAnyRole(rolesPermitidos)) {
    return true;
  }

  snackBar.open('No tienes permisos para acceder a esta sección', 'Cerrar', { duration: 3000 });
  router.navigate(['/dashboard']);
  return false;
};