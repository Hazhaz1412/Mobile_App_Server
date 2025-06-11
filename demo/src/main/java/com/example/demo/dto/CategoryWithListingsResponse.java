package com.example.demo.dto;

import java.util.List;

public class CategoryWithListingsResponse {
    private Long categoryId;
    private String categoryName;
    private String categoryDescription;
    private String categoryIconUrl;
    private List<ListingResponse> listings;

    public CategoryWithListingsResponse() {}

    public CategoryWithListingsResponse(Long categoryId, String categoryName, String categoryDescription, String categoryIconUrl, List<ListingResponse> listings) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryDescription = categoryDescription;
        this.categoryIconUrl = categoryIconUrl;
        this.listings = listings;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryDescription() { return categoryDescription; }
    public void setCategoryDescription(String categoryDescription) { this.categoryDescription = categoryDescription; }

    public String getCategoryIconUrl() { return categoryIconUrl; }
    public void setCategoryIconUrl(String categoryIconUrl) { this.categoryIconUrl = categoryIconUrl; }

    public List<ListingResponse> getListings() { return listings; }
    public void setListings(List<ListingResponse> listings) { this.listings = listings; }
}
