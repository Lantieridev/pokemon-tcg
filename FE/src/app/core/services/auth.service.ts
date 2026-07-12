import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { AuthResponseDTO } from '../models/game-state.models';
import { environment } from '../../../environments/environment';

export interface AuthUser {
  username: string;
  token: string;
  userId: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/auth`;

  public currentUser = signal<AuthUser | null>(null);

  /** Signal derivado: true si hay sesión activa */
  readonly isAuthenticated = computed(() => this.currentUser() !== null);

  constructor() {
    // Restaurar sesión desde localStorage al iniciar la app
    const token = localStorage.getItem('jwt');
    const username = localStorage.getItem('username');
    const userIdRaw = localStorage.getItem('userId');

    if (token && username && userIdRaw) {
      try {
        const payloadBase64 = token.split('.')[1];
        const payloadJson = atob(payloadBase64);
        const payload = JSON.parse(payloadJson);
        const exp = payload.exp * 1000;
        if (Date.now() >= exp) {
          localStorage.removeItem('jwt');
          localStorage.removeItem('username');
          localStorage.removeItem('userId');
        } else {
          this.currentUser.set({ username, token, userId: Number(userIdRaw) });
        }
      } catch (e) {
        localStorage.removeItem('jwt');
        localStorage.removeItem('username');
        localStorage.removeItem('userId');
      }
    }
  }

  get token(): string | undefined {
    return this.currentUser()?.token;
  }

  get username(): string | undefined {
    return this.currentUser()?.username;
  }

  get userId(): number | undefined {
    return this.currentUser()?.userId;
  }

  /**
   * POST /api/auth/login
   * Body: { username, password }
   * Response: { token, username, userId }
   */
  login(username: string, password: string): Observable<AuthResponseDTO> {
    return this.http
      .post<AuthResponseDTO>(`${this.API_URL}/login`, { username, password })
      .pipe(
        tap((res) => {
          localStorage.setItem('jwt', res.token);
          localStorage.setItem('username', res.username);
          localStorage.setItem('userId', String(res.userId));
          this.currentUser.set({ token: res.token, username: res.username, userId: res.userId });
        })
      );
  }

  /**
   * POST /api/auth/register
   * Body: { username, email, password }
   * Response: 200 OK (sin body)
   */
  register(username: string, email: string, password: string): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/register`, {
      username,
      email,
      password,
    });
  }

  /** Cierra sesión: limpia localStorage y resetea el signal */
  logout(): void {
    localStorage.removeItem('jwt');
    localStorage.removeItem('username');
    localStorage.removeItem('userId');
    this.currentUser.set(null);
  }

}
