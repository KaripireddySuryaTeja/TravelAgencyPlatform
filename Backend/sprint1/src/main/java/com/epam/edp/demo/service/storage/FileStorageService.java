package com.epam.edp.demo.service.storage;

public interface FileStorageService {

    StoredFileDescriptor store(String relativePath, byte[] content, String contentType);

    byte[] read(String storagePath);

    void delete(String storagePath);

    String provider();
}
