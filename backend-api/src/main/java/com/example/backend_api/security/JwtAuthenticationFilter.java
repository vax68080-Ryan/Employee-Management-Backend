package com.example.backend_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil; // 注入剛剛寫的工具人

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 從 Header 取得 Token
        // 前端傳過來會是 "Authorization: Bearer xxxxx.yyyyy.zzzzz"
        String headerAuth = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            token = headerAuth.substring(7); // 去掉 "Bearer " 前綴
            try {
                // 2. 驗證 Token 並取得使用者名稱
                if (jwtUtil.validateToken(token)) {
                    username = jwtUtil.getUsernameFromToken(token);
                }
            } catch (Exception e) {
                logger.error("Token 驗證失敗: " + e.getMessage());
            }
        }

        // 3. 如果驗證成功，且目前 SecurityContext 沒人登入
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // 建立一個已驗證的物件 (這裡簡單處理，不查資料庫權限，直接給過)
            // 如果需要權限控制 (如 ADMIN)，這裡要改查 UserDetailService
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
            
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 4. 把這個人放進 SecurityContext (代表登入成功)
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 5. 繼續往下一個過濾器走
        filterChain.doFilter(request, response);
    }
}