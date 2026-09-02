package com.khushirathi.docquery.query;

import com.khushirathi.docquery.document.Document;
import com.khushirathi.docquery.document.DocumentRepository;
import com.khushirathi.docquery.document.DocumentStatus;
import com.khushirathi.docquery.query.dto.QueryRequest;
import com.khushirathi.docquery.query.dto.QueryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class QueryServiceTest {

    private final ChunkRetriever chunkRetriever = mock(ChunkRetriever.class);
    private final PromptBuilder promptBuilder = mock(PromptBuilder.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final ChatClient.Builder chatClientBuilder =
            mock(ChatClient.Builder.class, RETURNS_DEEP_STUBS);

    private QueryService newService() {
        return new QueryService(chunkRetriever, promptBuilder, chatClientBuilder, documentRepository);
    }

    @Test
    void returnsHelpfulMessage_whenUserHasNoReadyDocuments() {
        UUID userId = UUID.randomUUID();
        when(documentRepository.findByUserIdOrderByUploadedAtDesc(userId))
                .thenReturn(List.of());   // user has no documents at all

        QueryResponse response = newService().answer(new QueryRequest("anything?"), userId);

        assertThat(response.answer()).contains("no processed documents");
        assertThat(response.citations()).isEmpty();
        verify(chunkRetriever, never()).retrieve(anyString(), any());  // never even searched
    }

    @Test
    void declines_whenNoRelevantChunksFound() {
        UUID userId = UUID.randomUUID();
        Document ready = readyDocument(userId);
        when(documentRepository.findByUserIdOrderByUploadedAtDesc(userId))
                .thenReturn(List.of(ready));
        when(chunkRetriever.retrieve(anyString(), any())).thenReturn(List.of());  // nothing matched

        QueryResponse response = newService().answer(new QueryRequest("unrelated?"), userId);

        assertThat(response.answer()).contains("could not find");
        assertThat(response.citations()).isEmpty();
    }

    @Test
    void buildsCitationsFromRetrievedChunks() {
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Document ready = readyDocument(userId);
        when(documentRepository.findByUserIdOrderByUploadedAtDesc(userId))
                .thenReturn(List.of(ready));

        org.springframework.ai.document.Document chunk =
                new org.springframework.ai.document.Document(
                        "This is the relevant passage text.",
                        Map.of("documentId", docId.toString(), "chunkIndex", 2));
        when(chunkRetriever.retrieve(anyString(), any())).thenReturn(List.of(chunk));
        when(promptBuilder.build(anyString(), any())).thenReturn("a prompt");
        when(chatClientBuilder.build().prompt().user(anyString()).call().content())
                .thenReturn("The grounded answer.");

        QueryResponse response = newService().answer(new QueryRequest("what?"), userId);

        assertThat(response.answer()).isEqualTo("The grounded answer.");
        assertThat(response.citations()).hasSize(1);
        assertThat(response.citations().get(0).documentId()).isEqualTo(docId.toString());
        assertThat(response.citations().get(0).chunkIndex()).isEqualTo(2);
        assertThat(response.citations().get(0).snippet()).contains("relevant passage");
    }

    private Document readyDocument(UUID userId) {
        Document doc = new Document(userId, "file.pdf", "application/pdf", 100L);
        doc.setStatus(DocumentStatus.READY);
        return doc;
    }
}
