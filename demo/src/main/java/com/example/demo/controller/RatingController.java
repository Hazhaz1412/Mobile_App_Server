package com.example.demo.controller;

import com.example.demo.dto.CreateRatingRequest;
import com.example.demo.dto.RatingResponse;
import com.example.demo.dto.UserRatingStatsResponse;
import com.example.demo.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {
    
    @Autowired
    private RatingService ratingService;
    
    /**
     * Tạo đánh giá mới
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRating(
            @RequestHeader("User-ID") Long userId,
            @Valid @RequestBody CreateRatingRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            RatingResponse rating = ratingService.createRating(userId, request);
            
            response.put("success", true);
            response.put("message", "Đánh giá đã được tạo thành công");
            response.put("data", rating);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Lấy tất cả đánh giá cho một user (user được đánh giá)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getRatingsForUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<RatingResponse> ratings = ratingService.getRatingsForUser(userId);
            
            response.put("success", true);
            response.put("data", ratings);
            response.put("count", ratings.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Lấy tất cả đánh giá mà user đã tạo (user đánh giá)
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Map<String, Object>> getRatingsByUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<RatingResponse> ratings = ratingService.getRatingsByUser(userId);
            
            response.put("success", true);
            response.put("data", ratings);
            response.put("count", ratings.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Lấy thống kê đánh giá cho user
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRatingStats(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserRatingStatsResponse stats = ratingService.getUserRatingStats(userId);
            
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Kiểm tra xem user có thể đánh giá transaction không
     */
    @GetMapping("/can-rate/{transactionId}")
    public ResponseEntity<Map<String, Object>> canUserRateTransaction(
            @RequestHeader("User-ID") Long userId,
            @PathVariable Long transactionId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean canRate = ratingService.canUserRateTransaction(userId, transactionId);
            
            response.put("success", true);
            response.put("canRate", canRate);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Lấy đánh giá của user cho transaction cụ thể
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<Map<String, Object>> getRatingForTransaction(
            @RequestHeader("User-ID") Long userId,
            @PathVariable Long transactionId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<RatingResponse> rating = ratingService.getRatingForTransaction(userId, transactionId);
            
            response.put("success", true);
            if (rating.isPresent()) {
                response.put("data", rating.get());
                response.put("hasRated", true);
            } else {
                response.put("data", null);
                response.put("hasRated", false);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
