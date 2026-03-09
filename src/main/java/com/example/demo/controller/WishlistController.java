package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Wishlist;
import com.example.demo.service.WishlistService;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("未登入");
        }
        return auth.getName();
    }

    private List<Map<String, Object>> toResponse(List<Wishlist> items) {
        return items.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("productId", item.getProduct().getId());
            map.put("productName", item.getProduct().getName());
            map.put("productPrice", item.getProduct().getPrice());
            map.put("productImages", item.getProduct().getImages().stream()
                    .map(img -> img.getImageUrl())
                    .collect(Collectors.toList()));
            map.put("createdAt", item.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 取得目前登入用戶的願望清單（從 JWT token 識別用戶）
     */
    @GetMapping
    public ResponseEntity<?> getMyWishlist() {
        try {
            String username = getCurrentUsername();
            List<Wishlist> items = wishlistService.getWishlistByUsername(username);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("items", toResponse(items));
            response.put("count", items.size());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "取得願望清單失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 加入目前登入用戶的願望清單
     */
    @PostMapping("/items")
    public ResponseEntity<?> addToMyWishlist(@RequestParam Long productId) {
        try {
            String username = getCurrentUsername();
            Wishlist item = wishlistService.addToWishlistByUsername(username, productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品已加入願望清單");
            response.put("wishlistItemId", item.getId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "加入願望清單失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 從目前登入用戶的願望清單移除商品
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<?> removeFromMyWishlist(@PathVariable Long productId) {
        try {
            String username = getCurrentUsername();
            wishlistService.removeFromWishlistByUsername(username, productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品已從願望清單移除");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "移除願望清單失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 檢查目前登入用戶的商品是否在願望清單
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkMyWishlist(@RequestParam Long productId) {
        try {
            String username = getCurrentUsername();
            boolean inWishlist = wishlistService.isInWishlistByUsername(username, productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("inWishlist", inWishlist);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "查詢失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 取得用戶願望清單（by userId，保留向下相容）
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getWishlist(@PathVariable Long userId) {
        try {
            List<Wishlist> items = wishlistService.getWishlistByUserId(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("items", toResponse(items));
            response.put("count", items.size());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "取得願望清單失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 加入願望清單
     */
    @PostMapping("/{userId}/items")
    public ResponseEntity<?> addToWishlist(
            @PathVariable Long userId,
            @RequestParam Long productId) {
        try {
            Wishlist item = wishlistService.addToWishlist(userId, productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品已加入願望清單");
            response.put("wishlistItemId", item.getId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "加入願望清單失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 從願望清單中移除商品
     */
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<?> removeFromWishlist(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        try {
            wishlistService.removeFromWishlist(userId, productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品已從願望清單移除");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "移除願望清單失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 檢查商品是否在願望清單
     */
    @GetMapping("/{userId}/check")
    public ResponseEntity<?> checkWishlist(
            @PathVariable Long userId,
            @RequestParam Long productId) {
        try {
            boolean inWishlist = wishlistService.isInWishlist(userId, productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("inWishlist", inWishlist);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "查詢失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
