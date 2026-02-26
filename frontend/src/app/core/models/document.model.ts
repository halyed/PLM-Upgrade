export interface Document {
  id: number;
  revisionId: number;
  fileName: string;
  filePath: string;
  fileType: string;
  gltfPath?: string;
  uploadedAt: string;
}
