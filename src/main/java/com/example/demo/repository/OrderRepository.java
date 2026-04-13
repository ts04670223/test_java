package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.User;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    /**
     * 根據用戶查詢所有訂單
     */
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 根據用戶ID查詢訂單
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 根據訂單編號查詢
     */
    Order findByOrderNumber(String orderNumber);

    /**
     * 根據狀態查詢訂單
     */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /**
     * 根據用戶和狀態查詢訂單
     */
    List<Order> findByUserAndStatusOrderByCreatedAtDesc(User user, OrderStatus status);

    /**
     * 查詢 PENDING 狀態且建立時間早於指定時間的訂單（用於自動取消進時未付款訂單）
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :threshold")
    List<Order> findPendingOrdersOlderThan(@Param("threshold") LocalDateTime threshold);
}
