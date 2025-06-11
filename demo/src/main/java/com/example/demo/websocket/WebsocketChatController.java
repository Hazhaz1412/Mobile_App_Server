package com.example.demo.websocket;

import com.example.demo.entity.ChatMessage;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class WebsocketChatController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private ChatService chatService;

    /**
     * Handle websocket message sending
     */
    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessageDTO sendMessage(@Payload ChatMessageDTO chatMessageDTO) {
        // Save to database
        ChatMessage msg = new ChatMessage();
        msg.setChatRoomId(chatMessageDTO.getChatRoomId());
        msg.setSenderId(chatMessageDTO.getSenderId());
        msg.setContent(chatMessageDTO.getContent());
        msg.setType(chatMessageDTO.getType());
        msg.setCreatedAt(LocalDateTime.now());
        msg.setIsRead(false);
        chatMessageRepository.save(msg);
        
        // Send to chat room topic
        messagingTemplate.convertAndSend("/topic/messages/" + chatMessageDTO.getChatRoomId(), chatMessageDTO);
        
        return chatMessageDTO;
    }
    
    /**
     * Handle read status updates
     */
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Map<String, Object> readStatus) {
        Long chatRoomId = Long.valueOf(readStatus.get("chatRoomId").toString());
        Long userId = Long.valueOf(readStatus.get("userId").toString());
        
        // Mark messages as read in the database
        int updatedCount = chatService.markMessagesAsRead(chatRoomId, userId);
        
        // Only notify if messages were updated
        if (updatedCount > 0) {
            Map<String, Object> response = new HashMap<>();
            response.put("chatRoomId", chatRoomId);
            response.put("readByUserId", userId);
            response.put("timestamp", LocalDateTime.now());
            response.put("updatedCount", updatedCount);
            
            // Broadcast to chat room topic
            messagingTemplate.convertAndSend("/topic/read-status/" + chatRoomId, response);
        }
    }
    
    /**
     * Handle typing indicator
     */
    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload Map<String, Object> typingStatus) {
        Long chatRoomId = Long.valueOf(typingStatus.get("chatRoomId").toString());
        Long userId = Long.valueOf(typingStatus.get("userId").toString());
        Boolean isTyping = Boolean.valueOf(typingStatus.get("isTyping").toString());
        
        Map<String, Object> response = new HashMap<>();
        response.put("chatRoomId", chatRoomId);
        response.put("userId", userId);
        response.put("isTyping", isTyping);
        response.put("timestamp", LocalDateTime.now());
        
        // Broadcast to chat room topic
        messagingTemplate.convertAndSend("/topic/typing/" + chatRoomId, response);
    }
}
