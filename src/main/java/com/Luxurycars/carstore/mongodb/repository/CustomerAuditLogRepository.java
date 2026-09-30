package com.Luxurycars.carstore.mongodb.repository;

import com.Luxurycars.carstore.mongodb.document.CustomerAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerAuditLogRepository extends MongoRepository<CustomerAuditLog, String> {
    List<CustomerAuditLog> findByCustomerIdentifierOrderByTimestampDesc(String customerIdentifier);
    List<CustomerAuditLog> findByActionTypeOrderByTimestampDesc(String actionType);
    List<CustomerAuditLog> findTop20ByOrderByTimestampDesc();
}
