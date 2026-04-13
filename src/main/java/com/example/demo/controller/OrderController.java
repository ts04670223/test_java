package com.example.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderResponseDto;
import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.User;
import com.example.demo.service.OrderService;

import jakarta.validation.Valid;

/**
 * 訂單控制器
 * 所有回應統一使用 ApiResponse<T> 包裝，例外由 GlobalExceptionHandler 統一處理
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 從購物車建立訂單
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrderFromCart(
                request.getUserId(),
                request.getShippingAddress(),
                request.getPhone(),
                request.getNote());
        log.info("訂單建立成功: orderId={}, userId={}", order.getId(), request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("訂單建立成功", OrderResponseDto.from(order)));
    }

    /**
     * 取得用戶的所有訂單
     * GET /api/orders/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getUserOrders(
            @PathVariable Long userId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!currentUser.getId().equals(userId) && !isAdmin) {
            log.warn("無權限存取他人資料: currentUserId={}, requestedUserId={}", currentUser.getId(), userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("無法存取他人資料"));
        }
        List<OrderResponseDto> orders = orderService.getUserOrders(userId)
                .stream().map(OrderResponseDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("取得訂單列表成功", orders));
    }

    /**
     * 根據 ID 取得訂單
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderById(
            @PathVariable Integer orderId) {
        Order order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Not Found: order id " + orderId));
        return ResponseEntity.ok(ApiResponse.success(OrderResponseDto.from(order)));
    }

    /**
     * 根據訂單編號查詢訂單
     * GET /api/orders/number/{orderNumber}
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderByNumber(
            @PathVariable String orderNumber) {
        Order order = orderService.getOrderByOrderNumber(orderNumber);
        if (order == null) {
            throw new RuntimeException("找不到訂單編號: " + orderNumber);
        }
        return ResponseEntity.ok(ApiResponse.success(OrderResponseDto.from(order)));
    }

    /**
     * 更新訂單狀態（管理員功能）
     * PUT /api/orders/{orderId}/status?status=SHIPPED
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponseDto>> updateOrderStatus(
            @PathVariable Integer orderId,
            @RequestParam OrderStatus status) {
        Order order = orderService.updateOrderStatus(orderId, status);
        log.info("訂單狀態已更新: orderId={}, newStatus={}", orderId, status);
        return ResponseEntity.ok(ApiResponse.success("訂單狀態已更新", OrderResponseDto.from(order)));
    }

    /**
     * 取消訂單
     * POST /api/orders/{orderId}/cancel?userId=1
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponseDto>> cancelOrder(
            @PathVariable Integer orderId,
            @RequestParam Long userId) {
        Order order = orderService.cancelOrder(orderId, userId);
        log.info("訂單已取消: orderId={}, userId={}", orderId, userId);
        return ResponseEntity.ok(ApiResponse.success("訂單已取消", OrderResponseDto.from(order)));
    }

    /**
     * 取得所有訂單（管理員功能）
     * GET /api/orders
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getAllOrders() {
        List<OrderResponseDto> orders = orderService.getAllOrders()
                .stream().map(OrderResponseDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("取得所有訂單成功", orders));
    }

    /**
     * 根據狀態查詢訂單（管理員功能）
     * GET /api/orders/status/{status}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getOrdersByStatus(
            @PathVariable OrderStatus status) {
        List<OrderResponseDto> orders = orderService.getOrdersByStatus(status)
                .stream().map(OrderResponseDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("查詢訂單成功", orders));
    }
}
