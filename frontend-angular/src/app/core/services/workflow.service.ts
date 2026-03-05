import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WorkflowResponse {
  processInstanceKey: string;
  revisionId: number;
  status: string;
  message: string;
}

export interface WorkflowInstance {
  processInstanceKey: string;
  revisionId: number;
  itemId: number;
  itemName: string;
  revisionCode: string;
  submittedBy: string;
  status: string;       // RUNNING | COMPLETED | REJECTED
  currentStep: string;  // MANAGER_REVIEW | QUALITY_REVIEW | RELEASED | REJECTED
  startedAt: string;
}

@Injectable({ providedIn: 'root' })
export class WorkflowService {
  constructor(private http: HttpClient) {}

  startApproval(revisionId: number, body: {
    itemId: number;
    itemName: string;
    revisionCode: string;
    submittedBy: string;
  }): Observable<WorkflowResponse> {
    return this.http.post<WorkflowResponse>(`/api/workflows/revisions/${revisionId}/start`, body);
  }

  getByRevision(revisionId: number): Observable<WorkflowInstance[]> {
    return this.http.get<WorkflowInstance[]>(`/api/workflows/revisions/${revisionId}`);
  }

  getAll(): Observable<WorkflowInstance[]> {
    return this.http.get<WorkflowInstance[]>('/api/workflows');
  }
}
