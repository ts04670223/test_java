# Spring Boot 電商平台功能補強實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 為現有電商 Spring Boot 專案補充 7 項核心功能：Spring Profiles、Flyway、Controller 統一格式、AOP 日誌、Spring Events、排程任務、完整測試

**Architecture:** 採用 TDD 導向，先設定測試環境（H2 + test profile），再實作各功能，最後補全測試。各功能模組互相獨立，可平行實作但有順序依賴（Profiles → Flyway → DTO → AOP → Events → Scheduler → Tests）。

**Tech Stack:** Spring Boot 3.2.0, Java 17, Maven, MySQL（prod/dev）, H2（test）, Flyway 9.x, JUnit 5, Mockito, MockMvc

---

## 檔案變動總覽

### 新增 - 設定
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-test.properties`
- `src/main/resources/application-prod.properties`
- `src/main/resources/db/migration/V1__baseline_schema.sql`
- `src/main/resources/db/migration/V2__add_scheduler_audit_log.sql`

### 新增 - 程式碼
- `src/main/java/com/example/demo/aspect/LoggingAspect.java`
- `src/main/java/com/example/demo/event/OrderCreatedEvent.java`
- `src/main/java/com/example/demo/event/OrderStatusChangedEvent.java`
- `src/main/java/com/example/demo/event/OrderEventListener.java`
- `src/main/java/com/example/demo/scheduler/CartCleanupScheduler.java`
- `src/main/java/com/example/demo/scheduler/OrderAutoCloseScheduler.java`
- `src/main/java/com/example/demo/scheduler/ProductStatsScheduler.java`
- `src/main/java/com/example/demo/dto/ProductResponseDto.java`
- `src/main/java/com/example/demo/dto/ProductDetailResponseDto.java`
- `src/main/java/com/example/demo/dto/OrderResponseDto.java`
- `src/main/java/com/example/demo/dto/OrderItemResponseDto.java`

### 新增 - 測試
- `src/test/java/com/example/demo/service/ProductServiceTest.java`
- `src/test/java/com/example/demo/service/OrderServiceTest.java`
- `src/test/java/com/example/demo/controller/ProductControllerTest.java`
- `src/test/java/com/example/demo/controller/OrderControllerTest.java`
- `src/test/java/com/example/demo/repository/ProductRepositoryTest.java`
- `src/test/java/com/example/demo/integration/EcommerceFlowIntegrationTest.java`

### 修改
- `pom.xml` → 加 Flyway、H2 scope 改 test、加 spring-boot-starter-aop
- `src/main/resources/application.properties` → 加 Flyway 基本設定、預設 profile
- `src/main/java/com/example/demo/controller/ProductController.java` → ApiResponse + DTO + Pageable
- `src/main/java/com/example/demo/controller/OrderController.java` → ApiResponse + DTO
- `src/main/java/com/example/demo/service/OrderService.java` → 發布 Spring Events
- `src/main/java/com/example/demo/service/ProductService.java` → 加 Pageable 支援

---

## Task 1: Spring Profiles 多環境設定

**Files:**
- Modify: `src/main/resources/application.properties`
- Create: `src/main/resources/application-dev.properties`
- Create: `src/main/resources/application-test.properties`
- Create: `src/main/resources/application-prod.properties`

- [ ] **Step 1.1: 修改主 application.properties，抽出環境差異設定**

在 `application.properties` 末尾加入：
```properties
# ==============================================================
# 預設啟用 dev profile（生產環境請設定環境變數 SPRING_PROFILES_ACTIVE=prod）
# ==============================================================
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```

移除或註解以下環境特定設定（這些將由各 profile 覆蓋）：
```properties
# 這些移到 application-dev.properties 和 application-prod.properties
# spring.jpa.hibernate.ddl-auto=...  (已使用環境變數，保留)
```

- [ ] **Step 1.2: 建立 application-dev.properties（本地開發）**

```properties
# ==================== DEV 環境設定 ====================
spring.config.activate.on-profile=dev

# MySQL 連線（VM 本地）
spring.datasource.url=jdbc:mysql://192.168.10.10:3306/spring_boot_demo?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Taipei&allowPublicKeyRetrieval=true
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:springboot}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:springboot123}

# JPA - dev 環境允許 update（自動建表/更新）
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1

# Redis
spring.redis.host=${SPRING_REDIS_HOST:192.168.10.10}
spring.redis.port=${SPRING_REDIS_PORT:6379}

# Logging - dev 開啟詳細日誌
logging.level.com.example.demo=DEBUG
logging.level.org.springframework.web=DEBUG

# WebAuthn
webauthn.rp.id=localhost
webauthn.rp.name=Spring Boot Demo (Dev)
```

- [ ] **Step 1.3: 建立 application-test.properties（測試環境）**

```properties
# ==================== TEST 環境設定 ====================
spring.config.activate.on-profile=test

# H2 in-memory 測試資料庫
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA - 測試自動建表
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# H2 console（方便 debug 時查看資料）
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Flyway - 測試環境關閉（由 Hibernate create-drop 管理 schema）
spring.flyway.enabled=false

