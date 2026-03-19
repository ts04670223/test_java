# System Design Document — Spring Boot 電商平台

**版本**: 1.0  
**日期**: 2026-03-19  
**目的**: 供技術 Lead / 外部審查者進行架構評估，著重設計決策與取捨

---

## 1. 系統概述

這是一個以 **Spring Boot 3.2.0** 為核心的電商平台，提供商品瀏覽、購物車、訂單管理、即時聊天、LLM 輔助與 Passkey 無密碼登入等功能。系統部署於 Kubernetes（單節點 Vagrant VM），以 Kong Gateway 作為 API 閘道，並整合 Prometheus + Grafana 監控。

### 技術棧總覽

| 層級 | 技術 | 版本 |
|------|------|------|
| 後端框架 | Spring Boot | 3.2.0 |
| 語言 | Java | 17 |
| 前端框架 | Vue 3 + Vuetify 3 | 3.3.8 / 3.4.0 |
| 前端建置 | Vite | 5.0 |
| 狀態管理 | Pinia | 2.1.7 |
| 主資料庫 | MySQL | 8.0 |
| 快取 | Redis | 7 (alpine) |
| API 閘道 | Kong Gateway | DB-less mode |
| 容器編排 | Kubernetes (kubeadm) | 單節點 |
| 虛擬化 | Vagrant + VirtualBox | — |
| 監控 | Prometheus + Grafana | — |
| LLM | Ollama (qwen2.5:0.5b) | — |

---

## 2. 高層架構

```
┌─────────────────────────────────────────────────────────────────┐
│  Client (Windows Host)                                          │
│  ┌────────────────┐  ┌──────────────────────────────────────┐  │
│  │ Vue 3 SPA      │  │ Browser (直接存取服務)               │  │
│  │ Vite dev:3000  │  │ Grafana / Prometheus / K8s Dashboard │  │
│  └───────┬────────┘  └──────────────────────────────────────┘  │
└──────────┼──────────────────────────────────────────────────────┘
           │ HTTP
┌──────────▼──────────────────────────────────────────────────────┐
│  Nginx (192.168.10.10:80)  ─── /                → Vite :3000   │
│                            ─── /api             → Kong :30000  │
│                            ─── /prometheus      → Kong :30000  │
│                            ─── /grafana         → Kong :30000  │
└──────────┬──────────────────────────────────────────────────────┘
           │
┌──────────▼──────────────────────────────────────────────────────┐
│  Kubernetes Cluster (Vagrant VM: 192.168.10.10)                │
│                                                                  │
│  default namespace:                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │   Kong   │  │   App    │  │  MySQL   │  │    Redis     │   │
│  │:30000    │→ │:8080     │  │:3306     │  │:6379         │   │
│  │:30003    │  │(HPA 1-3) │  │NodePort  │  │ClusterIP     │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘   │
│                                                                  │
│  monitoring namespace:                                          │
│  ┌──────────────────┐  ┌──────────────────────────────────┐    │
│  │  Prometheus:9090 │  │  Grafana:3000                    │    │
│  │  NodePort:30090  │  │  NodePort:30300                   │    │
│  └──────────────────┘  └──────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

**設計決策**: 選擇 Nginx 作為統一入口做域名路由（`test6.test`），Kong 負責 API 層路由與 CORS 插件管理。這樣分離的好處是 Nginx 處理靜態/前端，Kong 專注服務間的 API 治理。缺點是多一層 hop，且兩層需各自維護。

> **前端部署說明**: 架構圖中 Nginx → Vite `:3000` 描述的是**開發環境**設定（使用 Vite dev server 提供 HMR）。生產環境應執行 `vite build` 將 SPA 編譯為靜態檔案，再由 Nginx 直接服務靜態資源。目前 repo 未包含生產部署的 CI/CD pipeline，實際生產部署方式待確認。

---

## 3. 後端應用設計

### 3.1 分層架構

```
Controller Layer  →  Service Layer  →  Repository Layer  →  Database
     ↓                   ↓
  DTO / API             Domain Model
  (18 個 DTO)           (18 個 Entity)
