# DocQuery — Phases (3-week plan)

Work strictly one phase at a time. A phase is done only when its acceptance criteria
pass. Tick boxes as you go; mirror progress in Memory.md.

## Phase 0 — Environment  (Week 1, Day 1)
- [x] VS Code + Extension Pack for Java + Spring Boot Extension Pack installed
- [x] JDK 17 available (`java -version`), Git configured, GitHub account ready

## Phase 1 — Skeleton + CI  (Week 1, Day 1)
- [x] Generate from start.spring.io: Maven, Java 17, Boot 3.x; deps: Web, Security,
      Validation, Actuator, Lombok (NO JPA/Flyway/Spring AI yet — see Rules #3)
- [x] `./mvnw spring-boot:run` shows Spring Security's default login page
- [x] `./mvnw test` green (contextLoads)
- [x] Repo `docquery` on GitHub, code pushed
- [x] `.github/workflows/ci.yml` — checkout, temurin 17 + maven cache, `./mvnw -B verify`
- [x] README stub with CI badge
- **Acceptance**: green run visible in the GitHub Actions tab. ✅ DONE

## Phase 2 — Database  (Week 1, Day 2)
- [x] `docker-compose.yml`: service `db` from `pgvector/pgvector:pg16`, volume, env vars
      (note: host port 5433 — old local Postgres occupied 5432)
- [x] Add deps: Spring Data JPA, PostgreSQL driver, Flyway
- [x] `V1__init.sql`: CREATE EXTENSION vector; `users`; `documents`
- [x] `application.yml` datasource via env vars, ddl-auto: validate
- [x] CI updated with pgvector service + health check so contextLoads passes on runner
- **Acceptance**: app starts against the container; Flyway log shows V1 applied;
  tables visible via `psql`. ✅ DONE
  
## Phase 3 — Auth  (Week 1, Day 3)
- [x] `User` entity + repository; BCrypt password encoding
- [x] POST /auth/register → 201; POST /auth/login → 200 with JWT
- [x] JwtService (issue/validate), JwtAuthFilter, SecurityConfig (stateless;
      /auth/** and /actuator/health public; everything else authenticated)
- **Acceptance**: 401 without token; register→login→call a protected endpoint with
  the token succeeds (via Postman).

## Phase 4 — Documents API + polish  (Week 1, Days 4–5)
- [X] Multipart upload → file saved to volume path, `documents` row status=UPLOADED, 202
- [X] List / get / delete endpoints, scoped to the authenticated user (404 for
      other users' documents)
- [X] Validation: file type (PDF/Word), max size
- [X] GlobalExceptionHandler → ProblemDetail; Swagger/OpenAPI config with Auth,
      Documents tags and JWT authorize button
- **Acceptance (Week 1 milestone)**: register → login → upload PDF → appears in
  list as UPLOADED → delete works — all via Swagger UI.

## Phase 5 — Ingestion pipeline  (Week 2, Days 1–3)
- [X] Add Spring AI BOM + OpenAI starter + Tika reader + pgvector store deps
- [X] AsyncConfig (bounded pool) + @Async IngestionService
- [X] Tika extraction → TokenTextSplitter (~500–800 tokens, 10–15% overlap) →
      batched embeddings → PgVectorStore.add with metadata {userId, documentId,
      chunkIndex, page}
- [X] Status transitions PROCESSING→READY, failures→FAILED with error_message
- [X] Document delete also deletes vectors (same service method)
- **Acceptance**: upload a real PDF → status reaches READY → chunk_count set →
  rows visible in vector_store; a corrupt file lands in FAILED, app stays healthy.

## Phase 6 — RAG query  (Week 2, Days 4–5)
- [ ] POST /query: embed question → similaritySearch(topK, filter userId
      [+documentIds]) → prompt template with labeled chunks → chat model call
- [ ] Response: answer + citations assembled from the retrieved chunks
- [ ] Guardrails: empty retrieval → honest "not found in your documents" answer
- **Acceptance (Week 2 milestone)**: ask a question about an uploaded document via
  Swagger and get a correct, cited answer; a second user cannot query it.

## Phase 7 — Tests + hardening  (Week 3, Days 1–2)
- [ ] Unit tests: chunk metadata assembly, citation mapping, status transitions,
      auth service — embedding/chat clients mocked
- [ ] Testcontainers integration test: Flyway + repositories against real pgvector
- [ ] Validation + logging pass over all endpoints
- **Acceptance**: `./mvnw verify` green locally and in CI, including Testcontainers.

## Phase 8 — Deployment  (Week 3, Days 3–5)
- [ ] Dockerfile (multi-stage build) for the app; app service added to compose
- [ ] EC2 free-tier instance: Docker + compose installed, security group 80/443/22
- [ ] Secrets via env file on the box (never in git); health check on /actuator/health
- **Acceptance**: full Swagger demo flow works against the public URL.

## Phase 9 — Documentation + buffer  (Week 3, Days 4–5, overlaps 8)
- [ ] README: what/why, architecture diagram, local setup, deployment notes,
      design-decisions section (from Architecture.md #1–9)
- [ ] Demo script: sample PDF + 3 questions that showcase citations
- **Acceptance**: a stranger could clone, run locally, and understand every choice.
