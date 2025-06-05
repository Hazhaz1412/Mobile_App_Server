package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "listing_tags")
public class ListingTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "listing_id", nullable = false)
    private Long listingId;
    
    @Column(name = "tag_name", nullable = false, length = 50)
    private String tagName;
    
    // Constructors
    public ListingTag() {}
    
    public ListingTag(Long listingId, String tagName) {
        this.listingId = listingId;
        this.tagName = tagName;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }
    
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
}