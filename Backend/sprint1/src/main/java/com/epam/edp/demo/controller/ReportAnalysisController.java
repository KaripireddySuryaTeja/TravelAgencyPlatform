package com.epam.edp.demo.controller;

import com.epam.edp.demo.model.report.AnalysisResult;
import com.epam.edp.demo.service.report.FileParserService;
import com.epam.edp.demo.service.report.ReportAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Map;

/**
 * REST controller for the Admin Report Analysis feature.
 *
 * <p>All endpoints are protected by {@code ROLE_ADMIN} — enforced in
 * {@code SecurityConfig} via {@code /api/reports/**}.</p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /api/reports/analyze} — upload and analyse a report file</li>
 *   <li>{@code GET  /api/reports/health}  — liveness check</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Report Analysis", description = "Admin AI-powered report analysis endpoints")
public class ReportAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(ReportAnalysisController.class);

    private final FileParserService fileParserService;
    private final ReportAnalysisService reportAnalysisService;

    public ReportAnalysisController(FileParserService fileParserService,
                                     ReportAnalysisService reportAnalysisService) {
        this.fileParserService = fileParserService;
        this.reportAnalysisService = reportAnalysisService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/reports/analyze
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Accepts a report file (CSV / XLSX / PDF / TXT) and an optional report type,
     * parses its content, and returns structured AI insights.
     *
     * @param file       the uploaded report file (required, max 10 MB)
     * @param reportType "STAFF" or "BUSINESS_ACTIVITY" (default: STAFF)
     * @return structured {@link AnalysisResult} or a descriptive error JSON
     */
    @Operation(summary = "Analyse a report file using AI",
               description = "Upload CSV/XLSX/PDF/TXT — returns trends, anomalies, recommendations and charts.")
    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ResponseEntity<?> analyzeReport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "reportType", defaultValue = "STAFF") String reportType) {

        log.info("Received analyze request: file='{}', size={} bytes, reportType='{}'",
                file.getOriginalFilename(), file.getSize(), reportType);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "File is empty. Please upload a valid report.",
                    "code",  "EMPTY_FILE",
                    "timestamp", Instant.now().toString()));
        }

        // ── Step 1: Parse the file ──────────────────────────────────────────
        String reportContent;
        try {
            reportContent = fileParserService.parseFile(file);
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported file type: {}", e.getMessage());
            return ResponseEntity.status(422).body(Map.of(
                    "error", "Could not parse file. Supported: CSV, XLSX, PDF, TXT",
                    "code",  "PARSE_ERROR",
                    "timestamp", Instant.now().toString()));
        } catch (Exception e) {
            log.error("File parse error: {}", e.getMessage());
            return ResponseEntity.status(422).body(Map.of(
                    "error", "Could not parse file. Supported: CSV, XLSX, PDF, TXT",
                    "code",  "PARSE_ERROR",
                    "timestamp", Instant.now().toString()));
        }

        // ── Step 2: Call AI analysis ────────────────────────────────────────
        try {
            AnalysisResult result = reportAnalysisService.analyzeReport(reportContent, reportType);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";

            if (msg.startsWith("AI_FORMAT_ERROR")) {
                log.error("AI format error: {}", msg);
                return ResponseEntity.status(500).body(Map.of(
                        "error", "AI returned unexpected format.",
                        "code",  "AI_FORMAT_ERROR",
                        "timestamp", Instant.now().toString()));
            }

            // SERVICE_UNAVAILABLE or any other AI error
            log.error("AI service error: {}", msg);
            return ResponseEntity.status(503).body(Map.of(
                    "error", "AI service unavailable. Please try again.",
                    "code",  "SERVICE_UNAVAILABLE",
                    "timestamp", Instant.now().toString()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/reports/health
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Simple liveness check for the report analysis service.
     *
     * @return {@code {"status":"ok","timestamp":"ISO-8601"}}
     */
    @Operation(summary = "Report analysis health check")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status",    "ok",
                "timestamp", Instant.now().toString()));
    }
}

