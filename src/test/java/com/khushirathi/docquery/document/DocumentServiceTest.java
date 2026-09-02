package com.khushirathi.docquery.document;

import com.khushirathi.docquery.ingestion.IngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final IngestionService ingestionService = mock(IngestionService.class);
    private final VectorStore vectorStore = mock(VectorStore.class);

    private DocumentService newService() {
        return new DocumentService(documentRepository, fileStorageService,
                ingestionService, vectorStore);
    }

    @Test
    void rejectsEmptyFile() {
        UUID userId = UUID.randomUUID();
        MultipartFile empty = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> newService().upload(empty, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void rejectsUnsupportedFileType() {
        UUID userId = UUID.randomUUID();
        MultipartFile txt = new MockMultipartFile(
                "file", "note.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> newService().upload(txt, userId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void acceptsPdf_savesRowThenFile_andTriggersIngestion() {
        UUID userId = UUID.randomUUID();
        MultipartFile pdf = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "real content".getBytes());

        newService().upload(pdf, userId);

        verify(documentRepository).save(any(Document.class));     // row saved
        verify(fileStorageService).store(eq(pdf), any(UUID.class)); // file stored
        verify(ingestionService).ingest(any(UUID.class), any());  // ingestion triggered
    }

    @Test
    void get_returns404_whenDocumentBelongsToAnotherUser() {
        UUID askingUser = UUID.randomUUID();
        UUID someDocId = UUID.randomUUID();
        // repository finds nothing for this user+doc combination (it's someone else's)
        when(documentRepository.findByIdAndUserId(someDocId, askingUser))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService().get(someDocId, askingUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void delete_removesRowFileAndVectors() {
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Document doc = new Document(userId, "doc.pdf", "application/pdf", 100L);
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));

        newService().delete(docId, userId);

        verify(documentRepository).delete(doc);           // row removed
        verify(fileStorageService).delete(doc.getId());   // file removed
        verify(vectorStore).delete(any(org.springframework.ai.vectorstore.filter.Filter.Expression.class)); // vectors removed
    }
}
