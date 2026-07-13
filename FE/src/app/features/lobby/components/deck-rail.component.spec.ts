import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DeckRailComponent } from './deck-rail.component';
import { ActivatedRoute, Router } from '@angular/router';

describe('DeckRailComponent', () => {
  let component: DeckRailComponent;
  let fixture: ComponentFixture<DeckRailComponent>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [DeckRailComponent],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: {} }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DeckRailComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render active deck name, total cards count, and energy types', () => {
    component.deckData = {
      name: 'Super Electric Deck',
      totalCount: 60,
      energyTypes: ['lightning', 'colorless'],
      cards: [
        { name: 'Pikachu', img: 'pikachu.png' }
      ]
    };

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    
    // Check deck name
    expect(compiled.textContent).toContain('Super Electric Deck');
    
    // Check card count
    expect(compiled.textContent).toContain('60 / 60 cartas');
    
    // Check slots rendering
    const cards = compiled.querySelectorAll('.dock-card');
    expect(cards.length).toBe(1); // Returns 1 card as defined in component.deck
  });

  it('should navigate to deck builder on edit click', () => {
    component.deckId = 42;
    component.onEdit();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/deck'], { queryParams: { edit: 42 } });
  });

  it('should navigate to deck builder without query parameters if deckId is null', () => {
    component.deckId = null;
    component.onEdit();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/deck']);
  });
});
