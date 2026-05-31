import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '../services/auth.service';

describe('jwtInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      token: 'fake-jwt-token'
    });

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should add Authorization header for backend API requests', () => {
    httpClient.get('http://localhost:8081/api/matches').subscribe();

    const req = httpMock.expectOne('http://localhost:8081/api/matches');
    expect(req.request.headers.has('Authorization')).toBeTrue();
    expect(req.request.headers.get('Authorization')).toBe('Bearer fake-jwt-token');
  });

  it('should NOT add Authorization header for external API requests (like pokemontcg.io)', () => {
    httpClient.get('https://api.pokemontcg.io/v2/cards').subscribe();

    const req = httpMock.expectOne('https://api.pokemontcg.io/v2/cards');
    expect(req.request.headers.has('Authorization')).toBeFalse();
  });

  it('should NOT add Authorization header if token is missing', () => {
    Object.defineProperty(authServiceSpy, 'token', { get: () => null });

    httpClient.get('http://localhost:8081/api/matches').subscribe();

    const req = httpMock.expectOne('http://localhost:8081/api/matches');
    expect(req.request.headers.has('Authorization')).toBeFalse();
  });
});
