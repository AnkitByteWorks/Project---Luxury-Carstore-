-- ─── CAR TABLE ───
CREATE TABLE cars (
                      id BIGINT NOT NULL AUTO_INCREMENT,
                      name VARCHAR(100) NOT NULL,
                      brand VARCHAR(50) NOT NULL,
                      price DECIMAL(15,2) NOT NULL,
                      description VARCHAR(2000),
                      color_options VARCHAR(255),
                      showroom_location VARCHAR(100),
                      delivery_days INT,
                      payment_options VARCHAR(255),
                      image_data LONGBLOB,
                      image_name VARCHAR(255),
                      image_type VARCHAR(50),
                      created_at DATETIME(6),
                      updated_at DATETIME(6),
                      PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── ORDER TABLE ───
CREATE TABLE orders (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        car_id BIGINT NOT NULL,
                        car_name VARCHAR(100) NOT NULL,
                        unit_price DECIMAL(15,2) NOT NULL,
                        quantity INT NOT NULL,
                        total_amount DECIMAL(15,2) NOT NULL,
                        customer_name VARCHAR(100) NOT NULL,
                        customer_email VARCHAR(100) NOT NULL,
                        customer_phone VARCHAR(20) NOT NULL,
                        delivery_address VARCHAR(500) NOT NULL,
                        delivery_city VARCHAR(100),
                        delivery_pincode VARCHAR(10),
                        payment_method VARCHAR(50) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        ordered_at DATETIME(6),
                        updated_at DATETIME(6),
                        PRIMARY KEY (id),
                        CONSTRAINT fk_order_car FOREIGN KEY (car_id) REFERENCES cars(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;