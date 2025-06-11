package com.example.demo.dto;

import java.time.LocalDateTime;

public class ChatRoomResponse {
    private Long id;
    private Long user1Id;
    private Long user2Id;
    private String user1Name;
    private String user2Name;
    private String user1ProfilePic;
    private String user2ProfilePic;
    private LocalDateTime createdAt;
    private Boolean isBlocked;
    private Long blockBy;
    private Long reportBy;
    private String lastMessage;
    private String lastMessageType;
    private LocalDateTime lastMessageTime;
    private Long unreadCount;
    
    // Constructors
    public ChatRoomResponse() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUser1Id() {
        return user1Id;
    }
    
    public void setUser1Id(Long user1Id) {
        this.user1Id = user1Id;
    }
    
    public Long getUser2Id() {
        return user2Id;
    }
    
    public void setUser2Id(Long user2Id) {
        this.user2Id = user2Id;
    }
    
    public String getUser1Name() {
        return user1Name;
    }
    
    public void setUser1Name(String user1Name) {
        this.user1Name = user1Name;
    }
    
    public String getUser2Name() {
        return user2Name;
    }
    
    public void setUser2Name(String user2Name) {
        this.user2Name = user2Name;
    }
    
    public String getUser1ProfilePic() {
        return user1ProfilePic;
    }
    
    public void setUser1ProfilePic(String user1ProfilePic) {
        this.user1ProfilePic = user1ProfilePic;
    }
    
    public String getUser2ProfilePic() {
        return user2ProfilePic;
    }
    
    public void setUser2ProfilePic(String user2ProfilePic) {
        this.user2ProfilePic = user2ProfilePic;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Boolean getIsBlocked() {
        return isBlocked;
    }
    
    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }
    
    public Long getBlockBy() {
        return blockBy;
    }
    
    public void setBlockBy(Long blockBy) {
        this.blockBy = blockBy;
    }
    
    public Long getReportBy() {
        return reportBy;
    }
    
    public void setReportBy(Long reportBy) {
        this.reportBy = reportBy;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public String getLastMessageType() {
        return lastMessageType;
    }
    
    public void setLastMessageType(String lastMessageType) {
        this.lastMessageType = lastMessageType;
    }
    
    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }
    
    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    
    public Long getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
