package com.src.ap.controller;

import com.src.ap.dto.branch.BranchRequest;
import com.src.ap.dto.branch.BranchResponse;
import com.src.ap.dto.common.ApiResponse;
import com.src.ap.dto.common.PageResponse;
import com.src.ap.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BranchResponse>>> getAllBranches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<BranchResponse> branches = branchService.getAllBranches(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(branches));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAllBranchesList() {
        List<BranchResponse> branches = branchService.getAllBranchesList();
        return ResponseEntity.ok(ApiResponse.success(branches));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchById(@PathVariable Long id) {
        BranchResponse branch = branchService.getBranchById(id);
        return ResponseEntity.ok(ApiResponse.success(branch));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<BranchResponse>>> searchBranches(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<BranchResponse> branches = branchService.searchBranches(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(branches));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(@Valid @RequestBody BranchRequest request) {
        BranchResponse branch = branchService.createBranch(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Branch created successfully", branch));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody BranchRequest request) {
        BranchResponse branch = branchService.updateBranch(id, request);
        return ResponseEntity.ok(ApiResponse.success("Branch updated successfully", branch));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.success("Branch deleted successfully", null));
    }
}