package com.example.demo.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 取得所有啟用的商品
     */
    @Cacheable(value = "active_products")
    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    /**
     * 取得所有啟用的商品（分頁版本）
     */
    public Page<Product> getAllActiveProductsPaged(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable);
    }

    /**
     * 取得所有商品（含未啟用）
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * 根據ID取得商品
     */
    @Cacheable(value = "product", key = "#id")
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(Objects.requireNonNull(id));
    }

    /**
     * 根據類別查詢商品
     */
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndActiveTrue(category);
    }

    /**
     * 搜尋商品
     */
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(keyword);
    }

    /**
     * 查詢有庫存的商品
     */
    public List<Product> getAvailableProducts() {
        return productRepository.findAvailableProducts();
    }

    /**
     * 創建商品
     */
    @CacheEvict(value = "active_products", allEntries = true)
    public Product createProduct(Product product) {
        return productRepository.save(Objects.requireNonNull(product));
    }

    /**
     * 更新商品
     */
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "active_products", allEntries = true)
    })
    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("找不到商品，ID: " + id));

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStock(productDetails.getStock());
        product.setCategory(productDetails.getCategory());
        product.setActive(productDetails.getActive());

        // 更新其他新增的欄位
        if (productDetails.getShortDescription() != null) {
            product.setShortDescription(productDetails.getShortDescription());
        }
        if (productDetails.getBrand() != null) {
            product.setBrand(productDetails.getBrand());
        }
        if (productDetails.getOriginalPrice() != null) {
            product.setOriginalPrice(productDetails.getOriginalPrice());
        }

        return productRepository.save(product);
    }

    /**
     * 刪除商品（軟刪除 - 設為未啟用）
     */
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "active_products", allEntries = true)
    })
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("找不到商品，ID: " + id));
        product.setActive(false);
        productRepository.save(product);
    }

    /**
     * 刪除商品（硬刪除）
     */
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "active_products", allEntries = true)
    })
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("找不到商品，ID: " + id));
        productRepository.delete(Objects.requireNonNull(product));
    }

    /**
     * 更新庫存
     */
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#productId"),
            @CacheEvict(value = "active_products", allEntries = true)
    })
    public void updateStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(Objects.requireNonNull(productId))
                .orElseThrow(() -> new RuntimeException("找不到商品，ID: " + productId));

        int newStock = product.getStock() + quantity;
        if (newStock < 0) {
            throw new RuntimeException("庫存不足");
        }

        product.setStock(newStock);
        productRepository.save(product);
    }

    /**
     * 減少庫存
     */
    public void decreaseStock(Long productId, Integer quantity) {
        updateStock(productId, -quantity);
    }

    /**
     * 增加庫存
     */
    public void increaseStock(Long productId, Integer quantity) {
        updateStock(productId, quantity);
    }
}
