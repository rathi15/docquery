package com.khushirathi.docquery.ingestion;

import com.khushirathi.docquery.document.Document;
import com.khushirathi.docquery.document.DocumentRepository;
import com.khushirathi.docquery.document.DocumentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestionService {

    private final DocumentRepository documentRepository;
    private final TextExtractor textExtractor;
    private final TextChunker textChunker;
    private final VectorStore vectorStore;

    @Async("ingestionExecutor")
    public void ingest(UUID documentId, Path filePath) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            log.warn("Ingestion skipped, document not found documentId={}", documentId);
            return;
        }

        try {
            doc.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(doc);
            log.info("Ingestion started documentId={}", documentId);

            // 1. extract text
            String text = textExtractor.extractText(filePath);

            // 2. chunk it
            List<org.springframework.ai.document.Document> chunks = textChunker.chunk(text);

            // 3. tag each chunk with metadata (who owns it, which doc, chunk order)
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> meta = chunks.get(i).getMetadata();
                meta.put("userId", doc.getUserId().toString());
                meta.put("documentId", documentId.toString());
                meta.put("chunkIndex", i);
            }

            // 4. embed + store (Spring AI creates embeddings and writes to pgvector)
            vectorStore.add(chunks);

            // 5. mark READY
            doc.setStatus(DocumentStatus.READY);
            doc.setChunkCount(chunks.size());
            doc.setProcessedAt(Instant.now());
            documentRepository.save(doc);
            log.info("Ingestion complete documentId={} chunks={}", documentId, chunks.size());

        } catch (Exception e) {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage(e.getMessage());
            doc.setProcessedAt(Instant.now());
            documentRepository.save(doc);
            log.error("Ingestion failed documentId={}", documentId, e);
        }
    }
}