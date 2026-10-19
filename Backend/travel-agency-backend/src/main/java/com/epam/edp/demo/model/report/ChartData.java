package com.epam.edp.demo.model.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single chart returned by the AI analysis.
 * Supported types: "bar", "line", "pie".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChartData {

    /** Human-readable chart title. */
    private String title;

    /** Chart type: "bar" | "line" | "pie". */
    private String type;

    /** Labels for the X-axis or pie slices. */
    private List<String> labels;

    /** Numeric values corresponding to each label. */
    private List<Number> values;
}

