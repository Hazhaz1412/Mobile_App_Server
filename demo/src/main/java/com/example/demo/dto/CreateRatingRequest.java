package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateRatingRequest {
    
    @NotNull(message = "Transaction ID không được để trống")
    private Long transactionId;
    
    @NotNull(message = "Rated user ID không được để trống")
    private Long ratedUserId;
    
    @NotNull(message = "Rating không được để trống")
    @Min(value = 1, message = "Rating phải từ 1 đến 5")
    @Max(value = 5, message = "Rating phải từ 1 đến 5")
    private Integer rating;
    
    private String comment;
    
    // Constructors
    public CreateRatingRequest() {}
    
    public CreateRatingRequest(Long transactionId, Long ratedUserId, Integer rating, String comment) {
        this.transactionId = transactionId;
        this.ratedUserId = ratedUserId;
        this.rating = rating;
        this.comment = comment;
    }
    
    // Getters and Setters
    public Long getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
    
    public Long getRatedUserId() {
        return ratedUserId;
    }
    
    public void setRatedUserId(Long ratedUserId) {
        this.ratedUserId = ratedUserId;
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
}
