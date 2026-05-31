import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private router = inject(Router);
  private authService = inject(AuthService);

  username = '';
  password = '';
  isRegistering = false;
  errorMessage = '';

  toggleMode() {
    this.isRegistering = !this.isRegistering;
    this.errorMessage = '';
  }

  submit() {
    if (!this.username || !this.password) {
      this.errorMessage = 'Por favor completa todos los campos.';
      return;
    }

    if (this.isRegistering) {
      this.authService.register(this.username, this.password).subscribe({
        next: () => {
          // Si el registro fue exitoso, hacemos login automáticamente
          this.login();
        },
        error: (err) => {
          this.errorMessage = err.error || 'Error al registrar. Intenta nuevamente.';
        }
      });
    } else {
      this.login();
    }
  }

  private login() {
    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.router.navigate(['/lobby']);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || err.error || 'Credenciales inválidas.';
      }
    });
  }
}
