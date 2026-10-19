package com.epam.edp.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ConfirmBookingChangesRequestDTO {

    private List<String> removedGuestIds;
}