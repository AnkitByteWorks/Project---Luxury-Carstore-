-- ═══════════════════════════════════════════════════════════════
-- V8: Add Bespoke Configurator Specs & Live Hypercar Auctions
-- ═══════════════════════════════════════════════════════════════

-- 1. Orders: Add customOptions and customPrice
ALTER TABLE orders ADD COLUMN custom_options TEXT;
ALTER TABLE orders ADD COLUMN custom_price DECIMAL(15,2);

-- 2. Auction Lots Table
CREATE TABLE auction_lots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    image_url VARCHAR(500),
    starting_bid DECIMAL(15,2) NOT NULL,
    current_bid DECIMAL(15,2) NOT NULL,
    reserve_price DECIMAL(15,2),
    min_increment DECIMAL(15,2) NOT NULL DEFAULT 500000.00,
    reserve_met BIT(1) NOT NULL DEFAULT 0,
    active BIT(1) NOT NULL DEFAULT 1,
    lot_ends_at DATETIME(6) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Bids Table
CREATE TABLE bids (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lot_id BIGINT NOT NULL,
    bidder_name VARCHAR(100) NOT NULL,
    bidder_location VARCHAR(100),
    amount DECIMAL(15,2) NOT NULL,
    bid_placed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_bids_lot_id (lot_id),
    CONSTRAINT fk_bids_lot FOREIGN KEY (lot_id) REFERENCES auction_lots(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
