package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
public class Rating {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;
    
    @Column(name = "rater_id", nullable = false)
    private Long raterId; // ID của rating record
    
    @Column(name = "rater_user_id", nullable = false)
    private Long raterUserId; // Người đánh giá
    
    @Column(name = "rated_user_id", nullable = false)
    private Long ratedUserId; // Người được đánh giá
    
    @Column(name = "listing_id", nullable = false)
    private Long listingId;
    
    @Column(name = "rating", nullable = false)
    private Integer rating; // 1-5 stars
    
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public Rating() {}
      public Rating(Long transactionId, Long raterUserId, Long ratedUserId, Long listingId, Integer rating, String comment) {
        this.transactionId = transactionId;
        this.raterId = raterUserId; // Set raterId same as raterUserId for now
        this.raterUserId = raterUserId;
        this.ratedUserId = ratedUserId;
        this.listingId = listingId;
        this.rating = rating;
        this.comment = comment;
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
    
    public Long getRaterId() {
        return raterId;
    }
    
    public void setRaterId(Long raterId) {
        this.raterId = raterId;
    }
    
    public Long getRaterUserId() {
        return raterUserId;
    }
    
    public void setRaterUserId(Long raterUserId) {
        this.raterUserId = raterUserId;
    }
    
    public Long getRatedUserId() {
        return ratedUserId;
    }
    
    public void setRatedUserId(Long ratedUserId) {
        this.ratedUserId = ratedUserId;
    }
    
    public Long getListingId() {
        return listingId;
    }
    
    public void setListingId(Long listingId) {
        this.listingId = listingId;
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
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
