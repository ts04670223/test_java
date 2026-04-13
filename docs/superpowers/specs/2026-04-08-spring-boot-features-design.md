# Spring Boot 電商平台功能補強設計文件

**日期:** 2026-04-08  
**版本:** 1.0  
**範疇:** 為現有電商 API 補充 Spring Boot 核心學習功能

---

## 1. 背景與目標

現有專案已是功能齊全的電商平台（JWT 認證、商品/訂單/購物車/願望清單/AI 聊天），  
但缺少幾個 Spring Boot 學習與生產環境必備的橫切關注點（cross-cutting concerns）。

**目標：** 在不破壞現有功能的前提下，補充以下 7 項功能：

| # | 功能 | 技術重點 |
|---|------|---------|
| 1 | Spring Profiles 多環境設定 | `application-dev/test/prod.properties` |
| 2 | Flyway 資料庫版本控制 | `db/migration/V*.sql` |
| 3 | Controller 回應格式統一 | `ApiResponse<T>` + Response DTOs |
| 4 | AOP 切面日誌與效能監控 | `@Aspect`, `@Around`, `@AfterReturning` |
| 5 | Spring Events 事件驅動解耦 | `ApplicationEventPublisher`, `@EventListener` |
| 6 | `@Scheduled` 排程任務 | Cron 清理過期購物車、訂單自動取消 |
| 7 | 完整測試（單元 + 整合） | Mockito, `@WebMvcTest`, `@DataJpaTest` |

---

## 2. 現有專案狀態

- **已有：** `GlobalExceptionHandler`、`ApiResponse<T>`、18 個 DTO、5 個測試檔案
- **缺少：** AOP、Flyway、排程、事件、完整測試、統一 Controller 格式
- **測試 DB：** H2 已在 pom.xml（`<scope>runtime</scope>`），需改為測試可用
- **Profile 現況：** 只有 `application-mysql.properties`，無 dev/test/prod 分離

---

## 3. 設計決策

### 3.1 Spring Profiles

| Profile | 用途 | 資料庫 | ddl-auto |
|---------|------|--------|---------|
| `dev` | 本地開發（VM MySQL） | MySQL 192.168.10.10 | `update` |
| `test` | 單元/整合測試（CI） | H2 in-memory | `create-drop` |
| `prod` | 生產環境 | MySQL（環境變數） | `validate` |

主 `application.properties` 保留通用設定，各 profile 覆蓋環境特定值。

### 3.2 Flyway

- 現有 schema 已存在（`ddl-auto=validate`），使用 `baseline-on-migrate=true`
- `baseline-version=1`，baseline-description = "existing schema"
- 新功能的 schema 變更從 `V2__` 開始
- V1 SQL：記錄現有 tables（作為文件用途，由 Flyway baseline 跳過）
- 測試環境（H2）：`flyway.enabled=false`，使用 `create-drop`

### 3.3 Controller 回應格式統一

目標：所有 Controller 統一使用 `ResponseEntity<ApiResponse<T>>`

**需要建立的 Response DTO：**
- `ProductResponseDto`（避免序列化 Lazy 集合問題）
- `ProductDetailResponseDto`（含 images/variants）
- `OrderResponseDto`
- `OrderItemResponseDto`

**需要修改的 Controller：**
- `ProductController`：改用 `ApiResponse<T>` + `Page<ProductResponseDto>`
- `OrderController`：改用 `ApiResponse<T>` + `OrderResponseDto`

**不動的 Controller：** `AuthController`、`WishlistController`（已正確使用 ApiResponse）

### 3.4 AOP 切面

**檔案：** `aspect/LoggingAspect.java`

功能：
- `@Around` 所有 `controller` 層方法：記錄 URI、HTTP method、執行時間、回應狀態
- `@AfterReturning` 記錄成功的寫入操作（POST/PUT/DELETE）
- `@AfterThrowing` 記錄例外（補充 GlobalExceptionHandler）

