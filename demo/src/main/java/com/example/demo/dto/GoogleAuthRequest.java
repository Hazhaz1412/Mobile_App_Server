package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class GoogleAuthRequest {
    
    @NotBlank(message = "ID token is required")
    private String idToken;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;
    
    private String displayName;
    
    // Default constructor
    public GoogleAuthRequest() {}
    
    // Constructor with all fields
    public GoogleAuthRequest(String idToken, String email, String displayName) {
        this.idToken = idToken;
        this.email = email;
        this.displayName = displayName;
    }
    
    // Getters and setters
    public String getIdToken() {
        return idToken;
    }
    
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}