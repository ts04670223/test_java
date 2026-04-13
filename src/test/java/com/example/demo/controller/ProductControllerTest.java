package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.JwtService;
import com.example.demo.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProductController WebMvcTest
 *
 * 重點：
 * 1. @WebMvcTest 只載入 Controller layer（不含 Service、Repository 等）
 * 2. /api/products/** 是 public endpoint，搭配 @WithMockUser 避免 Security 攔截
 * 3. 驗證回應格式為 ApiResponse<Page<ProductResponseDto>>
 */
@WebMvcTest(ProductController.class)
@ActiveProfiles("test")
@DisplayName("ProductController WebMvc 測試")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    private Product buildProduct(Long id, String name, String category, BigDecimal price, int stock) {
        Product p = new Product(name, name + " 描述", price, stock);
        p.setId(id);
        p.setCategory(category);
        p.setActive(true);
        p.setStock(stock);
        return p;
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/products — 回應成功並含分頁資訊")
    void getAllProducts_shouldReturnPagedResponse() throws Exception {
        Product p1 = buildProduct(1L, "iPhone 15", "電子產品", new BigDecimal("35000"), 10);
        Product p2 = buildProduct(2L, "耳機", "電子產品", new BigDecimal("2000"), 5);
        Page<Product> productPage = new PageImpl<>(List.of(p1, p2),
                PageRequest.of(0, 20, Sort.by("id")), 2);

        when(productService.getAllActiveProductsPaged(any(Pageable.class))).thenReturn(productPage);

        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", is(2)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].name", is("iPhone 15")));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/products?page=0&size=1 — 分頁參數有效")
    void getAllProducts_withPagination_shouldHonorPageParams() throws Exception {
        Product p1 = buildProduct(1L, "iPhone 15", "電子產品", new BigDecimal("35000"), 10);
        Page<Product> productPage = new PageImpl<>(List.of(p1),
                PageRequest.of(0, 1), 5);

        when(productService.getAllActiveProductsPaged(any(Pageable.class))).thenReturn(productPage);

        mockMvc.perform(get("/api/products?page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements", is(5)))
                .andExpect(jsonPath("$.data.totalPages", is(5)));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/products/{id} — 商品存在時回傳 200 和商品詳情")
    void getProductById_whenExists_shouldReturn200() throws Exception {
        Product product = buildProduct(1L, "iPhone 15", "電子產品", new BigDecimal("35000"), 10);
        when(productService.getProductById(1L)).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("iPhone 15")));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/products/{id} — 商品不存在時回傳 404")
    void getProductById_whenNotFound_shouldReturn404() throws Exception {
        when(productService.getProductById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/products/category/{category} — 依分類篩選")
    void getProductsByCategory_shouldReturnFilteredProducts() throws Exception {
        Product p1 = buildProduct(1L, "iPhone 15", "電子產品", new BigDecimal("35000"), 10);
        when(productService.getProductsByCategory("電子產品")).thenReturn(List.of(p1));

        mockMvc.perform(get("/api/products/category/電子產品"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/products/search?keyword=iPhone — 關鍵字搜尋")
    void searchProducts_shouldReturnMatchingProducts() throws Exception {
        Product p1 = buildProduct(1L, "iPhone 15", "電子產品", new BigDecimal("35000"), 10);
        when(productService.searchProducts("iPhone")).thenReturn(List.of(p1));

        mockMvc.perform(get("/api/products/search?keyword=iPhone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name", containsString("iPhone")));
    }
}
