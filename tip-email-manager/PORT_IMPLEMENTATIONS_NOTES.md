# New dependencies & config for the port implementations

## Maven (pom.xml)

```xml
<!-- RichTextSanitizerPortImpl -->
<dependency>
    <groupId>com.googlecode.owasp-java-html-sanitizer</groupId>
    <artifactId>owasp-java-html-sanitizer</artifactId>
    <version>20240325.1</version>
</dependency>
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>

<!-- AttachmentStoragePortImpl (email attachments only — these ARE
     virus-scanned and stored in blob storage, since they're served
     back to recipients. The FILE_UPLOAD recipient file is NOT: see
     "Recipient file storage" below.) -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
    <version>12.25.3</version>
</dependency>
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-identity</artifactId>
    <version>1.11.4</version>
</dependency>

<!-- DataSourceQueryPortImpl, RestDistributionListDirectoryPort,
     RestContactDirectoryPort use spring-boot-starter-jdbc's JdbcTemplate
     and spring-boot-starter-web's RestTemplate — both already on the
     classpath via spring-boot-starter-data-jpa / spring-boot-starter-web. -->

<!-- RecipientFileParserPortImpl (FILE_UPLOAD recipient mode — parses
     sheet names, headers, and data rows out of the uploaded file) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

## Recipient file storage (FILE_UPLOAD mode) — no port, by design

Unlike attachments, the uploaded recipient file is stored directly in
`email_template_version.recipient_file_content` (`BYTEA`) — there is no
`RecipientFileStoragePort`/adapter, because there's no external system
to abstract: it's just another JPA-mapped column, persisted by the
normal `EmailTemplateVersionRepository.save()`. It is **not virus
scanned** — it's parsed straight into `email_template_file_recipient`
rows and never executed, downloaded, or served back to a user, unlike
`email_template_attachment`. `RecipientFileParserPort` (above) still
needs Apache POI, since parsing an uploaded file's structure is a real
piece of logic worth hiding behind a port even though storage isn't.

If the file sizes you actually see in practice run meaningfully above a
few MB, revisit this — `BYTEA` columns work fine at that size but every
`SELECT *` on `email_template_version` (and every backup) carries the
weight of whatever's in this column. `RecipientFileConstants.MAX_SIZE_BYTES`
(10MB) is the only current guard.

## application.yml

```yaml
tip:
  email-manager:
    attachments:
      blob-endpoint: https://<storage-account>.blob.core.windows.net    # required
      container-name: email-template-attachments                       # optional, has a default
      virus-scan-url: https://<internal-scan-service>/api/v1/scan       # required
      virus-scan-timeout-ms: 15000                                     # optional, has a default
    data-source-query:
      columns-endpoint-base-url: https://<data-manager-service>/api/v1/data-source-queries  # required — this module appends /{id}/columns
    directory:
      distribution-lists-url: https://<email-manager-service>/api/v1/email-manager/distribution-lists  # required
      contacts-url: https://<email-manager-service>/api/v1/email-manager/contacts                      # required
```

None of the four required URLs above have defaults on purpose — the
adapters fail fast at startup rather than silently pointing at nothing.
Nothing recipient-file-related needs a config entry here — there's
nothing external to point at.

## Assumptions to reconcile before wiring these up for real

- **DataSourceQueryPortImpl** matches the real `data_source_query`
  table from your EM-1 schema (UUID `id`, `asset_id` grouping a query's
  versions, `version` INT, status PENDING_REVIEW/ACTIVE/REJECTED/RETIRED,
  plain public schema, same database — queried directly via
  `JdbcTemplate`, not a separate service). `getColumns` resolves the
  row's own `id` from `(asset_id, version)` locally, then calls the
  existing columns endpoint with that id — adjust the URL shape in
  `DataSourceQueryPortImpl.getColumns` if the real endpoint addresses by
  something else (e.g. `asset_id` + `version` as query params instead).
  **Authorization gap:** this schema has no permission/ACL table, so
  `isAuthorized` can currently only confirm the query is ACTIVE, not
  that the specific user/role is entitled to it (EM-8 AC: "access to
  sensitive sources is permission-controlled"). Flag if that lives in a
  separate catalog (e.g. under Data Manager) so a real check can be
  wired in.
- **RestDistributionListDirectoryPort / RestContactDirectoryPort**
  assume the existing "get distribution lists" / "get contacts"
  endpoints return a flat JSON array of `{id, name, status}` and fetch
  the whole list to validate a selection against it. If either endpoint
  is paginated or only supports fetch-by-id, swap `findInvalidIds` for
  a loop of individual lookups (or a batch-validate endpoint, if one
  exists) — the `DistributionListDirectoryPort`/`ContactDirectoryPort`
  contracts don't need to change either way.
- **RestVirusScanClient** (email attachments only) assumes a synchronous
  scan endpoint that returns a verdict in an `X-Scan-Verdict` response
  header. Adjust to match the platform's actual scanning service
  contract (this may instead be async, ICAP-based, or return the
  verdict in the body).
- **Recipient-row email validation** (`RecipientFileConstants.EMAIL_PATTERN`)
  is a simple regex, not RFC 5322 validation — it exists to flag obviously
  malformed rows on the Preview screen, not to be a hard send-time gate.
