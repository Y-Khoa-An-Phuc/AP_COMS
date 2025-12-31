package com.src.ap.service;

import com.src.ap.dto.common.PageResponse;
import com.src.ap.dto.filter.FilterMetadataResponse;
import com.src.ap.dto.filter.OccupationFilterRequest;
import com.src.ap.dto.occupation.OccupationResponse;
import com.src.ap.entity.Occupation;
import com.src.ap.exception.BadRequestException;
import com.src.ap.filter.FilterType;
import com.src.ap.filter.OccupationFilterField;
import com.src.ap.mapper.OccupationMapper;
import com.src.ap.repository.OccupationRepository;
import com.src.ap.specification.OccupationSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for occupation filtering operations.
 * Provides filter metadata and handles filter execution.
 */
@Service
@RequiredArgsConstructor
public class OccupationFilterService {

    private final OccupationRepository occupationRepository;
    private final OccupationMapper occupationMapper;

    /**
     * Get metadata for all filterable occupation fields.
     * This metadata tells the frontend how to render filter controls.
     *
     * Security: No user input involved. Returns static configuration from enum.
     *
     * @return list of filter metadata for all filterable fields
     */
    public List<FilterMetadataResponse> getFilterMetadata() {
        return Arrays.stream(OccupationFilterField.values())
                .map(field -> FilterMetadataResponse.builder()
                        .field(field.getFieldName())
                        .label(field.getLabel())
                        .type(field.getType())
                        .operators(field.getAllowedOperators())
                        .valuesEndpoint(field.getValuesEndpoint())
                        .build())
                .toList();
    }

    /**
     * Get distinct values for a filterable field.
     * Used to populate ENUM-type filter dropdowns.
     *
     * Security:
     * - Field name validated against whitelist
     * - Only ENUM type fields return values
     *
     * @param fieldName the field name to get values for
     * @return list of distinct values for the field
     * @throws BadRequestException if field is invalid or not ENUM type
     */
    @Transactional(readOnly = true)
    public List<String> getFieldValues(String fieldName) {
        // 1. Validate field exists in whitelist
        OccupationFilterField filterField = OccupationFilterField.fromFieldName(fieldName)
                .orElseThrow(() -> new BadRequestException("Invalid field: " + fieldName + ". Allowed fields: name, description"));

        // 2. Validate field is ENUM type
        if (filterField.getType() != FilterType.ENUM) {
            throw new BadRequestException("Field '" + fieldName + "' is not an ENUM type. Only ENUM fields can return values.");
        }

        // 3. Fetch distinct values based on field
        return switch (filterField) {
            case NAME -> occupationRepository.findDistinctNames();
            // Future ENUM fields would be added here
            default -> throw new BadRequestException("Field '" + fieldName + "' does not support value enumeration.");
        };
    }

    /**
     * Filter occupations based on criteria.
     * Uses JPA Specifications for type-safe query building.
     *
     * Security:
     * - Field names validated against whitelist (OccupationFilterField enum)
     * - Operators validated against per-field allowed list
     * - Values validated for size and content
     * - Uses parameterized queries (no SQL injection)
     * - LIKE wildcards escaped
     *
     * @param request filter request with criteria and pagination
     * @return paginated occupation response
     */
    @Transactional(readOnly = true)
    public PageResponse<OccupationResponse> filterOccupations(OccupationFilterRequest request) {
        // Build specification from criteria
        Specification<Occupation> spec = OccupationSpecification.fromCriteria(request.getCriteria());

        // Build pageable with sorting
        Pageable pageable = buildPageable(request);

        // Execute query
        Page<Occupation> occupations = occupationRepository.findAll(spec, pageable);

        // Map to response
        Page<OccupationResponse> responsePage = occupations.map(occupationMapper::toResponse);

        return PageResponse.of(responsePage);
    }

    /**
     * Builds Pageable from filter request.
     * Supports multiple sort fields with direction (e.g., "name,asc", "createdAt,desc").
     *
     * @param request filter request
     * @return Pageable for query
     */
    private Pageable buildPageable(OccupationFilterRequest request) {
        Sort sort = Sort.unsorted();

        if (request.getSort() != null && !request.getSort().isEmpty()) {
            List<Sort.Order> orders = new ArrayList<>();

            for (String sortSpec : request.getSort()) {
                String[] parts = sortSpec.split(",");
                if (parts.length == 2) {
                    String property = parts[0].trim();
                    String direction = parts[1].trim();

                    Sort.Direction dir = direction.equalsIgnoreCase("desc")
                            ? Sort.Direction.DESC
                            : Sort.Direction.ASC;

                    orders.add(new Sort.Order(dir, property));
                }
            }

            if (!orders.isEmpty()) {
                sort = Sort.by(orders);
            }
        }

        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
