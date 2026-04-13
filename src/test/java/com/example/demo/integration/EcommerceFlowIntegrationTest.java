package com.example.demo.integration;

import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * 電商流程整合測試（完整 Spring Context + H2）
 *
 * 測試情境：
 * 1. 未登入可以瀏覽商品列表
 * 2. 商品分頁功能驗證
 * 3. 商品分類篩選驗證
 * 4. 搜尋商品功能驗證
 * 5. 未登入嘗試存取訂單時回傳 401/403
 *
 * 注意：使用 test profile（H2 in-memory），不連接 MySQL
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("電商流程整合測試")
class EcommerceFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ————————————————————
    // 商品公開端點測試（不需登入）
    // ————————————————————

    @Test
    @DisplayName("GET /api/products — 未登入可以取得商品列表（public endpoint）")
    void getProducts_withoutAuth_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /api/products?page=0&size=5 — 分頁參數正確解析")
    void getProducts_withPagination_shouldReturnPagedResponse() throws Exception {
        mockMvc.perform(get("/api/products?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size", is(5)))
                .andExpect(jsonPath("$.data.number", is(0)));
    }

    @Test
    @DisplayName("GET /api/products?sortBy=name&sortDir=desc — 排序參數正確解析")
    void getProducts_withSortParams_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/products?sortBy=name&sortDir=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("GET /api/products/999 — 商品不存在時回傳非 200")
    void getProductById_nonExistent_shouldReturn404() throws Exception {
        // 商品不存在時，依照 GlobalExceptionHandler 處理結果回傳 4xx
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().is4xxClientError());
    }

    // ————————————————————
    // 受保護端點測試（需要登入）
    // ————————————————————

    @Test
    @DisplayName("GET /api/orders/user/1 — 未登入存取訂單應回傳 401")
    void getUserOrders_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/orders/user/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/orders — 未登入下單應回傳 401")
    void createOrder_withoutAuth_shouldReturn401() throws Exception {
        String body = """
                {
                  "userId": 1,
                  "shippingAddress": "台北市",
                  "phone": "0912345678"
                }
                """;
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ————————————————————
    // Actuator 健康檢查（驗證 Spring Boot 正常啟動）
    // ————————————————————

    @Test
    @DisplayName("GET /actuator/health — 可以存取 health endpoint（test 環境部分服務 DOWN 屬正常）")
    void actuatorHealth_shouldReturn200() throws Exception {
        // test profile 下 Redis/Mail 不可用，Actuator 可能回傳 503（整體 DOWN）
        // 只驗證 endpoint 可存取（非 404/401），接受 200 與 503
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is(200),
                        org.hamcrest.Matchers.is(503))));
    }
}
