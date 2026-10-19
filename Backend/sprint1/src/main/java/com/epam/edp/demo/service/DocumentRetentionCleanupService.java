package com.epam.edp.demo.service;

import com.epam.edp.demo.config.DocumentProperties;
import com.epam.edp.demo.enums.BookingState;
import com.epam.edp.demo.enums.DocumentDeletionReason;
import com.epam.edp.demo.enums.DocumentLifecycleStatus;
import com.epam.edp.demo.enums.DocumentType;
import com.epam.edp.demo.enums.VerificationStatus;
import com.epam.edp.demo.model.Booking;
import com.epam.edp.demo.model.BookingDocument;
import com.epam.edp.demo.repository.BookingDocumentRepository;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.service.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocumentRetentionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(DocumentRetentionCleanupService.class);

    private final BookingRepository bookingRepository;
    private final BookingDocumentRepository bookingDocumentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentProperties documentProperties;

    public DocumentRetentionCleanupService(BookingRepository bookingRepository,
                                           BookingDocumentRepository bookingDocumentRepository,
                                           FileStorageService fileStorageService,
                                           DocumentProperties documentProperties) {
        this.bookingRepository = bookingRepository;
        this.bookingDocumentRepository = bookingDocumentRepository;
        this.fileStorageService = fileStorageService;
        this.documentProperties = documentProperties;
    }

    @Scheduled(cron = "${app.documents.retention.cleanup-cron:0 0 * * * *}")
    public void cleanupDocumentsByRetentionPolicy() {
        cleanupCancelledBookingPassportDocuments();
        cleanupUnverifiedExpiredPassportDocuments();
        retryPendingAndFailedPassportDeletions();
    }

    public int cleanupPassportDocumentsForCancelledBooking(String bookingId, String triggerEvent) {
        List<BookingDocument> passportDocuments = bookingDocumentRepository
                .findByBookingIdInAndDocumentTypeAndLifecycleStatus(
                        List.of(bookingId),
                        DocumentType.PASSPORT,
                        DocumentLifecycleStatus.ACTIVE);

        int deleted = deleteDocumentsWithLifecycle(
                passportDocuments,
                DocumentDeletionReason.BOOKING_CANCELLED,
                true,
                triggerEvent);

        if (deleted > 0) {
            refreshBookingDocumentCounts(List.of(bookingId));
            log.info("Passport cleanup completed for cancelled bookingId={} deletedCount={} trigger={}",
                    bookingId, deleted, triggerEvent);
        }

        return deleted;
    }

    public int cleanupDocumentsForRemovedGuests(String bookingId,
                                                List<String> guestIds,
                                                String triggerEvent) {
        if (guestIds == null || guestIds.isEmpty()) {
            return 0;
        }

        List<String> normalizedGuestIds = guestIds.stream()
                .filter(guestId -> guestId != null && !guestId.isBlank())
                .distinct()
                .toList();

        if (normalizedGuestIds.isEmpty()) {
            return 0;
        }

        List<BookingDocument> guestDocuments = bookingDocumentRepository
                .findByBookingIdAndGuestIdInAndLifecycleStatus(
                        bookingId,
                        normalizedGuestIds,
                        DocumentLifecycleStatus.ACTIVE);

        int deleted = deleteDocumentsWithLifecycle(
                guestDocuments,
                DocumentDeletionReason.USER_EXPLICIT_DELETE,
                true,
                triggerEvent);

        if (deleted > 0) {
            refreshBookingDocumentCounts(List.of(bookingId));
            log.info("Guest document cleanup completed for bookingId={} deletedCount={} guestIds={} trigger={}",
                    bookingId, deleted, normalizedGuestIds, triggerEvent);
        }

        return deleted;
    }

    public boolean deleteDocumentWithLifecycle(BookingDocument document,
                                               DocumentDeletionReason reason,
                                               boolean deletedBySystem,
                                               String triggerEvent) {
        if (document == null || document.getLifecycleStatus() == DocumentLifecycleStatus.DELETED) {
            return false;
        }

        DocumentDeletionReason effectiveReason = document.getDeletedReason() != null
            ? document.getDeletedReason()
            : reason;
        String effectiveTriggerEvent = (document.getDeletionTriggerEvent() != null
            && !document.getDeletionTriggerEvent().isBlank())
            ? document.getDeletionTriggerEvent()
            : triggerEvent;

        markPendingDeletion(document, effectiveReason, deletedBySystem, effectiveTriggerEvent);

        try {
            fileStorageService.delete(document.getStoragePath());
            markDeleted(document, effectiveReason, deletedBySystem, effectiveTriggerEvent);

            log.info("Document deleted bookingId={} documentId={} type={} reason={} trigger={} deletedBySystem={} deletedAt={}",
                    document.getBookingId(),
                    document.getDocumentId(),
                    document.getDocumentType(),
                effectiveReason,
                effectiveTriggerEvent,
                    deletedBySystem,
                    document.getDeletedAt());
            return true;
        } catch (RuntimeException ex) {
            markDeleteFailed(document, effectiveReason, deletedBySystem, effectiveTriggerEvent);
            log.warn("Failed deleting document bookingId={} documentId={} type={} reason={} trigger={} error={}",
                    document.getBookingId(),
                    document.getDocumentId(),
                    document.getDocumentType(),
                effectiveReason,
                effectiveTriggerEvent,
                    ex.getMessage());
            return false;
        }
    }

    private void cleanupCancelledBookingPassportDocuments() {
        List<Booking> cancelledBookings = bookingRepository.findByState(BookingState.CANCELLED);
        if (cancelledBookings.isEmpty()) {
            return;
        }

        List<String> cancelledBookingIds = cancelledBookings.stream()
                .map(Booking::getId)
                .toList();

        List<BookingDocument> passportDocuments = bookingDocumentRepository
                .findByBookingIdInAndDocumentTypeAndLifecycleStatus(
                        cancelledBookingIds,
                        DocumentType.PASSPORT,
                        DocumentLifecycleStatus.ACTIVE);

        if (passportDocuments.isEmpty()) {
            return;
        }

        int deleted = deleteDocumentsWithLifecycle(
                passportDocuments,
                DocumentDeletionReason.BOOKING_CANCELLED,
                true,
                "SCHEDULER_CANCELLED_BOOKING");

        if (deleted > 0) {
            refreshBookingDocumentCounts(cancelledBookingIds);
            log.info("Deleted {} passport documents for cancelled bookings", deleted);
        }
    }

    private void cleanupUnverifiedExpiredPassportDocuments() {
        int retentionDays = documentProperties.getRetention().getUnverifiedDays();
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        List<BookingDocument> passportDocuments = bookingDocumentRepository
                .findByVerificationStatusAndUploadTimestampBeforeAndDocumentTypeAndLifecycleStatus(
                        VerificationStatus.PENDING,
                        cutoff,
                        DocumentType.PASSPORT,
                        DocumentLifecycleStatus.ACTIVE);

        if (passportDocuments.isEmpty()) {
            return;
        }

        Set<String> activeBookingIds = bookingRepository.findByState(BookingState.BOOKED)
                .stream()
                .map(Booking::getId)
                .collect(Collectors.toSet());

        List<BookingDocument> eligibleDocuments = passportDocuments.stream()
                .filter(doc -> activeBookingIds.contains(doc.getBookingId()))
                .toList();

        int deleted = deleteDocumentsWithLifecycle(
                eligibleDocuments,
                DocumentDeletionReason.UNVERIFIED_TIMEOUT,
                true,
                "SCHEDULER_UNVERIFIED_TIMEOUT");

        if (deleted > 0) {
            List<String> bookingIds = eligibleDocuments.stream()
                    .map(BookingDocument::getBookingId)
                    .toList();
            refreshBookingDocumentCounts(bookingIds);
            log.info("Deleted {} unverified passport documents older than {} days", deleted, retentionDays);
        }
    }

    private void retryPendingAndFailedPassportDeletions() {
        List<BookingDocument> retryable = bookingDocumentRepository.findByDocumentTypeAndLifecycleStatusIn(
                DocumentType.PASSPORT,
                List.of(DocumentLifecycleStatus.PENDING_DELETION, DocumentLifecycleStatus.DELETE_FAILED));

        if (retryable.isEmpty()) {
            return;
        }

        int deleted = deleteDocumentsWithLifecycle(
                retryable,
                DocumentDeletionReason.UNVERIFIED_TIMEOUT,
                true,
                "SCHEDULER_RETRY");

        if (deleted > 0) {
            List<String> bookingIds = retryable.stream()
                    .map(BookingDocument::getBookingId)
                    .toList();
            refreshBookingDocumentCounts(bookingIds);
            log.info("Retry cleanup deleted {} passport documents in pending/failed state", deleted);
        }
    }

    private int deleteDocumentsWithLifecycle(List<BookingDocument> documents,
                                             DocumentDeletionReason reason,
                                             boolean deletedBySystem,
                                             String triggerEvent) {
        int deletedCount = 0;
        for (BookingDocument document : documents) {
            if (deleteDocumentWithLifecycle(document, reason, deletedBySystem, triggerEvent)) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    private void markPendingDeletion(BookingDocument document,
                                     DocumentDeletionReason reason,
                                     boolean deletedBySystem,
                                     String triggerEvent) {
        if (document.getLifecycleStatus() == DocumentLifecycleStatus.PENDING_DELETION) {
            return;
        }

        document.setLifecycleStatus(DocumentLifecycleStatus.PENDING_DELETION);
        document.setDeletionRequestedAt(Instant.now());
        document.setDeletedReason(reason);
        document.setDeletionTriggerEvent(triggerEvent);
        document.setDeletedBySystem(deletedBySystem);
        bookingDocumentRepository.save(document);
    }

    private void markDeleted(BookingDocument document,
                             DocumentDeletionReason reason,
                             boolean deletedBySystem,
                             String triggerEvent) {
        document.setLifecycleStatus(DocumentLifecycleStatus.DELETED);
        document.setDeletedAt(Instant.now());
        document.setDeletedReason(reason);
        document.setDeletionTriggerEvent(triggerEvent);
        document.setDeletedBySystem(deletedBySystem);
        document.setStoragePath(null);
        bookingDocumentRepository.save(document);
    }

    private void markDeleteFailed(BookingDocument document,
                                  DocumentDeletionReason reason,
                                  boolean deletedBySystem,
                                  String triggerEvent) {
        document.setLifecycleStatus(DocumentLifecycleStatus.DELETE_FAILED);
        document.setDeletedReason(reason);
        document.setDeletionTriggerEvent(triggerEvent);
        document.setDeletedBySystem(deletedBySystem);
        bookingDocumentRepository.save(document);
    }

    private void refreshBookingDocumentCounts(List<String> bookingIds) {
        Set<String> uniqueBookingIds = bookingIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());

        if (uniqueBookingIds.isEmpty()) {
            return;
        }

        List<Booking> bookings = bookingRepository.findAllById(uniqueBookingIds);
        for (Booking booking : bookings) {
            long count = bookingDocumentRepository.countByBookingIdAndLifecycleStatus(
                    booking.getId(),
                    DocumentLifecycleStatus.ACTIVE);
            booking.setDocumentCount((int) count);
        }
        bookingRepository.saveAll(bookings);
    }
}
