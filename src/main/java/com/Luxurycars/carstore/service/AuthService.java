package com.Luxurycars.carstore.service;



import com.Luxurycars.carstore.dto.AuthResponseDTO;
import com.Luxurycars.carstore.dto.LoginRequestDTO;
import com.Luxurycars.carstore.dto.RegisterRequestDTO;
import com.Luxurycars.carstore.entity.AppUser;
import com.Luxurycars.carstore.entity.Role;
import com.Luxurycars.carstore.exception.BadRequestException;
import com.Luxurycars.carstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.Luxurycars.carstore.dto.RefreshTokenRequestDTO;
import com.Luxurycars.carstore.dto.TokenRefreshResponseDTO;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       @Autowired(required = false) RefreshTokenService refreshTokenService,
                       @Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       RefreshTokenService refreshTokenService) {
        this(userRepository, passwordEncoder, jwtService, authenticationManager, refreshTokenService, null);
    }

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this(userRepository, passwordEncoder, jwtService, authenticationManager, null, null);
    }

    // ─── REGISTER ───
    public AuthResponseDTO register(RegisterRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        AppUser user = AppUser.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))  // BCrypt hash
                .fullName(dto.getFullName())
                .roles(Set.of(Role.USER))                             // default role
                .enabled(true)
                .build();

        userRepository.save(user);

        // Generate token
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + Role.USER.name())
                .build();

        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, user);
    }

    // ─── LOGIN ───
    public AuthResponseDTO login(LoginRequestDTO dto) {
        // Let Spring Security authenticate (throws if bad credentials)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        AppUser user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found"));

        return buildAuthResponse(token, user);
    }

    public TokenRefreshResponseDTO refreshToken(RefreshTokenRequestDTO dto) {
        if (dto == null || dto.getRefreshToken() == null) {
            throw new BadRequestException("Refresh token must not be null");
        }

        // Validate key existence in Redis before rotating/issuing new tokens
        if (stringRedisTemplate != null) {
            String redisKey = "refresh_token:" + dto.getRefreshToken();
            Boolean exists = stringRedisTemplate.hasKey(redisKey);
            if (!Boolean.TRUE.equals(exists)) {
                throw new BadRequestException("Refresh token is invalid, expired, or has been revoked");
            }
        }

        if (refreshTokenService == null) {
            throw new BadRequestException("Refresh token service is currently unavailable");
        }

        TokenRefreshResponseDTO response = refreshTokenService.rotateRefreshToken(dto.getRefreshToken());

        // Update Redis token store: delete old token and store new rotated token
        if (stringRedisTemplate != null) {
            stringRedisTemplate.delete("refresh_token:" + dto.getRefreshToken());
            if (response.getRefreshToken() != null) {
                stringRedisTemplate.opsForValue().set(
                        "refresh_token:" + response.getRefreshToken(),
                        String.valueOf(response.getUserId()),
                        Duration.ofSeconds(604800)
                );
            }
        }

        return response;
    }

    public void logout(RefreshTokenRequestDTO dto) {
        if (dto != null && dto.getRefreshToken() != null) {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.delete("refresh_token:" + dto.getRefreshToken());
            }
            if (refreshTokenService != null) {
                refreshTokenService.revokeToken(dto.getRefreshToken());
            }
        }
    }

    private AuthResponseDTO buildAuthResponse(String token, AppUser user) {
        Set<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        String refreshToken = null;
        if (refreshTokenService != null) {
            refreshToken = refreshTokenService.createRefreshToken(user).getToken();
            if (stringRedisTemplate != null && refreshToken != null) {
                stringRedisTemplate.opsForValue().set(
                        "refresh_token:" + refreshToken,
                        String.valueOf(user.getId()),
                        Duration.ofSeconds(604800)
                );
            }
        }

        return AuthResponseDTO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .type("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }
}
