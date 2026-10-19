package com.epam.edp.demo.service.storage;

import com.epam.edp.demo.model.DocumentContent;
import com.epam.edp.demo.repository.DocumentContentRepository;

public class MongoDbStorageService implements FileStorageService {

    private final DocumentContentRepository documentContentRepository;

    public MongoDbStorageService(DocumentContentRepository documentContentRepository) {
        this.documentContentRepository = documentContentRepository;
    }

    @Override
    public StoredFileDescriptor store(String relativePath, byte[] content, String contentType) {
        DocumentContent doc = DocumentContent.builder()
                .storagePath(relativePath)
                .content(content)
                .contentType(contentType)
                .build();
        documentContentRepository.save(doc);
        return new StoredFileDescriptor(relativePath, provider());
    }

    @Override
    public byte[] read(String storagePath) {
        return documentContentRepository.findById(storagePath)
                .map(DocumentContent::getContent)
                .orElseThrow(() -> new IllegalStateException("Document not found in database: " + storagePath));
    }

    @Override
    public void delete(String storagePath) {
        documentContentRepository.deleteById(storagePath);
    }

    @Override
    public String provider() {
        return "mongodb";
    }
}
