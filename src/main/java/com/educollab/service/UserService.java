package com.educollab.service;

import com.educollab.model.User;
import com.educollab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateAvatar(String userIdStr, String avatarUrl) {
        try {
            if (userIdStr == null || userIdStr.isEmpty()) {
                throw new RuntimeException("userId is required");
            }
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                throw new RuntimeException("avatar_url is required");
            }
            UUID userId = UUID.fromString(userIdStr);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userIdStr));
            
            System.out.println("Updating avatar for user " + userId + " -> " + avatarUrl);
            user.setAvatarUrl(avatarUrl.trim());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Avatar updated successfully");
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId().toString());
            data.put("avatarUrl", user.getAvatarUrl());
            response.put("data", data);
            return response;
        } catch (Exception e) {
            System.err.println("❌ Error updating avatar: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update avatar: " + e.getMessage(), e);
        }
    }
}
