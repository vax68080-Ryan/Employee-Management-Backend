package com.example.backend_api.controller;

import com.example.backend_api.EmployeeRepository;
import com.example.backend_api.security.JwtUtil; // 👈 1. 記得 Import 這個！

import java.util.Map;
import java.util.HashMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmployeeRepository employeeRepository;

    // 👇 2. 注入你寫好的工具人，用來產生簽名
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String id = loginRequest.get("id");
        String rawPassword = loginRequest.get("password");

        return employeeRepository.findById(id)
                .map(employee -> {
                    // 檢查比對結果
                    boolean isMatch = passwordEncoder.matches(rawPassword, employee.getPassword());
                    
                    if (isMatch) {
                        // 👇 3. 關鍵修改：產生真正的 JWT Token (亂碼字串)
                        String token = jwtUtil.generateToken(id);

                        Map<String, Object> response = new HashMap<>();
                        response.put("token", token); // 放入真 Token
                        response.put("level", employee.getLevel());
                        response.put("name", employee.getName());
                        
                        return ResponseEntity.ok(response);
                    }

                    return ResponseEntity.status(401).body("帳號或密碼錯誤");
                })
                .orElse(ResponseEntity.status(401).body("帳號不存在"));
    }
}