export type CrStatus = 'OPEN' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED' | 'CLOSED';

export interface ChangeRequest {
  id: number;
  title: string;
  description?: string;
  status: CrStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ChangeRequestRequest {
  title: string;
  description?: string;
  status?: CrStatus;
}
