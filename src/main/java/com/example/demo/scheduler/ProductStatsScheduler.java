package com.example.demo.scheduler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductReviewRepository;

/**
 * 商品統計重算排程任務
 *
 * 業務場景：
 * 商品的「平均評分」和「評論數」在高頻更新場景（大量用戶評論）時，
 * 即時計算開銷大，採用「批次重算」策略：
 * - 每次評論提交時直接更新（即時）
 * - 每日凌晨排程重算，修正任何不一致（兜底機制）
 *
 * 執行時間：每日凌晨 03:00
 */
@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ProductStatsScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProductStatsScheduler.class);

    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;

    public ProductStatsScheduler(ProductRepository productRepository,
                                  ProductReviewRepository productReviewRepository) {
        this.productRepository = productRepository;
        this.productReviewRepository = productReviewRepository;
    }

    /**
     * 每日凌晨 3:00 重算所有商品的評分統計
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void recalculateProductStats() {
        log.info("[Scheduler] ProductStats 開始重算商品統計...");
        List<Product> products = productRepository.findAll();
        int updated = 0;
        int failed = 0;

        for (Product product : products) {
            try {
                Double avgRatingRaw = productReviewRepository.getAverageRatingByProductId(product.getId());
                Long reviewCountRaw = productReviewRepository.getReviewCountByProductId(product.getId());

                BigDecimal avgRating = avgRatingRaw != null
                        ? BigDecimal.valueOf(avgRatingRaw).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                int reviewCount = reviewCountRaw != null ? reviewCountRaw.intValue() : 0;

                // 只有數值變動時才儲存，減少不必要的 DB 寫入
                boolean changed = !avgRating.equals(product.getAverageRating())
                        || reviewCount != product.getReviewCount();

                if (changed) {
                    product.setAverageRating(avgRating);
                    product.setReviewCount(reviewCount);
                    productRepository.save(product);
                    updated++;
                }
            } catch (Exception ex) {
                failed++;
                log.warn("[Scheduler] 商品 {} 統計更新失敗: {}", product.getId(), ex.getMessage());
            }
        }

        log.info("[Scheduler] ProductStats 重算完成，更新: {} 筆，失敗: {} 筆，總計: {} 筆",
                updated, failed, products.size());
    }
}
