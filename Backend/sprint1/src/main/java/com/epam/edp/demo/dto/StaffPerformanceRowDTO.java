package com.epam.edp.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerformanceRowDTO {
    private String agentId;
    private String agentName;
    private String agentEmail;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private int toursSold;
    private String deltaOfToursSold;
    private Double avgFeedback;
    private Double minFeedback;
    private String deltaOfAvgFeedback;
    private int reviewCount;
}
