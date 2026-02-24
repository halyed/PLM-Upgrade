export type RevisionStatus = 'IN_WORK' | 'IN_REVIEW' | 'RELEASED' | 'OBSOLETE';

export interface Revision {
  id: number;
  itemId: number;
  itemNumber: string;
  revisionCode: string;
  status: RevisionStatus;
  createdAt: string;
}

export interface RevisionRequest {
  revisionCode: string;
  status?: RevisionStatus;
}
