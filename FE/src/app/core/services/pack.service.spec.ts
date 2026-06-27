import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PackService, PackOpeningResultDTO } from './pack.service';

describe('PackService', () => {
  let service: PackService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PackService]
    });
    service = TestBed.inject(PackService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should call post /api/packs/open with packType query parameter', () => {
    const mockResult: PackOpeningResultDTO = {
      cards: [{ cardId: 'xy1-1', isFoil: true, isDuplicate: false }],
      coinsRefunded: 0
    };

    service.openPack('pack_raro').subscribe((res) => {
      expect(res).toEqual(mockResult);
    });

    const req = httpMock.expectOne('http://localhost:8081/api/packs/open?packType=pack_raro');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(mockResult);
  });

  it('should default to pack_base if no packType is provided', () => {
    service.openPack().subscribe();
    const req = httpMock.expectOne('http://localhost:8081/api/packs/open?packType=pack_base');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
