package com.example.demo.dto;

import java.util.List;

public class PagedApiResponse<T> {
    private boolean success;
    private String message;
    private List<T> data;
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    
    // Constructors
    public PagedApiResponse() {}
    
    public PagedApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public PagedApiResponse(boolean success, String message, List<T> data, 
                           int currentPage, int pageSize, long totalElements, int totalPages) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
    
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}