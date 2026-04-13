package com.example.demo.event;

import org.springframework.context.ApplicationEvent;

import com.example.demo.model.OrderStatus;

/**
 * 訂單狀態變更事件
 *
 * 由 OrderService.updateOrderStatus() 在狀態更新成功後發布
 * 訂閱者：OrderEventListener.handleOrderStatusChanged()
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
