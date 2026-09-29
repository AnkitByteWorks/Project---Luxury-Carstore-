package com.Luxurycars.carstore.repository;

import com.Luxurycars.carstore.entity.AuctionLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionLotRepository extends JpaRepository<AuctionLot, Long> {

    List<AuctionLot> findByActiveTrueOrderByLotEndsAtAsc();

    List<AuctionLot> findByActiveTrue();
}
