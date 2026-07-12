import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EnergyPipComponent } from './energy-pip.component';

describe('EnergyPipComponent', () => {
  let fixture: ComponentFixture<EnergyPipComponent>;
  let component: EnergyPipComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnergyPipComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(EnergyPipComponent);
    component = fixture.componentInstance;
  });

  function setType(type: string): void {
    fixture.componentRef.setInput('type', type);
    fixture.detectChanges();
  }

  it('resolves the correct glyph and color for a known energy type', () => {
    setType('fire');
    expect(component.glyph).toBe('🔥');
    expect(component.color).toBe('#ff7a3d');
    expect(fixture.nativeElement.querySelector('span').textContent.trim()).toBe('🔥');
  });

  it('resolves a different glyph and color for a different known type', () => {
    setType('water');
    expect(component.glyph).toBe('💧');
    expect(component.color).toBe('#4aa3ff');
  });

  it('is case-insensitive when matching the type', () => {
    setType('LIGHTNING');
    expect(component.glyph).toBe('⚡');
    expect(component.color).toBe('#ffcc33');
  });

  it('falls back to the colorless glyph and a neutral color for an unknown type', () => {
    setType('not-a-real-type');
    expect(component.glyph).toBe('✦');
    expect(component.color).toBe('#cfd6e4');
  });

  it('renders the default size when not overridden', () => {
    setType('fire');
    const div: HTMLElement = fixture.nativeElement.querySelector('div');
    expect(div.style.width).toBe('20px');
    expect(div.style.height).toBe('20px');
  });

  it('renders a custom size when provided', () => {
    fixture.componentRef.setInput('type', 'fire');
    fixture.componentRef.setInput('size', 40);
    fixture.detectChanges();

    const div: HTMLElement = fixture.nativeElement.querySelector('div');
    expect(div.style.width).toBe('40px');
    expect(div.style.height).toBe('40px');
  });

  it('applies a glow box-shadow when glow is true, and none when false', () => {
    fixture.componentRef.setInput('type', 'fire');
    fixture.componentRef.setInput('glow', true);
    fixture.detectChanges();
    const glowingDiv: HTMLElement = fixture.nativeElement.querySelector('div');
    expect(glowingDiv.style.boxShadow).not.toBe('none');

    fixture.componentRef.setInput('glow', false);
    fixture.detectChanges();
    const noGlowDiv: HTMLElement = fixture.nativeElement.querySelector('div');
    expect(noGlowDiv.style.boxShadow).toBe('none');
  });
});
