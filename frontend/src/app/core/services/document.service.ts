import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Document } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  constructor(private http: HttpClient) {}

  getByRevision(revisionId: number) {
    return this.http.get<Document[]>(`/api/revisions/${revisionId}/documents`);
  }
  upload(revisionId: number, file: File) {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Document>(`/api/revisions/${revisionId}/documents`, form);
  }
  delete(id: number) {
    return this.http.delete<void>(`/api/documents/${id}`);
  }
  getDownloadUrl(id: number) {
    return this.http.get<{ url: string }>(`/api/documents/${id}/download-url`);
  }
}
