package com.example.demo.dto;

import java.time.LocalDateTime;

/**
 * DTO representing a blocked user
 */
public class BlockedUser {
    private Long userId;
    private String displayName;
    private String avatarUrl;
    private String reason;
    private LocalDateTime blockedAt;
    
    public BlockedUser() {}
    
    public BlockedUser(Long userId, String displayName, String avatarUrl, String reason, LocalDateTime blockedAt) {
        this.userId = userId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.reason = reason;
        this.blockedAt = blockedAt;
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
    
    public String getAvatarUrl() { 
        return avatarUrl; 
    }
    
    public void setAvatarUrl(String avatarUrl) { 
        this.avatarUrl = avatarUrl; 
    }
    
    public String getReason() { 
        return reason; 
    }
    
    public void setReason(String reason) { 
        this.reason = reason; 
    }
    
    public LocalDateTime getBlockedAt() { 
        return blockedAt; 
    }
    
    public void setBlockedAt(LocalDateTime blockedAt) { 
        this.blockedAt = blockedAt; 
    }
    
    @Override
    public String toString() {
        return "BlockedUser{" +
                "userId=" + userId +
                ", displayName='" + displayName + '\'' +
                ", reason='" + reason + '\'' +
                ", blockedAt=" + blockedAt +
                '}';
    }
}
