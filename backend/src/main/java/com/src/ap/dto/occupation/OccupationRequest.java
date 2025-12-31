package com.src.ap.dto.occupation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OccupationRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
}