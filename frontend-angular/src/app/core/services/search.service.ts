import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ItemDocument {
  id: string;
  itemNumber: string;
  name: string;
  description: string;
  lifecycleState: string;
}

@Injectable({ providedIn: 'root' })
export class SearchService {
  constructor(private http: HttpClient) {}

  searchItems(q: string): Observable<ItemDocument[]> {
    return this.http.get<ItemDocument[]>('/api/search/items', { params: { q } });
  }

  filterByState(state: string): Observable<ItemDocument[]> {
    return this.http.get<ItemDocument[]>('/api/search/items/by-state', { params: { state } });
  }
}
