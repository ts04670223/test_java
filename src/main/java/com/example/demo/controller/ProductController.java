package com.example.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ProductResponseDto;
import com.example.demo.model.Product;
import com.example.demo.service.ProductService;

/**
 * 商品控制器
 * 所有回應統一使用 ApiResponse<T> 包裝，例外由 GlobalExceptionHandler 統一處理
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 取得所有啟用的商品（支援分頁與排序）
     * GET /api/products?page=0&size=20&sortBy=id&sortDir=asc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponseDto> products = productService.getAllActiveProductsPaged(pageable)
                .map(ProductResponseDto::from);
        return ResponseEntity.ok(ApiResponse.success("取得商品列表成功", products));
    }

    /**
     * 根據 ID 取得商品
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Not Found: product id " + id));
        return ResponseEntity.ok(ApiResponse.success(ProductResponseDto.from(product)));
    }

    /**
     * 根據類別查詢商品
     * GET /api/products/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getProductsByCategory(
            @PathVariable String category) {
        List<ProductResponseDto> products = productService.getProductsByCategory(category)
                .stream().map(ProductResponseDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("查詢類別商品成功", products));
    }

    /**
     * 搜尋商品
     * GET /api/products/search?keyword=xxx
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> searchProducts(
            @RequestParam String keyword) {
        List<ProductResponseDto> products = productService.searchProducts(keyword)
                .stream().map(ProductResponseDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("搜尋商品成功", products));
    }

    /**
     * 取得有庫存的商品
     * GET /api/products/available
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAvailableProducts() {
        List<ProductResponseDto> products = productService.getAvailableProducts()
                .stream().map(ProductResponseDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("取得有庫存商品成功", products));
    }

    /**
     * 建立商品（管理員功能）
     * POST /api/products
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
            @RequestBody Product product) {
        Product created = productService.createProduct(product);
        log.info("商品已建立: productId={}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("商品建立成功", ProductResponseDto.from(created)));
    }

    /**
     * 更新商品（管理員功能）
     * PUT /api/products/{id}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {
        Product updated = productService.updateProduct(id, product);
        log.info("商品已更新: productId={}", id);
        return ResponseEntity.ok(ApiResponse.success("商品更新成功", ProductResponseDto.from(updated)));
    }

    /**
     * 停用商品（管理員功能 - 軟刪除）
     * DELETE /api/products/{id}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deactivateProduct(id);
        log.info("商品已停用: productId={}", id);
        return ResponseEntity.ok(ApiResponse.success("商品已停用", null));
    }
}
