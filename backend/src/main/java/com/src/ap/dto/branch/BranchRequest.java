package com.src.ap.dto.branch;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequest {

    @NotBlank(message = "Branch name is required")
    private String name;

    private String address;

    private String city;

    private String country;
}