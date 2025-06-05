package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public class SearchCriteria {
    private String keyword;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Long conditionId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double maxDistance; // in kilometers
    private String sortBy; // RELEVANCE, NEWEST, PRICE_ASC, PRICE_DESC
    private int page = 0;
    private int size = 20;
    
    // Constructors
    public SearchCriteria() {}
    
    // Getters and Setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    
    public Long getConditionId() { return conditionId; }
    public void setConditionId(Long conditionId) { this.conditionId = conditionId; }
    
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    
    public Double getMaxDistance() { return maxDistance; }
    public void setMaxDistance(Double maxDistance) { this.maxDistance = maxDistance; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}