package com.epam.edp.demo.service.report;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses uploaded report files (CSV, XLSX, PDF, TXT) into plain text
 * suitable for submission to the AI analysis endpoint.
 */
@Service
public class FileParserService {

    private static final Logger log = LoggerFactory.getLogger(FileParserService.class);

    /**
     * Parses the uploaded file based on its extension / MIME type.
     *
     * @param file the uploaded report file
     * @return extracted text content
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if the file type is not supported
     */
    public String parseFile(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }

        log.debug("Parsing file: name={}, contentType={}, ext={}", originalFilename, contentType, ext);

        if ("csv".equals(ext) || "text/csv".equals(contentType) || "application/csv".equals(contentType)) {
            return parseCsv(file);
        } else if ("xlsx".equals(ext)
                || "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(contentType)) {
            return parseXlsx(file);
        } else if ("pdf".equals(ext) || "application/pdf".equals(contentType)) {
            return parsePdf(file);
        } else if ("txt".equals(ext) || "text/plain".equals(contentType)) {
            return parseTxt(file);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported file type: '" + ext + "'. Supported formats: CSV, XLSX, PDF, TXT.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private parsers
    // ─────────────────────────────────────────────────────────────────────────

    private String parseCsv(MultipartFile file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader,
                     CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

            List<String> headers = parser.getHeaderNames();
            if (!headers.isEmpty()) {
                sb.append(String.join(" | ", headers)).append("\n");
                sb.append("-".repeat(headers.size() * 15)).append("\n");
            }
            for (CSVRecord record : parser) {
                List<String> values = new ArrayList<>();
                for (String header : headers) {
                    values.add(record.get(header));
                }
                sb.append(String.join(" | ", values)).append("\n");
            }
        }
        log.debug("CSV parsed: {} chars", sb.length());
        return sb.toString();
    }

    private String parseXlsx(MultipartFile file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
                Sheet sheet = workbook.getSheetAt(si);
                sb.append("=== Sheet: ").append(sheet.getSheetName()).append(" ===\n");
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        cells.add(cell.toString());
                    }
                    sb.append(String.join(" | ", cells)).append("\n");
                }
                sb.append("\n");
            }
        }
        log.debug("XLSX parsed: {} chars", sb.length());
        return sb.toString();
    }

    private String parsePdf(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(bytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            log.debug("PDF parsed: {} chars", text.length());
            return text;
        }
    }

    private String parseTxt(MultipartFile file) throws IOException {
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        log.debug("TXT parsed: {} chars", text.length());
        return text;
    }
}

