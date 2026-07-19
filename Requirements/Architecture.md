# DocQuery — Architecture

## Fixed stack (do not substitute — see Rules.md)
- Java 17, Spring Boot 3.5.16 (final 3.x patch — see Memory.md decisions log for why
  pinned over Boot 4.1.0), Maven (wrapper `mvnw` committed)
- Spring Web, Spring Security (JWT, stateless), Spring Data JPA, Validation, Actuator, Lombok
- PostgreSQL + pgvector — Docker image `pgvector/pgvector:pg16`
- Flyway for schema migrations
- Spring AI: OpenAI `text-embedding-3-small` (1536 dims) for embeddings, OpenAI chat
  model for answers, `TikaDocumentReader` for PDF/Word extraction, `TokenTextSplitter`
  for chunking, `PgVectorStore` for vector storage
- JUnit 5, Mockito, Testcontainers
- Docker Compose (local + prod), GitHub Actions CI, AWS EC2 free tier

## The two flows
**Write path (ingestion)** — POST /documents stores the file + a `documents` row
(status UPLOADED) and returns 202. An @Async worker then: PROCESSING → Tika text
extraction → TokenTextSplitter (~500–800 tokens per chunk, 10–15% overlap) → batched
embedding calls → write vectors + metadata to pgvector → READY (or FAILED with
error_message captured).

**Read path (query)** — POST /query embeds the question with the SAME embedding
model → similaritySearch top-k with metadata filter on userId (+ documentIds if
given) → retrieved chunks stuffed into a prompt template → chat model call →
response = answer + citations built from the retrieved chunks (never LLM-generated).

## Data model
Owned via Flyway:
```sql
users     (id UUID PK, email UNIQUE, password_hash, created_at)
documents (id UUID PK, user_id FK→users, original_filename, content_type,
           size_bytes, status,           -- UPLOADED | PROCESSING | READY | FAILED
           error_message, chunk_count, uploaded_at, processed_at)
```
Managed by Spring AI `PgVectorStore` (do NOT map with JPA):
```
vector_store (id UUID, content TEXT, metadata JSONB, embedding VECTOR(1536))
  metadata keys: userId, documentId, chunkIndex, page
  index: HNSW, cosine distance
```

## REST API contract (source of truth — changes must be flagged explicitly)
```
POST   /api/v1/auth/register  {email, password}                → 201 {id, email}
POST   /api/v1/auth/login     {email, password}                → 200 {accessToken, expiresIn}
POST   /api/v1/documents      multipart "file"                 → 202 {id, filename, status}
GET    /api/v1/documents                                       → 200 [{id, filename, status, uploadedAt, chunkCount}]
GET    /api/v1/documents/{id}                                  → 200 {…, errorMessage?, processedAt?}
DELETE /api/v1/documents/{id}                                  → 204
POST   /api/v1/query          {question, documentIds?, topK?=5}→ 200 {answer, citations:[{documentId, filename, chunkIndex, page?, snippet, score}]}
Errors: RFC 9457 ProblemDetail via @RestControllerAdvice
```

## Package structure (package-by-feature, layers inside each feature)
```
com.khushirathi.docquery
├── DocqueryApplication.java
├── config/          SecurityConfig, OpenApiConfig, AsyncConfig, AiConfig
├── auth/            AuthController, AuthService, JwtService, JwtAuthFilter, dto/
├── user/            User (entity), UserRepository
├── document/        DocumentController, DocumentService, Document (entity),
│                    DocumentRepository, DocumentStatus (enum), FileStorageService, dto/
├── ingestion/       IngestionService (@Async), ChunkingProperties
├── query/           QueryController, QueryService (RAG orchestration), dto/
└── common/          GlobalExceptionHandler, error types
```

## Key design decisions (memorize the WHY — interview material)
1. **202 Accepted on upload** — ingestion takes seconds-to-minutes; blocking the HTTP
   request risks timeouts and ties up threads. Client polls document status instead.
2. **@Async + bounded thread pool, not a message queue** — one node, 3-week scope.
   A queue buys restart durability + horizontal workers; the `status` column is the
   seam where a queue would slot in later. Know this as the scale-up answer.
3. **Spring AI's managed vector_store, not a custom JPA chunk entity** — velocity and
   free integration with similaritySearch + metadata filtering. Trade-off accepted:
   no FK integrity, so document delete must call vectorStore.delete(filter) in the
   SAME service method as the JPA delete (centralized cascade semantics).
4. **Citations come from retrieval results, never from the LLM's own output** —
   we know exactly which chunks we passed in; LLM-generated citations can hallucinate.
5. **Same embedding model for ingestion and queries** — different models produce
   incompatible vector spaces; similarity would be meaningless.
6. **Chunking with overlap** — embeddings have input limits; retrieval works at chunk
   granularity; overlap prevents answers being split across a boundary.
7. **Flyway from day one** — schema is code-reviewed, versioned, reproducible in CI,
   Testcontainers, and prod identically.
8. **Single EC2 + Docker Compose, Postgres on the same box (not RDS)** — deliberate
   portfolio trade-off; articulate that RDS/backups/multi-AZ is the production answer.
9. **Stateless JWT auth** — no server session state; every request carries identity;
   fits horizontal scaling and API-first clients.
