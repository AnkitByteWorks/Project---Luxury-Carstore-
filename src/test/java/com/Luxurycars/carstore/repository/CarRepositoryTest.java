package com.Luxurycars.carstore.repository;

import com.Luxurycars.carstore.entity.Car;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest          // ← real JPA + H2, rollback after each test
@ActiveProfiles("test")
class CarRepositoryTest {

    @Autowired
    private CarRepository carRepository;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();

        carRepository.save(Car.builder()
                .name("Porsche 911").brand("Porsche")
                .price(new BigDecimal("28500000"))
                .showroomLocation("Mumbai").build());

        carRepository.save(Car.builder()
                .name("Ferrari SF90").brand("Ferrari")
                .price(new BigDecimal("75000000"))
                .showroomLocation("Delhi").build());

        carRepository.save(Car.builder()
                .name("Porsche Taycan").brand("Porsche")
                .price(new BigDecimal("24500000"))
                .showroomLocation("Mumbai").build());
    }

    @Test
    @DisplayName("findByBrandIgnoreCase - should return matching cars")
    void findByBrandIgnoreCase_shouldReturnMatches() {
        List<Car> porsches = carRepository.findByBrandIgnoreCase("porsche");

        assertThat(porsches).hasSize(2);
        assertThat(porsches).allMatch(c -> c.getBrand().equals("Porsche"));
    }

    @Test
    @DisplayName("findByPriceBetween - should filter by range")
    void findByPriceBetween_shouldFilter() {
        List<Car> midRange = carRepository.findByPriceBetween(
                new BigDecimal("20000000"),
                new BigDecimal("50000000"));

        assertThat(midRange).hasSize(2);  // Porsche 911 + Taycan
    }

    @Test
    @DisplayName("findByShowroomLocationIgnoreCase - should filter by city")
    void findByShowroomLocationIgnoreCase_shouldFilter() {
        List<Car> mumbai = carRepository.findByShowroomLocationIgnoreCase("mumbai");

        assertThat(mumbai).hasSize(2);
    }

    @Test
    @DisplayName("findAll with Pageable - should paginate")
    void findAll_withPageable_shouldPaginate() {
        Page<Car> firstPage = carRepository.findAll(
                PageRequest.of(0, 2, Sort.by("price").descending()));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        // Highest price first
        assertThat(firstPage.getContent().get(0).getBrand()).isEqualTo("Ferrari");
    }

    @Test
    @DisplayName("search by name or brand - should match either")
    void searchByNameOrBrand_shouldMatchEither() {
        List<Car> results = carRepository
                .findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase("taycan", "taycan");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Porsche Taycan");
    }
}