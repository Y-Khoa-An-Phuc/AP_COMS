package com.src.ap.validation;

import com.src.ap.dto.filter.FilterCriterion;
import com.src.ap.dto.filter.OccupationFilterRequest;
import com.src.ap.filter.FilterOperator;
import com.src.ap.filter.OccupationFilterField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

/**
 * Validator for filter criteria.
 * Enforces security rules:
 * - Field names must be in whitelist (OccupationFilterField enum)
 * - Operators must be allowed for the field
 * - Correct value/values must be provided based on operator
 */
public class FilterCriteriaValidator implements ConstraintValidator<ValidFilterCriteria, OccupationFilterRequest> {

    @Override
    public boolean isValid(OccupationFilterRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getCriteria() == null || request.getCriteria().isEmpty()) {
            // Empty criteria is valid (returns unfiltered results)
            return true;
        }

        context.disableDefaultConstraintViolation();

        for (int i = 0; i < request.getCriteria().size(); i++) {
            FilterCriterion criterion = request.getCriteria().get(i);

            if (criterion == null) {
                addViolation(context, "Criterion cannot be null", "criteria[" + i + "]");
                return false;
            }

            // Validate field exists in whitelist
            OccupationFilterField filterField = OccupationFilterField.fromFieldName(criterion.getField())
                    .orElse(null);

            if (filterField == null) {
                addViolation(context,
                        "Invalid field '" + criterion.getField() + "'. Allowed fields: name, description",
                        "criteria[" + i + "].field");
                return false;
            }

            // Validate operator is allowed for this field
            if (!filterField.isOperatorAllowed(criterion.getOperator())) {
                addViolation(context,
                        "Operator '" + criterion.getOperator() + "' is not allowed for field '" + criterion.getField() +
                        "'. Allowed operators: " + filterField.getAllowedOperators(),
                        "criteria[" + i + "].operator");
                return false;
            }

            // Validate value/values based on operator
            if (criterion.getOperator() == FilterOperator.IN) {
                if (!validateInOperator(criterion, context, i)) {
                    return false;
                }
            } else if (criterion.getOperator() == FilterOperator.CONTAINS) {
                if (!validateContainsOperator(criterion, context, i)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean validateInOperator(FilterCriterion criterion, ConstraintValidatorContext context, int index) {
        List<String> values = criterion.getValues();

        if (values == null || values.isEmpty()) {
            addViolation(context,
                    "IN operator requires non-empty 'values' array",
                    "criteria[" + index + "].values");
            return false;
        }

        // Check for null values in the list
        for (int j = 0; j < values.size(); j++) {
            if (values.get(j) == null || values.get(j).isBlank()) {
                addViolation(context,
                        "Values in IN operator cannot be null or blank",
                        "criteria[" + index + "].values[" + j + "]");
                return false;
            }
        }

        return true;
    }

    private boolean validateContainsOperator(FilterCriterion criterion, ConstraintValidatorContext context, int index) {
        String value = criterion.getValue();

        if (value == null || value.isBlank()) {
            addViolation(context,
                    "CONTAINS operator requires non-blank 'value' string",
                    "criteria[" + index + "].value");
            return false;
        }

        return true;
    }

    private void addViolation(ConstraintValidatorContext context, String message, String propertyPath) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyPath)
                .addConstraintViolation();
    }
}
