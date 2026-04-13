package com.example.demo.repository;

import com.example.demo.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductRepository 整合測試（使用 H2 in-memory）
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProductRepository 查詢測試")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        Product p1 = new Product("iPhone 15", "Apple 手機", new BigDecimal("35000"), 10);
        p1.setCategory("電子產品");
        p1.setActive(true);
        p1.setStock(10);

        Product p2 = new Product("Samsung S24", "Samsung 手機", new BigDecimal("28000"), 5);
        p2.setCategory("電子產品");
        p2.setActive(true);
        p2.setStock(5);

        Product p3 = new Product("Nike 球鞋", "運動鞋", new BigDecimal("3500"), 20);
        p3.setCategory("服飾");
        p3.setActive(true);
        p3.setStock(20);

        Product p4 = new Product("停售商品", "已停售", new BigDecimal("100"), 0);
        p4.setCategory("電子產品");
        p4.setActive(false);  // 停售
        p4.setStock(0);

        productRepository.saveAll(List.of(p1, p2, p3, p4));
    }

    @Test
    @DisplayName("findByActiveTrue(List) — 只回傳上架商品")
    void findByActiveTrue_shouldReturnOnlyActiveProducts() {
        List<Product> activeProducts = productRepository.findByActiveTrue();

        assertThat(activeProducts).hasSize(3)
                .extracting(Product::getName)
                .doesNotContain("停售商品");
    }

    @Test
    @DisplayName("findByActiveTrue(Pageable) — 分頁回傳上架商品")
    void findByActiveTrue_withPageable_shouldReturnPagedResult() {
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by("name").ascending());

        Page<Product> page = productRepository.findByActiveTrue(pageRequest);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("findByActiveTrue(Pageable) — 第二頁應有剩餘商品")
    void findByActiveTrue_withPageable_secondPageHasRemainingItems() {
        PageRequest pageRequest = PageRequest.of(1, 2, Sort.by("name").ascending());

        Page<Product> page = productRepository.findByActiveTrue(pageRequest);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.isLast()).isTrue();
    }

    @Test
    @DisplayName("findByCategoryAndActiveTrue — 只回傳同分類且上架的商品")
    void findByCategoryAndActiveTrue_shouldReturnMatchingActiveProducts() {
        List<Product> electronics = productRepository.findByCategoryAndActiveTrue("電子產品");

        assertThat(electronics).hasSize(2)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("iPhone 15", "Samsung S24");
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCaseAndActiveTrue — 關鍵字搜尋應不分大小寫")
    void findByNameContainingIgnoreCaseAndActiveTrue_shouldSearchCaseInsensitively() {
        List<Product> results = productRepository.findByNameContainingIgnoreCaseAndActiveTrue("iphone");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("iPhone 15");
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCaseAndActiveTrue — 停售商品不應出現在搜尋結果")
    void findByNameContainingIgnoreCaseAndActiveTrue_shouldExcludeInactiveProducts() {
        List<Product> results = productRepository.findByNameContainingIgnoreCaseAndActiveTrue("停售");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findAvailableProducts — 只回傳有庫存且上架的商品")
    void findAvailableProducts_shouldReturnOnlyAvailableProducts() {
        // 設一個上架但庫存為0的商品
        Product outOfStock = new Product("缺貨商品", "暫時缺貨", new BigDecimal("500"), 0);
        outOfStock.setCategory("服飾");
        outOfStock.setActive(true);
        outOfStock.setStock(0);
        productRepository.save(outOfStock);

        List<Product> available = productRepository.findAvailableProducts();

        assertThat(available)
                .extracting(Product::getName)
                .doesNotContain("停售商品", "缺貨商品");
    }
}
