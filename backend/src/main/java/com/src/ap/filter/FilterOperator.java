package com.src.ap.filter;

/**
 * Defines the comparison operators available for filtering.
 * Each operator specifies how values are matched against field data.
 */
public enum FilterOperator {
    /**
     * IN operator: Matches if field value is in the provided list.
     * SQL: WHERE field IN (value1, value2, ...)
     * Typically used with ENUM type fields.
     */
    IN,

    /**
     * CONTAINS operator: Matches if field contains the provided substring.
     * SQL: WHERE LOWER(field) LIKE LOWER('%value%')
     * Typically used with TEXT type fields.
     * Note: Wildcard characters in user input must be escaped.
     */
    CONTAINS

    // Extensible for future operators:
    // EQUALS,          // Exact match
    // NOT_EQUALS,      // Not equal
    // GREATER_THAN,    // Greater than (for numbers, dates)
    // LESS_THAN,       // Less than (for numbers, dates)
    // BETWEEN,         // Range filter (for numbers, dates)
    // IS_NULL,         // Field is null
    // IS_NOT_NULL      // Field is not null
}
