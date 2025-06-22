package com.example.demo.dto;

import com.example.demo.entity.VerificationAction;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public class SendVerificationRequest {
    @NotNull(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotNull(message = "Action is required")
    private VerificationAction action;
    
    // Constructors
    public SendVerificationRequest() {}
    
    public SendVerificationRequest(String email, VerificationAction action) {
        this.email = email;
        this.action = action;
    }
    
    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public VerificationAction getAction() { return action; }
    public void setAction(VerificationAction action) { this.action = action; }
}
