# TIP Email Manager — Data Source Connections (EM-1 / US #7574)

Spring Boot 3.2.5 / Java 17 module implementing **"Manage SQL Data Source Connections"**
for the FDIC TIP Email Manager, built from the user story, EM-1 wireframes (Data Source
Connections grid, Add/Edit Data Connection modals), and the EM-1 spec sheet.

## What's implemented

| Acceptance criterion | Where |
|---|---|
| Register a connection with name, description, database location, vaulted credential reference | `DataConnectionController#create`, `DataConnectionServiceImpl#create` |
| "Test Connection" confirms the connection works before saving; failures show a plain, safe message | `POST /data-connections/test`, `ConnectionTestServiceImpl` (never leaks the underlying `SQLException` message) |
| Active/Inactive status; only Active connections selectable for templates | `PATCH /data-connections/{id}/status`, `GET /data-connections/active` |
| Access is permission-controlled per connection, per author | `tip_data_connection_author` table, `POST/DELETE /data-connections/{id}/authors` |
| Unused + inactive connections can be permanently deleted; connections with history cannot | `tip_data_connection_usage` ledger + `DataConnectionServiceImpl#delete` guard |
| Every create/edit/status-change is audited | `tip_data_connection_audit_log`, `AuditLogService` (also covers test attempts and author grants/revokes) |
| Duplicate name shows "Data Connection Name already exists." (Add modal wireframe) | `DuplicateNameException` → HTTP 409 |

## Project layout

```
src/main/java/com/fdic/tip/emailmanager/
  config/         SecurityConfig (JWT + local profile), OpenApiConfig
  constant/       Messages, FieldLimits, SecurityRoles, ApiPaths
  controller/     DataConnectionController
  dto/            request/ and response/ payloads
  entity/         DataConnection, DataConnectionAuthor, DataConnectionAuditLog,
                  DataConnectionUsage, ConnectionStatus, AuditAction
  exception/      Domain exceptions + GlobalExceptionHandler
  mapper/         MapStruct entity <-> DTO mapper
  repository/     Spring Data JPA repositories
  service/        Interfaces + impl (business rules, audit, connection test, vault resolver)
src/main/resources/db/migration/   Flyway V1-V4
src/test/java/    Unit tests for the delete guard, duplicate-name check, not-found handling
```

## Security model

Two layers, intentionally:

1. **URL-level rules** in `SecurityConfig` (matches the pattern used elsewhere on TIP —
   more reliable in test slices than relying on method security alone).
2. **`@PreAuthorize`** on every service method (mirrored on controller methods) as
   defense-in-depth, using `SecurityRoles.CAN_MANAGE_CONNECTIONS` /
   `CAN_VIEW_CONNECTIONS` SpEL constants. Roles come from the Azure AD JWT `roles` claim,
   mapped to `ROLE_*` authorities in `SecurityConfig#jwtAuthenticationConverter`.

A `local` Spring profile (`spring.profiles.active=local`, the default) wires a
permit-all filter chain for local testing without standing up Azure AD — same pattern
used on the other TIP services. **Never** activate `local` outside a developer machine.

## Not yet wired (flagged with TODOs in code)

- **`AzureKeyVaultCredentialResolver`** is a stub — it throws `UnsupportedOperationException`
  until it's pointed at a real Key Vault `SecretClient`, consistent with the Key Vault
  integration already used by the TIP Quartz Scheduler service. Test Connection calls
  will fail until this is wired.
- **Template usage tracking**: `tip_data_connection_usage` is a generic ledger table with
  no writer yet, because the Email Manager Template module doesn't exist yet. Once it
  does, it should insert a row here the first time a template references a connection —
  that's what the delete guard checks against.

## Running locally

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Requires a local PostgreSQL instance matching `spring.datasource.*` in `application.yml`
(defaults to `localhost:5432/tip_email_manager`); Flyway creates the `txn` schema and
tables on startup.

## Verification note

This environment's outbound network allowlist does not include Maven Central, so a full
`mvn compile`/`mvn test` could not be executed here. The code was written and reviewed
carefully against Spring Boot 3.2 / Spring Data JPA / MapStruct APIs, but please run
`mvn clean verify` in your own environment before merging.
