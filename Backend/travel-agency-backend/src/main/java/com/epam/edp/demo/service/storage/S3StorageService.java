package com.epam.edp.demo.service.storage;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3StorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String keyPrefix;

    public S3StorageService(S3Client s3Client, String bucket, String keyPrefix) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.keyPrefix = keyPrefix == null ? "" : keyPrefix.trim();
    }

    @Override
    public StoredFileDescriptor store(String relativePath, byte[] content, String contentType) {
        String key = buildKey(relativePath);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .serverSideEncryption("AES256")
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content));
        return new StoredFileDescriptor(key, provider());
    }

    @Override
    public byte[] read(String storagePath) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storagePath)
                .build();
        ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(request);
        return bytes.asByteArray();
    }

    @Override
    public void delete(String storagePath) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storagePath)
                .build();
        s3Client.deleteObject(request);
    }

    @Override
    public String provider() {
        return "s3";
    }

    private String buildKey(String relativePath) {
        if (keyPrefix.isBlank()) {
            return relativePath;
        }
        return keyPrefix + "/" + relativePath;
    }
}
