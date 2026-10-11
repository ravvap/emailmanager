# Postman tests — Email Templates (EM-8 … EM-13)

1. Import `EmailTemplates.postman_collection.json` and `EmailTemplates.postman_environment.json`.
2. Fill the environment: four JWTs (maker, checker, analyst, manager), a real ACTIVE `dataSourceQueryId`
   (its `asset_id`), and a real `distListId` / `contactId`. Column names are **discovered** by step A1b
   (`GET /{templateId}/query-columns`) — don't set them by hand; the query must return an email-like column.
   - maker and checker **must be different users** (self-approval is blocked);
   - checker must be a Sr. Analyst or Manager (approver + history access).
3. Postman → Settings → Working directory → this repo's `postman/sample-files` (upload steps use those files).
4. The run **stops at the first unexpected status** (so one failure doesn't cascade into dozens); fix it and re-run.
   Each failing status assertion prints the response body.
5. Run the collection folder by folder, in order (A → H) — they share state (`tid`, `crId`, version ids).

Folders: A author + approve · B edit/withdraw/reject/approve + history/compare · C restore ·
D retire/reactivate · E delete · F reject new template · G manager withdraw + reassign · H lookups/security.

Notes
- Negative cases assert exact codes (400/403/404/409) via `GlobalExceptionHandler`.
- A30 / G6 / B20–B21 / H2 / H5 depend on your SecurityFilterChain and the roles in the JWTs.
- Data-dependent: A10 needs real list/contact ids; B3/A13 assume the query's columns don't change.

## RecipientModes collection (author -> submit, per recipient mode)

`RecipientModes.postman_collection.json` + `RecipientModes.postman_environment.json` — four independent flows
(CONTACT_DISTRIBUTION_LIST, DATA_SOURCE_QUERY, FILE_UPLOAD, DEFINE_AT_SEND), each: create draft -> verify query
columns -> content with merge fields -> attachment -> recipients -> preview -> submit -> verify the change-request row.
Built for the query `select ca_name, ca_type, status, email, name from em_contact where status=:status`.
Needs only a maker and a checker token (the checker just reads the approval queue). Templates are left PENDING_APPROVAL.

## EditTemplate collection (EM-9 edit, per recipient mode)

`EditTemplate.postman_collection.json` + `EditTemplate.postman_environment.json` — four flows. Edit needs an ACTIVE
template, so each flow first authors v1 and gets it approved (S steps), then edits it (E steps): start edit -> adopt latest
query version -> edit content -> edit the recipients (mode-specific) -> submit -> approval queue -> approve -> history -> compare.
Needs two different users (maker + checker). Same query and sample files as RecipientModes.

## Demo collection (happy path, full lifecycle per recipient mode)

`Demo.postman_collection.json` + `Demo.postman_environment.json` — for each of the four recipient modes, in six acts:
author -> approve · edit (v2) -> approve · restore v1 (v3) -> approve · retire -> approve · reactivate -> approve · version history.
No negative cases. After "View the template's version history", open the **Visualize** tab to show the history as a table.
Run one mode folder (or all four) in the Collection Runner. Needs a maker and a different checker.
