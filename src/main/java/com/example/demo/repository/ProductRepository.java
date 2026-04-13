package com.example.demo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 查詢所有啟用的商品
     */
    List<Product> findByActiveTrue();

    /**
     * 查詢所有啟用的商品（分頁）
     */
    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * 根據類別查詢商品
     */
    List<Product> findByCategoryAndActiveTrue(String category);

    /**
     * 根據名稱模糊搜尋商品
     */
    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    /**
     * 查詢有庫存的商品
     */
    @Query("SELECT p FROM Product p WHERE p.stock > 0 AND p.active = true")
    List<Product> findAvailableProducts();

    /**
     * 根據類別和名稱搜尋
     */
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.name LIKE %:keyword% AND p.active = true")
    List<Product> findByCategoryAndKeyword(@Param("category") String category, @Param("keyword") String keyword);
}
