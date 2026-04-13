package com.example.demo.dto;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 訂單回應 DTO
 * 安全地序列化訂單資料，避免 Lazy loading 問題
 */
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

    public static OrderResponseDto from(Order order) {
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
        } else {
            dto.setItems(Collections.emptyList());
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
