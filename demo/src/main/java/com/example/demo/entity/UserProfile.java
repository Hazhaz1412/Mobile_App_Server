package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    private Long userId;
    private User user;
    @Column(nullable = false)
    private String displayName;
    
    @Column(name = "profile_picture_url")
    private String profilePictureUrl;
    
    @Column
    private String bio;
    
    @Column(name = "contact_info")
    private String contactInfo;
    
    @Column(name = "rating_avg")
    private BigDecimal ratingAvg;
    
    @Column(name = "rating_count")
    private Integer ratingCount;
    
    // Constructors, getters, and setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    public UserProfile() {
    }
    
    public UserProfile(Long userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }
    
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
    
    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }
    
    public void setRatingAvg(BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }
    
    public Integer getRatingCount() {
        return ratingCount;
    }
    
    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }
}