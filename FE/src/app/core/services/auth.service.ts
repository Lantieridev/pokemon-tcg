import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { of, throwError, Observable, lastValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8081/api/auth';
  
  public currentUser = signal<{ username: string, token: string } | null>(null);

  constructor() {
    const token = localStorage.getItem('jwt');
    const username = localStorage.getItem('username');
    if (token && username) {
      this.currentUser.set({ username, token });
    }
  }

  get token() {
    return this.currentUser()?.token;
  }

  get username() {
    return this.currentUser()?.username;
  }

  async ensureDevUserAuthenticated(username: string = 'AshRivero', password: string = 'password123'): Promise<void> {
    if (this.currentUser()) return;

    try {
      await lastValueFrom(this.login(username, password));
    } catch (e) {
      console.log('Login failed, attempting to register...');
      try {
        await lastValueFrom(this.register(username, password));
        await lastValueFrom(this.login(username, password));
      } catch (err) {
        console.error('Failed to register/login dev user', err);
      }
    }
  }

  public login(username: string, password: string): Observable<any> {
    return this.http.post<{ token: string, username: string }>(`${this.API_URL}/login`, { username, password })
      .pipe(
        tap(res => {
          localStorage.setItem('jwt', res.token);
          localStorage.setItem('username', res.username);
          this.currentUser.set(res);
        })
      );
  }

  public register(username: string, password: string): Observable<any> {
    return this.http.post(`${this.API_URL}/register`, { username, email: `${username}@pokemon.com`, password });
  }
}
