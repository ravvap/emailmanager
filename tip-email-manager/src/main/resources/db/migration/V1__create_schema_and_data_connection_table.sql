-- Database Location / Vault Reference Table
CREATE TABLE database_location (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hostname VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    vault_credential_ref VARCHAR(255), -- Made optional for passwordless/Managed Identity environments
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Data Connection Table
CREATE TABLE data_connection (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    database_location_id BIGINT NOT NULL REFERENCES database_location(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    is_used_in_template BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Partial index to enforce uniqueness only among active (non-deleted) connections
CREATE UNIQUE INDEX idx_data_connection_name_unique 
    ON data_connection (name) 
    WHERE deleted_at IS NULL;

-- Single System-wide No-Reply Mailbox
CREATE TABLE no_reply_mailbox (
    id BIGINT PRIMARY KEY,
    email_address VARCHAR(255) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT single_row_check CHECK (id = 1)
);

CREATE UNIQUE INDEX idx_no_reply_mailbox_email_unique 
    ON no_reply_mailbox (email_address) 
    WHERE deleted_at IS NULL;

-- Internal Domain Allowlist Table
CREATE TABLE internal_domain_allowlist (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    domain VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX idx_internal_domain_allowlist_domain_unique 
    ON internal_domain_allowlist (domain) 
    WHERE deleted_at IS NULL;