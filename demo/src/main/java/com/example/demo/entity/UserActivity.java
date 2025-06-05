package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities")
public class UserActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "listing_id", nullable = false)
    private Long listingId;
    
    @Column(name = "activity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityType activityType;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public UserActivity() {
        this.createdAt = LocalDateTime.now();
    }
    
    public UserActivity(Long userId, Long listingId, ActivityType activityType) {
        this.userId = userId;
        this.listingId = listingId;
        this.activityType = activityType;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }
    
    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType activityType) { this.activityType = activityType; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}