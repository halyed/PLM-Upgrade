import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ChangeRequest, ChangeRequestRequest, CrStatus } from '../models/change-request.model';

@Injectable({ providedIn: 'root' })
export class ChangeRequestService {
  private base = '/api/change-requests';
  constructor(private http: HttpClient) {}

  getAll() { return this.http.get<ChangeRequest[]>(this.base); }
  create(req: ChangeRequestRequest) { return this.http.post<ChangeRequest>(this.base, req); }
  update(id: number, req: ChangeRequestRequest) { return this.http.put<ChangeRequest>(`${this.base}/${id}`, req); }
  updateStatus(id: number, status: CrStatus) {
    return this.http.patch<ChangeRequest>(`${this.base}/${id}/status`, null, { params: { status } });
  }
  submit(id: number) { return this.http.post<ChangeRequest>(`${this.base}/${id}/submit`, {}); }
  approve(id: number) { return this.http.post<ChangeRequest>(`${this.base}/${id}/approve`, {}); }
  reject(id: number) { return this.http.post<ChangeRequest>(`${this.base}/${id}/reject`, {}); }
  delete(id: number) { return this.http.delete<void>(`${this.base}/${id}`); }
}
