-- ═══════════════════════════════════════════════════════════════
-- V9: Add Secret Vault Allocations, VIP Inquiries & Order Monogramming
-- ═══════════════════════════════════════════════════════════════

-- 1. Orders: Add monogramText and monogramColor
ALTER TABLE orders ADD COLUMN monogram_text VARCHAR(255);
ALTER TABLE orders ADD COLUMN monogram_color VARCHAR(100);

-- 2. Vault Allocations Table
CREATE TABLE vault_allocations (
    id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    builder VARCHAR(255) NOT NULL,
    chassis_number VARCHAR(255) NOT NULL,
    production_run VARCHAR(255) NOT NULL,
    engine_specs VARCHAR(500) NOT NULL,
    horsepower INT NOT NULL,
    top_speed_kmh INT NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    status VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Seed Initial 4 Secret Vault Hypercars
INSERT INTO vault_allocations (id, name, builder, chassis_number, production_run, engine_specs, horsepower, top_speed_kmh, price, image_url, status)
VALUES
('v-01', 'Pagani Huayra R', 'Horacio Pagani Atelier (San Cesario sul Panaro)', 'Chassis #14/30 • Carbo-Titanium HP62 G2', '1 of 30 Worldwide', '6.0L Naturally Aspirated V12 (HWA AG)', 850, 380, 350000000.00, 'https://images.unsplash.com/photo-1544829099-b9a0c07fad1a?auto=format&fit=crop&w=1200&q=80', 'Private Allocation Available'),
('v-02', 'Koenigsegg Jesko Absolut', 'Koenigsegg Automotive (Ängelholm, Sweden)', 'Chassis #007 • Low-Drag High-Speed Monocoque', 'Strictly Limited Production', '5.0L Twin-Turbo Flat-Plane V8 (E85 Capable)', 1600, 531, 420000000.00, 'https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?auto=format&fit=crop&w=1200&q=80', 'Build Slot #04 Reserved for Delivery'),
('v-03', 'Aston Martin Valkyrie AMR Pro', 'Aston Martin Performance Technologies (Gaydon)', 'Chassis #22/40 • Full Carbon Aerocell', '1 of 40 Worldwide', '6.5L Naturally Aspirated Cosworth V12', 1000, 362, 385000000.00, 'https://images.unsplash.com/photo-1621135802920-133df287f89c?auto=format&fit=crop&w=1200&q=80', 'Final Chassis Available'),
('v-04', 'Bugatti Bolide Track Homologation', 'Bugatti Atelier (Molsheim, France)', 'Chassis #03/40 • FIA LMH Carbon Monocoque', '1 of 40 Worldwide', '8.0L Quad-Turbo W16 (110 Octane Race Fuel)', 1850, 500, 440000000.00, 'https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=1200&q=80', 'Confidential Slot Inquire Only');

-- 4. Vault Inquiries Table
CREATE TABLE vault_inquiries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    allocation_id VARCHAR(50) NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    client_phone VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_vault_inquiries_allocation (allocation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
