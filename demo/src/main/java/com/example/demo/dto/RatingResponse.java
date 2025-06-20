package com.example.demo.dto;

import java.time.LocalDateTime;

public class RatingResponse {
    
    private Long id;
    private Long transactionId;
    private Long raterUserId;
    private String raterUserName;
    private Long ratedUserId;
    private String ratedUserName;
    private Long listingId;
    private String listingTitle;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    
    // Constructors
    public RatingResponse() {}
    
    public RatingResponse(Long id, Long transactionId, Long raterUserId, String raterUserName,
                         Long ratedUserId, String ratedUserName, Long listingId, String listingTitle,
                         Integer rating, String comment, LocalDateTime createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.raterUserId = raterUserId;
        this.raterUserName = raterUserName;
        this.ratedUserId = ratedUserId;
        this.ratedUserName = ratedUserName;
        this.listingId = listingId;
        this.listingTitle = listingTitle;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
    
    public Long getRaterUserId() {
        return raterUserId;
    }
    
    public void setRaterUserId(Long raterUserId) {
        this.raterUserId = raterUserId;
    }
    
    public String getRaterUserName() {
        return raterUserName;
    }
    
    public void setRaterUserName(String raterUserName) {
        this.raterUserName = raterUserName;
    }
    
    public Long getRatedUserId() {
        return ratedUserId;
    }
    
    public void setRatedUserId(Long ratedUserId) {
        this.ratedUserId = ratedUserId;
    }
    
    public String getRatedUserName() {
        return ratedUserName;
    }
    
    public void setRatedUserName(String ratedUserName) {
        this.ratedUserName = ratedUserName;
    }
    
    public Long getListingId() {
        return listingId;
    }
    
    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }
    
    public String getListingTitle() {
        return listingTitle;
    }
    
    public void setListingTitle(String listingTitle) {
        this.listingTitle = listingTitle;
    }
    
    public Integer getRating() {
        return rating;
    }
    
    public void setRating(Integer rating) {
        this.rating = rating;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
