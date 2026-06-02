import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

/**
 * Interceptor funcional que inyecta el JWT en todas las peticiones
 * al backend local. NO agrega el header a llamadas externas (pokemontcg.io).
 * Si la respuesta es 401 o 403, limpia la sesión y redirige al login.
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.token;

  // Solo inyectar en llamadas al backend propio
  const isBackendRequest = req.url.includes('localhost:8081');

  let preparedReq = req;
  if (token && isBackendRequest) {
    preparedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(preparedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (isBackendRequest && (error.status === 401 || error.status === 403)) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
