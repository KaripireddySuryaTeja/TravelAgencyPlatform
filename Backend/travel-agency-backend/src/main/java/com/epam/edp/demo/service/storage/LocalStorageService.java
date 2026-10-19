package com.epam.edp.demo.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalStorageService implements FileStorageService {

    private final Path root;

    public LocalStorageService(String rootDir) {
        this.root = Paths.get(rootDir).toAbsolutePath().normalize();
    }

    @Override
    public StoredFileDescriptor store(String relativePath, byte[] content, String contentType) {
        try {
            Path target = root.resolve(relativePath).normalize();
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(target, content);
            return new StoredFileDescriptor(relativePath, provider());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store document locally", e);
        }
    }

    @Override
    public byte[] read(String storagePath) {
        try {
            Path target = root.resolve(storagePath).normalize();
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read document from local storage", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path target = root.resolve(storagePath).normalize();
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete document from local storage", e);
        }
    }

    @Override
    public String provider() {
        return "local";
    }
}
