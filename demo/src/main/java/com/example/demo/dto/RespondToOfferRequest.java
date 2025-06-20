package com.example.demo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class RespondToOfferRequest {
    @NotNull(message = "Action không được để trống")
    @Pattern(regexp = "ACCEPT|REJECT|COUNTER", message = "Action phải là ACCEPT, REJECT hoặc COUNTER")
    private String action; // ACCEPT, REJECT, COUNTER
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Counter amount phải lớn hơn 0")
    private BigDecimal counterAmount; // Only for COUNTER action
    
    @Size(max = 500, message = "Tin nhắn không được vượt quá 500 ký tự")
    private String message;
    
    // Constructors
    public RespondToOfferRequest() {}
    
    public RespondToOfferRequest(String action, String message) {
        this.action = action;
        this.message = message;
    }
    
    public RespondToOfferRequest(String action, BigDecimal counterAmount, String message) {
        this.action = action;
        this.counterAmount = counterAmount;
        this.message = message;
    }
    
    // Getters and Setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public BigDecimal getCounterAmount() { return counterAmount; }
    public void setCounterAmount(BigDecimal counterAmount) { this.counterAmount = counterAmount; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    // Validation method
    public boolean isValid() {
        if (action == null) return false;
        
        switch (action.toUpperCase()) {
            case "ACCEPT":
            case "REJECT":
                return true;
            case "COUNTER":
                return counterAmount != null && counterAmount.compareTo(BigDecimal.ZERO) > 0;
            default:
                return false;
        }
    }
}
