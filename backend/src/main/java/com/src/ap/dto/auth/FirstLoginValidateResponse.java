package com.src.ap.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirstLoginValidateResponse {

    private boolean valid;
    private String username;
    private String email;
}
