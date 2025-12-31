package com.src.ap.dto.occupation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OccupationHistoryRequest {

    @NotNull(message = "Occupation IDs list cannot be null")
    @NotEmpty(message = "Occupation IDs list cannot be empty")
    @Size(min = 1, max = 1000, message = "Occupation IDs list must contain between 1 and 1000 IDs")
    private List<Long> occupationIds;
}
