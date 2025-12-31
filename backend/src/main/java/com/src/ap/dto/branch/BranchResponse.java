package com.src.ap.dto.branch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String country;
    private int employeeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}