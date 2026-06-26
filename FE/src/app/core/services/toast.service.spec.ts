import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ToastService]
    });
    service = TestBed.inject(ToastService);
  });

  it('should start with no toasts', () => {
    expect(service.toasts().length).toBe(0);
  });

  it('should show success, error, and info toasts and auto-remove them', fakeAsync(() => {
    service.success('Operation succeeded');
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].message).toBe('Operation succeeded');
    expect(service.toasts()[0].type).toBe('success');

    service.error('Something went wrong');
    expect(service.toasts().length).toBe(2);
    expect(service.toasts()[1].message).toBe('Something went wrong');
    expect(service.toasts()[1].type).toBe('error');

    service.info('Just a heads up');
    expect(service.toasts().length).toBe(3);
    expect(service.toasts()[2].message).toBe('Just a heads up');
    expect(service.toasts()[2].type).toBe('info');

    // Wait 4000ms for them to be auto-removed
    tick(4000);
    expect(service.toasts().length).toBe(0);
  }));

  it('should allow manual removal of toasts', () => {
    service.success('A toast');
    service.info('Another toast');
    expect(service.toasts().length).toBe(2);

    const firstId = service.toasts()[0].id;
    service.remove(firstId);
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].message).toBe('Another toast');
  });
});
