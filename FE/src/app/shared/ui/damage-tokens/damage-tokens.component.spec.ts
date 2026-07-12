import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DamageTokensComponent } from './damage-tokens.component';

describe('DamageTokensComponent', () => {
  let fixture: ComponentFixture<DamageTokensComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DamageTokensComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(DamageTokensComponent);
    fixture.detectChanges();
  });

  function setStatus(status: string): void {
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();
  }

  it('renders nothing when status is the default "none"', () => {
    expect(fixture.nativeElement.querySelector('.status-token')).toBeNull();
  });

  it('renders nothing when status is explicitly "none"', () => {
    setStatus('none');
    expect(fixture.nativeElement.querySelector('.status-token')).toBeNull();
  });

  it('renders a top-positioned token with the Spanish label for burned', () => {
    setStatus('burned');
    const token: HTMLElement = fixture.nativeElement.querySelector('.status-token');
    expect(token).not.toBeNull();
    expect(token.classList.contains('burned')).toBeTrue();
    expect(token.textContent?.trim()).toBe('Quemado');
    expect(token.getAttribute('style')).toContain('top: -10px');
    expect(token.getAttribute('style')).toContain('left: 50%');
  });

  it('renders a top-positioned token with the Spanish label for poisoned', () => {
    setStatus('poisoned');
    const token: HTMLElement = fixture.nativeElement.querySelector('.status-token');
    expect(token.textContent?.trim()).toBe('Envenenado');
  });

  it('renders a top-positioned token with the Spanish label for paralyzed', () => {
    setStatus('paralyzed');
    const token: HTMLElement = fixture.nativeElement.querySelector('.status-token');
    expect(token.textContent?.trim()).toBe('Paralizado');
  });

  it('renders a right-positioned token for asleep, distinct from the burned/poisoned layout', () => {
    setStatus('asleep');
    const token: HTMLElement = fixture.nativeElement.querySelector('.status-token');
    expect(token.classList.contains('asleep')).toBeTrue();
    expect(token.textContent?.trim()).toBe('Dormido');
    expect(token.getAttribute('style')).toContain('right: -12px');
  });

  it('renders a right-positioned token for confused', () => {
    setStatus('confused');
    const token: HTMLElement = fixture.nativeElement.querySelector('.status-token');
    expect(token.classList.contains('confused')).toBeTrue();
    expect(token.textContent?.trim()).toBe('Confundido');
    expect(token.getAttribute('style')).toContain('right: -12px');
  });
});
