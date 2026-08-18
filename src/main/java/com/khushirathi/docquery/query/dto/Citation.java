package com.khushirathi.docquery.query.dto;

public record Citation(
        String documentId,
        Integer chunkIndex,
        String snippet) {}