```

Spring Boot 標準三層架構，透過 DI 解耦。Controller 只做 HTTP 映射與 DTO 轉換，業務邏輯全在 Service 層。

### 3.2 API 端點分類

| Controller | 端點前綴 | 存取控制 | 說明 |
|------------|---------|---------|------|
| `AuthController` | `/api/auth/**` | 公開 | 登入、註冊、Token 刷新 |
| `ProductController` | `/api/products/**` | 公開（讀）/ 認證（寫） | 商品 CRUD |
| `CartController` | `/api/cart/**` | **需認證** | 購物車管理 |
| `OrderController` | `/api/orders/**` | **需認證** | 訂單建立與查詢 |
| `UserController` | `/api/users/**` | 混合 | 個人資料（部分需認證） |
| `ChatController` | `/api/chat/**` | 公開 | 使用者間訊息（REST） |
| `WishlistController` | `/api/wishlist/**` | **需認證** | 願望清單 |
| `AiController` | `/api/ai/**` | 公開 | Ollama LLM 健康 / 聊天 |
| `WebAuthnController` | `/api/passkeys/**` | 混合 | Passkey 註冊與驗證 |
| `HomeController` | `/` | 公開 | 健康端點 |

> **設計取捨**: `/api/chat/**` 與 `/api/ai/**` 目前設為公開，便於開發階段測試。生產環境應加入認證。

### 3.3 領域模型（Entity 關係）

```
User ──────────┬──── Order ────── OrderItem ──── Product
               ├──── Cart  ────── CartItem  ──── Product
               ├──── ChatMessage (sender/receiver → User)
               ├──── Wishlist ─── Product
               ├──── ProductReview
               └──── PasskeyCredential

Product ───────┬──── ProductImage
               ├──── ProductVariant
               ├──── ProductTag
               └──── ProductReview

Coupon (獨立，含 CouponType enum)
```

`User` 實作 `UserDetails` 介面，直接整合 Spring Security 的認證體系，避免額外的 UserDetails 轉換層。

### 3.4 Service 層職責

| Service | 核心職責 | 特別設計 |
|---------|---------|---------|
| `UserService` | 使用者 CRUD、密碼管理 | BCrypt 加密 |
| `JwtService` | Token 產生與驗證 | secret 從環境變數注入 |
| `OrderService` | 訂單建立、狀態流轉 | `@Transactional` 保證一致性 |
| `OrderSecurityService` | 訂單存取授權 | 搭配 `@PreAuthorize` 做方法層級安全 |
| `CartService` | 購物車 CRUD | Redis 快取潛力（目前走 DB） |
| `ProductService` | 商品查詢、分類 | — |
| `ChatService` | 訊息儲存與查詢 | REST 模式（非 WebSocket） |
| `LlmService` | HTTP 呼叫 Ollama API | 串流支援，270s read timeout |
| `WebAuthnService` | Passkey 挑戰產生與驗證 | 使用 Yubico webauthn-server-core |
| `NotificationService` | 非同步訂單通知 | `@Async` 獨立執行緒，TODO: 接入郵件服務 |
| `DataInitializationService` | 初始測試資料 | 開發環境用 |

---

## 4. 安全設計

### 4.1 認證機制

系統支援兩種認證方式：

**方式一：JWT Bearer Token（傳統密碼登入）**

```
POST /api/auth/login
    ↓
UserService.loadUserByUsername()
    ↓ BCrypt.matches()
JwtService.generateToken()
    ↓
Response: { token: "eyJ..." }
    ↓
後續請求: Authorization: Bearer <token>
    ↓
JwtAuthenticationFilter（OncePerRequestFilter）
    ↓
SecurityContext
```

Token 有效期：86400000ms（24 小時），`jwt.secret` 由環境變數 `JWT_SECRET` 注入，不寫死在設定檔。

**方式二：WebAuthn / Passkey（無密碼登入）**

```
Registration: POST /api/passkeys/registration/start → /finish
              → 產生 PublicKeyCredentialCreationOptions
              → 瀏覽器 navigator.credentials.create()
              → 驗證並儲存 PasskeyCredential entity

Assertion:    POST /api/passkeys/assertion/start → /finish
              → AssertionRequest 暫存 HttpSession
              → 瀏覽器 navigator.credentials.get()
              → 驗證並回傳 JWT
```

> **注意**: WebAuthn 需要 HttpSession（暫存 AssertionRequest），而 Spring Security 設定為 `STATELESS`。這是一個架構矛盾點：WebAuthn Controller 實際上依賴了 Session，但全域設定為無狀態。此設計在負載均衡多副本場景下可能造成問題（Session affinity 未設定）。

### 4.2 JWT Token Lifecycle

- **簽名算法**: HS256（對稱式，`Keys.hmacShaKeyFor()`），secret 由 `JWT_SECRET` 環境變數注入
- **有效期**: 86400000ms（24 小時）
- **Token 換發**: `POST /api/auth/refresh` 接受現有**有效 access token**（`Authorization: Bearer <token>`），驗證通過後換發新 token（滑動視窗續期模式，非獨立 refresh token；無 rotate 機制）
- **Revocation 策略**: ⚠️ **目前無 Token 主動撤銷機制**。JWT 為無狀態設計，登出或密碼更改後，既有 Token 在到期前仍然有效。緩解方式：設定較短 expiration、或日後在 Redis 維護 token blacklist
- **密碼儲存**: BCrypt（Spring Security 預設 cost factor 10）

### 4.3 CSRF 防護

Spring Security 的 CSRF 保護已明確停用（`csrf.disable()`）。理由：API 設計為無狀態（JWT Bearer Token），不依賴 Cookie 進行認證，CSRF 攻擊向量不適用。搭配 `allowCredentials=true` 與精確 Origin 白名單實現安全的跨域控制。

### 4.4 授權層級

1. **URL 層級** — `SecurityConfig.filterChain()` 的 `authorizeHttpRequests`
2. **方法層級** — `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")`（用於 OrderController 管理員操作）
3. **業務層級** — `OrderSecurityService`（確保使用者只能操作自己的訂單）

### 4.5 CORS 設定

允許來源：`localhost:3000/3001/3002/5173`、`test6.test`、`192.168.10.10`。`allowCredentials=true`，搭配精確的 Origin 白名單（非 `*`）符合安全規範。

---

## 5. 資料層設計

### 5.1 MySQL（主資料庫）

- **連線池**: HikariCP，最大 10 連線，最小 idle 5
- **DDL 策略**: `validate`（生產環境防止自動改 schema）；本機開發可 override 為 `update`
- **Schema Migration 工具**: ⚠️ **目前未整合 Flyway / Liquibase**。Schema 演進靠手動 SQL 腳本執行，升級時需人工操作。這是架構層面的技術債，在多人協作或頻繁 schema 變更時存在風險
- **Dialect**: `org.hibernate.dialect.MySQLDialect`
- **Batch**: `hibernate.jdbc.batch_size=20`，啟用 `order_inserts/order_updates` 優化批次寫入

### 5.2 Redis（快取層）

- **連線**: Lettuce pool，max-active 8
- **CacheManager**: `RedisCacheManager`，序列化使用 `GenericJackson2JsonRedisSerializer`
- **ObjectMapper 配置**: 啟用 `JavaTimeModule`（支援 LocalDateTime）+ `Hibernate6Module`（防止 lazy proxy 序列化錯誤）
- **型別資訊**: 啟用 polymorphic type handling，快取值含類型資訊

**快取 TTL 設定**（`RedisConfig.java`）：

| 快取名稱 | TTL | 說明 |
|---------|-----|------|
| 預設（全域） | 30 分鐘 | 未特別指定的快取 |
| `product` | 10 分鐘 | 單一商品查詢 |
| `active_products` | 5 分鐘 | 商品列表（更新頻率較高）|

快取失效：依 TTL 自動過期；無主動 eviction（如資料更新後 `@CacheEvict`）設計待確認。

> **設計取捨**: Redis 目前主要作為 `@Cacheable` 基礎設施存在，CartService 仍走 DB。Redis 的 overhead 相對有限但已有實際快取命中（商品資料）。

### 5.3 持久化配置

`PersistentVolumeClaim` 為 MySQL 與 Redis 各自提供 PV 掛載，確保 Pod 重啟後資料不遺失。資料路徑由 `hostPath` 提供（VM 本機路徑），不適合多節點 K8s 叢集。

---

## 6. 前端設計

### 6.1 技術選型

| 技術 | 選擇原因 |
|------|---------|
| Vue 3 + Composition API | 現代響應式框架，支援 `<script setup>` |
| Vuetify 3 | Material Design 元件庫，加速 UI 建置 |
| Pinia | Vue 3 官方推薦的狀態管理（比 Vuex 輕量） |
| Vite | 極速 HMR，開發體驗優於 Webpack |
| Axios | HTTP 請求，含攔截器處理 Token 附加 |

### 6.2 頁面結構

```
App.vue
└── Layout.vue
    ├── pages/Shop.vue          ─── 商品列表
    ├── pages/ProductDetail.vue ─── 商品詳情
    ├── pages/Cart.vue          ─── 購物車
    ├── pages/Checkout.vue      ─── 結帳
    ├── pages/Orders.vue        ─── 訂單列表
    ├── pages/Chat.vue          ─── 使用者聊天
    ├── pages/Login.vue / Register.vue
    ├── pages/Profile.vue
    ├── pages/Wishlist.vue
    ├── pages/PasskeyManager.vue ─── Passkey 管理
    └── pages/admin/Dashboard.vue
```

### 6.3 狀態管理

- **`authStore`**: 用戶認證狀態、JWT Token、登入/登出動作
- **`cartStore`**: 購物車項目、數量同步

輔助工具：`cartEvents.js` / `wishlistEvents.js` — 使用自訂 EventBus 跨元件通知。

> **設計決策說明**: 這裡選擇 EventBus（自訂 Event 物件）而非 Pinia 跨 store action 的原因，是為了讓 Cart 更新通知（如加入購物車後更新 header badge）不需要建立 store 間的直接依賴。缺點是 EventBus 的事件流不如 Pinia DevTools 可追蹤，調試較困難。這是一個偏向 Vue 2 風格的設計選擇，在 Vue 3 生態中 Pinia action 是更主流的做法。

### 6.4 API 通訊

`services/api.js` 集中管理所有 API 呼叫，Axios instance 設定 Base URL，攔截器自動附加 `Authorization: Bearer <token>`。

---

## 7. LLM 整合設計

### 7.1 架構

```
AiController
    ↓
LlmService
    ↓ HTTP（`RestTemplate`，連線 timeout: 10s，read timeout: 270s）
Ollama API (http://localhost:11434)
    ↓
qwen2.5:0.5b model（輕量模型，0.5B 參數）
```

**部署位置**: Ollama 在 repo 中存在 `ollama-deployment.yaml`（K8s Deployment + PVC），但**目前實際運行於 VM 本機**（非透過 K8s 管理），Spring Boot 透過 `localhost:11434` 存取。K8s 部署設定作為未來容器化的預備，目前未啟用。

**設計取捨**:
- 優點：避免 K8s Pod 網路 overhead（VM 直連約 1-2 tok/s，K8s Pod 降至 0.27 tok/s）
- 缺點：Ollama 不在 K8s 管理範圍內，無法享有 K8s 的健康檢查與重啟機制

### 7.2 API 端點

| 端點 | 說明 |
|------|------|
| `GET /api/ai/health` | 健康檢查（回傳 Ollama 連線狀態） |
| `POST /api/ai/chat` | 阻塞式聊天（270s timeout，適合短回覆） |
| `POST /api/ai/chat/stream` | SSE 串流（建議大量輸出使用） |

`LlmService.chat()` 簽名：`chat(String prompt, List<AiMessage> history, String systemPrompt)`，支援帶上下文的多輪對話。

---

## 8. 觀測性設計

### 8.1 Metrics 收集

Spring Boot Actuator + Micrometer 自動暴露 `/actuator/prometheus` 端點，Prometheus 定期 scrape：

- JVM 指標（heap、GC、執行緒）
- HTTP 請求指標（count、duration、status）
- HikariCP 連線池指標
- 自訂 application 標籤（`spring.application.name`）

### 8.2 Grafana 儀表板

推薦匯入：
- **4701** JVM Micrometer（堆記憶體、GC 停頓）
- **11378** Spring Boot Statistics（HTTP throughput、P99 延遲）
- **315** Kubernetes Cluster（Pod CPU/Memory）

### 8.3 Log 策略

- 層級：Application `INFO`，Web `INFO`，Hibernate SQL `WARN`
- Log 檔案：`logback-spring.xml`（按日輪轉，輸出至 `logs/`；**未配置最大保留天數與單檔大小上限**，長時間 LLM 串流 + DEBUG log 存在磁碟耗盡風險）
- 目前沒有集中式 log 收集（ELK / Loki），僅本機檔案

---

## 9. 部署與伸縮設計

### 9.1 Kubernetes 資源配置

| 元件 | replicas | HPA | NodePort |
|------|---------|-----|---------|
| App (Spring Boot) | 1 (min) | CPU 80% → max 3 | 30080 |
| Kong Gateway | 1 | 無 | 30000 (proxy), 30003 (admin) |
| MySQL 8.0 | 1 | 無 | 30306 |
| Redis 7 | 1 | 無 | 無（ClusterIP） |
| Prometheus | 1 | 無 | 30090 |
| Grafana | 1 | 無 | 30300 |
| Ollama | 非 K8s | — | 11434 |

**HPA 觸發條件**: CPU 使用率超過 80% 時，`app` Deployment 可擴展至 3 副本。

### 9.2 Kong 路由初始化

Kong 運行於 **DB-less 模式**，沒有持久化的 Route 資料庫。路由設定透過 `kong/kong-setup-routes-job.yaml`（Kubernetes Job）在部署後自動初始化：

1. Init container 等待 Kong Admin API（port **8003**）就緒
   > 注：Kong 預設 Admin API 為 port 8001；此專案透過 `KONG_ADMIN_LISTEN: 0.0.0.0:8003` 環境變數覆蓋為 8003，NodePort 30003 作為外部存取點
2. Job container 透過 `curl` 呼叫 Kong Admin API 建立 Service 與 Route
3. Job `backoffLimit: 3`，失敗自動重試

> **Config Drift 風險**: Pod 重啟後路由設定不會自動重新套用（除非重跑 Job）。建議定期驗證路由狀態，或在自動重啟流程中整合路由驗證。

### 9.3 部署流程與 CI/CD

**目前流程（手動）**:

```
本機 Maven Build  →  vagrant-app:latest Docker image
                  →  kubectl apply -f *.yaml
                     init container: busybox 等待 MySQL 就緒
                  →  Spring Boot 啟動（DDL validate mode）
                  →  kong-setup-routes Job 執行
```

**映像版本管理**: 目前使用 `:latest` tag，在重新 build 後直接覆蓋。這在開發環境上可接受，但在生產環境中有以下風險：無法回滾至特定版本、多副本場景可能拉到不同版本的映像。

**CI/CD 成熟度**: 目前為 **Level 0**（純手動）。無自動化 pipeline，無 image registry，無 rolling update 腳本。生產化前需要引入至少基本的 CI 流程（如 GitHub Actions + image tag by commit SHA）。

### 9.4 已知限制

1. **WebAuthn + 多副本**: Session 沒有共享機制，Passkey 流程在多副本時可能失敗
2. **MySQL 單點**: 無主從複製，Pod 重啟會有短暫停機
3. **hostPath PV**: 資料綁定在單一節點，不支援 Pod 跨節點遷移
4. **Ollama 在 K8s 外**: GPU/CPU 資源不受 K8s 限制，也無自動重啟
5. **HPA Scale-down**: K8s 預設 scale-down stabilization window 為 5 分鐘；目前未設定記憶體指標，資源風險評估僅靠 CPU
6. **CI/CD 無自動化**: 全手動部署，無法回滾污染版本
7. **快取一致性視窗**: `@Cacheable` 無對應 `@CacheEvict`，商品更新後快取最長 TTL（10 分鐘）內仍提供舊資料

---

## 10. 設計取捨總結

| 決策 | 選擇 | 優點 | 缺點 |
|------|------|------|------|
| 認證架構 | JWT + WebAuthn 雙軌 | 現代無密碼支援 | WebAuthn 需 Session，與 STATELESS 衝突 |
| 聊天實作 | REST 輪詢 | 簡單易維護 | 無即時推送，需客戶端輪詢 |
| LLM 部署 | Ollama on VM | 效能較好 | 不受 K8s 管理 |
| 資料快取 | Redis（`@Cacheable`） | 具備快取能力 | 目前實際使用偏少 |
| DDL 策略 | `validate`（生產） | 防止意外 schema 變更 | 需手動執行 migration |
| API 閘道 | Kong DB-less | 輕量、聲明式設定 | 路由需透過腳本/Job 初始化 |
| 前端框架 | Vue 3 + Vuetify | 開發速度快 | Vuetify 包體較重 |
| 監控 | Prometheus + Grafana | 業界標準，免費 | 無集中式 log 收集 |

---

## 11. 後續改善建議

| 優先級 | 項目 | 原因 |
|--------|------|------|
| 高 | WebAuthn Session 共享（Redis Session） | 多副本時 Passkey 失效 |
| 高 | Chat 改為 WebSocket / SSE | REST 輪詢延遲高、效率差 |
| 中 | MySQL 主從或 PlanetScale | 消除單點故障 |
| 中 | 整合 Loki 或 ELK | 集中式 log 可觀測性 |
| 中 | `/api/chat/**` 與 `/api/ai/**` 加入認證 | 兩者目前公開存取，`/api/ai/**` 呼叫 LLM 有計算成本風險 |
| 低 | CartService 加入 Redis 快取 | 熱門商品購物車讀取優化 |
| 低 | Ollama 容器化至 K8s | 統一資源管理 |
