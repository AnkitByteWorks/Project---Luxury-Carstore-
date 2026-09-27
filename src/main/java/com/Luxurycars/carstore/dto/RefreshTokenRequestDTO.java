package com.Luxurycars.carstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequestDTO {

    @NotBlank(message = "Refresh token is required")
    @Schema(description = "The active refresh token UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String refreshToken;
}
