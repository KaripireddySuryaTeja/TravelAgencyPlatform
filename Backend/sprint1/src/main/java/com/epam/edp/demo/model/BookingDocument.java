package com.epam.edp.demo.model;

import com.epam.edp.demo.enums.DocumentType;
import com.epam.edp.demo.enums.DocumentDeletionReason;
import com.epam.edp.demo.enums.DocumentLifecycleStatus;
import com.epam.edp.demo.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "booking_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDocument {

    @Id
    private String documentId;

    @Indexed
    private String bookingId;

    @Indexed
    private String guestId;

    private String guestName;
    private String uploadedBy;
    private DocumentType documentType;
    private String originalFileName;
    private String storedFileName;
    private String storagePath;
    private String storageProvider;
    private long fileSize;
    private String mimeType;
    private Instant uploadTimestamp;
    private String encryptionStatus;
    private VerificationStatus verificationStatus;
    private DocumentLifecycleStatus lifecycleStatus;
    private Instant deletionRequestedAt;
    private Instant deletedAt;
    private DocumentDeletionReason deletedReason;
    private String deletionTriggerEvent;
    private boolean deletedBySystem;
}
