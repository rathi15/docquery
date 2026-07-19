# DocQuery — Design conventions

NOTE: DocQuery is backend-only — there is no visual layer, so no colors, fonts, or
typography. This file covers the backend equivalent: the "look and feel" of the API
and the code, so every AI-generated piece stays consistent. The only near-visual
surface is Swagger UI (see bottom).

## URL and resource design
- Base path `/api/v1`. Version lives in the path; breaking changes → `/v2`, never
  silent mutation of v1.
- Plural nouns, no verbs: `/documents`, `/documents/{id}`, `/query` (the one
  action-style endpoint — it's an RPC-ish operation, accepted deliberately).
- IDs are UUIDs, rendered as strings in JSON.

## JSON conventions
- camelCase field names everywhere.
- Timestamps: ISO-8601 UTC strings (e.g. `2026-07-17T09:30:00Z`).
- Enums as UPPER_SNAKE strings (`"READY"`, `"FAILED"`).
- No nulls for absent optionals — omit the field.

## Status code map (use these, no others without discussion)
- 200 read/query success · 201 register · 202 upload accepted for async processing
- 204 delete success (empty body)
- 400 validation failure (field details included) · 401 missing/invalid token
- 404 not found — ALSO used when a document exists but belongs to another user
  (never 403: a 403 leaks that the resource exists)
- 409 duplicate email on register · 413 file too large · 415 unsupported file type
- 500 unexpected — generic message only, details go to logs

## Error body (RFC 9457 ProblemDetail — every non-2xx)
```json
{ "type": "about:blank", "title": "Validation failed", "status": 400,
  "detail": "question must not be blank", "instance": "/api/v1/query" }
```

## Naming
- DTOs: records, `RegisterRequest`, `LoginResponse`, `DocumentResponse`,
  `QueryRequest`, `QueryResponse`, `Citation`.
- Services end in `Service`; one public responsibility each.
- Tests: `class DocumentServiceTest`, methods `delete_otherUsersDocument_returns404`.
- SQL: snake_case columns, singular-free table names as in Architecture.md.
- Config properties under a `docquery.` prefix (e.g. `docquery.ingestion.chunk-size`)
  bound via `@ConfigurationProperties`.

## Logging voice
- INFO: lifecycle events with ids — `Ingestion complete documentId={} chunks={}`.
- WARN: recoverable oddities. ERROR: failures with exception attached.
- Never: document text, questions/answers content, tokens, keys, passwords.

## Swagger UI (the project's only "front end")
- Three tags in this order: Auth, Documents, Query.
- Every endpoint: `@Operation(summary = ...)` one-liner, worded for a recruiter
  skimming the demo.
- JWT wired to the Authorize button (bearer scheme) so the whole demo runs in-browser.
- App title "DocQuery API", version matches the pom.
