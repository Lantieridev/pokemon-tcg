import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Interceptor funcional que inyecta el JWT en todas las peticiones
 * al backend local. NO agrega el header a llamadas externas (pokemontcg.io).
 * Solo hace logout automático en 401 (token inválido o expirado).
 * El 403 (Forbidden) se deja pasar al manejador de errores del componente.
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.token;

  // Solo inyectar en llamadas al backend propio
  const isBackendRequest = req.url.startsWith(environment.apiUrl);

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
      // Solo logout en 401 (token inválido/expirado), no en 403
      if (isBackendRequest && error.status === 401) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