日誌格式：`[API] POST /api/orders | User: johny | 142ms | Status: 201`

### 3.5 Spring Events 事件驅動

**解耦目標：** `OrderService` 不直接呼叫 `NotificationService`

**事件類別：**
- `OrderCreatedEvent`（含 orderId, userId, totalAmount）
- `OrderStatusChangedEvent`（含 orderId, oldStatus, newStatus）

**監聽器：**
- `OrderEventListener`（`@Async` 非同步，避免影響主流程）
  - 監聽 OrderCreated → 呼叫 NotificationService 發送確認
  - 監聽 OrderStatusChanged → 呼叫 NotificationService 發送狀態更新

### 3.6 排程任務

**檔案：** `scheduler/` 套件

| 類別 | Cron | 功能 |
|------|------|------|
| `CartCleanupScheduler` | `0 0 2 * * ?`（每日凌晨 2 點） | 刪除 30 天無操作的空購物車 |
| `OrderAutoCloseScheduler` | `0 0 1 * * ?`（每日凌晨 1 點） | 自動取消 7 天未付款 PENDING 訂單 |
| `ProductStatsScheduler` | `0 0 3 * * ?`（每日凌晨 3 點） | 重新計算商品評分均值與評論數 |

### 3.7 測試策略

| 層級 | 工具 | 範圍 |
|------|------|------|
| Service 單元測試 | Mockito + JUnit 5 | ProductService, OrderService, CartService |
| Controller 測試 | `@WebMvcTest` + MockMvc | ProductController, OrderController, AuthController |
| Repository 測試 | `@DataJpaTest` + H2 | ProductRepository, OrderRepository |
| 整合測試 | `@SpringBootTest` + H2 | 完整流程（註冊→登入→購物→下單） |

---

## 4. 檔案變動清單

### 新增檔案（16 個）

```
src/main/resources/
  application-dev.properties
  application-test.properties
  application-prod.properties
  db/migration/
    V1__baseline_schema.sql
    V2__add_scheduler_audit.sql

src/main/java/com/example/demo/
  aspect/
    LoggingAspect.java
  event/
    OrderCreatedEvent.java
    OrderStatusChangedEvent.java
    OrderEventListener.java
  scheduler/
    CartCleanupScheduler.java
    OrderAutoCloseScheduler.java
    ProductStatsScheduler.java
  dto/
    ProductResponseDto.java
    ProductDetailResponseDto.java
    OrderResponseDto.java
    OrderItemResponseDto.java

src/test/java/com/example/demo/
  service/
    ProductServiceTest.java
    OrderServiceTest.java
    CartServiceTest.java
  controller/
    ProductControllerTest.java
    OrderControllerTest.java
  repository/
    ProductRepositoryTest.java
  integration/
    EcommerceFlowIntegrationTest.java
```

### 修改檔案（5 個）

```
pom.xml                          → 加入 Flyway dependency, H2 scope 改為 test
application.properties           → 加入 flyway 基本設定, spring.profiles.active=dev
controller/ProductController.java → 改用 ApiResponse + ProductResponseDto + Pageable
controller/OrderController.java   → 改用 ApiResponse + OrderResponseDto
service/OrderService.java         → 發布 OrderCreatedEvent / OrderStatusChangedEvent
```

---

## 5. 風險與注意事項

1. **Lazy Loading 序列化：** Product 有 OneToMany 集合，DTO 轉換時只取需要的欄位，避免 N+1
2. **Flyway + 現有 DB：** 使用 `baseline-on-migrate=true` 保護現有資料
3. **@Async + @EventListener：** 需要 `@EnableAsync` 在 DemoApplication（已有），確認 ThreadPool 設定
4. **H2 SQL 相容性：** Flyway 在測試環境關閉，H2 用 `create-drop` 自動建表
5. **NotificationService 現況：** 目前 NotificationService 的郵件發送是空實作，事件驅動不影響功能
