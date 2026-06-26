import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BattlePassService, BattlePassStatusDTO } from './battle-pass.service';

describe('BattlePassService', () => {
  let service: BattlePassService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BattlePassService]
    });
    service = TestBed.inject(BattlePassService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should call get /status to retrieve battle pass status', () => {
    const mockStatus: BattlePassStatusDTO = {
      isPremium: true,
      currentXp: 120,
      currentLevel: 1,
      claimedFreeLevel: 0,
      claimedPremiumLevel: 0,
      levels: []
    };

    service.getStatus().subscribe((res) => {
      expect(res).toEqual(mockStatus);
    });

    const req = httpMock.expectOne('http://localhost:8081/api/battle-pass/status');
    expect(req.request.method).toBe('GET');
    req.flush(mockStatus);
  });

  it('should post to /claim with level and isPremium query parameters', () => {
    service.claimReward(5, true).subscribe();

    const req = httpMock.expectOne('http://localhost:8081/api/battle-pass/claim?level=5&isPremium=true');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(null);
  });

  it('should post to /purchase-premium', () => {
    service.purchasePremium().subscribe();

    const req = httpMock.expectOne('http://localhost:8081/api/battle-pass/purchase-premium');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(null);
  });
});
