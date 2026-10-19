package com.epam.edp.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "DestinationListResponse", description = "Destination suggestions matching the search query")
public class DestinationListResponseDTO {

    @Schema(description = "Matching destination names", example = "[\"Bali\", \"Bangkok\", \"Barcelona\"]")
    private List<String> destinations;

    public DestinationListResponseDTO() {}

    public DestinationListResponseDTO(List<String> destinations) {
        this.destinations = copyOf(destinations);
    }

    public List<String> getDestinations() {
        return copyOf(destinations);
    }

    public void setDestinations(List<String> destinations) {
        this.destinations = copyOf(destinations);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private List<String> destinations;

        public Builder destinations(List<String> destinations) {
            this.destinations = copyOf(destinations);
            return this;
        }

        public DestinationListResponseDTO build() {
            return new DestinationListResponseDTO(destinations);
        }
    }

    private static List<String> copyOf(List<String> destinations) {
        return destinations == null ? null : new ArrayList<>(destinations);
    }
}