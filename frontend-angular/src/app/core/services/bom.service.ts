import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BomLink, BomLinkRequest } from '../models/bom.model';

@Injectable({ providedIn: 'root' })
export class BomService {
  constructor(private http: HttpClient) {}

  getChildren(revisionId: number) {
    return this.http.get<BomLink[]>(`/api/revisions/${revisionId}/bom/children`);
  }
  addChild(revisionId: number, req: BomLinkRequest) {
    return this.http.post<BomLink>(`/api/revisions/${revisionId}/bom/children`, req);
  }
  removeChild(revisionId: number, childRevisionId: number) {
    return this.http.delete<void>(`/api/revisions/${revisionId}/bom/children/${childRevisionId}`);
  }
}