# Redis - 使用嵌入式或 mock（test 由 EmbeddedRedisConfig 提供）
spring.redis.host=localhost
spring.redis.port=6370

# Mail - 測試環境關閉
spring.mail.host=localhost
spring.mail.port=25
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false

# JWT
jwt.secret=testSecretKeyForJwtTokenGenerationWithEnoughLength12345678
jwt.expiration=86400000

# Scheduler - 測試環境關閉（避免干擾）
scheduling.enabled=false

# WebAuthn
webauthn.rp.id=localhost
webauthn.rp.name=Spring Boot Demo (Test)

# Logging
logging.level.com.example.demo=WARN
logging.level.org.springframework.security=WARN
```

- [ ] **Step 1.4: 建立 application-prod.properties（生產環境）**

```properties
# ==================== PROD 環境設定 ====================
spring.config.activate.on-profile=prod

# MySQL - 所有值必須來自環境變數
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

# JPA - 生產環境驗證 schema（不自動修改）
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=false
spring.flyway.validate-on-migrate=true

# Redis
spring.redis.host=${SPRING_REDIS_HOST}
spring.redis.port=${SPRING_REDIS_PORT:6379}
spring.redis.password=${SPRING_REDIS_PASSWORD:}

# Logging - 生產環境只記錄 WARN 以上
logging.level.com.example.demo=INFO
logging.level.org.springframework.web=WARN

# WebAuthn
webauthn.rp.id=${WEBAUTHN_RP_ID}
webauthn.rp.name=${WEBAUTHN_RP_NAME}
```

- [ ] **Step 1.5: 驗證 Profile 設定正確**

確認 application.properties 中設定 `spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}`，啟動時 console 應顯示：
```
The following 1 profile is active: "dev"
```

---

## Task 2: Flyway 資料庫版本控制

**Files:**
- Modify: `pom.xml`
- Create: `src/main/resources/db/migration/V1__baseline_schema.sql`
- Create: `src/main/resources/db/migration/V2__add_scheduler_audit_log.sql`

- [ ] **Step 2.1: 在 pom.xml 加入 Flyway 依賴**

在 `<dependencies>` 區塊中，Database 區段加入：
```xml
<!-- Flyway - 資料庫版本控制 -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

同時，將 H2 的 scope 從 `runtime` 改為可在 test 使用：
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2.2: 建立目錄結構**

建立 `src/main/resources/db/migration/` 目錄。

- [ ] **Step 2.3: 建立 V1__baseline_schema.sql（現有 schema 基準）**

> 注意：此腳本作為 Flyway baseline，**不會被執行**（因為設定了 `baseline-on-migrate=true` 且 `baseline-version=1`）。
> 它的用途是作為 schema 文件，讓 Flyway 從 V2 開始管理。

```sql
-- V1__baseline_schema.sql
-- 現有電商平台資料庫 schema 紀錄（Flyway baseline，不執行）
-- 由 Spring JPA ddl-auto 已建立

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
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at DATETIME,
    default_address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    created_at DATETIME,
    updated_at DATETIME
);

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
    active BOOLEAN NOT NULL DEFAULT TRUE,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
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
);

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
);

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
);

-- carts 表
CREATE TABLE IF NOT EXISTS carts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- cart_items 表
CREATE TABLE IF NOT EXISTS cart_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    quantity INT NOT NULL,
    price DECIMAL(10,2),
    cart_id INT NOT NULL,
    product_id BIGINT NOT NULL,
    FOREIGN KEY (cart_id) REFERENCES carts(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- wishlists 表
CREATE TABLE IF NOT EXISTS wishlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

- [ ] **Step 2.4: 建立 V2__add_scheduler_audit_log.sql（新功能 schema）**

```sql
-- V2__add_scheduler_audit_log.sql
-- 排程任務執行記錄表（Task 6 排程任務使用）

CREATE TABLE IF NOT EXISTS scheduler_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL,
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    message TEXT,
    affected_rows INT DEFAULT 0,
    duration_ms BIGINT DEFAULT 0
);

CREATE INDEX idx_scheduler_audit_task ON scheduler_audit_log(task_name);
CREATE INDEX idx_scheduler_audit_executed_at ON scheduler_audit_log(executed_at);
```

- [ ] **Step 2.5: 驗證 Flyway 設定**

啟動 dev profile 後，查看 flyway_schema_history 表：
```sql
SELECT * FROM flyway_schema_history;
```
應看到 `version=1, type=BASELINE`（不執行 SQL，只標記）和 `version=2, description=add scheduler audit log`（實際執行）。

---

## Task 3: Controller 回應格式統一化

**Files:**
- Create: `src/main/java/com/example/demo/dto/ProductResponseDto.java`
- Create: `src/main/java/com/example/demo/dto/ProductDetailResponseDto.java`
- Create: `src/main/java/com/example/demo/dto/OrderResponseDto.java`
- Create: `src/main/java/com/example/demo/dto/OrderItemResponseDto.java`
- Modify: `src/main/java/com/example/demo/controller/ProductController.java`
- Modify: `src/main/java/com/example/demo/controller/OrderController.java`
- Modify: `src/main/java/com/example/demo/service/ProductService.java`

- [ ] **Step 3.1: 建立 ProductResponseDto（列表用，輕量版）**

```java
package com.example.demo.dto;

