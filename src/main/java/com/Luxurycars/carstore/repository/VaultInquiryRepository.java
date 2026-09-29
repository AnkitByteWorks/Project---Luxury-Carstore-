package com.Luxurycars.carstore.repository;

import com.Luxurycars.carstore.entity.VaultInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaultInquiryRepository extends JpaRepository<VaultInquiry, Long> {
    List<VaultInquiry> findByAllocationIdOrderByCreatedAtDesc(String allocationId);
}
