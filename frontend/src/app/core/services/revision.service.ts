import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Revision, RevisionRequest, RevisionStatus } from '../models/revision.model';

@Injectable({ providedIn: 'root' })
export class RevisionService {
  constructor(private http: HttpClient) {}

  getByItem(itemId: number) {
    return this.http.get<Revision[]>(`/api/items/${itemId}/revisions`);
  }
  create(itemId: number, req: RevisionRequest) {
    return this.http.post<Revision>(`/api/items/${itemId}/revisions`, req);
  }
  nextRevision(itemId: number) {
    return this.http.post<Revision>(`/api/items/${itemId}/revisions/next`, null);
  }
  updateStatus(revisionId: number, status: RevisionStatus) {
    return this.http.patch<Revision>(`/api/revisions/${revisionId}/status`, null, { params: { status } });
  }
}
