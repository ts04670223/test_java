package com.example.demo.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 訂單建立事件
 *
 * 學習重點：
 * - 繼承 ApplicationEvent 表示這是一個 Spring 應用程式事件
 * - 由 OrderService.createOrderFromCart() 在訂單儲存成功後透過
 *   ApplicationEventPublisher.publishEvent() 發布
 * - 訂閱者：OrderEventListener.handleOrderCreated()
 *
 * 優點：OrderService 不需直接依賴 NotificationService，降低耦合
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
