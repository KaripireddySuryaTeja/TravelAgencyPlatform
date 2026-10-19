package com.epam.edp.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportRowDTO {
    private String tourId;
    private String tourName;
    private String destination;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private int bookingsCount;
    private String deltaBookings;
    private int totalGuests;
    private String deltaGuests;
}
