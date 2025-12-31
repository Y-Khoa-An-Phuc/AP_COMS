package com.src.ap.controller;

import com.src.ap.dto.common.ApiResponse;
import com.src.ap.dto.common.PageResponse;
import com.src.ap.dto.employee.EmployeeRequest;
import com.src.ap.dto.employee.EmployeeResponse;
import com.src.ap.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<EmployeeResponse> employees = employeeService.getAllEmployees(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(@PathVariable Long id) {
        EmployeeResponse employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> searchEmployees(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<EmployeeResponse> employees = employeeService.searchEmployees(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @GetMapping("/occupation/{occupationId}")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> getEmployeesByOccupation(
            @PathVariable Long occupationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<EmployeeResponse> employees = employeeService.getEmployeesByOccupation(occupationId, page, size);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse employee = employeeService.createEmployee(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created successfully", employee));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse employee = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", employee));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted successfully", null));
    }
}