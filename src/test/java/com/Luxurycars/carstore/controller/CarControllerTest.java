package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.config.JwtAuthFilter;
import com.Luxurycars.carstore.config.SecurityConfig;
import com.Luxurycars.carstore.dto.CarResponseDTO;
import com.Luxurycars.carstore.dto.PageResponseDTO;
import com.Luxurycars.carstore.service.CarService;
import com.Luxurycars.carstore.service.CustomUserDetailsService;
import com.Luxurycars.carstore.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Luxurycars.carstore.config.RateLimitFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.Luxurycars.carstore.config.CacheConfig;

@WebMvcTest(
        controllers = CarController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitFilter.class
        )
)
@Import({SecurityConfig.class, JwtAuthFilter.class, CacheConfig.class})
@ActiveProfiles("dev")
class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;             // simulates HTTP without server

    @MockitoBean
    private CarService carService;       // mocked service

    // JwtAuthFilter dependencies
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/cars - should return paginated cars")
    void getAllCars_shouldReturnPaginated() throws Exception {
        CarResponseDTO car = CarResponseDTO.builder()
                .id(1L).name("Porsche 911").brand("Porsche")
                .price(new BigDecimal("28500000")).hasImage(false).build();

        PageResponseDTO<CarResponseDTO> page = PageResponseDTO.<CarResponseDTO>builder()
                .content(List.of(car))
                .page(0).size(10).totalElements(1).totalPages(1)
                .first(true).last(true).empty(false)
                .build();

        when(carService.getCarsPaginated(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/cars")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Porsche 911"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/cars/{id} - should return 200 with car")
    void getCarById_shouldReturn200() throws Exception {
        CarResponseDTO car = CarResponseDTO.builder()
                .id(1L).name("Porsche 911").brand("Porsche")
                .price(new BigDecimal("28500000")).hasImage(false).build();

        when(carService.getCarById(1L)).thenReturn(car);

        mockMvc.perform(get("/api/cars/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.brand").value("Porsche"));
    }

    @Test
    @DisplayName("GET /api/cars/{id} - should return 404 when not found")
    void getCarById_shouldReturn404() throws Exception {
        when(carService.getCarById(999L))
                .thenThrow(new com.Luxurycars.carstore.exception.ResourceNotFoundException("Car not found with id: 999"));

        mockMvc.perform(get("/api/cars/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/cars - without auth should return 401")
    void createCar_withoutAuth_shouldReturn401() throws Exception {
        String body = """
                {
                    "name": "Test",
                    "brand": "Test",
                    "price": 5000000
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/cars - with USER role should return 403")
    void createCar_withUserRole_shouldReturn403() throws Exception {
        String body = """
                {
                    "name": "Test",
                    "brand": "Test",
                    "price": 5000000
                }
                """;

        mockMvc.perform(post("/api/cars")
                        .with(user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/cars - with ADMIN role should succeed")
    void createCar_withAdminRole_shouldSucceed() throws Exception {
        CarResponseDTO saved = CarResponseDTO.builder()
                .id(5L).name("Test").brand("Test")
                .price(new BigDecimal("5000000")).hasImage(false).build();

        when(carService.createCar(any())).thenReturn(saved);

        String body = """
                {
                    "name": "Test",
                    "brand": "Test",
                    "price": 5000000
                }
                """;

        mockMvc.perform(post("/api/cars")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }
}
