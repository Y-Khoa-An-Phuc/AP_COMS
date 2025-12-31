package com.src.ap.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {
    private Long id;
    private String email;
    private String fullName;
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate hireDt;
    private LocalDate terminationDt;
    private String citizenIdCard;
    private LocalDate dateOfBirth;
    private String passport;
    private String contractId;
    private String phone;
    private String workStatus;

    // Related entities
    private Long occupationId;
    private String occupationName;
    private Long branchId;
    private String branchName;
    private Long supervisorId;
    private String supervisorName;

    // Audit fields
    private String createUser;
    private LocalDateTime createdAt;
    private String lastUpdateUser;
    private LocalDateTime updatedAt;
}