package com.example.demo.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.event.OrderCreatedEvent;
import com.example.demo.event.OrderStatusChangedEvent;
import com.example.demo.model.Cart;
import com.example.demo.model.CartItem;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.UserRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductService productService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
            UserRepository userRepository,
            CartService cartService,
            ProductService productService,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.productService = productService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 從購物車建立訂單
     */
    @Transactional
    public Order createOrderFromCart(Long userId, String shippingAddress, String phone, String note) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new RuntimeException("找不到用戶，ID: " + userId));

        Cart cart = cartService.getCartByUserId(userId);

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("購物車是空的");
        }

        // 計算總金額並檢查庫存
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // 檢查庫存
            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("商品 " + product.getName() + " 庫存不足");
            }

            totalAmount = totalAmount.add(cartItem.getSubtotal());
        }

        // 建立訂單
        Order order = new Order(user, totalAmount);
        order.setShippingAddress(shippingAddress);
        order.setPhone(phone);
        order.setNote(note);
        order.setStatus(OrderStatus.PENDING);

        // 複製購物車項目到訂單項目
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem(
                    cartItem.getProduct(),
                    cartItem.getQuantity(),
                    cartItem.getPrice());
            order.addItem(orderItem);

            // 減少庫存
            productService.decreaseStock(cartItem.getProduct().getId(), cartItem.getQuantity());
        }

        // 儲存訂單
        Order savedOrder = orderRepository.save(order);

        // 清空購物車
        cartService.clearCart(userId);

        // 發布訂單建立事件（取代直接呼叫 NotificationService）
        // OrderEventListener 將從訂事 commit 後非同步統公
        eventPublisher.publishEvent(new OrderCreatedEvent(
                this,
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                user.getId(),
                user.getUsername(),
                savedOrder.getTotalAmount()));

        return savedOrder;
    }

    /**
     * 取得用戶的所有訂單
     */
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 根據ID取得訂單
     */
    public Optional<Order> getOrderById(Integer orderId) {
        return orderRepository.findById(Objects.requireNonNull(orderId));
    }

    /**
     * 根據訂單編號取得訂單
     */
    public Order getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    /**
     * 更新訂單狀態
     */
    @Transactional
    public Order updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(Objects.requireNonNull(orderId))
                .orElseThrow(() -> new RuntimeException("找不到訂單，ID: " + orderId));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        // 發布訂單狀態變更事件
        if (!oldStatus.equals(newStatus)) {
            eventPublisher.publishEvent(new OrderStatusChangedEvent(
                    this,
                    savedOrder.getId(),
                    savedOrder.getOrderNumber(),
                    savedOrder.getUser().getId(),
                    oldStatus,
                    newStatus));
        }

        return savedOrder;
    }

    /**
     * 取消訂單
     */
    @Transactional
    public Order cancelOrder(Integer orderId, Long userId) {
        Order order = orderRepository.findById(Objects.requireNonNull(orderId))
                .orElseThrow(() -> new RuntimeException("找不到訂單，ID: " + orderId));

        // 檢查訂單是否屬於該用戶
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("無權限取消此訂單");
        }

        // 只有 PENDING 和 PROCESSING 狀態可以取消
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
            throw new RuntimeException("訂單狀態不允許取消");
        }

        // 恢復庫存
        for (OrderItem item : order.getItems()) {
            productService.increaseStock(item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    /**
     * 取得所有訂單（管理員用）
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * 根據狀態查詢訂單
     */
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }
}
