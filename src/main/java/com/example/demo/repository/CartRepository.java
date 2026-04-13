package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Cart;
import com.example.demo.model.User;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    /**
     * 根據用戶查詢購物車
     */
    Optional<Cart> findByUser(User user);

    /**
     * 根據用戶ID查詢購物車
     */
    Optional<Cart> findByUserId(Long userId);

    /**
     * 查詢空購物車且最後更新時間早於指定時間（用於清理農程）
     */
    @Query("SELECT c FROM Cart c WHERE c.items IS EMPTY AND c.updatedAt < :threshold")
    List<Cart> findEmptyCartsOlderThan(@Param("threshold") LocalDateTime threshold);
}
