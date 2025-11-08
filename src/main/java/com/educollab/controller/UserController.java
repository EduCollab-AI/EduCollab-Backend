package com.educollab.controller;

import com.educollab.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PatchMapping("/{userId}/avatar")
    public ResponseEntity<Map<String, Object>> updateAvatar(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        System.out.println("Update avatar endpoint accessed for user " + userId);
        String avatarUrl = request.get("avatar_url") != null ? request.get("avatar_url").toString() : null;
        Map<String, Object> result = userService.updateAvatar(userId, avatarUrl);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String userId) {
        System.out.println("Get user endpoint accessed for user " + userId);
        Map<String, Object> result = userService.getUserById(userId);
        return ResponseEntity.ok(result);
    }
}
