import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface StoreItemDTO {
  id: number;
  name: string;
  description: string;
  price: number;
  itemType: 'TITLE' | 'AVATAR' | 'PACK' | 'COSMETIC';
  imageUrl: string;
}

export interface BuyRequestDTO {
  itemId: number;
}

@Injectable({ providedIn: 'root' })
export class StoreService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8081/api/store';

  getAvailableItems(): Observable<StoreItemDTO[]> {
    return this.http.get<StoreItemDTO[]>(`${this.API_URL}/items`);
  }

  buyItem(itemId: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/buy`, { itemId });
  }
}
