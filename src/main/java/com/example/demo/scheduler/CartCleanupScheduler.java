package com.example.demo.scheduler;

import com.example.demo.model.Cart;
import com.example.demo.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 購物車清理排程任務
 *
 * 學習重點：
 * 1. @Scheduled(cron = "...")：使用 Cron 表達式定義執行時間
 *    格式：秒 分 時 日 月 星期
 * 2. @ConditionalOnProperty：根據設定決定是否建立此 Bean
 *    test profile 設定 scheduling.enabled=false，排程不會啟動
 * 3. @Transactional：確保批次刪除在同一事務中執行
 * 4. @EnableScheduling 必須在 DemoApplication.java 上（已有）
 */
@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class CartCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CartCleanupScheduler.class);

    /** 30 天以上不活動的空購物車會被清理 */
    private static final int INACTIVE_DAYS = 30;

    private final CartRepository cartRepository;

    public CartCleanupScheduler(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    /**
     * 每日凌晨 2:00 執行：清理 30 天以上未更新的空購物車
     *
     * Cron 表達式：0 0 2 * * ?
     * - 0: 第 0 秒
     * - 0: 第 0 分
     * - 2: 凌晨 2 點
     * - *: 每月每日
     * - *: 每月
     * - ?: 不指定星期幾
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredCarts() {
        log.info("[Scheduler] CartCleanup 開始執行...");
        LocalDateTime threshold = LocalDateTime.now().minusDays(INACTIVE_DAYS);
        try {
            List<Cart> expiredCarts = cartRepository.findEmptyCartsOlderThan(threshold);
            int count = expiredCarts.size();
            if (count > 0) {
                cartRepository.deleteAll(expiredCarts);
                log.info("[Scheduler] CartCleanup 完成，刪除 {} 個過期空購物車", count);
            } else {
                log.info("[Scheduler] CartCleanup 完成，無需清理");
            }
        } catch (Exception ex) {
            log.error("[Scheduler] CartCleanup 失敗: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 每週一 04:00 執行：印出購物車統計（監控用途）
     *
     * Cron 表達式：0 0 4 ? * MON
     */
    @Scheduled(cron = "0 0 4 ? * MON")
    public void reportCartStats() {
        long totalCarts = cartRepository.count();
        log.info("[Scheduler] CartStats 週報 - 購物車總數: {}", totalCarts);
    }
}
