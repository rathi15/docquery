package com.khushirathi.docquery.document;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${docquery.storage.location}") String location) {
        this.root = Paths.get(location).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create storage folder", e);
        }
    }

    public void store(MultipartFile file, UUID documentId) {
        try {
            Path target = root.resolve(documentId.toString());
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }
    }
}