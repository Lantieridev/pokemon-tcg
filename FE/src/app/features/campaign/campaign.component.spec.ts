import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { CampaignComponent } from './campaign.component';
import { environment } from '../../../environments/environment';
import { CampaignNode } from '../../core/models/campaign.models';

function loginAs(userId: number, username: string): void {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: username, exp: Math.floor(Date.now() / 1000) + 3600 }));
  localStorage.setItem('jwt', `${header}.${payload}.signature`);
  localStorage.setItem('username', username);
  localStorage.setItem('userId', String(userId));
}

function makeNode(overrides: Partial<CampaignNode> = {}): CampaignNode {
  return {
    id: 1,
    name: 'Bosque Verde',
    botName: 'Líder Gary',
    status: 'UNLOCKED',
    rewardCoins: 100,
    rewardXp: 50,
    ...overrides,
  };
}

describe('CampaignComponent', () => {
  let fixture: ComponentFixture<CampaignComponent>;
  let component: CampaignComponent;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    loginAs(1, 'AshRivero');
    await TestBed.configureTestingModule({
      imports: [CampaignComponent, HttpClientTestingModule],
      providers: [provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(CampaignComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('loads progress on init and stops loading on success', fakeAsync(() => {
    fixture.detectChanges();
    expect(component.loading()).toBeTrue();

    httpMock.expectOne(`${environment.apiUrl}/campaign/progress`).flush({
      clearedNodesCount: 2, totalNodesCount: 8, nodes: [makeNode()],
    });
    tick();

    expect(component.loading()).toBeFalse();
    expect(component.progress()?.clearedNodesCount).toBe(2);
    expect(component.loadError()).toBeNull();
  }));

  it('sets a Spanish fallback error message when the progress fetch fails without a server message', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/campaign/progress`).flush('', { status: 500, statusText: 'Server Error' });
    tick();

    expect(component.loading()).toBeFalse();
    expect(component.loadError()).toBe('Http failure response for http://localhost:8081/api/campaign/progress: 500 Server Error');
  }));

  it('surfaces the server-provided error message when present', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/campaign/progress`)
      .flush({ message: 'Campaña no disponible' }, { status: 400, statusText: 'Bad Request' });
    tick();

    expect(component.loadError()).toBe('Campaña no disponible');
  }));

  describe('progressPercent', () => {
    it('is 0 with no progress loaded', () => {
      expect(component.progressPercent()).toBe(0);
    });

    it('rounds the cleared/total ratio to a whole percentage', fakeAsync(() => {
      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/campaign/progress`).flush({
        clearedNodesCount: 1, totalNodesCount: 3, nodes: [],
      });
      tick();
      expect(component.progressPercent()).toBe(33);
    }));

    it('is 0 when totalNodesCount is 0 (avoids a division by zero)', fakeAsync(() => {
      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/campaign/progress`).flush({
        clearedNodesCount: 0, totalNodesCount: 0, nodes: [],
      });
      tick();
      expect(component.progressPercent()).toBe(0);
    }));
  });

  describe('onNodeClick', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/campaign/progress`).flush({
        clearedNodesCount: 0, totalNodesCount: 1, nodes: [],
      });
      tick();
    }));

    it('does nothing for a locked node', () => {
      component.onNodeClick(makeNode({ status: 'LOCKED' }));
      expect(component.modalOpen()).toBeFalse();
      httpMock.expectNone(`${environment.apiUrl}/decks/user/1`);
    });

    it('opens the modal and loads decks for an unlocked node', fakeAsync(() => {
      const node = makeNode({ status: 'UNLOCKED' });
      component.onNodeClick(node);

      expect(component.modalOpen()).toBeTrue();
      expect(component.selectedNode()).toBe(node);
      expect(component.selectedDeckId()).toBeNull();

      httpMock.expectOne(`${environment.apiUrl}/decks/user/1`).flush([{ id: 10, name: 'Mazo Fuego', totalCards: 60 } as any]);
      httpMock.expectOne(`${environment.apiUrl}/decks/templates`).flush([{ id: 99, name: 'Plantilla', totalCards: 55 } as any]);
      tick();

      expect(component.sortedDecks().length).toBe(2);
    }));
  });

  describe('sortedDecks (usable decks first)', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/campaign/progress`).flush({
        clearedNodesCount: 0, totalNodesCount: 1, nodes: [],
      });
      tick();
      component.onNodeClick(makeNode());
      httpMock.expectOne(`${environment.apiUrl}/decks/user/1`).flush([
        { id: 1, name: 'Incompleto', totalCards: 40 },
        { id: 2, name: 'Completo', totalCards: 60 },
      ] as any);
      httpMock.expectOne(`${environment.apiUrl}/decks/templates`).flush([]);
      tick();
    }));

    it('marks a deck with fewer than 60 cards as unusable with a reason', () => {
      const incomplete = component.sortedDecks().find(d => d.summary.id === 1)!;
      expect(incomplete.usable).toBeFalse();
      expect(incomplete.reason).toContain('Mazo incompleto');
    });

    it('marks an exactly-60-card deck as usable', () => {
      const complete = component.sortedDecks().find(d => d.summary.id === 2)!;
      expect(complete.usable).toBeTrue();
      expect(complete.reason).toBeNull();
    });

    it('sorts usable decks before unusable ones', () => {
      const sorted = component.sortedDecks();
      expect(sorted[0].summary.id).toBe(2);
      expect(sorted[1].summary.id).toBe(1);
    });
  });

  describe('onDeckSelect', () => {
    it('ignores an unusable deck', () => {
      component.onDeckSelect({ summary: { id: 1 } as any, usable: false, reason: 'nope' });
      expect(component.selectedDeckId()).toBeNull();
    });

    it('selects a usable deck', () => {
      component.onDeckSelect({ summary: { id: 5 } as any, usable: true, reason: null });
      expect(component.selectedDeckId()).toBe(5);
    });
  });

  describe('closeModal', () => {
    it('resets all modal-related state', () => {
      component.selectedNode.set(makeNode());
      component.selectedDeckId.set(3);
      component.modalOpen.set(true);
      component.challengeError.set('some error');
      component.challenging.set(true);

      component.closeModal();

      expect(component.modalOpen()).toBeFalse();
      expect(component.selectedNode()).toBeNull();
      expect(component.selectedDeckId()).toBeNull();
      expect(component.challengeError()).toBeNull();
      expect(component.challenging()).toBeFalse();
    });
  });

  describe('startChallenge', () => {
    it('does nothing without a selected node or deck', fakeAsync(() => {
      component.startChallenge();
      tick();
      httpMock.expectNone((req) => req.url.includes('/campaign/challenge/'));
    }));

    it('navigates to the battle on success and closes the modal', fakeAsync(() => {
      spyOn(router, 'navigate');
      component.selectedNode.set(makeNode({ id: 4 }));
      component.selectedDeckId.set(10);
      component.modalOpen.set(true);

      component.startChallenge();
      tick();

      httpMock.expectOne(`${environment.apiUrl}/campaign/challenge/4?deckId=10`).flush({ matchId: 'match-123' });
      tick();

      expect(router.navigate).toHaveBeenCalledWith(['/battle', 'match-123']);
      expect(component.modalOpen()).toBeFalse();
    }));

    it('shows a Spanish message for a 400 without a server message', fakeAsync(() => {
      component.selectedNode.set(makeNode({ id: 4 }));
      component.selectedDeckId.set(10);

      component.startChallenge();
      tick();
      httpMock.expectOne(`${environment.apiUrl}/campaign/challenge/4?deckId=10`)
        .flush('', { status: 400, statusText: 'Bad Request' });
      tick();

      expect(component.challengeError()).toBe('El mazo seleccionado no es válido para este desafío.');
      expect(component.challenging()).toBeFalse();
    }));

    it('shows a Spanish message for a 403', fakeAsync(() => {
      component.selectedNode.set(makeNode({ id: 4 }));
      component.selectedDeckId.set(10);
      component.startChallenge();
      tick();
      httpMock.expectOne(`${environment.apiUrl}/campaign/challenge/4?deckId=10`)
        .flush('', { status: 403, statusText: 'Forbidden' });
      tick();
      expect(component.challengeError()).toBe('No tenés permiso para realizar esta acción.');
    }));

    it('surfaces the server error message over the generic status mapping', fakeAsync(() => {
      component.selectedNode.set(makeNode({ id: 4 }));
      component.selectedDeckId.set(10);
      component.startChallenge();
      tick();
      httpMock.expectOne(`${environment.apiUrl}/campaign/challenge/4?deckId=10`)
        .flush({ message: 'Nodo ya superado' }, { status: 400, statusText: 'Bad Request' });
      tick();
      expect(component.challengeError()).toBe('Nodo ya superado');
    }));
  });
});
