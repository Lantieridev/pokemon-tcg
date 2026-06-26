import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { signal } from '@angular/core';

describe('authGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let isAuthenticatedSignal: any;

  beforeEach(() => {
    isAuthenticatedSignal = signal(false);
    authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      isAuthenticated: isAuthenticatedSignal
    });
    routerSpy = jasmine.createSpyObj('Router', ['createUrlTree']);
    routerSpy.createUrlTree.and.callFake((commands: any[]) => {
      return { toString: () => commands.join('/') } as UrlTree;
    });

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
  });

  const runGuard = () => {
    return TestBed.runInInjectionContext(() => 
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
  };

  it('should return UrlTree to /login when not authenticated', () => {
    isAuthenticatedSignal.set(false);

    const result = runGuard();

    expect(result).not.toBeTrue();
    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/login']);
  });

  it('should return true when authenticated', () => {
    isAuthenticatedSignal.set(true);

    const result = runGuard();

    expect(result).toBeTrue();
    expect(routerSpy.createUrlTree).not.toHaveBeenCalled();
  });
});
