package com.epam.edp.demo.service.validation;

import com.epam.edp.demo.exception.DocumentValidationException;
import com.epam.edp.demo.service.security.MalwareScanner;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class DocumentValidationService {

    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-

    private final long maxFileSizeBytes;
    private final MalwareScanner malwareScanner;

    public DocumentValidationService(long maxFileSizeBytes, MalwareScanner malwareScanner) {
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.malwareScanner = malwareScanner;
    }

    public byte[] validateAndExtract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentValidationException("File is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new DocumentValidationException("File size exceeds 2MB limit");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!originalName.endsWith(".pdf")) {
            throw new DocumentValidationException("Only PDF files are allowed");
        }

        String mimeType = file.getContentType();
        if (mimeType != null && !mimeType.equalsIgnoreCase("application/pdf")) {
            throw new DocumentValidationException("Invalid MIME type. Only application/pdf is supported");
        }

        byte[] content = toBytes(file);
        validatePdfSignature(content);
        validateNoEmbeddedExecutableMarkers(content);
        malwareScanner.scan(content);
        return content;
    }

    private void validatePdfSignature(byte[] content) {
        if (content.length < PDF_MAGIC.length) {
            throw new DocumentValidationException("Corrupted PDF file");
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (content[i] != PDF_MAGIC[i]) {
                throw new DocumentValidationException("Invalid file signature. Expected PDF");
            }
        }
    }

    private void validateNoEmbeddedExecutableMarkers(byte[] content) {
        String lowerAscii = new String(content, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);

        // Block Windows/DOS executable header at the very start of the file
        if (content.length >= 2 && content[0] == 0x4D && content[1] == 0x5A) {
            throw new DocumentValidationException("File contains blocked executable/script markers");
        }

        // Block embedded JavaScript actions in PDF structure
        if (lowerAscii.contains("/javascript") || lowerAscii.contains("<script")) {
            throw new DocumentValidationException("File contains blocked executable/script markers");
        }
    }

    private byte[] toBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new DocumentValidationException("Failed to read uploaded file");
        }
    }
}
