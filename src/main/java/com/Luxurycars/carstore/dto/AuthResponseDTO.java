package com.Luxurycars.carstore.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    private String token;              // the JWT
    private String type = "Bearer";    // always "Bearer"
    private Long userId;
    private String username;
    private String email;
    private Set<String> roles;         // ["ADMIN", "USER"]

    public AuthResponseDTO(String token, Long userId, String username,
                           String email, Set<String> roles) {
        this.token = token;
        this.type = "Bearer";
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
}
