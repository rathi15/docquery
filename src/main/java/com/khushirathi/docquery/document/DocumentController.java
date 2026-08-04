package com.khushirathi.docquery.document;

import com.khushirathi.docquery.auth.CurrentUser;
import com.khushirathi.docquery.document.dto.DocumentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)   // 202
    public DocumentResponse upload(@RequestParam("file") MultipartFile file) {
        return documentService.upload(file, CurrentUser.id());
    }

    @GetMapping
    public List<DocumentResponse> list() {
        return documentService.list(CurrentUser.id());
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable UUID id) {
        return documentService.get(id, CurrentUser.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)  // 204
    public void delete(@PathVariable UUID id) {
        documentService.delete(id, CurrentUser.id());
    }
}