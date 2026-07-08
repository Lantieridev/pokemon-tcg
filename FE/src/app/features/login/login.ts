import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

type AuthMode = 'login' | 'register';

const USERNAME_PATTERN = /^[a-zA-Z0-9_-]+$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

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
  confirmPassword = signal('');
  loading = signal(false);
  errorMsg = signal<string | null>(null);
  showPassword = signal(false);

  /** Fields the user has already interacted with, so we don't scream "required"
   *  at every empty field the instant the form renders. */
  private touchedFields = signal<Set<string>>(new Set());
  submitAttempted = signal(false);

  get isLogin() {
    return this.mode() === 'login';
  }

  // ── Validación por campo ──────────────────────────────────────────────────
  usernameError = computed<string | null>(() => {
    const u = this.username().trim();
    if (!u) return 'El usuario es obligatorio.';
    if (u.length < 3) return 'Mínimo 3 caracteres.';
    if (u.length > 20) return 'Máximo 20 caracteres.';
    if (!USERNAME_PATTERN.test(u)) return 'Solo letras, números, guiones y guion bajo.';
    return null;
  });

  emailError = computed<string | null>(() => {
    if (this.isLogin) return null;
    const e = this.email().trim();
    if (!e) return 'El email es obligatorio.';
    if (!EMAIL_PATTERN.test(e)) return 'Ingresá un email válido.';
    return null;
  });

  passwordError = computed<string | null>(() => {
    const p = this.password();
    if (!p) return 'La contraseña es obligatoria.';
    if (this.mode() === 'register' && p.length < 6) return 'Mínimo 6 caracteres.';
    return null;
  });

  confirmPasswordError = computed<string | null>(() => {
    if (this.isLogin) return null;
    const cp = this.confirmPassword();
    if (!cp) return 'Confirmá tu contraseña.';
    if (cp !== this.password()) return 'Las contraseñas no coinciden.';
    return null;
  });

  formValid = computed(() =>
    !this.usernameError() && !this.emailError() && !this.passwordError() && !this.confirmPasswordError()
  );

  /** A field's error only surfaces once the user has left it (or tried to
   *  submit) — never on the very first render of an empty required field. */
  showError(field: string, error: string | null): boolean {
    return !!error && (this.touchedFields().has(field) || this.submitAttempted());
  }

  touch(field: string) {
    this.touchedFields.update((s) => new Set(s).add(field));
  }

  toggleShowPassword() {
    this.showPassword.update((v) => !v);
  }

  toggleMode() {
    this.mode.update((m) => (m === 'login' ? 'register' : 'login'));
    this.errorMsg.set(null);
    this.submitAttempted.set(false);
    this.touchedFields.set(new Set());
  }

  /** Envía el formulario según el modo actual */
  submit() {
    this.errorMsg.set(null);
    this.submitAttempted.set(true);

    if (!this.formValid()) return;

    const u = this.username().trim();
    const p = this.password();

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
