package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.TokenRefreshResponseDTO;
import com.Luxurycars.carstore.entity.AppUser;
import com.Luxurycars.carstore.entity.RefreshToken;
import com.Luxurycars.carstore.exception.BadRequestException;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class
RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final long refreshExpirationMs;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtService jwtService,
                               @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public RefreshToken createRefreshToken(AppUser user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new BadRequestException("Refresh token was revoked. Please log in again.");
        }
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new BadRequestException("Refresh token has expired. Please sign in again.");
        }
        return token;
    }

    @Transactional
    public TokenRefreshResponseDTO rotateRefreshToken(String requestRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        token = verifyExpiration(token);

        AppUser user = token.getUser();

        // Invalidate old token (Rotation security)
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        // Issue new refresh token
        RefreshToken newRefreshToken = createRefreshToken(user);

        // Generate new access token
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> "ROLE_" + role.name())
                        .toArray(String[]::new))
                .build();

        String newAccessToken = jwtService.generateToken(userDetails);

        Set<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        log.info("🔄 Successfully rotated tokens for user: {}", user.getUsername());

        return TokenRefreshResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    @Transactional
    public void revokeToken(String tokenString) {
        refreshTokenRepository.findByToken(tokenString).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("🚪 Revoked refresh token for user: {}", token.getUser().getUsername());
        });
    }
}
