package com.example.demo.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Cart;
import com.example.demo.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 取得用戶的購物車
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId) {
        try {
            Cart cart = cartService.getCartByUserId(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("id", cart.getId());
            response.put("userId", cart.getUser().getId());
            response.put("itemCount", cart.getItems().size());

            // 計算總金額 - 使用 BigDecimal 進行精確計算
            BigDecimal total = cart.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            response.put("total", total);

            List<Map<String, Object>> items = cart.getItems().stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("id", item.getId());
                        itemMap.put("productId", item.getProduct().getId());
                        itemMap.put("productName", item.getProduct().getName());
                        itemMap.put("price", item.getPrice()); // CartItem 儲存的價格
                        itemMap.put("productPrice", item.getProduct().getPrice()); // 產品當前價格
                        itemMap.put("quantity", item.getQuantity());
                        itemMap.put("subtotal", item.getSubtotal());
                        return itemMap;
                    })
                    .collect(Collectors.toList());
            response.put("items", items);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "取得購物車失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 添加商品到購物車
     */
    @PostMapping("/{userId}/items")
    public ResponseEntity<?> addItem(
            @PathVariable Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            Cart cart = cartService.addItemToCart(userId, productId, quantity);
            log.info("加入購物車: userId={}, productId={}, quantity={}", userId, productId, quantity);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品已添加到購物車");
            response.put("cartId", cart.getId());
            response.put("itemCount", cart.getItems().size());

            // 計算並返回總金額
            BigDecimal total = cart.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            response.put("total", total);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "添加商品失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 更新購物車項目數量
     */
    @PutMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<?> updateItemQuantity(
            @PathVariable Long userId,
            @PathVariable Integer cartItemId,
            @RequestParam Integer quantity) {
        try {
            Cart cart = cartService.updateCartItemQuantity(userId, cartItemId, quantity);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "數量已更新");
            response.put("cartId", cart.getId());
            response.put("itemCount", cart.getItems().size());

            // 計算並返回總金額
            BigDecimal total = cart.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            response.put("total", total);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "更新數量失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 從購物車移除項目
     */
    @DeleteMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<?> removeItem(
            @PathVariable Long userId,
            @PathVariable Integer cartItemId) {
        try {
            Cart cart = cartService.removeItemFromCart(userId, cartItemId);
            log.info("移除購物車商品: userId={}, cartItemId={}", userId, cartItemId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品已移除");
            response.put("cartId", cart.getId());
            response.put("itemCount", cart.getItems().size());

            // 計算並返回總金額
            BigDecimal total = cart.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            response.put("total", total);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "移除商品失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 清空購物車
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> clearCart(@PathVariable Long userId) {
        try {
            cartService.clearCart(userId);
            log.info("清空購物車: userId={}", userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "購物車已清空");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "清空購物車失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 取得購物車總金額
     */
    @GetMapping("/{userId}/total")
    public ResponseEntity<?> getCartTotal(@PathVariable Long userId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("total", cartService.calculateCartTotal(userId));
            response.put("itemCount", cartService.getCartItemCount(userId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "計算總金額失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
