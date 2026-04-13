package com.example.demo.controller;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.model.*;
import com.example.demo.service.JwtService;
import com.example.demo.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OrderController WebMvcTest
 *
 * 重點：
 * 1. POST /api/orders 需要 csrf token（Spring Security 預設啟用 CSRF）
 * 2. GET /api/orders/user/{userId} 需要 Authentication，使用 @WithMockUser
 * 3. GET /api/orders/{id} 由 Admin 存取需要 @WithMockUser(roles="ADMIN")
 */
@WebMvcTest(OrderController.class)
@ActiveProfiles("test")
@DisplayName("OrderController WebMvc 測試")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtService jwtService;

    private Order buildOrder(int id, User user, BigDecimal total) {
        Order order = new Order(user, total);
        order.setId(id);
        order.setOrderNumber("ORD-" + id);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress("台北市中正區");
        return order;
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/orders — 建立訂單成功，回傳 201 和訂單詳情")
    void createOrder_shouldReturn201WithOrderDetails() throws Exception {
        User user = buildUser(1L, "testuser");
        Order order = buildOrder(100, user, new BigDecimal("200.00"));

        when(orderService.createOrderFromCart(
                eq(1L), anyString(), anyString(), any())).thenReturn(order);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setShippingAddress("台北市中正區");
        request.setPhone("0912345678");
        request.setNote("請小心輕放");

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderNumber", is("ORD-100")))
                .andExpect(jsonPath("$.data.status", is("PENDING")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("GET /api/orders/{id} — Admin 可以存取任意訂單")
    void getOrderById_asAdmin_shouldReturn200() throws Exception {
        User user = buildUser(1L, "testuser");
        Order order = buildOrder(1, user, new BigDecimal("500.00"));

        when(orderService.getOrderById(1)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/orders/{id} — 訂單不存在時回傳 404")
    void getOrderById_whenNotFound_shouldReturn404() throws Exception {
        when(orderService.getOrderById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("GET /api/orders/status/{status} — Admin 依狀態查詢訂單")
    void getOrdersByStatus_asAdmin_shouldReturnFilteredOrders() throws Exception {
        User user = buildUser(1L, "testuser");
        Order order1 = buildOrder(1, user, new BigDecimal("100.00"));
        Order order2 = buildOrder(2, user, new BigDecimal("200.00"));

        when(orderService.getOrdersByStatus(OrderStatus.PENDING))
                .thenReturn(List.of(order1, order2));

        mockMvc.perform(get("/api/orders/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }
}
