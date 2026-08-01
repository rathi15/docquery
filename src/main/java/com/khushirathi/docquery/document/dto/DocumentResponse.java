package com.khushirathi.docquery.document.dto;

import com.khushirathi.docquery.document.Document;
import com.khushirathi.docquery.document.DocumentStatus;
import java.time.Instant;

public record DocumentResponse(
        String id,
        String filename,
        DocumentStatus status,
        long sizeBytes,
        Integer chunkCount,
        String errorMessage,
        Instant uploadedAt,
        Instant processedAt) {

    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
                d.getId().toString(),
                d.getOriginalFilename(),
                d.getStatus(),
                d.getSizeBytes(),
                d.getChunkCount(),
                d.getErrorMessage(),
                d.getUploadedAt(),
                d.getProcessedAt());
    }
}