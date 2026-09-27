package com.Luxurycars.carstore.repository;

import com.Luxurycars.carstore.entity.TestDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestDriveRepository extends JpaRepository<TestDrive, Long> {

    Optional<TestDrive> findByReferenceCode(String referenceCode);

    List<TestDrive> findAllByOrderByCreatedAtDesc();

    List<TestDrive> findByEmailOrderByCreatedAtDesc(String email);

    List<TestDrive> findByCarId(Long carId);
}
