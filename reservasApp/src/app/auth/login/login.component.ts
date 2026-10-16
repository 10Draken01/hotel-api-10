import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  form: FormGroup;
  cargando = false;
  ocultarPassword = true;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar,
    public themeService: ThemeService
  ) {
    this.form = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  ingresar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando = true;
    this.authService.login(this.form.value).subscribe({
      next: () => {
        this.cargando = false;
        this.snackBar.open('Sesión iniciada', 'Cerrar', { duration: 2500 });
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.status === 401 ? 'Credenciales inválidas' : 'Error al conectar con el servidor';
        this.snackBar.open(msg, 'Cerrar', { duration: 3000 });
      }
    });
  }
}