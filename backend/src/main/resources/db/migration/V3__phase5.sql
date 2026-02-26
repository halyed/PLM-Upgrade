-- Phase 5: async conversion status, CR workflow, audit log

-- Async conversion status on documents
ALTER TABLE documents ADD COLUMN IF NOT EXISTS conversion_status VARCHAR(20) DEFAULT 'N_A';
UPDATE documents SET conversion_status = CASE
    WHEN file_type IN ('STEP','STP') AND gltf_path IS NOT NULL THEN 'DONE'
    WHEN file_type IN ('STEP','STP') AND gltf_path IS NULL  THEN 'FAILED'
    ELSE 'N_A'
END;

-- CR workflow: linked item, submitter, reviewer
ALTER TABLE change_requests ADD COLUMN IF NOT EXISTS linked_item_id BIGINT REFERENCES items(id) ON DELETE SET NULL;
ALTER TABLE change_requests ADD COLUMN IF NOT EXISTS submitted_by  VARCHAR(100);
ALTER TABLE change_requests ADD COLUMN IF NOT EXISTS reviewed_by   VARCHAR(100);
ALTER TABLE change_requests ADD COLUMN IF NOT EXISTS reviewed_at   TIMESTAMP;

-- Audit log table
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT,
    details     VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_entity  ON audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log(created_at DESC);
