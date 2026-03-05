export type CrStatus = 'OPEN' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED' | 'CLOSED';

export interface ChangeRequest {
  id: number;
  title: string;
  description?: string;
  status: CrStatus;
  linkedItemId?: number;
  submittedBy?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChangeRequestRequest {
  title: string;
  description?: string;
  linkedItemId?: number;
}
