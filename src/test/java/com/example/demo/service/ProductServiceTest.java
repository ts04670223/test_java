package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProductService 單元測試
 *
 * 學習重點：
 * 1. @ExtendWith(MockitoExtension.class)：使用 Mockito 擴充，不啟動 Spring Context
 * 2. @Mock：建立 Mock 物件（假的 Repository，不訪問真實 DB）
 * 3. @InjectMocks：自動注入 Mock 到測試目標
 * 4. when(...).thenReturn(...)：定義 Mock 的行為
 * 5. verify(...)：驗證某個方法是否被呼叫（及呼叫幾次）
 * 6. AssertJ 的 assertThat：流暢的斷言語法
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 單元測試")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product activeProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        activeProduct = new Product();
        activeProduct.setId(1L);
        activeProduct.setName("iPhone 15 Pro");
        activeProduct.setPrice(new BigDecimal("999.00"));
        activeProduct.setStock(10);
        activeProduct.setActive(true);
        activeProduct.setCategory("Electronics");

        inactiveProduct = new Product();
        inactiveProduct.setId(2L);
        inactiveProduct.setName("老舊商品");
        inactiveProduct.setPrice(new BigDecimal("50.00"));
        inactiveProduct.setStock(0);
        inactiveProduct.setActive(false);
        inactiveProduct.setCategory("Electronics");
    }

    @Test
    @DisplayName("取得所有啟用商品 - 應只回傳 active=true 的商品")
    void getAllActiveProducts_shouldReturnOnlyActiveProducts() {
        when(productRepository.findByActiveTrue()).thenReturn(List.of(activeProduct));

        List<Product> result = productService.getAllActiveProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("iPhone 15 Pro");
        assertThat(result.get(0).getActive()).isTrue();
        verify(productRepository, times(1)).findByActiveTrue();
    }

    @Test
    @DisplayName("分頁取得啟用商品 - 應回傳正確的分頁資料")
    void getAllActiveProductsPaged_shouldReturnPagedResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(activeProduct), pageable, 1);
        when(productRepository.findByActiveTrue(pageable)).thenReturn(page);

        Page<Product> result = productService.getAllActiveProductsPaged(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("iPhone 15 Pro");
    }

    @Test
    @DisplayName("根據 ID 取得商品 - 商品存在時應回傳")
    void getProductById_whenExists_shouldReturnProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));

        Optional<Product> result = productService.getProductById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("根據 ID 取得商品 - 商品不存在時應回傳 empty")
    void getProductById_whenNotExists_shouldReturnEmpty() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Product> result = productService.getProductById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("根據類別查詢商品 - 應回傳符合類別的商品")
    void getProductsByCategory_shouldReturnMatchingProducts() {
        when(productRepository.findByCategoryAndActiveTrue("Electronics"))
                .thenReturn(List.of(activeProduct));

        List<Product> result = productService.getProductsByCategory("Electronics");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("搜尋商品 - 關鍵字搜尋應回傳符合名稱的商品")
    void searchProducts_shouldReturnMatchingByKeyword() {
        when(productRepository.findByNameContainingIgnoreCaseAndActiveTrue("iPhone"))
                .thenReturn(List.of(activeProduct));

        List<Product> result = productService.searchProducts("iPhone");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains("iPhone");
    }

    @Test
    @DisplayName("搜尋商品 - 無符合結果時應回傳空清單")
    void searchProducts_whenNoMatch_shouldReturnEmptyList() {
        when(productRepository.findByNameContainingIgnoreCaseAndActiveTrue("不存在商品"))
                .thenReturn(List.of());

        List<Product> result = productService.searchProducts("不存在商品");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("建立商品 - 應正確儲存並回傳商品")
    void createProduct_shouldSaveAndReturnProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(activeProduct);

        Product result = productService.createProduct(activeProduct);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("停用商品 - 應將 active 設為 false")
    void deactivateProduct_shouldSetActiveFalse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any(Product.class))).thenReturn(activeProduct);

        productService.deactivateProduct(1L);

        assertThat(activeProduct.getActive()).isFalse();
        verify(productRepository, times(1)).save(activeProduct);
    }
}
