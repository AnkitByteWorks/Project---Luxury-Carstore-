package com.Luxurycars.carstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasscodeVerificationRequestDTO {

    @NotBlank(message = "Passcode is required")
    private String passcode;
}
