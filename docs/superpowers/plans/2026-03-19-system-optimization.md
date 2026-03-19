# System Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修復架構分析中識別的 5 個關鍵問題：K8s 明文 Secrets、SecurityConfig 過於寬鬆、JPA DDL 設定不當、HPA 無法擴展、以及 AiController 被停用。

**Architecture:** 安全性優先 → 應用層設定修復 → 基礎設施調整 → 功能恢復。每個 Task 可獨立部署，互不依賴。

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Security, JUnit 5, MockMvc, Kubernetes YAML (K8s Secrets / HPA / Deployment)

---

## File Map（變動檔案總覽）

| 檔案 | 類型 | 目的 |
|------|------|------|
| `app-secret.yaml` | 建立 | 儲存 DB 密碼與 JWT secret 的 K8s Secret 資源 |
| `app-deployment.yaml` | 修改 | 移除明文環境變數，改用 `secretKeyRef` 引用 |
| `src/main/resources/application.properties` | 修改 | 移除密碼預設恢復值 (`:springboot123`)，移除硬編碼 JWT secret |
| `app-hpa.yaml` | 修改 | 將 `maxReplicas` 從 1 改為 3 |
| `src/main/java/com/example/demo/config/SecurityConfig.java` | 修改 | `/api/cart/**` 改為需要認證 |
| `src/main/java/com/example/demo/controller/AiController.java` | 修改 | 恢復 `@RestController` 註解 |
| `src/test/java/com/example/demo/config/SecurityConfigTest.java` | 建立 | 驗證 /api/cart 需要認證 |
| `src/test/java/com/example/demo/controller/AiControllerTest.java` | 建立 | 驗證 /api/ai/chat 端點可存取 |

---

## Task 1：建立 K8s Secret 並移除 Deployment 明文密碼

> **安全等級：高 — OWASP A02 (Cryptographic Failures)**
>
> 目前 `app-deployment.yaml` 有 `SPRING_DATASOURCE_PASSWORD: springboot123` 明文環境變數。

**Files:**
- Create: `app-secret.yaml`
- Modify: `app-deployment.yaml` (lines 64-71，env 區段)

- [ ] **Step 1：建立 `app-secret.yaml`**

  建立以下檔案。`stringData` 區段使用明文 (kubectl apply 時會自動 base64 encode)。  
  **注意：實際部署前請修改密碼值為真正的機密值。**

  ```yaml
  apiVersion: v1
  kind: Secret
  metadata:
    name: app-secret
    namespace: default
  type: Opaque
  stringData:
    db-password: "springboot123"
    db-username: "springboot"
    jwt-secret: "3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c"
  ```

- [ ] **Step 2：更新 `app-deployment.yaml`，以 `secretKeyRef` 取代明文環境變數**

  找到 env 區段中的以下內容：
  ```yaml
          - name: SPRING_DATASOURCE_PASSWORD
            value: springboot123
          - name: SPRING_DATASOURCE_URL
            value: jdbc:mysql://mysql:3306/spring_boot_demo?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Taipei&allowPublicKeyRetrieval=true
          - name: SPRING_DATASOURCE_USERNAME
            value: springboot
  ```
  替換為：
  ```yaml
          - name: SPRING_DATASOURCE_PASSWORD
            valueFrom:
              secretKeyRef:
                name: app-secret
                key: db-password
          - name: SPRING_DATASOURCE_URL
            value: jdbc:mysql://mysql:3306/spring_boot_demo?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Taipei&allowPublicKeyRetrieval=true
          - name: SPRING_DATASOURCE_USERNAME
            valueFrom:
              secretKeyRef:
                name: app-secret
                key: db-username
  ```

- [ ] **Step 3：在 `app-deployment.yaml` 的 env 區段新增 JWT_SECRET 環境變數**

  在 `SPRING_DATASOURCE_USERNAME` 之後新增：
  ```yaml
          - name: JWT_SECRET
            valueFrom:
              secretKeyRef:
                name: app-secret
                key: jwt-secret
  ```

- [ ] **Step 4：驗證 YAML 語法正確**

  ```powershell
  kubectl apply --dry-run=client -f app-secret.yaml
  kubectl apply --dry-run=client -f app-deployment.yaml
  ```
  Expected: `secret/app-secret configured (dry run)` 及 `deployment.apps/app configured (dry run)`

