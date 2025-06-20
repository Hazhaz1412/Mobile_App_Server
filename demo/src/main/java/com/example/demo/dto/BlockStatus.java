package com.example.demo.dto;

/**
 * DTO representing block status between two users
 */
public class BlockStatus {
    private Boolean isBlocked;      // Current user blocked target user
    private Boolean isBlockedBy;    // Target user blocked current user
    private Boolean canInteract;    // Can they interact with each other
    
    public BlockStatus() {}
    
    public BlockStatus(Boolean isBlocked, Boolean isBlockedBy, Boolean canInteract) {
        this.isBlocked = isBlocked;
        this.isBlockedBy = isBlockedBy;
        this.canInteract = canInteract;
    }
    
    // Getters and setters
    public Boolean getIsBlocked() { 
        return isBlocked; 
    }
    
    public void setIsBlocked(Boolean isBlocked) { 
        this.isBlocked = isBlocked; 
    }
    
    public Boolean getIsBlockedBy() { 
        return isBlockedBy; 
    }
    
    public void setIsBlockedBy(Boolean isBlockedBy) { 
        this.isBlockedBy = isBlockedBy; 
    }
    
    public Boolean getCanInteract() { 
        return canInteract; 
    }
    
    public void setCanInteract(Boolean canInteract) { 
        this.canInteract = canInteract; 
    }
    
    @Override
    public String toString() {
        return "BlockStatus{" +
                "isBlocked=" + isBlocked +
                ", isBlockedBy=" + isBlockedBy +
                ", canInteract=" + canInteract +
                '}';
    }
}
