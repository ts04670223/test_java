package com.example.demo.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.demo.model.User;

@Service("orderSecurityService")
public class OrderSecurityService {

    /**
     * 判斷目前登入的使用者是否為指定 userId 的本人
     */
    public boolean isOwner(Authentication authentication, Long userId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getId() != null && user.getId().equals(userId);
        }
        return false;
    }
}
