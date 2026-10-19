package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.DocumentMetadataResponseDTO;
import com.epam.edp.demo.dto.DocumentUploadResponseDTO;
import com.epam.edp.demo.enums.DocumentType;
import com.epam.edp.demo.service.DocumentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponseDTO> uploadDocument(
            @PathVariable String bookingId,
            @RequestParam DocumentType documentType,
            @RequestParam(required = false) String guestId,
            @RequestParam(required = false) String guestName,
            @RequestParam("file") MultipartFile file) {
        String userId = getAuthenticatedUserId();
        DocumentUploadResponseDTO response = documentService.uploadDocument(
                bookingId, userId, documentType, guestId, guestName, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentMetadataResponseDTO>> listDocuments(@PathVariable String bookingId) {
        String userId = getAuthenticatedUserId();
        return ResponseEntity.ok(documentService.getBookingDocuments(bookingId, userId));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String bookingId,
                                                   @PathVariable String documentId) {
        String userId = getAuthenticatedUserId();
        DocumentService.DownloadedDocument document =
                documentService.downloadDocument(bookingId, documentId, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(document.fileName()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(document.content());
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String bookingId,
                                               @PathVariable String documentId) {
        String userId = getAuthenticatedUserId();
        documentService.deleteDocument(bookingId, documentId, userId);
        return ResponseEntity.noContent().build();
    }

    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return auth.getName();
    }
}
