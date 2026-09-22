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
    
    
CREATE TABLE approved_sender (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    mailbox_address VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);    

-- Main attribute metadata table
CREATE TABLE contact_attribute (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE, -- Editable name, unique across system
    type VARCHAR(20) NOT NULL CHECK (type IN ('Text', 'Number', 'Date', 'Fixed List')), -- Type is immutable after creation
    default_value VARCHAR(500), -- Optional single default for Text/Number/Date types
    status VARCHAR(20) NOT NULL DEFAULT 'Active' CHECK (status IN ('Active', 'Inactive')),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Table for allowed option values when type = 'Fixed List'
CREATE TABLE contact_attribute_option (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    attribute_id BIGINT NOT NULL REFERENCES contact_attribute(id),
    option_value VARCHAR(255) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_attribute_option UNIQUE(attribute_id, option_value)
);
-- Table storing main contact entity
CREATE TABLE contact (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    organization VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Active' CHECK (status IN ('Active', 'Inactive')),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Partial unique index ensuring active contacts have unique emails
CREATE UNIQUE INDEX uq_contact_active_email ON contact (LOWER(email)) WHERE status = 'Active' AND deleted_at IS NULL;

-- Dynamic metadata attributes associated with contacts (EM-5 integration)
CREATE TABLE contact_attribute_value (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contact_id BIGINT NOT NULL REFERENCES contact(id) ON DELETE CASCADE,
    attribute_id BIGINT NOT NULL REFERENCES contact_attribute(id),
    attribute_value VARCHAR(500) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_contact_attribute UNIQUE (contact_id, attribute_id)
);


-- GIN Trigram index on jsonb cast directly to lower text
CREATE INDEX idx_audit_details_lower_trgm 
ON business_events_audit USING gin (LOWER((details)::text) gin_trgm_ops);

CREATE INDEX idx_audit_email_manager 
ON business_events_audit (timestamp DESC) 
WHERE LOWER(target_entity_label) LIKE '%email manager%';

CREATE INDEX idx_audit_actor_timestamp 
ON business_events_audit (actor_label, timestamp DESC);

CREATE INDEX idx_audit_module_timestamp 
ON business_events_audit (module, timestamp DESC);

-- GIN Trigram index casting details directly to text without a custom function
CREATE INDEX idx_audit_details_trgm 
ON business_events_audit USING gin (LOWER((details)::text) gin_trgm_ops);