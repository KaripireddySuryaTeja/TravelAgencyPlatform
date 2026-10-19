package com.epam.edp.demo.dto;

import com.epam.edp.demo.enums.DocumentType;
import com.epam.edp.demo.enums.VerificationStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class DocumentUploadResponseDTO {
    String documentId;
    String bookingId;
    String guestId;
    String guestName;
    DocumentType documentType;
    String originalFileName;
    String storedFileName;
    long fileSize;
    String mimeType;
    String encryptionStatus;
    VerificationStatus verificationStatus;
    Instant uploadTimestamp;
}
