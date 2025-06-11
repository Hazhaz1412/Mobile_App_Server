package com.example.demo.dto;

import org.springframework.web.multipart.MultipartFile;

public class ChatMessageRequest {
    private Long chatRoomId;
    private Long senderId;
    private String content;
    private String type; // TEXT, IMAGE, EMOJI
    private MultipartFile imageFile;
    
    // Constructors
    public ChatMessageRequest() {}
    
    // Getters and Setters
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
    
    public MultipartFile getImageFile() {
        return imageFile;
    }
    
    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }
}