import java.math.BigDecimal;

/**
 * 商品列表回應 DTO（輕量版，不含 Lazy 集合）
 */
public class ProductResponseDto {
    private Long id;
    private String name;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String brand;
    private String category;
    private String subCategory;
    private Boolean active;
    private Boolean featured;
    private Integer soldCount;
    private Double averageRating;
    private Integer reviewCount;

    // Static factory method from entity
    public static ProductResponseDto from(com.example.demo.model.Product product) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setShortDescription(product.getShortDescription());
        dto.setPrice(product.getPrice());
        dto.setOriginalPrice(product.getOriginalPrice());
        dto.setStock(product.getStock());
        dto.setBrand(product.getBrand());
        dto.setCategory(product.getCategory());
        dto.setSubCategory(product.getSubCategory());
        dto.setActive(product.getActive());
        dto.setFeatured(product.getFeatured());
        dto.setSoldCount(product.getSoldCount());
        dto.setAverageRating(product.getAverageRating());
        dto.setReviewCount(product.getReviewCount());
        return dto;
    }

    // Getters and Setters（全部 field）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
    public Integer getSoldCount() { return soldCount; }
    public void setSoldCount(Integer soldCount) { this.soldCount = soldCount; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
}
```

- [ ] **Step 3.2: 建立 OrderItemResponseDto**

```java
package com.example.demo.dto;

import java.math.BigDecimal;

public class OrderItemResponseDto {
    private Integer id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;

