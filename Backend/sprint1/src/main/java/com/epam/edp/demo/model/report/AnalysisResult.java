package com.epam.edp.demo.model.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full structured result returned by the AI model after analysing a report.
 * Maps directly to the JSON schema described in the AI system prompt.
 * Unknown fields from the AI response are silently ignored.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisResult {

    private String summary;
    private List<String> keyTrends;
    private List<String> anomalies;
    private List<String> potentialIssues;
    private List<String> actionableRecommendations;
    /** "LOW", "MEDIUM", or "HIGH" */
    private String riskLevel;
    /** 0-100 */
    private int dataQualityScore;
    private List<ChartData> charts;
}

