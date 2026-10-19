package com.epam.edp.demo.service;

import com.epam.edp.demo.config.DocumentProperties;
import com.epam.edp.demo.enums.DocumentDeletionReason;
import com.epam.edp.demo.enums.DocumentLifecycleStatus;
import com.epam.edp.demo.enums.DocumentType;
import com.epam.edp.demo.model.Booking;
import com.epam.edp.demo.model.BookingDocument;
import com.epam.edp.demo.repository.BookingDocumentRepository;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentRetentionCleanupServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingDocumentRepository bookingDocumentRepository;

    @Mock
    private FileStorageService fileStorageService;

    private DocumentRetentionCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        DocumentProperties props = new DocumentProperties();
        props.getRetention().setUnverifiedDays(2);
        cleanupService = new DocumentRetentionCleanupService(
                bookingRepository,
                bookingDocumentRepository,
                fileStorageService,
                props);
    }

    @Test
    void cleanupPassportDocumentsForCancelledBooking_deletesPassportAndRefreshesCount() {
        BookingDocument passport = activePassport("doc-1", "booking-1", "path/passport.pdf");
        Booking booking = new Booking();
        booking.setId("booking-1");

        when(bookingDocumentRepository.findByBookingIdInAndDocumentTypeAndLifecycleStatus(
                List.of("booking-1"),
                DocumentType.PASSPORT,
                DocumentLifecycleStatus.ACTIVE)).thenReturn(List.of(passport));
        when(bookingRepository.findAllById(any())).thenReturn(List.of(booking));
        when(bookingDocumentRepository.countByBookingIdAndLifecycleStatus("booking-1", DocumentLifecycleStatus.ACTIVE))
                .thenReturn(0L);
        when(bookingDocumentRepository.save(any(BookingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int deleted = cleanupService.cleanupPassportDocumentsForCancelledBooking("booking-1", "BOOKING_CANCEL_API");

        assertEquals(1, deleted);
        verify(fileStorageService).delete("path/passport.pdf");
        verify(bookingDocumentRepository).findByBookingIdInAndDocumentTypeAndLifecycleStatus(
                List.of("booking-1"),
                DocumentType.PASSPORT,
                DocumentLifecycleStatus.ACTIVE);
        verify(bookingRepository).saveAll(List.of(booking));
    }

    @Test
    void deleteDocumentWithLifecycle_marksDeletedAndClearsStoragePathOnSuccess() {
        BookingDocument passport = activePassport("doc-2", "booking-2", "path/passport2.pdf");
        when(bookingDocumentRepository.save(any(BookingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean deleted = cleanupService.deleteDocumentWithLifecycle(
                passport,
                DocumentDeletionReason.BOOKING_CANCELLED,
                true,
                "BOOKING_CANCEL_API");

        assertTrue(deleted);

        ArgumentCaptor<BookingDocument> captor = ArgumentCaptor.forClass(BookingDocument.class);
        verify(bookingDocumentRepository, atLeastOnce()).save(captor.capture());
        BookingDocument latest = captor.getValue();

        assertEquals(DocumentLifecycleStatus.DELETED, latest.getLifecycleStatus());
        assertEquals(DocumentDeletionReason.BOOKING_CANCELLED, latest.getDeletedReason());
        assertTrue(latest.isDeletedBySystem());
        assertNotNull(latest.getDeletedAt());
        assertNull(latest.getStoragePath());
    }

    @Test
    void deleteDocumentWithLifecycle_marksDeleteFailedWhenStorageDeleteThrows() {
        BookingDocument passport = activePassport("doc-3", "booking-3", "path/passport3.pdf");
        when(bookingDocumentRepository.save(any(BookingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("storage unavailable"))
                .when(fileStorageService)
                .delete(eq("path/passport3.pdf"));

        boolean deleted = cleanupService.deleteDocumentWithLifecycle(
                passport,
                DocumentDeletionReason.UNVERIFIED_TIMEOUT,
                true,
                "SCHEDULER_UNVERIFIED_TIMEOUT");

        assertFalse(deleted);

        ArgumentCaptor<BookingDocument> captor = ArgumentCaptor.forClass(BookingDocument.class);
        verify(bookingDocumentRepository, atLeastOnce()).save(captor.capture());
        BookingDocument latest = captor.getValue();

        assertEquals(DocumentLifecycleStatus.DELETE_FAILED, latest.getLifecycleStatus());
        assertEquals(DocumentDeletionReason.UNVERIFIED_TIMEOUT, latest.getDeletedReason());
        assertTrue(latest.isDeletedBySystem());
        assertNotNull(latest.getDeletionRequestedAt());
        assertNull(latest.getDeletedAt());
    }

        @Test
        void cleanupDocumentsForRemovedGuests_deletesActiveDocumentsAndRefreshesCount() {
                BookingDocument passport = activePassport("doc-4", "booking-4", "path/passport4.pdf");
                passport.setGuestId("GUEST_3");
                Booking booking = new Booking();
                booking.setId("booking-4");

                when(bookingDocumentRepository.findByBookingIdAndGuestIdInAndLifecycleStatus(
                                "booking-4",
                                List.of("GUEST_3"),
                                DocumentLifecycleStatus.ACTIVE)).thenReturn(List.of(passport));
                when(bookingRepository.findAllById(any())).thenReturn(List.of(booking));
                when(bookingDocumentRepository.countByBookingIdAndLifecycleStatus("booking-4", DocumentLifecycleStatus.ACTIVE))
                                .thenReturn(0L);
                when(bookingDocumentRepository.save(any(BookingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

                int deleted = cleanupService.cleanupDocumentsForRemovedGuests(
                                "booking-4",
                                List.of("GUEST_3"),
                                "BOOKING_EDIT_CONFIRM");

                assertEquals(1, deleted);
                verify(fileStorageService).delete("path/passport4.pdf");
                verify(bookingRepository).saveAll(List.of(booking));
        }

    private static BookingDocument activePassport(String documentId, String bookingId, String storagePath) {
        BookingDocument doc = new BookingDocument();
        doc.setDocumentId(documentId);
        doc.setBookingId(bookingId);
        doc.setDocumentType(DocumentType.PASSPORT);
        doc.setStoragePath(storagePath);
        doc.setLifecycleStatus(DocumentLifecycleStatus.ACTIVE);
        return doc;
    }
}
