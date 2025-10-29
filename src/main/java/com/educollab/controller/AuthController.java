package com.educollab.controller;

import com.educollab.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @GetMapping("/health")
    public String health() {
        System.out.println("Auth health endpoint accessed");
        return "Auth service is running";
    }
    
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> request) {
        System.out.println("Register endpoint accessed with data: " + request);
        return authService.register(request);
    }
    
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> request) {
        System.out.println("Login endpoint accessed with data: " + request);
        return authService.login(request);
    }
}