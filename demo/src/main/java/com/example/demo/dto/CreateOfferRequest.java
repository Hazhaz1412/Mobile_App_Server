package com.example.demo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateOfferRequest {
    @NotNull(message = "Listing ID không được để trống")
    private Long listingId;
    
    @NotNull(message = "Giá đề nghị không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá đề nghị phải lớn hơn 0")
    private BigDecimal offerAmount;
    
    @Size(max = 500, message = "Tin nhắn không được vượt quá 500 ký tự")
    private String message;
    
    // Constructors
    public CreateOfferRequest() {}
    
    public CreateOfferRequest(Long listingId, BigDecimal offerAmount, String message) {
        this.listingId = listingId;
        this.offerAmount = offerAmount;
        this.message = message;
    }
    
    // Getters and Setters
    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }
    
    public BigDecimal getOfferAmount() { return offerAmount; }
    public void setOfferAmount(BigDecimal offerAmount) { this.offerAmount = offerAmount; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
