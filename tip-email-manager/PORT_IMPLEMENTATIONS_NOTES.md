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

## File storage (attachments + FILE_UPLOAD recipient file)

Both are stored directly in `BYTEA` columns
(`email_template_attachment.file_content`,
`email_template_version.recipient_file_content`) via normal JPA saves —
no blob storage, no storage ports, **no virus scanning anywhere in this
module**. That is a deliberate removal of a security control; if
attachments are ever sent to external recipients, consider scanning at
the mail-send boundary instead. Keep an eye on row size: every
`SELECT *` and backup carries these columns (10MB cap per file).

## application.yml

```yaml
tip:
  email-manager:
    data-source-query:
      columns-endpoint-base-url: https://<data-manager-service>/api/v1/data-source-queries  # required — this module appends /{id}/columns
    directory:
      distribution-lists-url: https://<email-manager-service>/api/v1/email-manager/distribution-lists  # required
      contacts-url: https://<email-manager-service>/api/v1/email-manager/contacts                      # required
```

None of the three required URLs above have defaults on purpose — the
adapters fail fast at startup rather than silently pointing at nothing.

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
- **Recipient-row email validation** (`RecipientFileConstants.EMAIL_PATTERN`)
  is a simple regex, not RFC 5322 validation — it exists to flag obviously
  malformed rows on the Preview screen, not to be a hard send-time gate.
