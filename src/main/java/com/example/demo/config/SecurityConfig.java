package com.example.demo.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint unauthorizedHandler = (request, response, ex) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write("{\"error\":\"\u672a登入，請提供有效 Token\"}");
        };

        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 使用 STATELESS：每個 API 請求只靠 JWT 驗證身份，不建立也不使用 HttpSession 儲存 SecurityContext
                // WebAuthnController 直接注入 HttpSession，不受此設定影響
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .authorizeHttpRequests(authz -> authz
                        // 公開 API
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/chat/**").permitAll() // 已被更有針對性的規則取代
                        .requestMatchers("/api/products/**").permitAll() // 已被更有針對性的規則取代
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/prometheus", "/actuator/info", "/actuator/metrics").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // AI 助手 API：全部公開（LLM 不存取用戶敏感資料）
                        .requestMatchers("/api/ai/**").permitAll()

                        // Passkeys / WebAuthn 公開端點（啟動與完成不需要預先登入）
                        .requestMatchers("/api/passkeys/registration/**").permitAll()
                        .requestMatchers("/api/passkeys/assertion/**").permitAll()

                        // 用戶相關 API - 允許讀取用戶信息 (GET)
                        .requestMatchers("/api/users/*/profile").authenticated()
                        .requestMatchers("/api/users/**").permitAll()

                        // 購物車 API 必須認證
                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/orders/**").authenticated() // 需要認證，管理員操作由 @PreAuthorize("hasRole('ADMIN')") 保護
                        .requestMatchers("/api/wishlist/**").authenticated()
                        .requestMatchers("/api/user/**").authenticated()

                        // Passkeys 管理需要登入（查看、刪除）
                        .requestMatchers("/api/passkeys/**").authenticated()

                        // 管理員 API
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 其他所有請求都需要認證
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3002",
                "http://localhost:5173",
                "http://test6.test",
                "https://test6.test",
                "http://192.168.10.10",
                "http://localhost"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1小時的預檢快取

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
