package com.Luxurycars.carstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshResponseDTO {

    @Schema(description = "Newly issued short-lived JWT access token")
    private String accessToken;

    @Schema(description = "Rotated refresh token")
    private String refreshToken;

    @Builder.Default
    @Schema(description = "Token type", example = "Bearer")
    private String tokenType = "Bearer";

    private Long userId;
    private String username;
    private String email;
    private Set<String> roles;
}
