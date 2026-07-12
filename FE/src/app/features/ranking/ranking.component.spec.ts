import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RankingComponent } from './ranking.component';
import { environment } from '../../../environments/environment';

describe('RankingComponent', () => {
  let fixture: ComponentFixture<RankingComponent>;
  let component: RankingComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RankingComponent, HttpClientTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(RankingComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('starts in a loading state with no rankings and no error', () => {
    expect(component.isLoading()).toBeTrue();
    expect(component.rankings()).toEqual([]);
    expect(component.errorMsg()).toBe('');
  });

  it('loads the top 50 global rankings on init and stops loading', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.apiUrl}/rankings?page=0&size=50`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ username: 'AshRivero', mmr: 1500, rank: 1 }] });

    expect(component.isLoading()).toBeFalse();
    expect(component.rankings().length).toBe(1);
    expect(component.rankings()[0].username).toBe('AshRivero');
  });

  it('sets a Spanish error message and stops loading when the request fails', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.apiUrl}/rankings?page=0&size=50`);
    req.flush('server error', { status: 500, statusText: 'Internal Server Error' });

    expect(component.isLoading()).toBeFalse();
    expect(component.errorMsg()).toBe('No se pudo cargar el ranking global.');
    expect(component.rankings()).toEqual([]);
  });
});
