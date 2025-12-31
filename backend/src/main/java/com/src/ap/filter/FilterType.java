package com.src.ap.filter;

/**
 * Defines the data type of a filterable field.
 * Determines how the frontend renders the filter control.
 */
public enum FilterType {
    /**
     * ENUM type: Multi-select from predefined values.
     * Frontend renders as dropdown/multi-select.
     * Values are fetched from valuesEndpoint.
     */
    ENUM,

    /**
     * TEXT type: Free text input.
     * Frontend renders as text input field.
     * Typically used with CONTAINS operator for substring search.
     */
    TEXT

    // Extensible for future types:
    // DATE,    // For date range filtering
    // NUMBER,  // For numeric range filtering
    // BOOLEAN  // For true/false toggle
}
