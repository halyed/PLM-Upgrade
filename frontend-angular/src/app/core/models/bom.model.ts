export interface BomLink {
  id: number;
  parentRevisionId: number;
  childRevisionId: number;
  childItemNumber: string;
  childRevisionCode: string;
  quantity: number;
}

export interface BomLinkRequest {
  childRevisionId: number;
  quantity: number;
}
