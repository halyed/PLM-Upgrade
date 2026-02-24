import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Item, ItemRequest, LifecycleState } from '../models/item.model';

@Injectable({ providedIn: 'root' })
export class ItemService {
  private base = '/api/items';
  constructor(private http: HttpClient) {}

  getAll() { return this.http.get<Item[]>(this.base); }
  getById(id: number) { return this.http.get<Item>(`${this.base}/${id}`); }
  create(req: ItemRequest) { return this.http.post<Item>(this.base, req); }
  update(id: number, req: ItemRequest) { return this.http.put<Item>(`${this.base}/${id}`, req); }
  delete(id: number) { return this.http.delete<void>(`${this.base}/${id}`); }
  transitionLifecycle(id: number, state: LifecycleState) {
    return this.http.patch<Item>(`${this.base}/${id}/lifecycle`, null, { params: { state } });
  }
}
