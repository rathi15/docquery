package com.khushirathi.docquery.document;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public Document(UUID userId, String originalFilename, String contentType, long sizeBytes) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = DocumentStatus.UPLOADED;
        this.uploadedAt = Instant.now();
    }
}