package com.epam.edp.demo.config;

import com.epam.edp.demo.repository.DocumentContentRepository;
import com.epam.edp.demo.service.security.ClamAvMalwareScanner;
import com.epam.edp.demo.service.security.DocumentEncryptionService;
import com.epam.edp.demo.service.security.MalwareScanner;
import com.epam.edp.demo.service.security.NoOpMalwareScanner;
import com.epam.edp.demo.service.storage.FileStorageService;
import com.epam.edp.demo.service.storage.MongoDbStorageService;
import com.epam.edp.demo.service.storage.S3StorageService;
import com.epam.edp.demo.service.validation.DocumentValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(DocumentProperties.class)
public class DocumentStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageConfig.class);

    @org.springframework.beans.factory.annotation.Value("${aws.access-key:}")
    private String awsAccessKey;

    @org.springframework.beans.factory.annotation.Value("${aws.secret-key:}")
    private String awsSecretKey;

    @org.springframework.beans.factory.annotation.Value("${aws.session-token:}")
    private String awsSessionToken;

    @org.springframework.beans.factory.annotation.Value("${app.documents.storage-provider:mongodb}")
    private String storageProvider;

    @Autowired
    private DocumentContentRepository documentContentRepository;

    @Bean
    public FileStorageService fileStorageService(DocumentProperties properties) {
        if ("local".equalsIgnoreCase(storageProvider)) {
            String localDir = System.getProperty("user.dir") + "/document-uploads";
            log.info("[DocumentStorage] LOCAL | dir={}", localDir);
            return new com.epam.edp.demo.service.storage.LocalStorageService(localDir);
        }
        if ("mongodb".equalsIgnoreCase(storageProvider)) {
            log.info("[DocumentStorage] MONGODB | storing document content in database");
            return new MongoDbStorageService(documentContentRepository);
        }
        FileStorageService svc = createS3Storage(properties);
        log.info("[DocumentStorage] S3 | bucket={} | region={} | keyPrefix={}",
                properties.getS3().getBucket(),
                properties.getS3().getRegion(),
                properties.getS3().getKeyPrefix());
        return svc;
    }

    @Bean
    public MalwareScanner malwareScanner(DocumentProperties properties) {
        if (properties.getClamav().isEnabled()) {
            return new ClamAvMalwareScanner(properties.getClamav().getHost(), properties.getClamav().getPort());
        }
        return new NoOpMalwareScanner();
    }

    @Bean
    public DocumentValidationService documentValidationService(DocumentProperties properties,
                                                               MalwareScanner malwareScanner) {
        return new DocumentValidationService(properties.getMaxFileSizeBytes(), malwareScanner);
    }

    @Bean
    public DocumentEncryptionService documentEncryptionService(DocumentProperties properties) {
        return new DocumentEncryptionService(properties.getSecurity().getEncryptionKeyBase64());
    }

    private FileStorageService createS3Storage(DocumentProperties properties) {
        if (properties.getS3().getBucket() == null || properties.getS3().getBucket().isBlank()) {
            throw new IllegalStateException(
                "S3 storage provider is selected (STORAGE_PROVIDER=s3) but no bucket is configured. "
                + "Set DOCUMENTS_S3_BUCKET or AWS_S3_BUCKET environment variable.");
        }

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getS3().getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.getS3().isPathStyleAccessEnabled())
                        .build());

        // Use explicit credentials from Spring properties if available
        if (awsAccessKey != null && !awsAccessKey.isBlank() && awsSecretKey != null && !awsSecretKey.isBlank()) {
            if (awsSessionToken != null && !awsSessionToken.isBlank()) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create(awsAccessKey, awsSecretKey, awsSessionToken)));
            } else {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(awsAccessKey, awsSecretKey)));
            }
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (properties.getS3().getEndpoint() != null && !properties.getS3().getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getS3().getEndpoint()));
        }

        return new S3StorageService(builder.build(), properties.getS3().getBucket(), properties.getS3().getKeyPrefix());
    }
}
