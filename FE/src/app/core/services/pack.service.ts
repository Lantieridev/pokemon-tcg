import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PulledCardDTO {
  cardId: string;
  isFoil: boolean;
  isDuplicate: boolean;
}

export interface PackOpeningResultDTO {
  cards: PulledCardDTO[];
  coinsRefunded: number;
}

@Injectable({ providedIn: 'root' })
export class PackService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/packs`;

  openPack(packType: string = 'pack_base'): Observable<PackOpeningResultDTO> {
    return this.http.post<PackOpeningResultDTO>(`${this.API_URL}/open?packType=${packType}`, {});
  }
}
