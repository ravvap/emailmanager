# Postman tests — Email Templates (EM-8 … EM-13)

1. Import `EmailTemplates.postman_collection.json` and `EmailTemplates.postman_environment.json`.
2. Fill the environment: four JWTs (maker, checker, analyst, manager), a real ACTIVE `dataSourceQueryId`
   (its `asset_id`), column names that query returns (`emailColumn`, `nameColumn`, `mergeColumn`),
   and a real `distListId` / `contactId`.
   - maker and checker **must be different users** (self-approval is blocked);
   - checker must be a Sr. Analyst or Manager (approver + history access).
3. Postman → Settings → Working directory → this repo's `postman/sample-files` (upload steps use those files).
4. Run the collection folder by folder, in order (A → H) — they share state (`tid`, `crId`, version ids).

Folders: A author + approve · B edit/withdraw/reject/approve + history/compare · C restore ·
D retire/reactivate · E delete · F reject new template · G manager withdraw + reassign · H lookups/security.

Notes
- Negative cases assert exact codes (400/403/404/409) via `GlobalExceptionHandler`.
- A30 / G6 / B20–B21 / H2 / H5 depend on your SecurityFilterChain and the roles in the JWTs.
- Data-dependent: A10 needs real list/contact ids; B3/A13 assume the query's columns don't change.
