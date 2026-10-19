package com.epam.edp.demo.repository;

import com.epam.edp.demo.enums.DocumentType;
import com.epam.edp.demo.enums.DocumentLifecycleStatus;
import com.epam.edp.demo.enums.VerificationStatus;
import com.epam.edp.demo.model.BookingDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingDocumentRepository extends MongoRepository<BookingDocument, String> {
    List<BookingDocument> findByBookingIdAndLifecycleStatusOrderByUploadTimestampDesc(
        String bookingId,
        DocumentLifecycleStatus lifecycleStatus);

    List<BookingDocument> findByBookingIdAndGuestIdInAndLifecycleStatus(
        String bookingId,
        List<String> guestIds,
        DocumentLifecycleStatus lifecycleStatus);

    List<BookingDocument> findByBookingIdInAndDocumentTypeAndLifecycleStatus(
        List<String> bookingIds,
        DocumentType documentType,
        DocumentLifecycleStatus lifecycleStatus);

    List<BookingDocument> findByVerificationStatusAndUploadTimestampBeforeAndDocumentTypeAndLifecycleStatus(
        VerificationStatus verificationStatus,
        Instant uploadTimestamp,
        DocumentType documentType,
        DocumentLifecycleStatus lifecycleStatus);

    List<BookingDocument> findByDocumentTypeAndLifecycleStatusIn(
        DocumentType documentType,
        List<DocumentLifecycleStatus> lifecycleStatuses);

    Optional<BookingDocument> findByDocumentIdAndLifecycleStatus(String documentId, DocumentLifecycleStatus lifecycleStatus);

    long countByBookingIdAndLifecycleStatus(String bookingId, DocumentLifecycleStatus lifecycleStatus);

    long countByBookingIdAndDocumentTypeAndLifecycleStatus(String bookingId,
                               DocumentType documentType,
                               DocumentLifecycleStatus lifecycleStatus);

    boolean existsByBookingIdAndGuestIdAndDocumentTypeAndLifecycleStatus(String bookingId,
                                     String guestId,
                                     DocumentType documentType,
                                     DocumentLifecycleStatus lifecycleStatus);

    void deleteByBookingId(String bookingId);
}
