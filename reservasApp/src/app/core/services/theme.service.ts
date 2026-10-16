import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

const STORAGE_KEY = 'tema-oscuro';

@Injectable({ providedIn: 'root' })
export class ThemeService {

  private oscuro$ = new BehaviorSubject<boolean>(this.obtenerPreferenciaInicial());
  oscuroObservable = this.oscuro$.asObservable();

  constructor() {
    this.aplicarTema(this.oscuro$.value);
  }

  get esOscuro(): boolean {
    return this.oscuro$.value;
  }

  alternar(): void {
    const nuevoValor = !this.oscuro$.value;
    this.oscuro$.next(nuevoValor);
    this.aplicarTema(nuevoValor);
    localStorage.setItem(STORAGE_KEY, String(nuevoValor));
  }

  private aplicarTema(oscuro: boolean): void {
    document.body.classList.toggle('dark-theme', oscuro);
  }

  private obtenerPreferenciaInicial(): boolean {
    const guardado = localStorage.getItem(STORAGE_KEY);
    if (guardado !== null) return guardado === 'true';
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
}