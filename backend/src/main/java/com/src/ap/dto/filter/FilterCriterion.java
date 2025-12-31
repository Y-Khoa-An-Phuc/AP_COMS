package com.src.ap.dto.filter;

import com.src.ap.filter.FilterOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single filter criterion.
 * Contains a field, operator, and the value(s) to filter by.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterCriterion {

    /**
     * Field name to filter on.
     * Must match a field in OccupationFilterField enum (validated by custom validator).
     */
    @NotBlank(message = "Field name is required")
    private String field;

    /**
     * Operator to apply for this filter.
     * Must be allowed for the specified field (validated by custom validator).
     */
    @NotNull(message = "Operator is required")
    private FilterOperator operator;

    /**
     * Values for IN operator.
     * Required when operator is IN, must be ignored otherwise.
     * Max 50 values, each max 200 characters.
     */
    @Size(max = 50, message = "Maximum 50 values allowed for IN operator")
    private List<@NotBlank(message = "Value cannot be blank") @Size(max = 200, message = "Each value must be at most 200 characters") String> values;

    /**
     * Single value for CONTAINS operator.
     * Required when operator is CONTAINS, must be ignored otherwise.
     * Max 200 characters.
     */
    @Size(max = 200, message = "Value must be at most 200 characters")
    private String value;
}
