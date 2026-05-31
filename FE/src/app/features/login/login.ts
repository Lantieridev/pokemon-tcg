import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

type AuthMode = 'login' | 'register';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  private authService = inject(AuthService);
  private router = inject(Router);

  // ── Estado del formulario (Signals) ──────────────────────────────────────
  mode = signal<AuthMode>('login');
  username = signal('');
  email = signal('');
  password = signal('');
  loading = signal(false);
  errorMsg = signal<string | null>(null);

  get isLogin() {
    return this.mode() === 'login';
  }

  toggleMode() {
    this.mode.update((m) => (m === 'login' ? 'register' : 'login'));
    this.errorMsg.set(null);
  }

  /** Envía el formulario según el modo actual */
  submit() {
    this.errorMsg.set(null);

    const u = this.username().trim();
    const p = this.password().trim();

    if (!u || !p) {
      this.errorMsg.set('Completá usuario y contraseña.');
      return;
    }

    this.loading.set(true);

    if (this.isLogin) {
      this.authService.login(u, p).subscribe({
        next: () => {
          this.loading.set(false);
          this.router.navigate(['/lobby']);
        },
        error: (err) => {
          this.loading.set(false);
          this.errorMsg.set(
            err.status === 400
              ? 'Usuario o contraseña incorrectos.'
              : 'Error de conexión. Intentá más tarde.'
          );
        },
      });
    } else {
      const e = this.email().trim();
      if (!e) {
        this.loading.set(false);
        this.errorMsg.set('El email es requerido para registrarse.');
        return;
      }

      this.authService.register(u, e, p).subscribe({
        next: () => {
          // Registro exitoso → login automático
          this.authService.login(u, p).subscribe({
            next: () => {
              this.loading.set(false);
              this.router.navigate(['/lobby']);
            },
            error: () => {
              this.loading.set(false);
              this.errorMsg.set('Registro exitoso. Iniciá sesión manualmente.');
              this.mode.set('login');
            },
          });
        },
        error: (err) => {
          this.loading.set(false);
          this.errorMsg.set(
            typeof err.error === 'string'
              ? err.error
              : 'No se pudo registrar. El usuario puede ya existir.'
          );
        },
      });
    }
  }
}
