package com.example.backend_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // ⭐ 新增
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration; // ⭐ 新增
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // ⭐ 新增
import java.util.Arrays; // ⭐ 新增

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. ⭐ 啟用並設定 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2. 關閉 CSRF 防護以允許 POST/PATCH/DELETE
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ⭐ 允許所有的 OPTIONS 預檢請求，這是解決 CORS 的關鍵
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().permitAll() // 開發期間允許所有請求
                );
        return http.build();
    }

    // ⭐ 3. 新增 CORS 配置源：定義哪些來源、方法、Header 是允許的
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("http://localhost:4200")); // 允許的前端來源
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // ⭐ 務必包含 PATCH
        config.setAllowedHeaders(Arrays.asList("*")); // 允許所有 Header
        config.setAllowCredentials(true); // 允許攜帶 Cookie 或認證資訊

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 對所有路徑生效
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}