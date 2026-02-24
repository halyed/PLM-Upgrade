-- PLM Core Schema

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100) UNIQUE NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20) NOT NULL DEFAULT 'ENGINEER',
    enabled     BOOLEAN NOT NULL DEFAULT TRUE
);


CREATE TABLE items (
    id              BIGSERIAL PRIMARY KEY,
    item_number     VARCHAR(50) UNIQUE NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    lifecycle_state VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE revisions (
    id              BIGSERIAL PRIMARY KEY,
    item_id         BIGINT NOT NULL REFERENCES items(id),
    revision_code   VARCHAR(10) NOT NULL,  -- A, B, C...
    status          VARCHAR(50) NOT NULL DEFAULT 'IN_WORK',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (item_id, revision_code)
);

CREATE TABLE bom_links (
    id                   BIGSERIAL PRIMARY KEY,
    parent_revision_id   BIGINT NOT NULL REFERENCES revisions(id),
    child_revision_id    BIGINT NOT NULL REFERENCES revisions(id),
    quantity             NUMERIC(10, 4) NOT NULL DEFAULT 1,
    UNIQUE (parent_revision_id, child_revision_id)
);

CREATE TABLE documents (
    id           BIGSERIAL PRIMARY KEY,
    revision_id  BIGINT NOT NULL REFERENCES revisions(id),
    file_name    VARCHAR(255) NOT NULL,
    file_path    VARCHAR(1024) NOT NULL,
    file_type    VARCHAR(50),  -- STEP, PDF, etc.
    uploaded_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE change_requests (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
