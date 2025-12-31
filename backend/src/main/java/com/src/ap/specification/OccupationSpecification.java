package com.src.ap.specification;

import com.src.ap.dto.filter.FilterCriterion;
import com.src.ap.entity.Occupation;
import com.src.ap.filter.FilterOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification builder for Occupation entity filtering.
 * Creates type-safe queries using Criteria API (no SQL string concatenation).
 *
 * Security: Uses parameterized queries, escapes LIKE wildcards.
 */
public class OccupationSpecification {

    /**
     * Builds a Specification from a list of filter criteria.
     * Combines all criteria using AND logic.
     *
     * @param criteria list of filter criteria
     * @return Specification for querying Occupation entities
     */
    public static Specification<Occupation> fromCriteria(List<FilterCriterion> criteria) {
        return (root, query, criteriaBuilder) -> {
            if (criteria == null || criteria.isEmpty()) {
                // No criteria = no filtering
                return criteriaBuilder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            for (FilterCriterion criterion : criteria) {
                Predicate predicate = buildPredicate(criterion, root, query, criteriaBuilder);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Builds a single predicate for a filter criterion.
     *
     * @param criterion the filter criterion
     * @param root the root entity
     * @param query the criteria query
     * @param cb the criteria builder
     * @return the predicate, or null if criterion is invalid
     */
    private static Predicate buildPredicate(FilterCriterion criterion,
                                           Root<Occupation> root,
                                           CriteriaQuery<?> query,
                                           CriteriaBuilder cb) {
        if (criterion.getOperator() == FilterOperator.IN) {
            return buildInPredicate(criterion, root, cb);
        } else if (criterion.getOperator() == FilterOperator.CONTAINS) {
            return buildContainsPredicate(criterion, root, cb);
        }

        return null;
    }

    /**
     * Builds an IN predicate.
     * SQL: WHERE field IN (:values)
     * Null-safe: null fields do not match.
     *
     * @param criterion the filter criterion
     * @param root the root entity
     * @param cb the criteria builder
     * @return the IN predicate
     */
    private static Predicate buildInPredicate(FilterCriterion criterion,
                                             Root<Occupation> root,
                                             CriteriaBuilder cb) {
        if (criterion.getValues() == null || criterion.getValues().isEmpty()) {
            return cb.disjunction(); // Always false
        }

        // Field IN (value1, value2, ...)
        // Null values in the field will not match
        return root.get(criterion.getField()).in(criterion.getValues());
    }

    /**
     * Builds a CONTAINS predicate with case-insensitive matching.
     * SQL: WHERE LOWER(field) LIKE LOWER('%value%')
     * Escapes % and _ wildcards in user input to prevent injection.
     * Null-safe: null fields do not match.
     *
     * @param criterion the filter criterion
     * @param root the root entity
     * @param cb the criteria builder
     * @return the CONTAINS predicate
     */
    private static Predicate buildContainsPredicate(FilterCriterion criterion,
                                                   Root<Occupation> root,
                                                   CriteriaBuilder cb) {
        if (criterion.getValue() == null || criterion.getValue().isBlank()) {
            return cb.disjunction(); // Always false
        }

        // Escape LIKE wildcards: % and _
        String escapedValue = escapeLikeWildcards(criterion.getValue());

        // Case-insensitive LIKE: LOWER(field) LIKE LOWER('%value%')
        String pattern = "%" + escapedValue.toLowerCase() + "%";

        // Add null check: field IS NOT NULL AND field LIKE pattern
        Predicate notNull = cb.isNotNull(root.get(criterion.getField()));
        Predicate like = cb.like(
                cb.lower(root.get(criterion.getField())),
                pattern,
                '\\'  // Escape character
        );

        return cb.and(notNull, like);
    }

    /**
     * Escapes LIKE wildcard characters (% and _) in user input.
     * Prevents users from injecting wildcards that could match unintended records.
     *
     * @param value the user input value
     * @return the escaped value
     */
    private static String escapeLikeWildcards(String value) {
        return value
                .replace("\\", "\\\\")  // Escape the escape character first
                .replace("%", "\\%")    // Escape %
                .replace("_", "\\_");   // Escape _
    }
}