- [ ] **Step 5：將 `app-secret.yaml` 加入 `.gitignore` 並 Commit**

  > **安全警告：** `app-secret.yaml` 包含明文憑證，不應進入版本控制。  
  > 生產環境請改用 Sealed Secrets 或外部 Secret Store（如 Vault、AWS Secrets Manager）。

  確認 `.gitignore` 包含：
  ```
  app-secret.yaml
  ```

  Apply Secret 後手動執行（不透過 CI 推送）：
  ```powershell
  kubectl apply -f app-secret.yaml
  ```

  僅 commit Deployment 變更：
  ```bash
  git add app-deployment.yaml .gitignore
  git commit -m "security: replace plaintext credentials with K8s Secret references"
  ```

---

## Task 2：修復 `application.properties` 密碼預設回退值

> 目前 `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:springboot123}` 若環境變數未設定，會靜默地使用明文密碼。應移除預設回退，強制要求環境變數存在。
>
> 同時將 `jwt.secret` 改為從環境變數讀取，而非硬編碼。

**Files:**
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1：移除 `application.properties` 中的密碼預設回退值**

  找到：
  ```properties
  spring.datasource.username=${SPRING_DATASOURCE_USERNAME:springboot}
  spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:springboot123}
  ```
  替換為（移除 `:springboot` 與 `:springboot123` 回退）：
  ```properties
  spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
  spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
  ```

- [ ] **Step 2：將硬編碼的 JWT secret 改為環境變數**

  找到：
  ```properties
  jwt.secret=3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c
  ```
  替換為：
  ```properties
  jwt.secret=${JWT_SECRET}
  ```

- [ ] **Step 3：新增本地開發用的 `application-local.properties`（方便本地執行，不進 git）**

  建立 `src/main/resources/application-local.properties`：
  ```properties
  # 本地開發用設定 — 此檔案不應進入版本控制
  spring.datasource.username=springboot
  spring.datasource.password=springboot123
  jwt.secret=3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c
  ```

- [ ] **Step 4：確認 `.gitignore` 排除 `application-local.properties`**

  檢查 `.gitignore` 是否有以下內容，若無則新增：
  ```
  src/main/resources/application-local.properties
  ```

- [ ] **Step 5：執行現有測試，確認無回歸**

  ```powershell
  mvn test -Dspring.profiles.active=local -pl . -q
  ```
  Expected: `BUILD SUCCESS`

- [ ] **Step 6：Commit**

  ```bash
  git add src/main/resources/application.properties .gitignore
  git commit -m "security: remove plaintext password fallbacks and hardcoded JWT secret"
  ```

  > `application-local.properties` 已由 `.gitignore` 排除，不應 commit。

---

## Task 3：修復 SecurityConfig — Cart API 需要認證

> 目前 `/api/cart/**` 設定為 `.permitAll()`，導致任何未認證用戶都可以讀取/修改任意用戶的購物車。

**Files:**
- Create: `src/test/java/com/example/demo/config/SecurityConfigTest.java`
- Modify: `src/main/java/com/example/demo/config/SecurityConfig.java`

- [ ] **Step 1：撰寫失敗測試，驗證 `/api/cart/**` 應需要認證**

  > **前置条件：** 測試使用 `@ActiveProfiles("local")`，需要 `src/main/resources/application-local.properties`。
  > 若 Task 2 尚未執行，請先照 Task 2 Step 3 建立該檔案再執行此任務。

  建立 `src/test/java/com/example/demo/config/SecurityConfigTest.java`：

  ```java
  package com.example.demo.config;

  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.test.context.ActiveProfiles;
  import org.springframework.test.web.servlet.MockMvc;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  @SpringBootTest
  @AutoConfigureMockMvc
  @ActiveProfiles("local")
  public class SecurityConfigTest {

      @Autowired
      private MockMvc mockMvc;

      @Test
      public void cartEndpoint_withoutAuth_shouldReturn401() throws Exception {
          mockMvc.perform(get("/api/cart/1"))
                 .andExpect(status().isUnauthorized());
      }

      @Test
      public void productsEndpoint_withoutAuth_shouldReturn200() throws Exception {
          mockMvc.perform(get("/api/products"))
                 .andExpect(status().isOk());
      }
  }
  ```

