package com.Luxurycars.carstore.mongodb.repository;

import com.Luxurycars.carstore.mongodb.document.VehicleTelemetryLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleTelemetryLogRepository extends MongoRepository<VehicleTelemetryLog, String> {
    List<VehicleTelemetryLog> findByOrderIdOrderByRecordedAtDesc(Long orderId);
    List<VehicleTelemetryLog> findTop50ByCarrierIdOrderByRecordedAtDesc(String carrierId);
}
