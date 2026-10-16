import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UsuarioResponse, ROLES, ROL_LABELS, Rol } from '../../core/models/usuario.model';
import { UsuarioFormComponent } from '../usuario-form/usuario-form.component';
import { UsuarioService } from '../usuario.service';

@Component({
  selector: 'app-usuario-list',
  standalone: false,
  templateUrl: './usuario-list.component.html',
  styleUrl: './usuario-list.component.scss'
})
export class UsuarioListComponent implements OnInit {

  columnas = ['username', 'roles', 'acciones'];
  usuarios: UsuarioResponse[] = [];
  cargando = false;

  readonly rolAdmin = ROLES[0];
  readonly rolLabels = ROL_LABELS;

  constructor(
    private usuarioService: UsuarioService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.buscar();
  }

  getRolLabel(rol: Rol): string { return this.rolLabels[rol]; }

  buscar(): void {
    this.cargando = true;
    this.usuarioService.listar().subscribe({
      next: (data) => {
        this.usuarios = data;
        this.cargando = false;
      },
      error: () => {
        this.mostrarMensaje('Error al cargar la lista de usuarios');
        this.cargando = false;
      }
    });
  }

  abrirFormulario(usuario?: UsuarioResponse): void {
    const ref = this.dialog.open(UsuarioFormComponent, {
      width: '450px',
      data: usuario ?? null
    });

    ref.afterClosed().subscribe((guardado) => {
      if (guardado) this.buscar();
    });
  }

  eliminar(usuario: UsuarioResponse): void {
    if (!confirm(`¿Eliminar al usuario "${usuario.username}"?`)) return;

    this.usuarioService.eliminar(usuario.username).subscribe({
      next: () => {
        this.mostrarMensaje('Usuario eliminado correctamente');
        this.buscar();
      },
      error: () => this.mostrarMensaje('Error al eliminar el usuario')
    });
  }

  private mostrarMensaje(mensaje: string): void {
    this.snackBar.open(mensaje, 'Cerrar', { duration: 3000 });
  }
}