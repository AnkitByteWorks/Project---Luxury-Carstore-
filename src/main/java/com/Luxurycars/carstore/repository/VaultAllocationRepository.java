package com.Luxurycars.carstore.repository;

import com.Luxurycars.carstore.entity.VaultAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VaultAllocationRepository extends JpaRepository<VaultAllocation, String> {
}
