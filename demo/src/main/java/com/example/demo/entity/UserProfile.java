package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    private Long userId;
    
    @Column(nullable = false)
    private String displayName;
    
    @Column(name = "profile_image_path")
    private String profilePictureUrl;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    private String contactInfo;
    
    @Column(nullable = false)
    private Double ratingAvg = 0.0; // Set default value
    
    @Column(nullable = false)
    private Integer ratingCount = 0; // Set default value
    
    @OneToOne
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;
    
    // Default constructor
    public UserProfile() {}
    
    // Constructor
    public UserProfile(Long userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
        this.ratingAvg = 0.0;     // Initialize with default
        this.ratingCount = 0;     // Initialize with default
    }
    
    // Getters and setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }
    
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public String getContactInfo() {
        return contactInfo;
    }
    
    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
    
    public Double getRatingAvg() {
        return ratingAvg;
    }
    
    public void setRatingAvg(Double ratingAvg) {
        this.ratingAvg = ratingAvg;
    }
    
    public Integer getRatingCount() {
        return ratingCount;
    }
    
    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
}