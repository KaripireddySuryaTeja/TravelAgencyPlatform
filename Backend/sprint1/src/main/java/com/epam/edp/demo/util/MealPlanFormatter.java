package com.epam.edp.demo.util;

/**
 * Shared utility for formatting meal-plan codes into display strings.
 * Extracted to eliminate duplication between TourService and BookingService.
 */
public final class MealPlanFormatter {

    private MealPlanFormatter() {}

    public static String format(String code) {if (code == null) {
            return null;
        }
        return switch (code) {
            case "BB" -> "Breakfast (BB)";
            case "HB" -> "Half-board (HB)";
            case "FB" -> "Full-board (FB)";
            case "AI" -> "All inclusive (AI)";
            default   -> code;
        };
    }
}

