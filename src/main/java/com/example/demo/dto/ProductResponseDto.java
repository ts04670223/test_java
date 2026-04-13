package com.example.demo.dto;

import com.example.demo.model.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表回應 DTO（輕量版，不含 Lazy 載入集合）
 * 用於列表頁面顯示，避免序列化 Hibernate Lazy proxy 問題
 */
public class ProductResponseDto {

    private Long id;
    private String name;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String brand;
    private String category;
    private String subCategory;
    private Boolean active;
    private Boolean featured;
    private Integer soldCount;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;

    /**
     * 從 Product entity 建立 DTO（只取安全欄位，不觸發 Lazy loading）
     */
    public static ProductResponseDto from(Product product) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setShortDescription(product.getShortDescription());
        dto.setPrice(product.getPrice());
        dto.setOriginalPrice(product.getOriginalPrice());
        dto.setStock(product.getStock());
        dto.setBrand(product.getBrand());
        dto.setCategory(product.getCategory());
        dto.setSubCategory(product.getSubCategory());
        dto.setActive(product.getActive());
        dto.setFeatured(product.getFeatured());
        dto.setSoldCount(product.getSoldCount());
        dto.setAverageRating(product.getAverageRating());
        dto.setReviewCount(product.getReviewCount());
        dto.setCreatedAt(product.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }

    public Integer getSoldCount() { return soldCount; }
    public void setSoldCount(Integer soldCount) { this.soldCount = soldCount; }

    public BigDecimal getAverageRating() { return averageRating; }
    public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
