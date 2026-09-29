package com.Luxurycars.carstore.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasscodeVerificationResponseDTO {
    private boolean valid;
    private String tier;
    private String token;
    private String message;
}