- [ ] **Step 2：執行測試，確認 `cartEndpoint_withoutAuth_shouldReturn401` 目前失敗**

  ```powershell
  mvn test -Dtest=SecurityConfigTest#cartEndpoint_withoutAuth_shouldReturn401 -Dspring.profiles.active=local -q
  ```
  Expected: FAIL（因目前 CartController 是 `permitAll()`）

- [ ] **Step 3：修改 `SecurityConfig.java`，`/api/cart/**` 改為 `.authenticated()`**

  找到：
  ```java
                          // 需要認證的 API (暫時允許購物車 API 進行測試)
                          .requestMatchers("/api/cart/**").permitAll()
  ```
  替換為：
  ```java
                          // 購物車 API 必須認證
                          .requestMatchers("/api/cart/**").authenticated()
  ```

- [ ] **Step 4：執行測試，確認兩個測試均通過**

  ```powershell
  mvn test -Dtest=SecurityConfigTest -Dspring.profiles.active=local -q
  ```
  Expected: BUILD SUCCESS，兩個測試皆 PASS

- [ ] **Step 5：Commit**

  ```bash
  git add src/main/java/com/example/demo/config/SecurityConfig.java \
          src/test/java/com/example/demo/config/SecurityConfigTest.java
  git commit -m "security: require authentication for /api/cart/** endpoints"
  ```

---

## Task 4：恢復 AiController

> `AiController.java` 中的 `@RestController` 被注解掉，導致所有 `/api/ai/**` 端點不可用。
> Ollama 服務已在 `ollama-deployment.yaml` 中設定，`ollama.base-url` 在 `application.properties` 中也已設定。

**Files:**
- Create: `src/test/java/com/example/demo/controller/AiControllerTest.java`
- Modify: `src/main/java/com/example/demo/controller/AiController.java`

- [ ] **Step 1：撰寫失敗測試，驗證 `/api/ai/health` 端點存在**

  建立 `src/test/java/com/example/demo/controller/AiControllerTest.java`：

  ```java
  package com.example.demo.controller;

  import org.junit.jupiter.api.Test;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.Mockito.when;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
  import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
  import org.springframework.boot.test.mock.mockito.MockBean;
  import org.springframework.http.MediaType;
  import org.springframework.test.web.servlet.MockMvc;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  import com.example.demo.service.LlmService;
  import com.example.demo.service.ProductService;

  @WebMvcTest(controllers = AiController.class)
  @AutoConfigureMockMvc(addFilters = false)
  public class AiControllerTest {

      @Autowired
      private MockMvc mockMvc;

      @MockBean
      private LlmService llmService;

      @MockBean
      private ProductService productService;

      @Test
      public void healthEndpoint_shouldReturn200() throws Exception {
          mockMvc.perform(get("/api/ai/health"))
                 .andExpect(status().isOk());
      }

      @Test
      public void chatEndpoint_withValidRequest_shouldReturn200() throws Exception {
          when(llmService.chat(any())).thenReturn("這是 AI 的回應");

          String body = "{\"message\": \"你好\"}";
          mockMvc.perform(post("/api/ai/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
                 .andExpect(status().isOk());
      }
  }
  ```

- [ ] **Step 2：執行測試，確認目前兩個測試均失敗（因 `@RestController` 被注解掉，MVC 找不到 handler）**

  ```powershell
  mvn test -Dtest=AiControllerTest -Dspring.profiles.active=local -q
  ```
  Expected: FAIL

- [ ] **Step 3：恢復 `AiController.java` 的 `@RestController` 註解**

  找到 `AiController.java`：
  ```java
  /**
   * 本地 LLM（Ollama）API
   * 基礎路由：/api/ai
   *
   * TODO: AI 功能暫時停用 — 移除 @RestController 使 Spring 不註冊此 Controller，
   *       所有 /api/ai/* 端點均不可存取。恢復時改回 @RestController 即可。
   */
  // @RestController  // TODO: AI 功能暫時停用
  @RequestMapping("/api/ai")
  ```
  替換為：
  ```java
  /**
   * 本地 LLM（Ollama）API
   * 基礎路由：/api/ai
   */
  @RestController
  @RequestMapping("/api/ai")
  ```

