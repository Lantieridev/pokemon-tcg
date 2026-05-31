import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Interceptor funcional que inyecta el JWT en todas las peticiones
 * al backend local. NO agrega el header a llamadas externas (pokemontcg.io).
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  const token = authService.token;

  // Solo inyectar en llamadas al backend propio
  const isBackendRequest = req.url.includes('localhost:8081');

  if (token && isBackendRequest) {
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
    return next(cloned);
  }

  return next(req);
};
