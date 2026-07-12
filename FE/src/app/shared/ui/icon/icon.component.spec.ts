import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IconComponent } from './icon.component';

describe('IconComponent', () => {
  let fixture: ComponentFixture<IconComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IconComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(IconComponent);
  });

  function setName(name: string): void {
    fixture.componentRef.setInput('name', name);
    fixture.detectChanges();
  }

  it('renders the path for a known icon name', () => {
    setName('home');
    const paths = fixture.nativeElement.querySelectorAll('path');
    expect(paths.length).toBe(1);
    expect(paths[0].getAttribute('d')).toContain('M3 11l9-8 9 8');
  });

  it('renders a different path for a different known icon name', () => {
    setName('close');
    const paths = fixture.nativeElement.querySelectorAll('path');
    expect(paths.length).toBe(1);
    expect(paths[0].getAttribute('d')).toBe('M18 6L6 18M6 6l12 12');
  });

  it('renders no path for an unrecognized icon name', () => {
    setName('does-not-exist');
    const paths = fixture.nativeElement.querySelectorAll('path');
    expect(paths.length).toBe(0);
  });

  it('applies the default svgClass and stroke when not overridden', () => {
    setName('home');
    const svg = fixture.nativeElement.querySelector('svg');
    expect(svg.classList.contains('w-5')).toBeTrue();
    expect(svg.classList.contains('h-5')).toBeTrue();
    expect(svg.getAttribute('stroke-width')).toBe('1.6');
  });

  it('applies a custom svgClass and stroke when provided', () => {
    fixture.componentRef.setInput('name', 'home');
    fixture.componentRef.setInput('svgClass', 'w-8 h-8 text-red-500');
    fixture.componentRef.setInput('stroke', 2);
    fixture.detectChanges();

    const svg = fixture.nativeElement.querySelector('svg');
    expect(svg.classList.contains('w-8')).toBeTrue();
    expect(svg.classList.contains('h-8')).toBeTrue();
    expect(svg.classList.contains('text-red-500')).toBeTrue();
    expect(svg.getAttribute('stroke-width')).toBe('2');
  });
});
