package com.src.ap.dto.occupation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupationHistoryResponse {
    private Long txId;
    private String op;
    private Long id;
    private String oId;
    private String name;
    private String oName;
    private String description;
    private String oDescription;
    private LocalDateTime createdAt;
    private String oCreatedAt;
    private LocalDateTime updatedAt;
    private String oUpdatedAt;
    private LocalDateTime changedAt;
    private String actor;
}
