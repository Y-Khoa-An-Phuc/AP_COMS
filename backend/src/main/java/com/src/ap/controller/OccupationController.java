package com.src.ap.controller;

import com.src.ap.dto.common.ApiResponse;
import com.src.ap.dto.common.PageResponse;
import com.src.ap.dto.filter.FilterMetadataResponse;
import com.src.ap.dto.filter.OccupationFilterRequest;
import com.src.ap.dto.occupation.OccupationHistoryRequest;
import com.src.ap.dto.occupation.OccupationHistoryResponse;
import com.src.ap.dto.occupation.OccupationRequest;
import com.src.ap.dto.occupation.OccupationResponse;
import com.src.ap.service.OccupationFilterService;
import com.src.ap.service.OccupationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/occupations")
@RequiredArgsConstructor
public class OccupationController {

    private final OccupationService occupationService;
    private final OccupationFilterService occupationFilterService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OccupationResponse>>> getAllOccupations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<OccupationResponse> occupations = occupationService.getAllOccupations(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(occupations));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<OccupationResponse>>> getAllOccupationsList() {
        List<OccupationResponse> occupations = occupationService.getAllOccupationsList();
        return ResponseEntity.ok(ApiResponse.success(occupations));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<OccupationResponse>> getOccupationById(@PathVariable Long id) {
        OccupationResponse occupation = occupationService.getOccupationById(id);
        return ResponseEntity.ok(ApiResponse.success(occupation));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<OccupationResponse>>> searchOccupations(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<OccupationResponse> occupations = occupationService.searchOccupations(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(occupations));
    }

    @GetMapping("/filter-metadata")
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<FilterMetadataResponse>>> getFilterMetadata() {
        List<FilterMetadataResponse> metadata = occupationFilterService.getFilterMetadata();
        return ResponseEntity.ok(ApiResponse.success(metadata));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OccupationResponse>>> filterOccupations(
            @Valid @RequestBody OccupationFilterRequest request) {
        PageResponse<OccupationResponse> occupations = occupationFilterService.filterOccupations(request);
        return ResponseEntity.ok(ApiResponse.success(occupations));
    }

    @GetMapping("/filter-values")
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getFilterValues(@RequestParam @NotBlank String field) {
        List<String> values = occupationFilterService.getFieldValues(field);
        return ResponseEntity.ok(ApiResponse.success(values));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<OccupationResponse>> createOccupation(@Valid @RequestBody OccupationRequest request) {
        OccupationResponse occupation = occupationService.createOccupation(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Occupation created successfully", occupation));
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<OccupationResponse>> updateOccupation(
            @PathVariable Long id,
            @Valid @RequestBody OccupationRequest request) {
        OccupationResponse occupation = occupationService.updateOccupation(id, request);
        return ResponseEntity.ok(ApiResponse.success("Occupation updated successfully", occupation));
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOccupation(@PathVariable Long id) {
        occupationService.deleteOccupation(id);
        return ResponseEntity.ok(ApiResponse.success("Occupation deleted successfully", null));
    }

    @PostMapping("/history")
    public ResponseEntity<ApiResponse<Map<Long, List<OccupationHistoryResponse>>>> getOccupationHistoryByIds(
            @Valid @RequestBody OccupationHistoryRequest request) {
        Map<Long, List<OccupationHistoryResponse>> history = occupationService.getOccupationHistoryByIds(request.getOccupationIds());
        return ResponseEntity.ok(ApiResponse.success("Occupation history retrieved successfully", history));
    }

    @GetMapping("/{id:[0-9]+}/history")
    public ResponseEntity<ApiResponse<List<OccupationHistoryResponse>>> getOccupationHistory(@PathVariable Long id) {
        List<OccupationHistoryResponse> history = occupationService.getOccupationHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
