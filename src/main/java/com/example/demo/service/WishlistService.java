package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.model.Wishlist;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WishlistRepository;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistRepository wishlistRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public List<Wishlist> getWishlistByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到用戶，ID: " + userId));
        return wishlistRepository.findByUser(user);
    }

    public List<Wishlist> getWishlistByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("找不到用戶: " + username));
        return wishlistRepository.findByUser(user);
    }

    @Transactional
    public Wishlist addToWishlistByUsername(String username, Long productId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("找不到用戶: " + username));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("找不到商品，ID: " + productId));
        if (wishlistRepository.existsByUserAndProduct(user, product)) {
            throw new RuntimeException("商品已在願望清單中");
        }
        return wishlistRepository.save(new Wishlist(user, product));
    }

    @Transactional
    public void removeFromWishlistByUsername(String username, Long productId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("找不到用戶: " + username));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("找不到商品，ID: " + productId));
        wishlistRepository.deleteByUserAndProduct(user, product);
    }

    public boolean isInWishlistByUsername(String username, Long productId) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return false;
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) return false;
        return wishlistRepository.existsByUserAndProduct(userOpt.get(), productOpt.get());
    }

    @Transactional
    public Wishlist addToWishlist(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到用戶，ID: " + userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("找不到商品，ID: " + productId));

        if (wishlistRepository.existsByUserAndProduct(user, product)) {
            throw new RuntimeException("商品已在願望清單中");
        }

        return wishlistRepository.save(new Wishlist(user, product));
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到用戶，ID: " + userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("找不到商品，ID: " + productId));

        wishlistRepository.deleteByUserAndProduct(user, product);
    }

    public boolean isInWishlist(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到用戶，ID: " + userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("找不到商品，ID: " + productId));

        return wishlistRepository.existsByUserAndProduct(user, product);
    }
}
