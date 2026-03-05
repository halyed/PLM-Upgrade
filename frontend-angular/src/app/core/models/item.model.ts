export type LifecycleState = 'DRAFT' | 'IN_REVIEW' | 'RELEASED' | 'OBSOLETE';

export interface Item {
  id: number;
  itemNumber: string;
  name: string;
  description?: string;
  lifecycleState: LifecycleState;
  createdAt: string;
  updatedAt: string;
}

export interface ItemRequest {
  itemNumber: string;
  name: string;
  description?: string;
  lifecycleState?: LifecycleState;
}
