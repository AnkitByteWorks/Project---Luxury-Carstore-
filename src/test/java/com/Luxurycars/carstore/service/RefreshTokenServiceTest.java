package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.TokenRefreshResponseDTO;
import com.Luxurycars.carstore.entity.AppUser;
import com.Luxurycars.carstore.entity.RefreshToken;
import com.Luxurycars.carstore.entity.Role;
import com.Luxurycars.carstore.exception.BadRequestException;
import com.Luxurycars.carstore.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    private RefreshTokenService refreshTokenService;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtService, 604800000L);
        testUser = AppUser.builder()
                .id(1L)
                .username("testuser")
                .email("test@luxurycars.com")
                .password("encoded_pass")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("createRefreshToken - should create and return a valid refresh token")
    void createRefreshToken_shouldCreateToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotEmpty();
        assertThat(token.getUser()).isEqualTo(testUser);
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("rotateRefreshToken - should rotate token and return new access + refresh token")
    void rotateRefreshToken_shouldRotateAndReturnNewTokens() {
        RefreshToken oldToken = RefreshToken.builder()
                .id(10L)
                .token("old-refresh-token")
                .user(testUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("mocked-new-jwt-token");

        TokenRefreshResponseDTO response = refreshTokenService.rotateRefreshToken("old-refresh-token");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mocked-new-jwt-token");
        assertThat(response.getRefreshToken()).isNotEqualTo("old-refresh-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(oldToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("verifyExpiration - should throw when token is expired")
    void verifyExpiration_shouldThrow_whenExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(10L)
                .token("expired-token")
                .user(testUser)
                .expiryDate(Instant.now().minusSeconds(3600))
                .revoked(false)
                .build();

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(expiredToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("verifyExpiration - should throw when token is revoked")
    void verifyExpiration_shouldThrow_whenRevoked() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(10L)
                .token("revoked-token")
                .user(testUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(revokedToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("revoked");
    }
}
