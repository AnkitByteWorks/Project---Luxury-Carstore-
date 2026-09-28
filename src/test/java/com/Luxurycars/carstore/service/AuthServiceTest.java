package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.*;
import com.Luxurycars.carstore.entity.AppUser;
import com.Luxurycars.carstore.entity.RefreshToken;
import com.Luxurycars.carstore.entity.Role;
import com.Luxurycars.carstore.exception.BadRequestException;
import com.Luxurycars.carstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        testUser = AppUser.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_pass")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("register - should store refresh token in Redis with 7-day TTL")
    void register_shouldStoreRefreshTokenInRedis() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(
                RefreshToken.builder().token("sample-refresh-token").user(testUser).build()
        );
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        RegisterRequestDTO dto = RegisterRequestDTO.builder()
                .username("newuser")
                .email("new@example.com")
                .password("secret")
                .fullName("New User")
                .build();

        AuthResponseDTO response = authService.register(dto);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("sample-refresh-token");
        verify(valueOperations, times(1)).set(
                eq("refresh_token:sample-refresh-token"),
                anyString(),
                eq(Duration.ofSeconds(604800))
        );
    }

    @Test
    @DisplayName("login - should store refresh token in Redis with 7-day TTL")
    void login_shouldStoreRefreshTokenInRedis() {
        Authentication auth = mock(Authentication.class);
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User("testuser", "pass", java.util.List.of());
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(
                RefreshToken.builder().token("login-refresh-token").user(testUser).build()
        );
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        LoginRequestDTO dto = LoginRequestDTO.builder().username("testuser").password("pass").build();
        AuthResponseDTO response = authService.login(dto);

        assertThat(response.getRefreshToken()).isEqualTo("login-refresh-token");
        verify(valueOperations, times(1)).set(
                eq("refresh_token:login-refresh-token"),
                eq("1"),
                eq(Duration.ofSeconds(604800))
        );
    }

    @Test
    @DisplayName("refreshToken - throws BadRequestException when key is not in Redis")
    void refreshToken_shouldThrow_whenNotInRedis() {
        when(stringRedisTemplate.hasKey("refresh_token:revoked-token")).thenReturn(Boolean.FALSE);

        RefreshTokenRequestDTO dto = RefreshTokenRequestDTO.builder()
                .refreshToken("revoked-token")
                .build();

        assertThatThrownBy(() -> authService.refreshToken(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Refresh token is invalid, expired, or has been revoked");

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("refreshToken - deletes old key and stores new key in Redis on rotation")
    void refreshToken_shouldRotateInRedis() {
        when(stringRedisTemplate.hasKey("refresh_token:valid-old-token")).thenReturn(Boolean.TRUE);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        TokenRefreshResponseDTO rotationResponse = TokenRefreshResponseDTO.builder()
                .accessToken("new-access-token")
                .refreshToken("new-fresh-token")
                .tokenType("Bearer")
                .userId(1L)
                .username("testuser")
                .build();

        when(refreshTokenService.rotateRefreshToken("valid-old-token")).thenReturn(rotationResponse);

        RefreshTokenRequestDTO dto = RefreshTokenRequestDTO.builder()
                .refreshToken("valid-old-token")
                .build();

        TokenRefreshResponseDTO result = authService.refreshToken(dto);

        assertThat(result.getRefreshToken()).isEqualTo("new-fresh-token");
        // Verifies DEL of old token and SET of new token with 7-day TTL
        verify(stringRedisTemplate, times(1)).delete("refresh_token:valid-old-token");
        verify(valueOperations, times(1)).set(
                eq("refresh_token:new-fresh-token"),
                eq("1"),
                eq(Duration.ofSeconds(604800))
        );
    }

    @Test
    @DisplayName("logout - immediately deletes refresh token key from Redis")
    void logout_shouldDeleteKeyFromRedis() {
        RefreshTokenRequestDTO dto = RefreshTokenRequestDTO.builder()
                .refreshToken("token-to-logout")
                .build();

        authService.logout(dto);

        verify(stringRedisTemplate, times(1)).delete("refresh_token:token-to-logout");
        verify(refreshTokenService, times(1)).revokeToken("token-to-logout");
    }
}
