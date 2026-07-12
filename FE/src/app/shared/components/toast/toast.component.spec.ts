import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastComponent } from './toast.component';
import { ToastService } from '../../../core/services/toast.service';

describe('ToastComponent', () => {
  let fixture: ComponentFixture<ToastComponent>;
  let toastService: ToastService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(ToastComponent);
    toastService = TestBed.inject(ToastService);
  });

  describe('getIcon', () => {
    it('returns a checkmark for success', () => {
      expect(fixture.componentInstance.getIcon('success')).toBe('✓');
    });

    it('returns a cross for error', () => {
      expect(fixture.componentInstance.getIcon('error')).toBe('✕');
    });

    it('returns an info glyph for the info type and any unrecognized type', () => {
      expect(fixture.componentInstance.getIcon('info')).toBe('ℹ');
      expect(fixture.componentInstance.getIcon('something-else')).toBe('ℹ');
    });
  });

  describe('rendering toasts from the service', () => {
    it('renders nothing when there are no toasts', () => {
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelectorAll('.toast').length).toBe(0);
    });

    it('renders one .toast element per active toast, with the right type class and message', () => {
      toastService.success('Deck guardado');
      toastService.error('No se pudo conectar');
      fixture.detectChanges();

      const toasts = fixture.nativeElement.querySelectorAll('.toast');
      expect(toasts.length).toBe(2);
      expect(toasts[0].classList.contains('success')).toBeTrue();
      expect(toasts[0].textContent).toContain('Deck guardado');
      expect(toasts[1].classList.contains('error')).toBeTrue();
      expect(toasts[1].textContent).toContain('No se pudo conectar');
    });

    it('removes a toast from the service when clicked', () => {
      toastService.info('Click me');
      fixture.detectChanges();

      fixture.nativeElement.querySelector('.toast').click();
      fixture.detectChanges();

      expect(toastService.toasts().length).toBe(0);
      expect(fixture.nativeElement.querySelectorAll('.toast').length).toBe(0);
    });

    it('auto-hides a toast after 4 seconds', () => {
      jasmine.clock().install();
      try {
        toastService.success('Bye soon');
        fixture.detectChanges();
        expect(toastService.toasts().length).toBe(1);

        jasmine.clock().tick(4001);
        fixture.detectChanges();

        expect(toastService.toasts().length).toBe(0);
      } finally {
        jasmine.clock().uninstall();
      }
    });
  });
});