- [ ] **Step 4：執行測試，確認通過**

  ```powershell
  mvn test -Dtest=AiControllerTest -Dspring.profiles.active=local -q
  ```
  Expected: BUILD SUCCESS

- [ ] **Step 5：Commit**

  ```bash
  git add src/main/java/com/example/demo/controller/AiController.java \
          src/test/java/com/example/demo/controller/AiControllerTest.java
  git commit -m "feat: re-enable AiController for Ollama LLM endpoints"
  ```

---

## Task 5：修復 JPA DDL-auto（移除 Deployment 中的 `update`）

> `app-deployment.yaml` 中有 `SPRING_JPA_HIBERNATE_DDL_AUTO: update`。在 K8s 環境中，此設定會在每次 Pod 啟動時嘗試修改資料庫 schema，有資料損毀風險。應改為 `validate`（schema 由 DBA 或 migration 工具管理）。

**Files:**
- Modify: `app-deployment.yaml`

- [ ] **Step 1：將 `app-deployment.yaml` 中的 DDL-auto 從 `update` 改為 `validate`**

  找到：
  ```yaml
            - name: SPRING_JPA_HIBERNATE_DDL_AUTO
              value: update
  ```
  替換為：
  ```yaml
            - name: SPRING_JPA_HIBERNATE_DDL_AUTO
              value: validate
  ```

- [ ] **Step 2：同步修改 `application.properties` 中的 DDL 設定**

  找到：
  ```properties
  spring.jpa.hibernate.ddl-auto=update
  ```
  替換為：
  ```properties
  spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}
  ```
  
  > 說明：本地開發的 `application-local.properties` 可加入 `spring.jpa.hibernate.ddl-auto=update` 以保持開發便利性。

- [ ] **Step 3：在 `application-local.properties` 加入 DDL 設定**

  > **依賴說明：** 若已執行 Task 2，此檔案已存在，直接在其中新增即可。  
  > 若尚未執行 Task 2，需先建立 `src/main/resources/application-local.properties`，內容為：
  > ```properties
  > spring.datasource.username=springboot
  > spring.datasource.password=springboot123
  > jwt.secret=3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c
  > ```

  在 `src/main/resources/application-local.properties` 新增：
  ```properties
  spring.jpa.hibernate.ddl-auto=update
  ```

- [ ] **Step 4：執行現有測試確認無回歸**

  ```powershell
  mvn test -Dspring.profiles.active=local -q
  ```
  Expected: BUILD SUCCESS

- [ ] **Step 5：Commit**

  ```bash
  git add app-deployment.yaml src/main/resources/application.properties
  git commit -m "config: change JPA DDL-auto to validate in production, update only in local profile"
  ```
  
  > `application-local.properties` 已被 `.gitignore` 排除，不需也不應加入 `git add`。

---

## Task 6：修復 HPA maxReplicas（1 → 3）

> `app-hpa.yaml` 中 `maxReplicas: 1` 等同於停用自動擴展。正常值應介於 3-5。

**Files:**
- Modify: `app-hpa.yaml`

- [ ] **Step 1：將 `app-hpa.yaml` 的 `maxReplicas` 從 1 修改為 3**

  找到：
  ```yaml
    minReplicas: 1
    maxReplicas: 1
  ```
  替換為：
  ```yaml
    minReplicas: 1
    maxReplicas: 3
  ```

- [ ] **Step 2：驗證 YAML 語法正確**

  ```powershell
  kubectl apply --dry-run=client -f app-hpa.yaml
  ```
  Expected: `horizontalpodautoscaler.autoscaling/app configured (dry run)`

- [ ] **Step 3：Commit**

  ```bash
  git add app-hpa.yaml
  git commit -m "infra: set HPA maxReplicas to 3 to enable horizontal scaling"
  ```

---

## 最終驗證

- [ ] **執行所有測試**

  ```powershell
  mvn test -Dspring.profiles.active=local -q
  ```
  Expected: BUILD SUCCESS

- [ ] **確認 git log 包含所有 6 個 commits**

  ```bash
  git log --oneline -6
  ```
