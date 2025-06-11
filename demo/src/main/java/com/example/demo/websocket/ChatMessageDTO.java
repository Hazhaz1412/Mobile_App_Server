package com.example.demo.websocket;

import java.time.LocalDateTime;

public class ChatMessageDTO {
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String senderProfilePic;
    private String content;
    private String type; // TEXT, IMAGE, EMOJI
    private LocalDateTime timestamp;
    private Boolean isRead;
    
    // Constructors
    public ChatMessageDTO() {
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }
    
    // Getters and setters
    public Long getChatRoomId() { 
        return chatRoomId; 
    }
    
    public void setChatRoomId(Long chatRoomId) { 
        this.chatRoomId = chatRoomId; 
    }
    
    public Long getSenderId() { 
        return senderId; 
    }
    
    public void setSenderId(Long senderId) { 
        this.senderId = senderId; 
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getSenderProfilePic() {
        return senderProfilePic;
    }
    
    public void setSenderProfilePic(String senderProfilePic) {
        this.senderProfilePic = senderProfilePic;
    }
    
    public String getContent() { 
        return content; 
    }
    
    public void setContent(String content) { 
        this.content = content; 
    }
    
    public String getType() { 
        return type; 
    }
    
    public void setType(String type) { 
        this.type = type; 
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