    public static OrderItemResponseDto from(com.example.demo.model.OrderItem item) {
        OrderItemResponseDto dto = new OrderItemResponseDto();
        dto.setId(item.getId());
        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
        }
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
```

- [ ] **Step 3.3: 建立 OrderResponseDto**

```java
package com.example.demo.dto;

import com.example.demo.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class OrderResponseDto {
    private Integer id;
    private String orderNumber;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String shippingAddress;
    private String phone;
    private String note;
    private Long userId;
    private String username;
    private List<OrderItemResponseDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponseDto from(com.example.demo.model.Order order) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setPhone(order.getPhone());
        dto.setNote(order.getNote());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getId());
            dto.setUsername(order.getUser().getUsername());
        }
        if (order.getItems() != null) {
            dto.setItems(order.getItems().stream()
                .map(OrderItemResponseDto::from)
                .collect(Collectors.toList()));
        }
        return dto;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public List<OrderItemResponseDto> getItems() { return items; }
    public void setItems(List<OrderItemResponseDto> items) { this.items = items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3.4: 修改 ProductService，加入 Pageable 支援**

在 `ProductService.java` 中，加入 import 和新方法：
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// 加入新方法（保留原有方法不動）
public Page<Product> getAllActiveProductsPaged(Pageable pageable) {
    return productRepository.findByActiveTrue(pageable);
}
```

同時在 `ProductRepository` 加入分頁版本：
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// 加入此方法到 ProductRepository interface
Page<Product> findByActiveTrue(Pageable pageable);
```

- [ ] **Step 3.5: 修改 ProductController，統一使用 ApiResponse**

以下是修改後的 ProductController 關鍵方法（移除所有 HashMap + try-catch 改為讓 GlobalExceptionHandler 處理）：

```java
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

// 修改 getAllProducts
@GetMapping
public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getAllProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir) {

    Sort sort = sortDir.equalsIgnoreCase("desc")
        ? Sort.by(sortBy).descending()
        : Sort.by(sortBy).ascending();
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<ProductResponseDto> products = productService.getAllActiveProductsPaged(pageable)
        .map(ProductResponseDto::from);
    return ResponseEntity.ok(ApiResponse.success("取得商品列表成功", products));
}

// 修改 getProductById
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable Long id) {
    Product product = productService.getProductById(id)
        .orElseThrow(() -> new RuntimeException("找不到商品，ID: " + id));
    return ResponseEntity.ok(ApiResponse.success(ProductResponseDto.from(product)));
}

// 修改 getProductsByCategory
@GetMapping("/category/{category}")
public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getProductsByCategory(
        @PathVariable String category) {
    List<ProductResponseDto> products = productService.getProductsByCategory(category)
        .stream().map(ProductResponseDto::from).collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success("查詢類別商品成功", products));
}

// 修改 searchProducts
@GetMapping("/search")
public ResponseEntity<ApiResponse<List<ProductResponseDto>>> searchProducts(
        @RequestParam String keyword) {
    List<ProductResponseDto> products = productService.searchProducts(keyword)
        .stream().map(ProductResponseDto::from).collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success("搜尋商品成功", products));
}
```

**移除 ProductController 中所有 try-catch 和 HashMap** — 改由 GlobalExceptionHandler 統一處理。

- [ ] **Step 3.6: 修改 OrderController，統一使用 ApiResponse**

```java
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.OrderResponseDto;

// 修改 createOrder
@PostMapping
public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
        @Valid @RequestBody CreateOrderRequest request) {
    Order order = orderService.createOrderFromCart(
        request.getUserId(), request.getShippingAddress(),
        request.getPhone(), request.getNote());
    log.info("訂單建立成功: orderId={}, userId={}", order.getId(), request.getUserId());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("訂單建立成功", OrderResponseDto.from(order)));
}

// 修改 getUserOrders
@GetMapping("/user/{userId}")
public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getUserOrders(
        @PathVariable Long userId, Authentication authentication) {
    com.example.demo.model.User currentUser = (com.example.demo.model.User) authentication.getPrincipal();
    boolean isAdmin = currentUser.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    if (!currentUser.getId().equals(userId) && !isAdmin) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("無權限存取他人訂單"));
    }
    List<OrderResponseDto> orders = orderService.getUserOrders(userId)
        .stream().map(OrderResponseDto::from).collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success("取得訂單列表成功", orders));
}
```

**同樣移除所有 try-catch 和 HashMap**。

---

## Task 4: AOP 切面日誌

**Files:**
- Modify: `pom.xml` → 加入 spring-boot-starter-aop
- Create: `src/main/java/com/example/demo/aspect/LoggingAspect.java`

- [ ] **Step 4.1: 在 pom.xml 加入 AOP 依賴**

```xml
<!-- AOP - 切面程式設計 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

- [ ] **Step 4.2: 建立 LoggingAspect.java**

```java
package com.example.demo.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * 切入點：所有 controller 套件下的方法
     */
    @Pointcut("execution(* com.example.demo.controller..*(..))")
    public void controllerMethods() {}

    /**
     * 切入點：所有 service 套件下的方法
     */
    @Pointcut("execution(* com.example.demo.service..*(..))")
    public void serviceMethods() {}

    /**
     * @Around：記錄 Controller 方法執行時間與請求資訊
     */
    @Around("controllerMethods()")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 取得當前 HTTP 請求
        String requestInfo = getRequestInfo();
        String methodName = joinPoint.getSignature().toShortString();
        String currentUser = getCurrentUsername();

        log.info("[API] 開始執行 {} | 請求: {} | 用戶: {}", methodName, requestInfo, currentUser);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[API] 完成 {} | 耗時: {}ms | 用戶: {}", methodName, duration, currentUser);
            return result;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("[API] 例外 {} | 耗時: {}ms | 錯誤: {}", methodName, duration, ex.getMessage());
            throw ex;
        }
    }

    /**
     * @Around：記錄 Service 慢方法（超過 500ms 發出警告）
     */
    @Around("serviceMethods()")
    public Object logSlowServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        if (duration > 500) {
            log.warn("[SLOW SERVICE] {} 耗時 {}ms（超過 500ms 閾值）",
                joinPoint.getSignature().toShortString(), duration);
        }
        return result;
    }

    private String getRequestInfo() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getMethod() + " " + request.getRequestURI();
            }
        } catch (Exception ignored) {}
        return "UNKNOWN";
    }

    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "anonymous";
    }
}
```

- [ ] **Step 4.3: 驗證 AOP 生效**

啟動應用後呼叫 `GET /api/products`，console 應看到：
```
[API] 開始執行 ProductController.getAllProducts(..) | 請求: GET /api/products | 用戶: anonymous
[API] 完成 ProductController.getAllProducts(..) | 耗時: 45ms | 用戶: anonymous
```

---

## Task 5: Spring Events 事件驅動解耦

**Files:**
- Create: `src/main/java/com/example/demo/event/OrderCreatedEvent.java`
- Create: `src/main/java/com/example/demo/event/OrderStatusChangedEvent.java`
- Create: `src/main/java/com/example/demo/event/OrderEventListener.java`
- Modify: `src/main/java/com/example/demo/service/OrderService.java`

- [ ] **Step 5.1: 建立 OrderCreatedEvent**

```java
package com.example.demo.event;

import org.springframework.context.ApplicationEvent;
import java.math.BigDecimal;

/**
 * 訂單建立事件
 * 由 OrderService.createOrderFromCart() 在事務成功後發布
 */
public class OrderCreatedEvent extends ApplicationEvent {

    private final Integer orderId;
    private final String orderNumber;
    private final Long userId;
    private final String username;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(Object source, Integer orderId, String orderNumber,
                             Long userId, String username, BigDecimal totalAmount) {
        super(source);
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.username = username;
        this.totalAmount = totalAmount;
    }

