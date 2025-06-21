package com.example.demo.dto;

import com.example.demo.entity.OfferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OfferResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private String listingImageUrl;
    private Long buyerId;
    private String buyerName;
    private String buyerProfilePic;
    private Long sellerId;
    private String sellerName;
    private String sellerProfilePic;
    private BigDecimal offerAmount;
    private BigDecimal listingPrice; // Original listing price for comparison
    private String message;
    private OfferStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private boolean isExpired;
    private boolean canBeAccepted;
    private boolean canBeRejected;
    private boolean canBeCountered;
    private boolean hasPaidTransaction;
    
    // Constructors
    public OfferResponse() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }
    
    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }
    
    public String getListingImageUrl() { return listingImageUrl; }
    public void setListingImageUrl(String listingImageUrl) { this.listingImageUrl = listingImageUrl; }
    
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    
    public String getBuyerProfilePic() { return buyerProfilePic; }
    public void setBuyerProfilePic(String buyerProfilePic) { this.buyerProfilePic = buyerProfilePic; }
    
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    
    public String getSellerProfilePic() { return sellerProfilePic; }
    public void setSellerProfilePic(String sellerProfilePic) { this.sellerProfilePic = sellerProfilePic; }
    
    public BigDecimal getOfferAmount() { return offerAmount; }
    public void setOfferAmount(BigDecimal offerAmount) { this.offerAmount = offerAmount; }
    
    public BigDecimal getListingPrice() { return listingPrice; }
    public void setListingPrice(BigDecimal listingPrice) { this.listingPrice = listingPrice; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public boolean isExpired() { return isExpired; }
    public void setExpired(boolean expired) { isExpired = expired; }
    
    public boolean isCanBeAccepted() { return canBeAccepted; }
    public void setCanBeAccepted(boolean canBeAccepted) { this.canBeAccepted = canBeAccepted; }
    
    public boolean isCanBeRejected() { return canBeRejected; }
    public void setCanBeRejected(boolean canBeRejected) { this.canBeRejected = canBeRejected; }
    
    public boolean isCanBeCountered() { return canBeCountered; }
    public void setCanBeCountered(boolean canBeCountered) { this.canBeCountered = canBeCountered; }
    
    public boolean isHasPaidTransaction() { return hasPaidTransaction; }
    public void setHasPaidTransaction(boolean hasPaidTransaction) { this.hasPaidTransaction = hasPaidTransaction; }
    
    // Helper methods
    public String getDiscountPercentage() {
        if (listingPrice != null && listingPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = listingPrice.subtract(offerAmount);
            BigDecimal percentage = discount.divide(listingPrice, 4, BigDecimal.ROUND_HALF_UP)
                                           .multiply(new BigDecimal("100"));
            return percentage.setScale(1, BigDecimal.ROUND_HALF_UP).toString() + "%";
        }
        return "0%";
    }
}
