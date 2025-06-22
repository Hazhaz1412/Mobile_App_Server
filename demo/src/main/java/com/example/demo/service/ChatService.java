package com.example.demo.service;

import com.example.demo.dto.ChatMessageRequest;
import com.example.demo.dto.ChatMessageResponse;
import com.example.demo.dto.ChatRoomResponse;
import com.example.demo.entity.ActivityType;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.UserProfile;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.ChatRoomRepository;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.websocket.ChatMessageDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Create a new chat room between two users
     */
    public ChatRoomResponse createChatRoom(Long user1Id, Long user2Id) {
        // Check if a chat room already exists between these users
        Optional<ChatRoom> existingRoom = chatRoomRepository.findRoomByUserIds(user1Id, user2Id);
        
        ChatRoom chatRoom;
        if (existingRoom.isPresent()) {
            chatRoom = existingRoom.get();
        } else {
            chatRoom = new ChatRoom();
            chatRoom.setUser1Id(user1Id);
            chatRoom.setUser2Id(user2Id);
            chatRoom.setCreatedAt(LocalDateTime.now());
            chatRoom.setIsBlocked(false);
            chatRoom = chatRoomRepository.save(chatRoom);
        }
        
        return convertToChatRoomResponse(chatRoom, user1Id);
    }

    /**
     * Get all chat rooms for a user
     */
    public List<ChatRoomResponse> getUserChatRooms(Long userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByUser1IdOrUser2Id(userId, userId);
        
        // Convert list of chat rooms to list of chat room responses
        return chatRooms.stream()
                .map(room -> convertToChatRoomResponse(room, userId))
                .sorted(Comparator.comparing(ChatRoomResponse::getLastMessageTime, 
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Get chat room by ID and include last message and unread count
     */
    public ChatRoomResponse getChatRoomById(Long roomId, Long userId) {
        Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findById(roomId);
        if (chatRoomOptional.isPresent()) {
            ChatRoom chatRoom = chatRoomOptional.get();
            return convertToChatRoomResponse(chatRoom, userId);
        }
        return null;
    }    /**
     * Get all messages in a chat room
     */
    @Transactional
    public List<ChatMessageResponse> getChatMessages(Long chatRoomId, Long userId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId);
        
        // Mark messages as read
        markMessagesAsRead(chatRoomId, userId);
        
        return messages.stream()
                .map(this::convertToChatMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Send a text message
     */
    @Transactional
    public ChatMessageResponse sendTextMessage(ChatMessageRequest request) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChatRoomId(request.getChatRoomId());
        chatMessage.setSenderId(request.getSenderId());
        chatMessage.setContent(request.getContent());
        chatMessage.setType("TEXT");
        chatMessage.setCreatedAt(LocalDateTime.now());
        chatMessage.setIsRead(false);
        
        chatMessage = chatMessageRepository.save(chatMessage);
        
        // Send via WebSocket
        notifyMessageReceived(chatMessage);
        
        return convertToChatMessageResponse(chatMessage);
    }

    /**
     * Send an image message
     */
    @Transactional
    public ChatMessageResponse sendImageMessage(Long chatRoomId, Long senderId, MultipartFile imageFile) {
        // Upload the image
        String imageUrl = fileStorageService.uploadImage(imageFile, "chat-images");
        
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChatRoomId(chatRoomId);
        chatMessage.setSenderId(senderId);
        chatMessage.setContent(imageUrl);
        chatMessage.setType("IMAGE");
        chatMessage.setCreatedAt(LocalDateTime.now());
        chatMessage.setIsRead(false);
        
        chatMessage = chatMessageRepository.save(chatMessage);
        
        // Send via WebSocket
        notifyMessageReceived(chatMessage);
        
        return convertToChatMessageResponse(chatMessage);
    }

    /**
     * Mark all messages in a chat room as read for a user
     */
    @Transactional
    public int markMessagesAsRead(Long chatRoomId, Long userId) {
        int updatedCount = chatMessageRepository.markAllMessagesAsRead(chatRoomId, userId);
        
        // Notify the sender that their messages have been read
        if (updatedCount > 0) {
            Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatRoomId);
            if (roomOpt.isPresent()) {
                ChatRoom room = roomOpt.get();
                Long otherUserId = room.getUser1Id().equals(userId) ? room.getUser2Id() : room.getUser1Id();
                
                // Create a read status update message for WebSocket
                Map<String, Object> readStatus = new HashMap<>();
                readStatus.put("chatRoomId", chatRoomId);
                readStatus.put("readByUserId", userId);
                readStatus.put("timestamp", LocalDateTime.now());
                
                // Send to the message sender's channel
                messagingTemplate.convertAndSend("/topic/read-status/" + otherUserId, readStatus);
            }
        }
        
        return updatedCount;
    }

    /**
     * Block a chat room
     */
    @Transactional
    public ChatRoomResponse blockChatRoom(Long chatRoomId, Long userId) {
        Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findById(chatRoomId);
        if (chatRoomOptional.isPresent()) {
            ChatRoom chatRoom = chatRoomOptional.get();
            
            // Verify user is part of this chat room
            if (!chatRoom.getUser1Id().equals(userId) && !chatRoom.getUser2Id().equals(userId)) {
                throw new IllegalArgumentException("User is not part of this chat room");
            }
            
            chatRoom.setIsBlocked(true);
            chatRoom.setBlockBy(userId);
            chatRoom = chatRoomRepository.save(chatRoom);
            
            // Notify the other user
            Long otherUserId = chatRoom.getUser1Id().equals(userId) ? chatRoom.getUser2Id() : chatRoom.getUser1Id();
            Map<String, Object> blockStatus = new HashMap<>();
            blockStatus.put("chatRoomId", chatRoomId);
            blockStatus.put("blockedByUserId", userId);
            blockStatus.put("timestamp", LocalDateTime.now());
            messagingTemplate.convertAndSend("/topic/block-status/" + otherUserId, blockStatus);
            
            return convertToChatRoomResponse(chatRoom, userId);
        }
        return null;
    }

    /**
     * Report a chat room
     */
    @Transactional
    public ChatRoomResponse reportChatRoom(Long chatRoomId, Long userId) {
        Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findById(chatRoomId);
        if (chatRoomOptional.isPresent()) {
            ChatRoom chatRoom = chatRoomOptional.get();
            
            // Verify user is part of this chat room
            if (!chatRoom.getUser1Id().equals(userId) && !chatRoom.getUser2Id().equals(userId)) {
                throw new IllegalArgumentException("User is not part of this chat room");
            }
            
            chatRoom.setReportBy(userId);
            chatRoom = chatRoomRepository.save(chatRoom);
            
            // Admin notification could be added here
            
            return convertToChatRoomResponse(chatRoom, userId);
        }
        return null;
    }

    /**
     * Unblock a chat room
     */
    @Transactional
    public ChatRoomResponse unblockChatRoom(Long chatRoomId, Long userId) {
        Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findById(chatRoomId);
        if (chatRoomOptional.isPresent()) {
            ChatRoom chatRoom = chatRoomOptional.get();
            
            // Verify user is the one who blocked
            if (!userId.equals(chatRoom.getBlockBy())) {
                throw new IllegalArgumentException("Only the user who blocked can unblock");
            }
            
            chatRoom.setIsBlocked(false);
            chatRoom.setBlockBy(null);
            chatRoom = chatRoomRepository.save(chatRoom);
            
            // Notify the other user
            Long otherUserId = chatRoom.getUser1Id().equals(userId) ? chatRoom.getUser2Id() : chatRoom.getUser1Id();
            Map<String, Object> unblockStatus = new HashMap<>();
            unblockStatus.put("chatRoomId", chatRoomId);
            unblockStatus.put("unblockedByUserId", userId);
            unblockStatus.put("timestamp", LocalDateTime.now());
            messagingTemplate.convertAndSend("/topic/unblock-status/" + otherUserId, unblockStatus);
            
            return convertToChatRoomResponse(chatRoom, userId);
        }
        return null;
    }

    /**
     * Helper method to convert ChatRoom entity to ChatRoomResponse DTO
     */
    private ChatRoomResponse convertToChatRoomResponse(ChatRoom chatRoom, Long currentUserId) {
        ChatRoomResponse response = new ChatRoomResponse();
        response.setId(chatRoom.getId());
        response.setUser1Id(chatRoom.getUser1Id());
        response.setUser2Id(chatRoom.getUser2Id());
        response.setCreatedAt(chatRoom.getCreatedAt());
        response.setIsBlocked(chatRoom.getIsBlocked());
        response.setBlockBy(chatRoom.getBlockBy());
        response.setReportBy(chatRoom.getReportBy());
        
        // Get user profiles for display names and profile pics
        Optional<UserProfile> user1Profile = userProfileRepository.findById(chatRoom.getUser1Id());
        Optional<UserProfile> user2Profile = userProfileRepository.findById(chatRoom.getUser2Id());
        
        user1Profile.ifPresent(profile -> {
            response.setUser1Name(profile.getDisplayName());
            response.setUser1ProfilePic(profile.getProfilePictureUrl());
        });
        
        user2Profile.ifPresent(profile -> {
            response.setUser2Name(profile.getDisplayName());
            response.setUser2ProfilePic(profile.getProfilePictureUrl());
        });
        
        // Get the latest message
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoom.getId());
        if (!messages.isEmpty()) {
            ChatMessage latestMessage = messages.get(messages.size() - 1);
            response.setLastMessage(latestMessage.getContent());
            response.setLastMessageType(latestMessage.getType());
            response.setLastMessageTime(latestMessage.getCreatedAt());
        }
        
        // Count unread messages for current user
        long unreadCount = chatMessageRepository.countUnreadMessagesForUser(chatRoom.getId(), currentUserId);
        response.setUnreadCount(unreadCount);
        
        return response;
    }    /**
     * Helper method to convert ChatMessage entity to ChatMessageResponse DTO
     */
    private ChatMessageResponse convertToChatMessageResponse(ChatMessage chatMessage) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(chatMessage.getId());
        response.setChatRoomId(chatMessage.getChatRoomId());
        response.setSenderId(chatMessage.getSenderId());
        response.setContent(chatMessage.getContent());
        response.setType(chatMessage.getType());
        response.setCreatedAt(chatMessage.getCreatedAt());        response.setUpdatedAt(chatMessage.getUpdatedAt());
        response.setIsRead(chatMessage.getIsRead());
        response.setIsEdited(chatMessage.getIsEdited());
        response.setOriginalContent(chatMessage.getOriginalContent());
        
        // Get sender profile for display name and profile pic
        Optional<UserProfile> senderProfile = userProfileRepository.findById(chatMessage.getSenderId());
        senderProfile.ifPresent(profile -> {
            response.setSenderName(profile.getDisplayName());
            response.setSenderProfilePic(profile.getProfilePictureUrl());
        });
        
        return response;
    }

    /**
     * Helper method to notify via WebSocket about new message
     */
    private void notifyMessageReceived(ChatMessage chatMessage) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setChatRoomId(chatMessage.getChatRoomId());
        dto.setSenderId(chatMessage.getSenderId());
        dto.setContent(chatMessage.getContent());
        dto.setType(chatMessage.getType());
        
        // Send to the chat room's specific channel
        messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getChatRoomId(), dto);
        
        // Find the recipient to send to their personal channel
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatMessage.getChatRoomId());
        if (roomOpt.isPresent()) {
            ChatRoom room = roomOpt.get();
            Long recipientId = room.getUser1Id().equals(chatMessage.getSenderId()) ? 
                    room.getUser2Id() : room.getUser1Id();
            
            // Send to recipient's personal channel for notifications
            messagingTemplate.convertAndSend("/topic/notifications/" + recipientId, dto);
        }
    }
    
    // 🔥 Update message method
    @Transactional
    public ChatMessageResponse updateMessage(Long messageId, String newContent, Long userId) {
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            return null;
        }
        
        ChatMessage message = messageOpt.get();
        
        // Verify that the user is the sender of this message
        if (!message.getSenderId().equals(userId)) {
            return null;
        }        // Save original content if this is the first edit
        if (!message.getIsEdited() && message.getOriginalContent() == null) {
            message.setOriginalContent(message.getContent());
        }
        
        // Update the message content
        message.setContent(newContent);
        message.setUpdatedAt(LocalDateTime.now());
        message.setIsEdited(true);
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Notify via WebSocket about message update
        notifyMessageUpdated(savedMessage);
        
        return convertToChatMessageResponse(savedMessage);
    }
    
    // 🔥 Delete message method
    @Transactional
    public boolean deleteMessage(Long messageId, Long userId) {
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            return false;
        }
        
        ChatMessage message = messageOpt.get();
        
        // Verify that the user is the sender of this message
        if (!message.getSenderId().equals(userId)) {
            return false;
        }
        
        // Delete the message
        chatMessageRepository.delete(message);
        
        // Notify via WebSocket about message deletion
        notifyMessageDeleted(messageId, message.getChatRoomId());
        
        return true;
    }
    
    // 🔥 Helper method to notify about message update
    private void notifyMessageUpdated(ChatMessage chatMessage) {
        Map<String, Object> updateNotification = new HashMap<>();
        updateNotification.put("type", "MESSAGE_UPDATED");
        updateNotification.put("messageId", chatMessage.getId());
        updateNotification.put("chatRoomId", chatMessage.getChatRoomId());
        updateNotification.put("newContent", chatMessage.getContent());
        updateNotification.put("timestamp", LocalDateTime.now());
        
        // Send to the chat room's channel
        messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getChatRoomId(), updateNotification);
    }
    
    // 🔥 Helper method to notify about message deletion
    private void notifyMessageDeleted(Long messageId, Long chatRoomId) {
        Map<String, Object> deleteNotification = new HashMap<>();
        deleteNotification.put("type", "MESSAGE_DELETED");
        deleteNotification.put("messageId", messageId);
        deleteNotification.put("chatRoomId", chatRoomId);
        deleteNotification.put("timestamp", LocalDateTime.now());
        
        // Send to the chat room's channel
        messagingTemplate.convertAndSend("/topic/messages/" + chatRoomId, deleteNotification);
    }
}
