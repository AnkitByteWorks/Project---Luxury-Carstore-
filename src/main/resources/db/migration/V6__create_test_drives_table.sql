-- ═══════════════════════════════════════════════
-- V6: Create Test Drives Table for VIP Bookings
-- ═══════════════════════════════════════════════

CREATE TABLE test_drives (
    id BIGINT NOT NULL AUTO_INCREMENT,
    car_id BIGINT NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    preferred_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    experience_type VARCHAR(20) NOT NULL,
    reference_code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_test_drives_ref_code (reference_code),
    CONSTRAINT fk_test_drives_car FOREIGN KEY (car_id) REFERENCES cars(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_test_drives_ref_code ON test_drives(reference_code);
CREATE INDEX idx_test_drives_email ON test_drives(email);
CREATE INDEX idx_test_drives_status ON test_drives(status);
