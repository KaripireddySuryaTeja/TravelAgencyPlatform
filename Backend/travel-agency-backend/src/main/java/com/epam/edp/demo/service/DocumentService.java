package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.DocumentMetadataResponseDTO;
import com.epam.edp.demo.dto.DocumentUploadResponseDTO;
import com.epam.edp.demo.enums.DocumentDeletionReason;
import com.epam.edp.demo.enums.DocumentLifecycleStatus;
import com.epam.edp.demo.enums.DocumentType;
import com.epam.edp.demo.enums.BookingState;
import com.epam.edp.demo.enums.Role;
import com.epam.edp.demo.enums.VerificationStatus;
import com.epam.edp.demo.model.Booking;
import com.epam.edp.demo.model.BookingDocument;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.repository.BookingDocumentRepository;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.service.security.DocumentEncryptionService;
import com.epam.edp.demo.service.storage.FileStorageService;
import com.epam.edp.demo.service.storage.StoredFileDescriptor;
import com.epam.edp.demo.service.validation.DocumentValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
            .withZone(ZoneOffset.UTC);

    private final BookingRepository bookingRepository;
    private final BookingDocumentRepository bookingDocumentRepository;
    private final UserRepository userRepository;
    private final DocumentValidationService documentValidationService;
    private final DocumentEncryptionService documentEncryptionService;
    private final FileStorageService fileStorageService;
    private final DocumentRetentionCleanupService documentRetentionCleanupService;

    public DocumentService(BookingRepository bookingRepository,
                           BookingDocumentRepository bookingDocumentRepository,
                           UserRepository userRepository,
                           DocumentValidationService documentValidationService,
                           DocumentEncryptionService documentEncryptionService,
                           FileStorageService fileStorageService,
                           DocumentRetentionCleanupService documentRetentionCleanupService) {
        this.bookingRepository = bookingRepository;
        this.bookingDocumentRepository = bookingDocumentRepository;
        this.userRepository = userRepository;
        this.documentValidationService = documentValidationService;
        this.documentEncryptionService = documentEncryptionService;
        this.fileStorageService = fileStorageService;
        this.documentRetentionCleanupService = documentRetentionCleanupService;
    }

    public DocumentUploadResponseDTO uploadDocument(String bookingId,
                                                    String authenticatedUserId,
                                                    DocumentType documentType,
                                                    String guestId,
                                                    String guestName,
                                                    MultipartFile file) {
        Booking booking = getBookingOrThrow(bookingId);
        User user = getUserOrThrow(authenticatedUserId);

        enforceUploadAccess(booking, user);

        // For payment confirmation, limit uploads to number of guests
        if (documentType == DocumentType.PAYMENT_CONFIRMATION) {
            int guestCount = booking.getPersonalDetails() != null ? booking.getPersonalDetails().size() : 0;
            long paymentDocCount = bookingDocumentRepository.countByBookingIdAndDocumentTypeAndLifecycleStatus(
                bookingId, DocumentType.PAYMENT_CONFIRMATION, DocumentLifecycleStatus.ACTIVE);
            if (paymentDocCount >= guestCount) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Maximum " + guestCount + " payment receipt(s) allowed (one per guest)");
            }
        }

        GuestContext guestContext = resolveGuestContext(booking, guestId, guestName);
        if (bookingDocumentRepository.existsByBookingIdAndGuestIdAndDocumentTypeAndLifecycleStatus(
            bookingId, guestContext.guestId(), documentType, DocumentLifecycleStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Duplicate upload. Document already exists for this booking/guest/document type");
        }

        byte[] validatedBytes = documentValidationService.validateAndExtract(file);
        byte[] encryptedBytes = documentEncryptionService.encrypt(validatedBytes);

        Instant now = Instant.now();
        // For payment confirmation, add a suffix number (1, 2, 3...)
        String suffixNumber = "";
        if (documentType == DocumentType.PAYMENT_CONFIRMATION) {
            long paymentDocCount = bookingDocumentRepository.countByBookingIdAndDocumentTypeAndLifecycleStatus(
                bookingId, DocumentType.PAYMENT_CONFIRMATION, DocumentLifecycleStatus.ACTIVE);
            suffixNumber = String.valueOf(paymentDocCount + 1);
        }
        String storedFileName = buildStoredFileName(authenticatedUserId, bookingId,
                guestContext.guestName(), documentType, now, suffixNumber);
        String relativePath = bookingId + "/" + storedFileName;

        StoredFileDescriptor stored;
        try {
            stored = fileStorageService.store(relativePath, encryptedBytes, "application/octet-stream");
        } catch (RuntimeException ex) {
            log.error("S3 upload failed for booking={}: {}", bookingId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Document storage is unavailable. Verify S3 bucket/credentials and retry.");
        }

        BookingDocument entity = BookingDocument.builder()
                .bookingId(bookingId)
                .guestId(guestContext.guestId())
                .guestName(guestContext.guestName())
                .uploadedBy(authenticatedUserId)
                .documentType(documentType)
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .storagePath(stored.storagePath())
                .storageProvider(stored.provider())
                .fileSize(file.getSize())
                .mimeType("application/pdf")
                .uploadTimestamp(now)
                .encryptionStatus("AES_GCM")
                .verificationStatus(VerificationStatus.PENDING)
                .lifecycleStatus(DocumentLifecycleStatus.ACTIVE)
                .build();

        BookingDocument saved = bookingDocumentRepository.save(entity);
            booking.setDocumentCount((int) bookingDocumentRepository.countByBookingIdAndLifecycleStatus(
                bookingId,
                DocumentLifecycleStatus.ACTIVE));
        bookingRepository.save(booking);

        return toUploadDto(saved);
    }

    public List<DocumentMetadataResponseDTO> getBookingDocuments(String bookingId, String authenticatedUserId) {
        Booking booking = getBookingOrThrow(bookingId);
        User user = getUserOrThrow(authenticatedUserId);
        enforceReadAccess(booking, user);

        return bookingDocumentRepository.findByBookingIdAndLifecycleStatusOrderByUploadTimestampDesc(
                bookingId,
                DocumentLifecycleStatus.ACTIVE)
                .stream()
                .map(this::toMetadataDto)
                .toList();
    }

    public DownloadedDocument downloadDocument(String bookingId,
                                               String documentId,
                                               String authenticatedUserId) {
        Booking booking = getBookingOrThrow(bookingId);
        User user = getUserOrThrow(authenticatedUserId);
        enforceReadAccess(booking, user);

        BookingDocument document = bookingDocumentRepository
            .findByDocumentIdAndLifecycleStatus(documentId, DocumentLifecycleStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (!bookingId.equals(document.getBookingId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found for booking");
        }

        byte[] encryptedBytes;
        try {
            encryptedBytes = fileStorageService.read(document.getStoragePath());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Document storage is unavailable. Unable to download the file right now.");
        }
        byte[] plainBytes = documentEncryptionService.decrypt(encryptedBytes);
        return new DownloadedDocument(document.getStoredFileName(), document.getMimeType(), plainBytes);
    }

    public void deleteDocument(String bookingId, String documentId, String authenticatedUserId) {
        Booking booking = getBookingOrThrow(bookingId);
        User user = getUserOrThrow(authenticatedUserId);

        // Only the booking owner or admins can delete
        if (!booking.getUserId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only delete documents from your own booking");
        }

        BookingDocument document = bookingDocumentRepository
            .findByDocumentIdAndLifecycleStatus(documentId, DocumentLifecycleStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (!bookingId.equals(document.getBookingId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found for booking");
        }

        if (document.getDocumentType() == DocumentType.PAYMENT_CONFIRMATION && user.getRole() != Role.ADMIN) {
            boolean tripConfirmed = booking.getState() == BookingState.FINISHED;
            boolean travelAgentApproved = document.getVerificationStatus() == VerificationStatus.VERIFIED;
            if (tripConfirmed || travelAgentApproved) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payment confirmation cannot be deleted after trip confirmation or agent approval");
            }
        }

        boolean deleted = documentRetentionCleanupService.deleteDocumentWithLifecycle(
            document,
            user.getRole() == Role.ADMIN ? DocumentDeletionReason.ADMIN_EXPLICIT_DELETE
                : DocumentDeletionReason.USER_EXPLICIT_DELETE,
            false,
            "API_DELETE_DOCUMENT");

        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Document storage is unavailable. Unable to delete the file right now.");
        }

        // Update document count
        booking.setDocumentCount((int) bookingDocumentRepository.countByBookingIdAndLifecycleStatus(
            bookingId,
            DocumentLifecycleStatus.ACTIVE));
        bookingRepository.save(booking);
    }

    private Booking getBookingOrThrow(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private void enforceUploadAccess(Booking booking, User user) {
        if (user.getRole() != Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only customers are allowed to upload booking documents");
        }
        if (!booking.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can upload documents only for your own booking");
        }
    }

    private void enforceReadAccess(Booking booking, User user) {
        if (booking.getUserId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.TRAVEL_AGENT) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access these documents");
    }

    private GuestContext resolveGuestContext(Booking booking, String guestIdInput, String guestNameInput) {
        if (booking.getPersonalDetails() == null || booking.getPersonalDetails().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking does not contain guest details");
        }

        if (guestIdInput != null && !guestIdInput.isBlank()) {
            GuestContext guestById = resolveGuestContextById(booking, guestIdInput);
            if (guestNameInput != null && !guestNameInput.isBlank()) {
                String normalizedProvidedName = normalizeGuestKey(guestNameInput);
                String normalizedResolvedName = normalizeGuestKey(guestById.guestName());
                if (!normalizedProvidedName.equals(normalizedResolvedName)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "guestId and guestName do not refer to the same booking guest");
                }
            }
            return guestById;
        }

        if (guestNameInput == null || guestNameInput.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "guestId or guestName is required for passport and payment uploads");
        }

        String normalizedInput = normalizeGuestKey(guestNameInput);
        GuestContext matchedGuest = null;
        for (int i = 0; i < booking.getPersonalDetails().size(); i++) {
            Booking.PersonalDetail pd = booking.getPersonalDetails().get(i);
            String bookingGuestName = (pd.getFirstName() == null ? "" : pd.getFirstName())
                    + (pd.getLastName() == null ? "" : pd.getLastName());
            if (normalizeGuestKey(bookingGuestName).equals(normalizedInput)) {
                GuestContext currentMatch = buildGuestContext(pd, i);
                if (matchedGuest != null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Multiple guests share the same name. Please upload using a unique guestId");
                }
                matchedGuest = currentMatch;
            }
        }

        if (matchedGuest != null) {
            return matchedGuest;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "guestName is not part of this booking");
    }

    private GuestContext resolveGuestContextById(Booking booking, String guestIdInput) {
        String normalizedGuestId = guestIdInput.trim().toUpperCase(Locale.ROOT);
        if (!normalizedGuestId.startsWith("GUEST_")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "guestId must follow the GUEST_n format");
        }

        int guestIndex;
        try {
            guestIndex = Integer.parseInt(normalizedGuestId.substring("GUEST_".length()));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "guestId must follow the GUEST_n format");
        }

        if (guestIndex < 1 || guestIndex > booking.getPersonalDetails().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "guestId is not part of this booking");
        }

        Booking.PersonalDetail personalDetail = booking.getPersonalDetails().get(guestIndex - 1);
        return buildGuestContext(personalDetail, guestIndex - 1);
    }

    private GuestContext buildGuestContext(Booking.PersonalDetail personalDetail, int guestIndex) {
        String displayName = (safeNamePart(personalDetail.getFirstName()) + " "
                + safeNamePart(personalDetail.getLastName())).trim();
        if (displayName.isBlank()) {
            displayName = "Guest " + (guestIndex + 1);
        }
        return new GuestContext("GUEST_" + (guestIndex + 1), displayName);
    }

    private String buildStoredFileName(String userId,
                                       String bookingId,
                                       String guestName,
                                       DocumentType documentType,
                                       Instant now,
                                       String suffixNumber) {
        String cleanUserId = sanitize(userId);
        String cleanBookingId = sanitize(bookingId);
        String cleanGuestName = sanitize(guestName);
        String cleanDocType = sanitize(documentType.name().toLowerCase(Locale.ROOT));
        String timestamp = TS_FORMAT.format(now);
        
        // For payment confirmation with suffix number, use it instead of guest name
        if (!suffixNumber.isEmpty()) {
            cleanGuestName = suffixNumber;
        }
        
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        return String.format("%s_%s_%s_%s_%s_%s.pdf",
                cleanUserId, cleanBookingId, cleanGuestName, cleanDocType, timestamp, suffix);
    }

    private String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "na";
        }
        return input.replaceAll("[^A-Za-z0-9]", "");
    }

    private String normalizeGuestKey(String name) {
        return sanitize(name).toLowerCase(Locale.ROOT);
    }

    private String safeNamePart(String value) {
        return value == null ? "" : value.trim();
    }

    private DocumentUploadResponseDTO toUploadDto(BookingDocument document) {
        return DocumentUploadResponseDTO.builder()
                .documentId(document.getDocumentId())
                .bookingId(document.getBookingId())
                .guestId(document.getGuestId())
                .guestName(document.getGuestName())
                .documentType(document.getDocumentType())
                .originalFileName(document.getOriginalFileName())
                .storedFileName(document.getStoredFileName())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .encryptionStatus(document.getEncryptionStatus())
                .verificationStatus(document.getVerificationStatus())
                .uploadTimestamp(document.getUploadTimestamp())
                .build();
    }

    private DocumentMetadataResponseDTO toMetadataDto(BookingDocument document) {
        return DocumentMetadataResponseDTO.builder()
                .documentId(document.getDocumentId())
                .bookingId(document.getBookingId())
                .guestId(document.getGuestId())
                .guestName(document.getGuestName())
                .documentType(document.getDocumentType())
                .originalFileName(document.getOriginalFileName())
                .storedFileName(document.getStoredFileName())
                .storagePath(document.getStoragePath())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .encryptionStatus(document.getEncryptionStatus())
                .verificationStatus(document.getVerificationStatus())
                .uploadTimestamp(document.getUploadTimestamp())
                .build();
    }

    private record GuestContext(String guestId, String guestName) {
    }

    public record DownloadedDocument(String fileName, String contentType, byte[] content) {
    }
}
