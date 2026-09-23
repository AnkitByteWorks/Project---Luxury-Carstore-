package com.Luxurycars.carstore.repository;


import com.Luxurycars.carstore.entity.Order;
import com.Luxurycars.carstore.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find all orders for a specific car
    List<Order> findByCarId(Long carId);

    // Find all orders by customer email
    List<Order> findByCustomerEmailIgnoreCase(String email);

    // Find all orders with a specific status
    List<Order> findByStatus(OrderStatus status);
}