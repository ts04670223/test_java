-- V1__baseline_schema.sql
-- 現有電商平台資料庫 schema 基準記錄
-- 此腳本因設定 spring.flyway.baseline-on-migrate=true 且 baseline-version=1，
-- 不會被 Flyway 執行，僅作為 schema 文件使用。
-- 後續新功能 schema 變更從 V2 開始。

-- users 表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    avatar VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    gender VARCHAR(10),
    date_of_birth DATE,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    email_verified TINYINT(1) NOT NULL DEFAULT 0,
    phone_verified TINYINT(1) NOT NULL DEFAULT 0,
    last_login_at DATETIME,
    default_address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- products 表
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    short_description TEXT,
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    stock INT NOT NULL DEFAULT 0,
    brand VARCHAR(100),
    category VARCHAR(100),
    sub_category VARCHAR(100),
    active TINYINT(1) NOT NULL DEFAULT 1,
    featured TINYINT(1) NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    sold_count INT NOT NULL DEFAULT 0,
    meta_title VARCHAR(200),
    meta_description TEXT,
    slug VARCHAR(200),
    average_rating DOUBLE,
    review_count INT,
    weight DOUBLE,
    dimensions VARCHAR(100),
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- orders 表
CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    shipping_address VARCHAR(255),
    phone VARCHAR(20),
    note TEXT,
    user_id BIGINT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- order_items 表
CREATE TABLE IF NOT EXISTS order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    order_id INT NOT NULL,
    product_id BIGINT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- carts 表
CREATE TABLE IF NOT EXISTS carts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- cart_items 表
CREATE TABLE IF NOT EXISTS cart_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    quantity INT NOT NULL,
    price DECIMAL(10,2),
    cart_id INT NOT NULL,
    product_id BIGINT NOT NULL,
    FOREIGN KEY (cart_id) REFERENCES carts(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- wishlists 表
CREATE TABLE IF NOT EXISTS wishlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- chat_messages 表
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT,
    receiver_id BIGINT,
    message TEXT NOT NULL,
    is_read TINYINT(1) DEFAULT 0,
    timestamp DATETIME,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- coupons 表
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    coupon_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    value DECIMAL(10,2) NOT NULL,
    minimum_amount DECIMAL(10,2),
    maximum_discount DECIMAL(10,2),
    usage_limit INT,
    used_count INT DEFAULT 0,
    start_date DATETIME,
    end_date DATETIME,
    active TINYINT(1) DEFAULT 1,
    created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- passkey_credentials 表
CREATE TABLE IF NOT EXISTS passkey_credentials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    credential_id VARCHAR(500) NOT NULL,
    public_key_cose LONGBLOB,
    sign_count BIGINT DEFAULT 0,
    transports VARCHAR(255),
    created_at DATETIME,
    last_used_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- product_images 表
CREATE TABLE IF NOT EXISTS product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(200),
    sort_order INT DEFAULT 0,
    is_primary TINYINT(1) DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- product_variants 表
CREATE TABLE IF NOT EXISTS product_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100),
    size VARCHAR(50),
    color VARCHAR(50),
    material VARCHAR(100),
    stock INT DEFAULT 0,
    price_adjustment DECIMAL(10,2),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- product_reviews 表
CREATE TABLE IF NOT EXISTS product_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    title VARCHAR(200),
    comment TEXT,
    verified TINYINT(1) DEFAULT 0,
    approved TINYINT(1) DEFAULT 0,
    created_at DATETIME,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- product_tags 表
CREATE TABLE IF NOT EXISTS product_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
