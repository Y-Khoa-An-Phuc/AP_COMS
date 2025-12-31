package com.src.ap.dto.filter;

import com.src.ap.validation.ValidFilterCriteria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for filtering occupations.
 * Contains filter criteria and pagination parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidFilterCriteria
public class OccupationFilterRequest {

    /**
     * List of filter criteria.
     * Optional - if null or empty, returns unfiltered results.
     * Each criterion is combined using AND logic.
     */
    @Valid
    private List<FilterCriterion> criteria = new ArrayList<>();

    /**
     * Page number (zero-based).
     */
    @Min(value = 0, message = "Page number must be >= 0")
    private int page = 0;

    /**
     * Page size.
     */
    @Min(value = 1, message = "Page size must be >= 1")
    @Max(value = 100, message = "Page size must be <= 100")
    private int size = 20;

    /**
     * Sort specification.
     * Format: ["field,direction"] where direction is "asc" or "desc".
     * Example: ["name,asc", "createdAt,desc"]
     */
    private List<String> sort = new ArrayList<>();
}
