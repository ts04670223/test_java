package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.JwtRequest;
import com.example.demo.dto.JwtResponse;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.model.User;
import com.example.demo.service.JwtService;
import com.example.demo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "認證", description = "用戶認證相關 API")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    @Operation(summary = "用戶登入", description = "使用用戶名和密碼進行登入，返回 JWT Token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody JwtRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            User user = (User) authentication.getPrincipal();
            String token = jwtService.generateToken(user);
            
            // 異步更新登入時間（不阻擋主要登入流程）
            try {
                userService.updateLastLoginTime(user.getUsername());
            } catch (Exception e) {
                // 忽略登入時間更新錯誤
                logger.warn("更新登入時間失敗: {}", e.getMessage());
            }
            
            // 創建用戶響應
            UserResponse userResponse = new UserResponse();
            userResponse.setId(user.getId());
            userResponse.setUsername(user.getUsername());
            userResponse.setEmail(user.getEmail());
            userResponse.setFirstName(user.getFirstName());
            userResponse.setLastName(user.getLastName());
            userResponse.setRole(user.getRole().name());
            
            // 創建登入響應
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(token);
            loginResponse.setUser(userResponse);

            logger.info("用戶登入成功: userId={}", user.getId());
            return ResponseEntity.ok(ApiResponse.success("登入成功", loginResponse));
            
        } catch (BadCredentialsException e) {
            logger.warn("登入失敗（帳號或密碼錯誤）");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("用戶名或密碼錯誤"));
        } catch (Exception e) {
            logger.error("登入時發生未預期錯誤", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("登入失敗", e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "用戶註冊", description = "創建新用戶帳號")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // 使用事務性註冊方法，避免連線洩漏
            User user = userService.registerUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getFirstName(),
                registerRequest.getLastName(),
                registerRequest.getGender()
            );

            UserResponse userResponse = new UserResponse();
            userResponse.setId(user.getId());
            userResponse.setUsername(user.getUsername());
            userResponse.setEmail(user.getEmail());
            userResponse.setFirstName(user.getFirstName());
            userResponse.setLastName(user.getLastName());

            logger.info("新用戶註冊成功: userId={}", user.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("註冊成功", userResponse));

        } catch (RuntimeException e) {
            logger.warn("用戶註冊失敗: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("註冊時發生未預期錯誤", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("註冊失敗", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token", description = "使用現有 Token 獲取新的 Token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("無效的授權標頭"));
            }

            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);
            
            if (username != null && jwtService.isTokenValid(token)) {
                return userService.getUserByUsername(username)
                        .map(user -> {
                            String newToken = jwtService.generateToken(user);
                            JwtResponse jwtResponse = new JwtResponse(newToken, user.getUsername(), user.getRole().name());
                            return ResponseEntity.ok(ApiResponse.success("Token 刷新成功", jwtResponse));
                        })
                        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error("無效的 Token")));
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("無效的 Token"));
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Token 刷新失敗", e.getMessage()));
        }
    }

    @GetMapping("/users")
    @Operation(summary = "取得所有用戶", description = "取得系統中所有用戶列表（用於聊天室）")
    public ResponseEntity<ApiResponse<java.util.List<UserResponse>>> getAllUsers() {
        try {
            java.util.List<User> users = userService.getAllUsers();
            java.util.List<UserResponse> userResponses = users.stream()
                .map(user -> {
                    UserResponse response = new UserResponse();
                    response.setId(user.getId());
                    response.setUsername(user.getUsername());
                    response.setEmail(user.getEmail());
                    response.setFirstName(user.getFirstName());
                    response.setLastName(user.getLastName());
                    response.setRole(user.getRole().name());
                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("取得用戶列表成功", userResponses));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("取得用戶列表失敗", e.getMessage()));
        }
    }
}
