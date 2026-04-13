package com.example.demo.service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.demo.model.Order;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    /**
     * 非同步發送訂單確認郵件（舊版，保留向下相容）
     */
    @Async
    public CompletableFuture<Void> sendOrderConfirmation(Order order) {
        try {
            logger.info("開始發送訂單確認郵件給用戶: {}, 訂單ID: {}", order.getUser().getEmail(), order.getId());
            // TODO: 接入實際郵件服務
            logger.info("訂單確認通知已處理，訂單ID: {}", order.getId());
        } catch (Exception e) {
            logger.error("訂單通知處理失敗，訂單ID: {}, 錯誤: {}", order.getId(), e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 發送訂單確認通知（被 OrderEventListener 呼叫）
     */
    public void sendOrderConfirmation(Long userId, String orderNumber, BigDecimal totalAmount) {
        logger.info("[Notification] 訂單確認: userId={}, orderNumber={}, totalAmount={}",
                userId, orderNumber, totalAmount);
        // TODO: 實際郵件發送（需要設定 spring.mail 相關設定）
    }

    /**
     * 發送訂單狀態更新通知
     */
    public void sendOrderStatusUpdate(Long userId, String orderNumber,
                                      String oldStatus, String newStatus) {
        logger.info("[Notification] 訂單狀態更新: userId={}, orderNumber={}, {} -> {}",
                userId, orderNumber, oldStatus, newStatus);
        // TODO: 實際郵件發送
    }
}

