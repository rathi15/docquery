package com.khushirathi.docquery.query;

import com.khushirathi.docquery.document.DocumentRepository;
import com.khushirathi.docquery.document.DocumentStatus;
import com.khushirathi.docquery.query.dto.Citation;
import com.khushirathi.docquery.query.dto.QueryRequest;
import com.khushirathi.docquery.query.dto.QueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class QueryService {

    private final ChunkRetriever chunkRetriever;
    private final PromptBuilder promptBuilder;
    private final ChatClient chatClient;
    private final DocumentRepository documentRepository;   


    public QueryService(ChunkRetriever chunkRetriever,
                        PromptBuilder promptBuilder,
                        ChatClient.Builder chatClientBuilder,DocumentRepository documentRepository) {
        this.chunkRetriever = chunkRetriever;
        this.promptBuilder = promptBuilder;
        this.chatClient = chatClientBuilder.build();
        this.documentRepository = documentRepository;
    }

    public QueryResponse answer(QueryRequest request, UUID userId) {
        // Guardrail 1: does the user have any READY documents at all?
        boolean hasReadyDocs = documentRepository.findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .anyMatch(d -> d.getStatus() == DocumentStatus.READY);

        if (!hasReadyDocs) {
            return new QueryResponse(
                    "You have no processed documents yet. Upload a document and wait for it to be ready, then ask again.",
                    List.of());
        }
        // 1. retrieve relevant chunks (already filtered to this user)
        List<Document> chunks = chunkRetriever.retrieve(request.question(), userId);

        // 2. guardrail: nothing relevant found
        if (chunks.isEmpty()) {
            return new QueryResponse(
                    "I could not find that in your documents.",
                    List.of());
        }

        // 3. build the prompt and call the chat model
        String prompt = promptBuilder.build(request.question(), chunks);
        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // 4. build citations from the retrieved chunks (never from the LLM)
        List<Citation> citations = chunks.stream()
                .map(chunk -> new Citation(
                        String.valueOf(chunk.getMetadata().get("documentId")),
                        toInt(chunk.getMetadata().get("chunkIndex")),
                        snippet(chunk.getText())))
                .toList();

        log.info("Query answered userId={} chunks={}", userId, chunks.size());
        return new QueryResponse(answer, citations);
    }

    private String snippet(String text) {
        if (text == null) return "";
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }

    private Integer toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        try { return Integer.valueOf(String.valueOf(value)); }
        catch (Exception e) { return null; }
    }
}