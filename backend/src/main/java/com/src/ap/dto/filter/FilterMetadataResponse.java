package com.src.ap.dto.filter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.src.ap.filter.FilterOperator;
import com.src.ap.filter.FilterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for filter metadata.
 * Describes how the frontend should render a filter field.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterMetadataResponse {

    /**
     * Field name (matches entity property name).
     */
    private String field;

    /**
     * Human-readable label for frontend display (Vietnamese).
     */
    private String label;

    /**
     * Type of filter control (ENUM, TEXT, etc.).
     * Determines how the frontend renders the input.
     */
    private FilterType type;

    /**
     * List of operators allowed for this field.
     * Frontend should only allow these operators for this field.
     */
    private List<FilterOperator> operators;

    /**
    * Endpoint to fetch values for ENUM type fields.
    * Only populated for ENUM types, null otherwise.
    * Frontend calls this endpoint to get the list of selectable values.
    */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String valuesEndpoint;
}
