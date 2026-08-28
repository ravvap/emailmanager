INSERT INTO txn.permission (code, resource, action, version, display_name, description, created_at, created_by)
VALUES 
-- Data Connection (Add, Edit, Delete, View, Test)
('EM_DATA_CONNECTION_ADD', 'DataConnection', 'add', 1, 'Add Data Connection', 'Add Data Connection', NOW(), 'system'),
('EM_DATA_CONNECTION_EDIT', 'DataConnection', 'edit', 1, 'Edit Data Connection', 'Edit Data Connection', NOW(), 'system'),
('EM_DATA_CONNECTION_DELETE', 'DataConnection', 'delete', 1, 'Delete Data Connection', 'Delete Data Connection', NOW(), 'system'),
('EM_DATA_CONNECTION_VIEW', 'DataConnection', 'view', 1, 'View Data Connection', 'View Data Connection', NOW(), 'system'),
('EM_DATA_CONNECTION_TEST', 'DataConnection', 'test', 1, 'Test Data Connection', 'Test Data Connection', NOW(), 'system')

-- No-Reply Mailbox
('EM_NOREPLY_ADD', 'NoReplyMailbox', 'add', 1, 'Add No-Reply Email Address', 'Add No-Reply Email Address', NOW(), 'system'),
('EM_NOREPLY_EDIT', 'NoReplyMailbox', 'edit', 1, 'Edit No-Reply Email Address', 'Edit No-Reply Email Address', NOW(), 'system'),
('EM_NOREPLY_VIEW', 'NoReplyMailbox', 'view', 1, 'View No-Reply Email Address', 'View No-Reply Email Address', NOW(), 'system'),
('EM_NOREPLY_DELETE', 'NoReplyMailbox', 'delete', 1, 'Delete No-Reply Email Address', 'Delete No-Reply Email Address', NOW(), 'system'),

-- Internal Domain Allowlist
('EM_DOMAIN_ADD', 'InternalDomain', 'add', 1, 'Add Internal Domain Allowlist', 'Add Internal Domain Allowlist', NOW(), 'system'),
('EM_DOMAIN_EDIT', 'InternalDomain', 'edit', 1, 'Edit Internal Domain Allowlist', 'Edit Internal Domain Allowlist', NOW(), 'system'),

-- Approved Senders & Contact Attributes
('EM_APPROVED_SENDER_ADD', 'ApprovedSender', 'add', 1, 'Add approved senders', 'Add approved senders', NOW(), 'system'),
('EM_APPROVED_SENDER_EDIT', 'ApprovedSender', 'edit', 1, 'Edit approved senders', 'Edit approved senders', NOW(), 'system'),
('EM_CONTACT_ATTR_ADD', 'ContactAttribute', 'add', 1, 'Add contact attribute', 'Add contact attribute', NOW(), 'system'),

-- Email Contacts
('EM_CONTACT_ADD', 'EmailContact', 'add', 1, 'Add email contacts', 'Add email contacts', NOW(), 'system'),
('EM_CONTACT_EDIT', 'EmailContact', 'edit', 1, 'Edit email contacts', 'Edit email contacts', NOW(), 'system'),
('EM_CONTACT_DELETE', 'EmailContact', 'delete', 1, 'Delete email contacts', 'Delete email contacts', NOW(), 'system'),

-- Distribution List
('EM_DISTRIBUTION_LIST_VIEW', 'DistributionList', 'view', 1, 'View Distribution list', 'View Distribution list', NOW(), 'system'),
('EM_DISTRIBUTION_LIST_ADD', 'DistributionList', 'add', 1, 'Add a distribution list', 'Add a distribution list', NOW(), 'system'),
('EM_DISTRIBUTION_LIST_EDIT', 'DistributionList', 'edit', 1, 'Edit a distribution list', 'Edit a distribution list', NOW(), 'system'),
('EM_DISTRIBUTION_LIST_DELETE', 'DistributionList', 'delete', 1, 'Delete a distribution list', 'Delete a distribution list', NOW(), 'system'),

-- Email Templates
('EM_TEMPLATE_ADD', 'EmailTemplate', 'add', 1, 'Add an email template', 'Add an email template', NOW(), 'system'),
('EM_TEMPLATE_EDIT', 'EmailTemplate', 'edit', 1, 'Edit an existing email template', 'Edit an existing email template', NOW(), 'system'),
('EM_TEMPLATE_DEACTIVATE', 'EmailTemplate', 'deactivate', 1, 'Deactivate an email template', 'Deactivate an email template', NOW(), 'system'),
('EM_TEMPLATE_REACTIVATE', 'EmailTemplate', 'reactivate', 1, 'Reactivate an email template', 'Reactivate an email template', NOW(), 'system'),
('EM_TEMPLATE_APPROVE', 'EmailTemplate', 'approve', 1, 'Approve template changes', 'Approve template changes', NOW(), 'system'),

-- Send & Execution
('EM_TEMPLATE_SEND_APPROVED', 'EmailTemplate', 'send', 1, 'Send approved email template', 'Send approved email template', NOW(), 'system'),
('EM_SEND_EXTERNAL_APPROVE', 'EmailSend', 'approve_external', 1, 'Approve sends to external recipients', 'Approve sends to external recipients', NOW(), 'system'),
('EM_SEND_STATUS_TRACK', 'EmailSend', 'track', 1, 'Track send status', 'Track send status', NOW(), 'system'),
('EM_SEND_EXCEL_UPLOAD', 'EmailSend', 'excel_upload', 1, 'Send via Excel Recipient Upload', 'Send via Excel Recipient Upload', NOW(), 'system'),

-- Audit & Version History
('EM_TEMPLATE_HISTORY_VIEW', 'EmailTemplate', 'view_history', 1, 'View Template Version History', 'View Template Version History', NOW(), 'system'),
('EM_TEMPLATE_RESTORE', 'EmailTemplate', 'restore', 1, 'Restore a previous approved template', 'Restore a previous approved template', NOW(), 'system'),
('EM_AUDIT_RECORD_VIEW', 'AuditRecord', 'view', 1, 'View audit record', 'View audit record', NOW(), 'system'),

-- Named Queries
('EM_QUERY_ADD', 'NamedQuery', 'add', 1, 'Add a named query', 'Add a named query', NOW(), 'system'),
('EM_QUERY_EDIT', 'NamedQuery', 'edit', 1, 'Edit a query', 'Edit a query', NOW(), 'system'),
('EM_QUERY_DELETE', 'NamedQuery', 'delete', 1, 'Delete a query', 'Delete a query', NOW(), 'system')
ON CONFLICT (code) DO NOTHING;