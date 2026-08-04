package com.khushirathi.docquery.document;

import com.khushirathi.docquery.document.dto.DocumentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"); // .docx

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    public DocumentResponse upload(MultipartFile file, UUID userId) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF and Word (.docx) files are allowed");
        }

        Document doc = new Document(userId, file.getOriginalFilename(),
                file.getContentType(), file.getSize());
        documentRepository.save(doc);                 // 1. save the row (status = UPLOADED)
        fileStorageService.store(file, doc.getId());  // 2. save the bytes to disk

        return DocumentResponse.from(doc);
    }

    public List<DocumentResponse> list(UUID userId) {
        return documentRepository.findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public DocumentResponse get(UUID id, UUID userId) {
        Document doc = documentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return DocumentResponse.from(doc);
    }

    public void delete(UUID id, UUID userId) {
        Document doc = documentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        documentRepository.delete(doc);
        fileStorageService.delete(doc.getId());
        // (Phase 5 note: this is also where we'll delete the document's vectors)
    }
}