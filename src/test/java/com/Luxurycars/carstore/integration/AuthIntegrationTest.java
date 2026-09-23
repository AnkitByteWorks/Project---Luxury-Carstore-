package com.Luxurycars.carstore.integration;

import com.Luxurycars.carstore.dto.AuthResponseDTO;
import com.Luxurycars.carstore.dto.LoginRequestDTO;
import com.Luxurycars.carstore.dto.RegisterRequestDTO;
import com.Luxurycars.carstore.entity.AppUser;
import com.Luxurycars.carstore.entity.Role;
import com.Luxurycars.carstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/auth";
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Register new user - should return token")
    void register_shouldReturnToken() {
        RegisterRequestDTO dto = RegisterRequestDTO.builder()
                .username("testuser")
                .email("test@example.com")
                .password("secret123")
                .fullName("Test User")
                .build();

        ResponseEntity<AuthResponseDTO> response =
                restTemplate.postForEntity(baseUrl() + "/register", dto, AuthResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getRoles()).contains("USER");
    }

    @Test
    @DisplayName("Register duplicate username - should return 400")
    void register_duplicateUsername_shouldReturn400() {
        RegisterRequestDTO dto = RegisterRequestDTO.builder()
                .username("dup").email("dup@example.com").password("secret123").build();
        restTemplate.postForEntity(baseUrl() + "/register", dto, AuthResponseDTO.class);

        RegisterRequestDTO duplicate = RegisterRequestDTO.builder()
                .username("dup").email("other@example.com").password("secret123").build();

        ResponseEntity<String> response =
                restTemplate.postForEntity(baseUrl() + "/register", duplicate, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Login with valid credentials - should return token")
    void login_validCredentials_shouldReturnToken() {
        AppUser admin = AppUser.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Admin")
                .roles(Set.of(Role.ADMIN, Role.USER))
                .enabled(true)
                .build();
        userRepository.save(admin);

        LoginRequestDTO login = LoginRequestDTO.builder()
                .username("admin").password("admin123").build();

        ResponseEntity<AuthResponseDTO> response =
                restTemplate.postForEntity(baseUrl() + "/login", login, AuthResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getRoles()).contains("ADMIN");
    }

    @Test
    @DisplayName("Login with wrong password - should return 401")
    void login_wrongPassword_shouldReturn401() {
        AppUser user = AppUser.builder()
                .username("test").email("t@x.com")
                .password(passwordEncoder.encode("correct"))
                .roles(Set.of(Role.USER)).enabled(true).build();
        userRepository.save(user);

        LoginRequestDTO login = LoginRequestDTO.builder()
                .username("test").password("wrong").build();

        ResponseEntity<String> response =
                restTemplate.postForEntity(baseUrl() + "/login", login, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Full flow: register → login → access protected endpoint")
    void fullFlow_registerLoginAccess() {
        // 1. Register
        RegisterRequestDTO reg = RegisterRequestDTO.builder()
                .username("flowuser").email("flow@x.com")
                .password("secret123").build();
        ResponseEntity<AuthResponseDTO> regResponse =
                restTemplate.postForEntity(baseUrl() + "/register", reg, AuthResponseDTO.class);

        String token = regResponse.getBody().getToken();

        // 2. Access protected endpoint with token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> result = restTemplate.exchange(
                "http://localhost:" + port + "/api/orders",
                HttpMethod.GET, request, String.class);

        // Should succeed (200) since authenticated
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}