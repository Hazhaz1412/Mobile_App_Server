package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@CrossOrigin(origins = "*")
public class TestController {
    
    @Autowired
    private com.example.demo.service.UserProfileService userProfileService;
      @PersistenceContext
    private EntityManager entityManager;
    
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
            "status", "success",
            "message", "Payment API is working!",
            "timestamp", System.currentTimeMillis()
        );
    }
    
    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> data) {
        return Map.of(
            "status", "success",
            "message", "Echo test successful",
            "receivedData", data,
            "timestamp", System.currentTimeMillis()
        );
    }
    
    @GetMapping("/fix-user-profiles")
    public Map<String, Object> fixUserProfiles() {
        try {
            int created = userProfileService.createMissingUserProfiles();
            return Map.of(
                "status", "success",
                "message", "Created " + created + " missing user profiles",
                "created_count", created,
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "status", "error",
                "message", "Error creating user profiles: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }    }
    
    @GetMapping("/fix-user-profiles-sql")
    @Transactional
    public Map<String, Object> fixUserProfilesSQL() {
        try {
            String sql = """
                INSERT IGNORE INTO user_profiles (user_id, display_name, bio, contact_info, rating_avg, rating_count, profile_image_path) 
                SELECT u.id, CONCAT('Test User ', u.id), '', '', 0.0, 0, NULL
                FROM users u 
                LEFT JOIN user_profiles up ON u.id = up.user_id 
                WHERE up.user_id IS NULL
                """;
            
            int result = entityManager.createNativeQuery(sql).executeUpdate();
            
            return Map.of(
                "status", "success",
                "message", "Created " + result + " missing user profiles using SQL",
                "created_count", result,
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "status", "error",
                "message", "Error creating user profiles: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
}