    public Integer getOrderId() { return orderId; }
    public String getOrderNumber() { return orderNumber; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
```

- [ ] **Step 5.2: 建立 OrderStatusChangedEvent**

```java
package com.example.demo.event;

import com.example.demo.model.OrderStatus;
import org.springframework.context.ApplicationEvent;

/**
 * 訂單狀態變更事件
 * 由 OrderService.updateOrderStatus() 發布
 */
public class OrderStatusChangedEvent extends ApplicationEvent {

    private final Integer orderId;
    private final String orderNumber;
    private final Long userId;
    private final OrderStatus oldStatus;
    private final OrderStatus newStatus;

    public OrderStatusChangedEvent(Object source, Integer orderId, String orderNumber,
                                   Long userId, OrderStatus oldStatus, OrderStatus newStatus) {
        super(source);
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public Integer getOrderId() { return orderId; }
    public String getOrderNumber() { return orderNumber; }
    public Long getUserId() { return userId; }
    public OrderStatus getOldStatus() { return oldStatus; }
    public OrderStatus getNewStatus() { return newStatus; }
}
```

- [ ] **Step 5.3: 建立 OrderEventListener**

```java
package com.example.demo.event;

import com.example.demo.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 訂單事件監聽器
 * 使用 @Async 確保通知發送不阻塞訂單主流程
 * 使用 @TransactionalEventListener 確保事務成功後才處理
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 訂單建立後，非同步發送確認通知
     * AFTER_COMMIT：確保訂單已成功寫入資料庫後才發送
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[Event] 處理訂單建立事件: orderId={}, orderNumber={}, userId={}",
            event.getOrderId(), event.getOrderNumber(), event.getUserId());
        try {
            notificationService.sendOrderConfirmation(
                event.getUserId(), event.getOrderNumber(), event.getTotalAmount());
            log.info("[Event] 訂單確認通知發送成功: orderNumber={}", event.getOrderNumber());
        } catch (Exception ex) {
            log.error("[Event] 發送訂單確認通知失敗: orderNumber={}, error={}",
                event.getOrderNumber(), ex.getMessage());
            // 不重新拋出例外，避免影響訂單流程
        }
    }

    /**
     * 訂單狀態變更，非同步發送狀態更新通知
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("[Event] 處理訂單狀態變更事件: orderId={}, {} -> {}",
            event.getOrderId(), event.getOldStatus(), event.getNewStatus());
        try {
            notificationService.sendOrderStatusUpdate(
                event.getUserId(), event.getOrderNumber(),
                event.getOldStatus().name(), event.getNewStatus().name());
            log.info("[Event] 訂單狀態通知發送成功: orderNumber={}", event.getOrderNumber());
        } catch (Exception ex) {
            log.error("[Event] 發送訂單狀態通知失敗: orderNumber={}, error={}",
                event.getOrderNumber(), ex.getMessage());
        }
    }
}
```

- [ ] **Step 5.4: 修改 NotificationService，加入所需方法**

確認 NotificationService 中有以下方法（若無則新增）：
```java
public void sendOrderConfirmation(Long userId, String orderNumber, java.math.BigDecimal totalAmount) {
    log.info("[Notification] 訂單確認: userId={}, orderNumber={}, totalAmount={}",
        userId, orderNumber, totalAmount);
    // TODO: 實際郵件發送（需要 email 配置）
}

public void sendOrderStatusUpdate(Long userId, String orderNumber, String oldStatus, String newStatus) {
    log.info("[Notification] 訂單狀態更新: userId={}, orderNumber={}, {} -> {}",
        userId, orderNumber, oldStatus, newStatus);
    // TODO: 實際郵件發送
}
```

- [ ] **Step 5.5: 修改 OrderService，改用事件發布**

在 OrderService 中：
1. 注入 `ApplicationEventPublisher`
2. 移除直接注入 `NotificationService`
3. 在 `createOrderFromCart()` 末尾發布事件

```java
import org.springframework.context.ApplicationEventPublisher;
import com.example.demo.event.OrderCreatedEvent;
import com.example.demo.event.OrderStatusChangedEvent;

// 建構子改為：
public OrderService(OrderRepository orderRepository,
        UserRepository userRepository,
        CartService cartService,
        ProductService productService,
        ApplicationEventPublisher eventPublisher) {  // 取代 NotificationService
    this.orderRepository = orderRepository;
    this.userRepository = userRepository;
    this.cartService = cartService;
    this.productService = productService;
    this.eventPublisher = eventPublisher;
}

// 在 createOrderFromCart() 儲存訂單後加入：
Order savedOrder = orderRepository.save(order);
// ...清空購物車...
// 發布訂單建立事件（NotificationService 將異步接收）
eventPublisher.publishEvent(new OrderCreatedEvent(
    this, savedOrder.getId(), savedOrder.getOrderNumber(),
    user.getId(), user.getUsername(), savedOrder.getTotalAmount()));
return savedOrder;

// 在 updateOrderStatus() 中加入：
eventPublisher.publishEvent(new OrderStatusChangedEvent(
    this, order.getId(), order.getOrderNumber(),
    order.getUser().getId(), oldStatus, newStatus));
```

---

## Task 6: @Scheduled 排程任務

**Files:**
- Create: `src/main/java/com/example/demo/scheduler/CartCleanupScheduler.java`
- Create: `src/main/java/com/example/demo/scheduler/OrderAutoCloseScheduler.java`
- Create: `src/main/java/com/example/demo/scheduler/ProductStatsScheduler.java`

> 前提：`DemoApplication.java` 已有 `@EnableScheduling`，確認後續不用再加。
> 測試環境：`application-test.properties` 已設定 `scheduling.enabled=false`，
> 需要在各 Scheduler 加 `@ConditionalOnProperty(name="scheduling.enabled", havingValue="true", matchIfMissing=true)`

- [ ] **Step 6.1: 建立 CartCleanupScheduler**

```java
package com.example.demo.scheduler;

import com.example.demo.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 購物車清理排程
 * 每日凌晨 2 點執行：刪除 30 天以上的空購物車
 */
@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class CartCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CartCleanupScheduler.class);

    private final CartRepository cartRepository;

    public CartCleanupScheduler(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    /**
     * 每日凌晨 2:00 執行
     * Cron: 秒 分 時 日 月 週
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredCarts() {
        log.info("[Scheduler] 開始清理過期空購物車...");
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        try {
            // 查詢 30 天前更新、且沒有商品的購物車
            List<com.example.demo.model.Cart> expiredCarts =
                cartRepository.findEmptyCartsOlderThan(threshold);
            int count = expiredCarts.size();
            cartRepository.deleteAll(expiredCarts);
            log.info("[Scheduler] 清理完成，共刪除 {} 個過期空購物車", count);
        } catch (Exception ex) {
            log.error("[Scheduler] 清理過期購物車失敗: {}", ex.getMessage());
        }
    }

    /**
     * 每週一 4:00 執行：取得購物車統計資訊（用於監控）
     */
    @Scheduled(cron = "0 0 4 * * MON")
    public void reportCartStats() {
        long totalCarts = cartRepository.count();
        log.info("[Scheduler] 購物車統計 - 總數: {}", totalCarts);
    }
}
```

在 `CartRepository` 中加入查詢方法：
```java
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

