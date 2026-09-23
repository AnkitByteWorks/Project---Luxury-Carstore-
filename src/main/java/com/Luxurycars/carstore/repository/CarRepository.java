package com.Luxurycars.carstore.repository;

import com.Luxurycars.carstore.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    @Override
    List<Car> findAll();

    List<Car> findByBrandIgnoreCase(String brand);

    List<Car> findByNameContainingIgnoreCase(String keyword);

    List<Car> findByPriceBetween(BigDecimal min, BigDecimal max);

    List<Car> findByShowroomLocationIgnoreCase(String location);

    List<Car> findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(
            String name, String brand);
}
