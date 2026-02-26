-- Add gltf_path column to documents for storing converted GLB path
ALTER TABLE documents ADD COLUMN IF NOT EXISTS gltf_path VARCHAR(1024);