@Query("SELECT c FROM Cart c WHERE c.items IS EMPTY AND c.updatedAt < :threshold")
List<com.example.demo.model.Cart> findEmptyCartsOlderThan(@Param("threshold") LocalDateTime threshold);
```

- [ ] **Step 6.2: 建立 OrderAutoCloseScheduler**

```java
package com.example.demo.scheduler;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 訂單自動取消排程
 * 每日凌晨 1:00：自動取消 7 天以上未付款的 PENDING 訂單
 */
@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class OrderAutoCloseScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoCloseScheduler.class);

    private final OrderRepository orderRepository;

    public OrderAutoCloseScheduler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void autoClosePendingOrders() {
        log.info("[Scheduler] 開始自動取消過期 PENDING 訂單...");
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        try {
            List<Order> pendingOrders = orderRepository.findPendingOrdersOlderThan(threshold);
            int count = 0;
            for (Order order : pendingOrders) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                count++;
                log.debug("[Scheduler] 訂單 {} 已自動取消", order.getOrderNumber());
            }
            log.info("[Scheduler] 自動取消完成，共取消 {} 筆訂單", count);
        } catch (Exception ex) {
            log.error("[Scheduler] 自動取消訂單失敗: {}", ex.getMessage());
        }
    }
}
```

在 `OrderRepository` 加入：
```java
import java.time.LocalDateTime;

@Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :threshold")
List<Order> findPendingOrdersOlderThan(@Param("threshold") LocalDateTime threshold);
```

- [ ] **Step 6.3: 建立 ProductStatsScheduler**

```java
package com.example.demo.scheduler;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品統計重算排程
 * 每日凌晨 3:00：重新計算所有商品的評分均值與評論數
 */
@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ProductStatsScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProductStatsScheduler.class);

    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;

    public ProductStatsScheduler(ProductRepository productRepository,
                                  ProductReviewRepository productReviewRepository) {
        this.productRepository = productRepository;
        this.productReviewRepository = productReviewRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void recalculateProductStats() {
        log.info("[Scheduler] 開始重算商品統計資訊...");
        List<Product> products = productRepository.findAll();
        int updated = 0;
        for (Product product : products) {
            try {
                Double avgRating = productReviewRepository.calculateAverageRating(product.getId());
                Integer reviewCount = productReviewRepository.countByProductId(product.getId());
                product.setAverageRating(avgRating != null ? avgRating : 0.0);
                product.setReviewCount(reviewCount != null ? reviewCount : 0);
                productRepository.save(product);
                updated++;
            } catch (Exception ex) {
                log.warn("[Scheduler] 更新商品 {} 統計失敗: {}", product.getId(), ex.getMessage());
            }
        }
        log.info("[Scheduler] 商品統計重算完成，共更新 {} 筆", updated);
    }
}
```

在 `ProductReviewRepository` 確認/加入：
```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.id = :productId AND r.approved = true")
Double calculateAverageRating(@Param("productId") Long productId);

