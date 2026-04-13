package com.example.demo.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.example.demo.event.OrderCreatedEvent;
import com.example.demo.model.Cart;
import com.example.demo.model.CartItem;
import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.UserRepository;

/**
 * OrderService 單元測試
 *
 * 重點測試：
 * - 正常下單流程（事件發布驗證）
 * - 邊界條件：空購物車、庫存不足、用戶不存在
 * - verify() 確認 Spring Event 有被正確發布
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 單元測試")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartService cartService;
    @Mock private ProductService productService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("測試商品");
        testProduct.setPrice(new BigDecimal("100.00"));
        testProduct.setStock(5);

        cartItem = new CartItem();
        cartItem.setProduct(testProduct);
        cartItem.setQuantity(2);
        cartItem.setPrice(new BigDecimal("100.00"));

        testCart = new Cart();
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>(List.of(cartItem)));
    }

    @Test
    @DisplayName("建立訂單 - 正常流程應成功，並發布 OrderCreatedEvent")
    void createOrderFromCart_withValidCart_shouldSucceedAndPublishEvent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCartByUserId(1L)).thenReturn(testCart);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100);
            return o;
        });

        Order result = orderService.createOrderFromCart(
                1L, "台北市中正區", "0912345678", "請小心輕放");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getShippingAddress()).isEqualTo("台北市中正區");

        // 驗證 OrderCreatedEvent 確實發布了一次
        verify(eventPublisher, times(1)).publishEvent(any(OrderCreatedEvent.class));
        // 驗證購物車有被清空
        verify(cartService, times(1)).clearCart(1L);
        // 驗證庫存有被減少
        verify(productService, times(1)).decreaseStock(1L, 2);
    }

    @Test
    @DisplayName("建立訂單 - 空購物車應拋出例外，且不發布事件")
    void createOrderFromCart_withEmptyCart_shouldThrowAndNotPublishEvent() {
        testCart.setItems(new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCartByUserId(1L)).thenReturn(testCart);

        assertThatThrownBy(() ->
                orderService.createOrderFromCart(1L, "台北市", "0912345678", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("購物車是空的");

        // 空購物車不應發布事件
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("建立訂單 - 庫存不足應拋出例外，且不發布事件")
    void createOrderFromCart_withInsufficientStock_shouldThrowAndNotPublishEvent() {
        testProduct.setStock(1); // 庫存只有 1，但購物車要買 2
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCartByUserId(1L)).thenReturn(testCart);

        assertThatThrownBy(() ->
                orderService.createOrderFromCart(1L, "台北市", "0912345678", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("庫存不足");

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("建立訂單 - 用戶不存在應拋出例外")
    void createOrderFromCart_withNonExistentUser_shouldThrow() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.createOrderFromCart(999L, "台北市", "0912345678", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("找不到用戶");
    }

    @Test
    @DisplayName("取得用戶訂單 - 應回傳正確的訂單列表")
    void getUserOrders_shouldReturnUserOrders() {
        Order order = new Order(testUser, new BigDecimal("200.00"));
        order.setId(1);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        List<Order> result = orderService.getUserOrders(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("取消訂單 - 非本人取消應拋出例外")
    void cancelOrder_withWrongUser_shouldThrow() {
        Order order = new Order(testUser, new BigDecimal("200.00"));
        order.setId(1);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        // 用戶 ID 2 嘗試取消用戶 1 的訂單
        assertThatThrownBy(() -> orderService.cancelOrder(1, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("無權限");
    }
}
