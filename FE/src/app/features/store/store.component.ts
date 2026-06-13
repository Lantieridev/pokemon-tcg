import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StoreService, StoreItemDTO } from '../../core/services/store.service';
import { ToastService } from '../../core/services/toast.service';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-store',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './store.component.html',
  styleUrl: './store.component.css'
})
export class StoreComponent implements OnInit {
  private storeService = inject(StoreService);
  private profileService = inject(ProfileService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);

  items = signal<StoreItemDTO[]>([]);
  pokecoins = signal<number>(0);
  loading = signal<boolean>(true);

  ngOnInit() {
    this.loadStore();
    this.loadBalance();
  }

  loadStore() {
    this.storeService.getAvailableItems().subscribe({
      next: (res) => {
        this.items.set(res);
        this.loading.set(false);
      },
      error: (err) => {
        this.toastService.error('Error al cargar la tienda');
        this.loading.set(false);
      }
    });
  }

  loadBalance() {
    if (this.authService.username) {
      this.profileService.getProfile(this.authService.username).subscribe({
        next: (profile) => this.pokecoins.set(profile.pokecoins),
        error: () => {}
      });
    }
  }

  buyItem(item: StoreItemDTO) {
    if (this.pokecoins() < item.price) {
      this.toastService.error('Pokecoins insuficientes');
      return;
    }

    this.storeService.buyItem(item.id).subscribe({
      next: () => {
        this.toastService.success(`¡Has comprado ${item.name}!`);
        this.loadBalance(); // Refresh balance
      },
      error: (err) => {
        this.toastService.error(err.error?.message || 'Error al procesar la compra');
      }
    });
  }
}
