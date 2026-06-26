import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({ sub: 'AshRivero', exp: Math.floor(Date.now() / 1000) + 3600 }));
    const signature = 'signature';
    const validToken = `${header}.${payload}.${signature}`;
    localStorage.setItem('jwt', validToken);
    localStorage.setItem('username', 'AshRivero');
    localStorage.setItem('userId', '1');

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should initialize with cached user from localStorage if token is valid', () => {
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.username).toBe('AshRivero');
    expect(service.userId).toBe(1);
    expect(service.token).toBeDefined();
  });

  it('should login successfully and save to localStorage', () => {
    service.logout();
    expect(service.isAuthenticated()).toBeFalse();

    const mockResponse = { token: 'new-token', username: 'AshRivero', userId: 1 };
    service.login('AshRivero', 'password123').subscribe(res => {
      expect(res).toEqual(mockResponse);
      expect(service.isAuthenticated()).toBeTrue();
      expect(localStorage.getItem('jwt')).toBe('new-token');
    });

    const req = httpMock.expectOne('http://localhost:8081/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should register successfully', () => {
    service.register('AshRivero', 'ash@pokemon.com', 'password123').subscribe();

    const req = httpMock.expectOne('http://localhost:8081/api/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('should clear localStorage on logout', () => {
    service.logout();
    expect(service.isAuthenticated()).toBeFalse();
    expect(localStorage.getItem('jwt')).toBeNull();
  });
});
