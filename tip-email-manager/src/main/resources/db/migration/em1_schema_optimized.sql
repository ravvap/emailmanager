-- =====================================================================
-- EM-1 / TIP Email Manager Data Connections — Optimized Schema
-- Changes from original are called out in comments below each fix.
-- =====================================================================

-- Database Location / Vault Reference Table
CREATE TABLE database_location (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hostname VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    vault_credential_ref VARCHAR(255), -- optional for passwordless/Managed Identity environments
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- FIX: nothing previously stopped two active rows pointing at the same
-- host/port/service/user — easy to end up with duplicate vault entries.
CREATE UNIQUE INDEX idx_database_location_unique
    ON database_location (hostname, port, service_name, username)
    WHERE deleted_at IS NULL;

-- Data Connection Table
CREATE TABLE data_connection (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    database_location_id BIGINT NOT NULL
        REFERENCES database_location(id) ON DELETE RESTRICT,
    -- FIX: explicit ON DELETE RESTRICT — this is a reference table, a
    -- location should never silently vanish out from under a connection.
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    is_used_in_template BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX idx_data_connection_name_unique
    ON data_connection (name)
    WHERE deleted_at IS NULL;

-- FIX: FK columns aren't auto-indexed in Postgres. "Test Connection" /
-- "list connections for a location" style lookups need this.
CREATE INDEX idx_data_connection_database_location_id
    ON data_connection (database_location_id);

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

-- FIX: case-insensitive, consistent with the contact-email pattern below.
CREATE UNIQUE INDEX idx_no_reply_mailbox_email_unique
    ON no_reply_mailbox (LOWER(email_address))
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

-- FIX: domains are case-insensitive (RFC 1035); "FDIC.gov" and
-- "fdic.gov" were previously allowed as two distinct rows.
CREATE UNIQUE INDEX idx_internal_domain_allowlist_domain_unique
    ON internal_domain_allowlist (LOWER(domain))
    WHERE deleted_at IS NULL;

CREATE TABLE approved_sender (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    mailbox_address VARCHAR(255) NOT NULL,
    -- FIX: dropped the inline UNIQUE constraint — see index below.
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- FIX: the original `UNIQUE` was a table-level constraint, so it ignored
-- deleted_at — once a sender was soft-deleted, its address could never
-- be re-registered. Every other soft-deletable table in this schema
-- uses a partial index; this brings approved_sender in line with that
-- pattern and normalizes case at the same time.
CREATE UNIQUE INDEX idx_approved_sender_mailbox_unique
    ON approved_sender (LOWER(mailbox_address))
    WHERE deleted_at IS NULL;

-- Main attribute metadata table
CREATE TABLE contact_attribute (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    -- FIX: dropped inline UNIQUE — same soft-delete problem as
    -- approved_sender above (a deleted attribute name could never be
    -- reused). See partial index below.
    type VARCHAR(20) NOT NULL CHECK (type IN ('Text', 'Number', 'Date', 'Fixed List')), -- immutable after creation (enforce in app layer)
    default_value VARCHAR(500), -- optional single default for Text/Number/Date types
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    -- FIX: was 'Active'/'Inactive' here vs 'ACTIVE'/'INACTIVE' on every
    -- other status column in the schema (data_connection,
    -- internal_domain_allowlist, approved_sender). Same casing mismatch
    -- existed on `contact` below. Standardized to uppercase everywhere
    -- so the app-layer status enum/constants file (per your usual
    -- pattern of centralizing these) only needs one set of values.
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX idx_contact_attribute_name_unique
    ON contact_attribute (LOWER(name))
    WHERE deleted_at IS NULL;

-- Table for allowed option values when type = 'Fixed List'
CREATE TABLE contact_attribute_option (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    attribute_id BIGINT NOT NULL
        REFERENCES contact_attribute(id) ON DELETE RESTRICT,
    option_value VARCHAR(255) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
    -- FIX: added updated_by/deleted_by/deleted_at — every other table
    -- in the schema soft-deletes; this one only had created_at, so an
    -- option value couldn't be retired without a hard DELETE, which
    -- would silently orphan any contact_attribute_value rows still
    -- pointing at that option's text.
);

-- FIX: constraint moved off the table definition and scoped to
-- deleted_at, same reasoning as contact_attribute/approved_sender.
CREATE UNIQUE INDEX idx_contact_attribute_option_unique
    ON contact_attribute_option (attribute_id, option_value)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_contact_attribute_option_attribute_id
    ON contact_attribute_option (attribute_id);

-- Table storing main contact entity
CREATE TABLE contact (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    organization VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uq_contact_active_email
    ON contact (LOWER(email)) WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- Dynamic metadata attributes associated with contacts (EM-5 integration)
CREATE TABLE contact_attribute_value (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contact_id BIGINT NOT NULL REFERENCES contact(id) ON DELETE CASCADE,
    attribute_id BIGINT NOT NULL
        REFERENCES contact_attribute(id) ON DELETE RESTRICT,
    attribute_value VARCHAR(500) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    -- FIX: added updated_by/updated_at — every other table tracks who
    -- last touched a row; this one had no way to record an edit vs. a
    -- fresh insert (e.g. a Fixed List value changed later).
    CONSTRAINT uq_contact_attribute UNIQUE (contact_id, attribute_id)
);

-- FIX: attribute_id wasn't indexed on its own — the leading column of
-- uq_contact_attribute is contact_id, so "give me every value recorded
-- against attribute X" (e.g. reporting on a Fixed List field) would
-- have forced a full scan.
CREATE INDEX idx_contact_attribute_value_attribute_id
    ON contact_attribute_value (attribute_id);

-- NOTE (not enforceable in plain DDL, flagging for the app/service
-- layer): nothing here stops a Number- or Date-typed attribute from
-- getting a garbage string in attribute_value, and nothing stops an
-- option_value being retired from contact_attribute_option while a
-- contact_attribute_value still references its text by string equality
-- rather than by FK. Worth validating type + Fixed-List membership in
-- the service layer before insert/update, since there's no clean way
-- to express "value must match one of this attribute's options" as a
-- table-level CHECK across two tables without a trigger.

    
    
-- Main Distribution List table
CREATE TABLE distribution_list (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Active' CHECK (status IN ('Active', 'Inactive')),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Unique index ensuring active distribution list names are unique
CREATE UNIQUE INDEX uq_distribution_list_active_name 
ON distribution_list (LOWER(name)) 
WHERE status = 'Active' AND deleted_at IS NULL;

-- Junction table mapping Distribution Lists to Contacts
CREATE TABLE distribution_list_member (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    list_id BIGINT NOT NULL REFERENCES distribution_list(id) ON DELETE CASCADE,
    contact_id BIGINT NOT NULL REFERENCES contact(id) ON DELETE CASCADE,
    added_by VARCHAR(100) NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_list_contact UNIQUE (list_id, contact_id)
);

CREATE TABLE data_source_query (
    id UUID NOT NULL PRIMARY KEY,
    asset_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    connection_id VARCHAR(255) NOT NULL,
    sql_text TEXT NOT NULL,
    parameters VARCHAR(1000),
    version INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    reference_count INT NOT NULL DEFAULT 0,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMPTZ,
    
    -- Check constraint for allowed status values
    CONSTRAINT chk_query_status CHECK (status IN ('PENDING_REVIEW', 'ACTIVE', 'REJECTED', 'RETIRED')),
    
    -- Foreign key linking to data_connection table
    CONSTRAINT fk_ds_query_connection FOREIGN KEY (connection_id) 
        REFERENCES data_connection (id) ON DELETE RESTRICT
);

-- Index for optimizing joins/filters on connection_id
CREATE INDEX idx_ds_query_connection 
ON data_source_query (connection_id);

-- Index to quickly query all versions of a query asset ordered by version descending
CREATE INDEX idx_ds_query_asset_version 
ON data_source_query (asset_id, version DESC);

-- Index for listing/filtering queries by status and creation time
CREATE INDEX idx_ds_query_status_created 
ON data_source_query (status, created_at DESC);