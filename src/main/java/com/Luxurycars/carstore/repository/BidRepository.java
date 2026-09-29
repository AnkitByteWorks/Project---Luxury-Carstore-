package com.Luxurycars.carstore.repository;

import com.Luxurycars.carstore.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByLotIdOrderByBidPlacedAtDesc(Long lotId);

    Optional<Bid> findTopByLotIdOrderByAmountDesc(Long lotId);
}
