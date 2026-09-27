package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.dto.AuthResponseDTO;
import com.Luxurycars.carstore.dto.LoginRequestDTO;
import com.Luxurycars.carstore.dto.RegisterRequestDTO;
import com.Luxurycars.carstore.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Luxurycars.carstore.dto.RefreshTokenRequestDTO;
import com.Luxurycars.carstore.dto.TokenRefreshResponseDTO;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register, login, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO dto) {
        return new ResponseEntity<>(authService.register(dto), HttpStatus.CREATED);
    }

    @Operation(summary = "Login and get a JWT token + Refresh Token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @Operation(summary = "Rotate refresh token and issue new access token")
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO dto) {
        return ResponseEntity.ok(authService.refreshToken(dto));
    }

    @Operation(summary = "Logout and revoke refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @Valid @RequestBody RefreshTokenRequestDTO dto) {
        authService.logout(dto);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
