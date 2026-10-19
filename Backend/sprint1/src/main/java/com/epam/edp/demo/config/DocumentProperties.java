package com.epam.edp.demo.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.documents")
public class DocumentProperties {

    private long maxFileSizeBytes = 2 * 1024 * 1024;
    private S3 s3 = new S3();
    private Security security = new Security();
    private ClamAv clamav = new ClamAv();
    private Retention retention = new Retention();

    @Getter
    @Setter
    public static class S3 {
        /** S3 bucket name. Required when storage-provider=s3. */
        private String bucket;
        /** AWS region where the bucket lives. Defaults to eu-west-3 to match team bucket. */
        private String region = "eu-west-3";
        /** Key prefix (folder) inside the bucket for all uploaded documents. */
        private String keyPrefix = "tour-documents";
        /** Optional custom endpoint (e.g. LocalStack for local integration tests). Leave blank for real AWS. */
        private String endpoint;
        /** Set true only when using path-style S3 URLs (LocalStack / MinIO). False for real AWS. */
        private boolean pathStyleAccessEnabled = false;
    }

    @Getter
    @Setter
    public static class Security {
        /** Base64-encoded 32-byte AES key. No default: must be supplied via DOCUMENTS_ENCRYPTION_KEY_BASE64. */
        private String encryptionKeyBase64;
    }

    @Getter
    @Setter
    public static class ClamAv {
        private boolean enabled = false;
        private String host = "localhost";
        @Min(1)
        @Max(65535)
        private int port = 3310;
    }

    @Getter
    @Setter
    public static class Retention {
        @Min(1)
        private int unverifiedDays = 2;
        private String cleanupCron = "0 0 * * * *";
    }
}
