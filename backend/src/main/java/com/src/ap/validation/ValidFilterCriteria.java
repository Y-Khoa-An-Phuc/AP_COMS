package com.src.ap.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for filter criteria.
 * Validates that:
 * - Field names exist in the whitelist
 * - Operators are allowed for the specified fields
 * - Values/value are provided correctly based on the operator
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FilterCriteriaValidator.class)
@Documented
public @interface ValidFilterCriteria {
    String message() default "Invalid filter criteria";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
