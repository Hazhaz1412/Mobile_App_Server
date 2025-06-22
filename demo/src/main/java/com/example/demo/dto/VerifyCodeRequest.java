package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VerifyCodeRequest {
    @NotBlank(message = "Verification code is required")
    @Size(min = 6, max = 6, message = "Verification code must be 6 digits")
    @Pattern(regexp = "\\d{6}", message = "Verification code must contain only digits")
    private String verificationCode;
    
    // Constructors
    public VerifyCodeRequest() {}
    
    public VerifyCodeRequest(String verificationCode) {
        this.verificationCode = verificationCode;
    }
    
    // Getters and Setters
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
}
