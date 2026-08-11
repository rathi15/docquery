package com.khushirathi.docquery.ingestion;

import java.nio.file.Path;
import java.util.stream.Collectors;

import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Component
public class TextExtractor {
    public String extractText(Path filePath){
        TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(filePath));
        return reader.get().stream()
                .map(document -> document.getText())
                .collect(Collectors.joining("\n"));
    }

}
