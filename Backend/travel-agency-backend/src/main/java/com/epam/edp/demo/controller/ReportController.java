package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.SalesReportRowDTO;
import com.epam.edp.demo.dto.StaffPerformanceRowDTO;
import com.epam.edp.demo.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Access restricted to ROLE_ADMIN by SecurityConfig — no manual role check needed
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/locations")
    public ResponseEntity<List<String>> getLocations() {
        return ResponseEntity.ok(reportService.getLocations());
    }

    @GetMapping("/staff-performance")
    public ResponseEntity<List<StaffPerformanceRowDTO>> getStaffPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String location
    ) {
        return ResponseEntity.ok(reportService.getStaffPerformance(from, to, location));
    }

    @GetMapping("/sales")
    public ResponseEntity<List<SalesReportRowDTO>> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String location
    ) {
        return ResponseEntity.ok(reportService.getSalesReport(from, to, location));
    }
}