@Query("SELECT COUNT(r) FROM ProductReview r WHERE r.product.id = :productId AND r.approved = true")
Integer countByProductId(@Param("productId") Long productId);
```

- [ ] **Step 6.4: 確認 @EnableScheduling 已存在**

檢查 `DemoApplication.java`，應有：
```java
@EnableScheduling  // 已有
@EnableAsync       // 已有
@SpringBootApplication
public class DemoApplication {
```

若無則加入。

---

## Task 7: 完整測試（單元 + 整合）

**Files:**
- Create: `src/test/java/com/example/demo/service/ProductServiceTest.java`
- Create: `src/test/java/com/example/demo/service/OrderServiceTest.java`
- Create: `src/test/java/com/example/demo/controller/ProductControllerTest.java`
- Create: `src/test/java/com/example/demo/controller/OrderControllerTest.java`
- Create: `src/test/java/com/example/demo/repository/ProductRepositoryTest.java`
- Create: `src/test/java/com/example/demo/integration/EcommerceFlowIntegrationTest.java`

> 所有測試使用 `@ActiveProfiles("test")` 啟用 H2。

- [ ] **Step 7.1: 建立 ProductServiceTest（純 Mockito，不啟動 Spring）**

```java
package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("測試商品");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);
        testProduct.setActive(true);
        testProduct.setCategory("Electronics");
    }

    @Test
    void getAllActiveProducts_shouldReturnActiveProducts() {
        when(productRepository.findByActiveTrue()).thenReturn(List.of(testProduct));

        List<Product> result = productService.getAllActiveProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("測試商品");
        verify(productRepository, times(1)).findByActiveTrue();
    }

    @Test
    void getProductById_whenExists_shouldReturnProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        Optional<Product> result = productService.getProductById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void getProductById_whenNotExists_shouldReturnEmpty() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Product> result = productService.getProductById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void getProductsByCategory_shouldReturnMatchingProducts() {
        when(productRepository.findByCategoryAndActiveTrue("Electronics"))
            .thenReturn(List.of(testProduct));

        List<Product> result = productService.getProductsByCategory("Electronics");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("Electronics");
    }

    @Test
    void searchProducts_shouldReturnMatchingByKeyword() {
        when(productRepository.findByNameContainingIgnoreCaseAndActiveTrue("測試"))
            .thenReturn(List.of(testProduct));

        List<Product> result = productService.searchProducts("測試");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllActiveProductsPaged_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageable, 1);
        when(productRepository.findByActiveTrue(pageable)).thenReturn(productPage);

        Page<Product> result = productService.getAllActiveProductsPaged(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
    }
}
```

Run: `mvn test -Dtest=ProductServiceTest -pl . -q`  
Expected: BUILD SUCCESS, 1 test class passed.

- [ ] **Step 7.2: 建立 OrderServiceTest**

```java
package com.example.demo.service;

