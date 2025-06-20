package com.example.demo.dto;

/**
 * Request DTO for blocking a user
 */
public class BlockUserRequest {
    private String reason;
    
    public BlockUserRequest() {}
    
    public BlockUserRequest(String reason) {
        this.reason = reason;
    }
    
    public String getReason() { 
        return reason; 
    }
    
    public void setReason(String reason) { 
        this.reason = reason; 
    }
    
    @Override
    public String toString() {
        return "BlockUserRequest{reason='" + reason + "'}";
    }
}
