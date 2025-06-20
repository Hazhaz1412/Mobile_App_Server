package com.example.demo.dto;

import java.util.Map;

public class UserRatingStatsResponse {
    
    private Long userId;
    private String userName;
    private Double averageRating;
    private Long totalRatings;
    private Map<Integer, Long> ratingDistribution; // {1: 5, 2: 3, 3: 10, 4: 25, 5: 57}
    
    // Constructors
    public UserRatingStatsResponse() {}
    
    public UserRatingStatsResponse(Long userId, String userName, Double averageRating, 
                                  Long totalRatings, Map<Integer, Long> ratingDistribution) {
        this.userId = userId;
        this.userName = userName;
        this.averageRating = averageRating;
        this.totalRatings = totalRatings;
        this.ratingDistribution = ratingDistribution;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public Double getAverageRating() {
        return averageRating;
    }
    
    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }
    
    public Long getTotalRatings() {
        return totalRatings;
    }
    
    public void setTotalRatings(Long totalRatings) {
        this.totalRatings = totalRatings;
    }
    
    public Map<Integer, Long> getRatingDistribution() {
        return ratingDistribution;
    }
    
    public void setRatingDistribution(Map<Integer, Long> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }
}
