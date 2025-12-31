package com.src.ap.service;

import com.src.ap.dto.common.PageResponse;
import com.src.ap.dto.employee.EmployeeRequest;
import com.src.ap.dto.employee.EmployeeResponse;
import com.src.ap.entity.Employee;
import com.src.ap.entity.Occupation;
import com.src.ap.exception.DuplicateResourceException;
import com.src.ap.exception.ResourceNotFoundException;
import com.src.ap.mapper.EmployeeMapper;
import com.src.ap.repository.EmployeeRepository;
import com.src.ap.repository.OccupationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OccupationRepository occupationRepository;
    private final EmployeeMapper employeeMapper;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> getAllEmployees(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Employee> employees = employeeRepository.findAll(pageable);
        Page<EmployeeResponse> responsePage = employees.map(employeeMapper::toResponse);
        return PageResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return employeeMapper.toResponse(employee);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> searchEmployees(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Employee> employees = employeeRepository.searchByKeyword(keyword, pageable);
        Page<EmployeeResponse> responsePage = employees.map(employeeMapper::toResponse);
        return PageResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> getEmployeesByOccupation(Long occupationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Employee> employees = employeeRepository.findByOccupationId(occupationId, pageable);
        Page<EmployeeResponse> responsePage = employees.map(employeeMapper::toResponse);
        return PageResponse.of(responsePage);
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Employee", "email", request.getEmail());
        }

        Employee employee = employeeMapper.toEntity(request);

        if (request.getOccupationId() != null) {
            Occupation occupation = occupationRepository.findById(request.getOccupationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Occupation", "id", request.getOccupationId()));
            employee.setOccupation(occupation);
        }

        employee = employeeRepository.save(employee);
        return employeeMapper.toResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        if (!employee.getEmail().equals(request.getEmail()) && employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Employee", "email", request.getEmail());
        }

        employeeMapper.updateEntity(request, employee);

        if (request.getOccupationId() != null) {
            Occupation occupation = occupationRepository.findById(request.getOccupationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Occupation", "id", request.getOccupationId()));
            employee.setOccupation(occupation);
        } else {
            employee.setOccupation(null);
        }

        employee = employeeRepository.save(employee);
        return employeeMapper.toResponse(employee);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee", "id", id);
        }
        employeeRepository.deleteById(id);
    }
}