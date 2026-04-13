package com.example.demo.event;

import com.example.demo.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 訂單事件監聽器
 *
 * 學習重點：
 * 1. @Async：讓事件處理在獨立執行緒中執行，不阻塞訂單主流程
 * 2. @TransactionalEventListener：確保事務成功提交後才處理事件
 *    - TransactionPhase.AFTER_COMMIT：事務 commit 後才執行（預設也是）
 *    - 若訂單建立失敗（rollback），通知就不會發送
 * 3. 例外不往上拋：通知失敗不應影響已成功的訂單
 *
 * 架構優點：
 * - OrderService 只需發布事件，不需知道「誰來處理通知」
 * - 新增通知管道（SMS、Push）只需新增 @EventListener 方法，不動 OrderService
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 訂單建立後，非同步發送確認通知
     * AFTER_COMMIT：確保訂單已成功寫入資料庫後才發送，避免發通知但訂單其實失敗
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[Event] 收到訂單建立事件: orderId={}, orderNumber={}, userId={}",
                event.getOrderId(), event.getOrderNumber(), event.getUserId());
        try {
            notificationService.sendOrderConfirmation(
                    event.getUserId(), event.getOrderNumber(), event.getTotalAmount());
            log.info("[Event] 訂單確認通知處理完成: orderNumber={}", event.getOrderNumber());
        } catch (Exception ex) {
            // 不重新拋出例外：通知失敗不應影響已成功建立的訂單
            log.error("[Event] 訂單確認通知處理失敗: orderNumber={}, error={}",
                    event.getOrderNumber(), ex.getMessage());
        }
    }

    /**
     * 訂單狀態變更後，非同步發送狀態更新通知
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("[Event] 收到訂單狀態變更事件: orderId={}, {} -> {}",
                event.getOrderId(), event.getOldStatus(), event.getNewStatus());
        try {
            notificationService.sendOrderStatusUpdate(
                    event.getUserId(), event.getOrderNumber(),
                    event.getOldStatus().name(), event.getNewStatus().name());
            log.info("[Event] 訂單狀態通知處理完成: orderNumber={}", event.getOrderNumber());
        } catch (Exception ex) {
            log.error("[Event] 訂單狀態通知處理失敗: orderNumber={}, error={}",
                    event.getOrderNumber(), ex.getMessage());
        }
    }
}