import com.example.demo.event.OrderCreatedEvent;
import com.example.demo.model.*;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartService cartService;
    @Mock private ProductService productService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("測試商品");
        testProduct.setPrice(new BigDecimal("100.00"));
        testProduct.setStock(5);

        CartItem cartItem = new CartItem();
        cartItem.setProduct(testProduct);
        cartItem.setQuantity(2);
        cartItem.setPrice(new BigDecimal("100.00"));

        testCart = new Cart();
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>(List.of(cartItem)));
    }

    @Test
    void createOrderFromCart_withValidCart_shouldReturnOrder() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCartByUserId(1L)).thenReturn(testCart);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100);
            return o;
        });

        Order result = orderService.createOrderFromCart(
            1L, "台北市", "0912345678", "請小心輕放");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        // 驗證事件已發布
        verify(eventPublisher, times(1)).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void createOrderFromCart_withEmptyCart_shouldThrowException() {
        testCart.setItems(new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCartByUserId(1L)).thenReturn(testCart);

        assertThatThrownBy(() ->
            orderService.createOrderFromCart(1L, "台北市", "0912345678", null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("購物車是空的");
    }

    @Test
    void createOrderFromCart_withInsufficientStock_shouldThrowException() {
        testProduct.setStock(1); // 庫存只有 1，但購物車要買 2
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCartByUserId(1L)).thenReturn(testCart);

        assertThatThrownBy(() ->
            orderService.createOrderFromCart(1L, "台北市", "0912345678", null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("庫存不足");
        
        // 庫存不足時，不應發布事件
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createOrderFromCart_withNonExistentUser_shouldThrowException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            orderService.createOrderFromCart(99L, "台北市", "0912345678", null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("找不到用戶");
    }
}
```

- [ ] **Step 7.3: 建立 ProductControllerTest（@WebMvcTest）**

```java
package com.example.demo.controller;

import com.example.demo.config.JwtAuthenticationFilter;
import com.example.demo.config.SecurityConfig;
import com.example.demo.dto.ProductResponseDto;
import com.example.demo.model.Product;
import com.example.demo.service.JwtService;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProductController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ProductService productService;
    @MockBean private JwtService jwtService;
    @MockBean private UserService userService;

    @Test
    @WithMockUser
    void getAllProducts_shouldReturn200WithProducts() throws Exception {
        Product p = new Product();
        p.setId(1L);
        p.setName("測試商品");
        p.setPrice(new BigDecimal("99.99"));
        p.setStock(10);
        p.setActive(true);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("id").ascending());
        Page<Product> page = new PageImpl<>(List.of(p), pageable, 1);
        when(productService.getAllActiveProductsPaged(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].name").value("測試商品"))
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void getProductById_whenExists_shouldReturn200() throws Exception {
        Product p = new Product();
        p.setId(1L);
        p.setName("測試商品");
        p.setPrice(new BigDecimal("99.99"));
        p.setActive(true);
        when(productService.getProductById(1L)).thenReturn(Optional.of(p));

        mockMvc.perform(get("/api/products/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser
    void getProductById_whenNotExists_shouldReturn404() throws Exception {
        when(productService.getProductById(99L))
            .thenThrow(new RuntimeException("找不到商品，ID: 99"));

        mockMvc.perform(get("/api/products/99").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());  // GlobalExceptionHandler 處理
    }

    @Test
    @WithMockUser
    void searchProducts_shouldReturnMatchingProducts() throws Exception {
        Product p = new Product();
        p.setId(2L);
        p.setName("iPhone 15");
        p.setPrice(new BigDecimal("999.00"));
        p.setActive(true);
        when(productService.searchProducts("iPhone")).thenReturn(List.of(p));

        mockMvc.perform(get("/api/products/search?keyword=iPhone")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].name").value("iPhone 15"));
    }
}
```

- [ ] **Step 7.4: 建立 ProductRepositoryTest（@DataJpaTest + H2）**

```java
package com.example.demo.repository;

import com.example.demo.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        Product p1 = new Product();
        p1.setName("iPhone 15");
        p1.setPrice(new BigDecimal("999.00"));
        p1.setStock(5);
        p1.setActive(true);
        p1.setCategory("Electronics");

        Product p2 = new Product();
        p2.setName("Android Phone");
        p2.setPrice(new BigDecimal("599.00"));
        p2.setStock(3);
        p2.setActive(true);
        p2.setCategory("Electronics");

        Product p3 = new Product();
        p3.setName("Inactive Product");
        p3.setPrice(new BigDecimal("100.00"));
        p3.setStock(0);
        p3.setActive(false);
        p3.setCategory("Electronics");

        productRepository.saveAll(List.of(p1, p2, p3));
    }

    @Test
    void findByActiveTrue_shouldReturnOnlyActiveProducts() {
        List<Product> products = productRepository.findByActiveTrue();
        assertThat(products).hasSize(2);
        assertThat(products).allMatch(Product::getActive);
    }

    @Test
    void findByActiveTrue_paged_shouldReturnPage() {
        Page<Product> page = productRepository.findByActiveTrue(PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findByCategoryAndActiveTrue_shouldReturnMatchingProducts() {
        List<Product> products = productRepository.findByCategoryAndActiveTrue("Electronics");
        assertThat(products).hasSize(2);
    }

    @Test
    void findByNameContainingIgnoreCaseAndActiveTrue_shouldBeCaseInsensitive() {
        List<Product> results = productRepository.findByNameContainingIgnoreCaseAndActiveTrue("iphone");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("iPhone 15");
    }

    @Test
    void findAvailableProducts_shouldReturnProductsWithStock() {
        List<Product> available = productRepository.findAvailableProducts();
        assertThat(available).hasSize(2);
        assertThat(available).allMatch(p -> p.getStock() > 0);
    }
}
```

- [ ] **Step 7.5: 建立整合測試 EcommerceFlowIntegrationTest**

```java
package com.example.demo.integration;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 電商流程整合測試
 * 使用 test profile（H2），啟動完整 Spring context
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EcommerceFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_andLogin_shouldReturnJwtToken() throws Exception {
        // 1. 註冊
        RegisterRequest register = new RegisterRequest();
        register.setUsername("integrationtest_" + System.currentTimeMillis());
        register.setEmail("integration_" + System.currentTimeMillis() + "@test.com");
        register.setPassword("Password123!");
        register.setFirstName("整合");
        register.setLastName("測試");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn();

        // 2. 登入取得 JWT
        LoginRequest login = new LoginRequest();
        login.setUsername(register.getUsername());
        login.setPassword("Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").exists())
            .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        assertThat(responseBody).contains("token");
    }

    @Test
    void getProducts_publicEndpoint_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/products")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getProducts_withPagination_shouldReturnPagedResult() throws Exception {
        mockMvc.perform(get("/api/products?page=0&size=5")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.size").value(5));
    }
}
```

- [ ] **Step 7.6: 驗證所有測試通過**

執行所有測試：
```bash
mvn test -Dspring.profiles.active=test
```
Expected:
```
Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 最終驗證清單

- [ ] `mvn compile` 無編譯錯誤
- [ ] `mvn test -Dspring.profiles.active=test` 所有測試通過
- [ ] 啟動 dev profile，Flyway baseline 標記成功
- [ ] 呼叫 `GET /api/products` 回應格式為 `{"success":true,"data":{"content":[...],"totalElements":N}}`
- [ ] Console 看到 `[API]` AOP 日誌
- [ ] 建立訂單後 Console 看到 `[Event] 處理訂單建立事件`
- [ ] `application-test.properties` 啟用後，Scheduler 不執行（不干擾測試）
