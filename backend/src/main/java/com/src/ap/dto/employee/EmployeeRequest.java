package com.src.ap.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Hire date is required")
    private LocalDate hireDt;

    private LocalDate terminationDt;

    private String citizenIdCard;

    private LocalDate dateOfBirth;

    private String passport;

    private String contractId;

    private String phone;

    private String workStatus;

    private Long occupationId;

    private Long branchId;

    private Long supervisorId;
}