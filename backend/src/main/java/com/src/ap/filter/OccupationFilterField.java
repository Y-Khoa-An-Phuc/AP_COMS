package com.src.ap.filter;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Defines the whitelist of filterable fields for Occupation entity.
 * This enum is the SINGLE SOURCE OF TRUTH for allowed filter fields.
 *
 * Security: No arbitrary field names are accepted.
 * Each field explicitly defines its type, allowed operators, and metadata endpoint.
 */
@Getter
public enum OccupationFilterField {
    /**
     * Name field: Filter by occupation name.
     * Type: ENUM - Select from existing occupation names.
     * Operator: IN - Match any of the selected values.
     * Values are fetched from the valuesEndpoint.
     */
    NAME(
            "name",
            "Tên nghề nghiệp",
            FilterType.ENUM,
            List.of(FilterOperator.IN),
            "/occupations/filter-values?field=name"
    ),

    /**
     * Description field: Search in occupation descriptions.
     * Type: TEXT - Free text input.
     * Operator: CONTAINS - Substring match (case-insensitive).
     */
    DESCRIPTION(
            "description",
            "Mô tả",
            FilterType.TEXT,
            List.of(FilterOperator.CONTAINS),
            null
    );

    /**
     * Field name matching the Occupation entity property.
     * Must match exactly for JPA Criteria API queries.
     */
    private final String fieldName;

    /**
     * Human-readable label in Vietnamese for frontend display.
     */
    private final String label;

    /**
     * Filter type determining frontend control rendering.
     */
    private final FilterType type;

    /**
     * List of operators allowed for this field.
     * Whitelist approach prevents operator injection.
     */
    private final List<FilterOperator> allowedOperators;

    /**
     * Endpoint to fetch values for ENUM type fields.
     * Null for non-ENUM types.
     */
    private final String valuesEndpoint;

    OccupationFilterField(String fieldName, String label, FilterType type,
                          List<FilterOperator> allowedOperators, String valuesEndpoint) {
        this.fieldName = fieldName;
        this.label = label;
        this.type = type;
        this.allowedOperators = allowedOperators;
        this.valuesEndpoint = valuesEndpoint;
    }

    /**
     * Find a filter field by field name.
     * Used for validation in filter execution endpoints.
     *
     * @param fieldName the field name to search for
     * @return Optional containing the matching field, or empty if not found
     */
    public static Optional<OccupationFilterField> fromFieldName(String fieldName) {
        return Arrays.stream(values())
                .filter(field -> field.fieldName.equals(fieldName))
                .findFirst();
    }

    /**
     * Check if an operator is allowed for this field.
     *
     * @param operator the operator to check
     * @return true if the operator is in the allowed list, false otherwise
     */
    public boolean isOperatorAllowed(FilterOperator operator) {
        return allowedOperators.contains(operator);
    }
}
