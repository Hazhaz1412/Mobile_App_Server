package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ChatMessageRequest;
import com.example.demo.dto.ChatMessageResponse;
import com.example.demo.dto.ChatRoomResponse;
import com.example.demo.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ChatController {
    @Autowired
    private ChatService chatService;
      /**
     * Verify that the authenticated user has access to a chat room
     */
    private boolean verifyUserAccess(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("=== verifyUserAccess DEBUG ===");
        System.out.println("Requested userId: " + userId);
        System.out.println("Authentication: " + authentication);
        
        if (authentication != null && authentication.isAuthenticated()) {
            System.out.println("Authentication is not null and is authenticated");
            // Get the user ID from the authentication object (set in JwtAuthenticationFilter)
            Object principal = authentication.getPrincipal();
            System.out.println("Principal: " + principal);
            System.out.println("Principal class: " + (principal != null ? principal.getClass().getName() : "null"));
            
            if (principal instanceof Long) {
                Long authenticatedUserId = (Long) principal;
                System.out.println("Authenticated userId: " + authenticatedUserId);
                boolean accessGranted = authenticatedUserId.equals(userId);
                System.out.println("Access granted: " + accessGranted);
                System.out.println("=== END DEBUG ===");
                return accessGranted;
            } else {
                System.out.println("Principal is not a Long instance");
            }
        } else {
            System.out.println("Authentication is null or not authenticated");
        }
        System.out.println("=== END DEBUG ===");
        return false;
    }

    /**
     * Create a new chat room between two users
     */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> createChatRoom(@RequestBody Map<String, Long> request) {
        Long user1Id = request.get("user1Id");
        Long user2Id = request.get("user2Id");
        
        if (user1Id == null || user2Id == null) {
            return ResponseEntity.badRequest().build();
        }
        
        ChatRoomResponse chatRoom = chatService.createChatRoom(user1Id, user2Id);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatRoom);
    }    /**
     * Get all chat rooms for a user
     */
    @GetMapping("/rooms/user/{userId}")
    public ResponseEntity<?> getUserChatRooms(@PathVariable Long userId) {
        // Verify that the authenticated user is requesting their own chat rooms
        if (!verifyUserAccess(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, "Access denied", null));
        }
        
        List<ChatRoomResponse> chatRooms = chatService.getUserChatRooms(userId);
        return ResponseEntity.ok(chatRooms);
    }    /**
     * Get a specific chat room by ID
     */
    @GetMapping("/rooms/{roomId}/user/{userId}")
    public ResponseEntity<?> getChatRoomById(
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        
        // Verify that the authenticated user is requesting their own chat room
        if (!verifyUserAccess(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, "Access denied", null));
        }
        
        ChatRoomResponse chatRoom = chatService.getChatRoomById(roomId, userId);
        if (chatRoom == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(chatRoom);
    }    /**
     * Get all messages in a chat room
     */
    @GetMapping("/messages/{chatRoomId}/user/{userId}")
    public ResponseEntity<?> getChatMessages(
            @PathVariable Long chatRoomId,
            @PathVariable Long userId) {
        
        // Verify that the authenticated user is requesting their own messages
        if (!verifyUserAccess(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, "Access denied", null));
        }
        
        List<ChatMessageResponse> messages = chatService.getChatMessages(chatRoomId, userId);
        return ResponseEntity.ok(messages);
    }    /**
     * Send a text message
     */
    @PostMapping("/messages")
    public ResponseEntity<?> sendTextMessage(@RequestBody ChatMessageRequest request) {
        if (request.getChatRoomId() == null || request.getSenderId() == null || request.getContent() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Missing required fields", null));
        }
        
        // Verify that the authenticated user is sending the message
        if (!verifyUserAccess(request.getSenderId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, "Access denied", null));
        }
        
        ChatMessageResponse message = chatService.sendTextMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }    /**
     * Send an image message
     */
    @PostMapping("/messages/image")
    public ResponseEntity<?> sendImageMessage(
            @RequestParam("chatRoomId") Long chatRoomId,
            @RequestParam("senderId") Long senderId,
            @RequestParam("imageFile") MultipartFile imageFile) {
        if (chatRoomId == null || senderId == null || imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Missing required fields", null));
        }
        
        // Verify that the authenticated user is sending the message
        if (!verifyUserAccess(senderId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, "Access denied", null));
        }
        
        ChatMessageResponse message = chatService.sendImageMessage(chatRoomId, senderId, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /**
     * Mark messages as read
     */
    @PutMapping("/messages/read/{chatRoomId}/user/{userId}")
    public ResponseEntity<Map<String, Integer>> markMessagesAsRead(
            @PathVariable Long chatRoomId,
            @PathVariable Long userId) {
        int updatedCount = chatService.markMessagesAsRead(chatRoomId, userId);
        return ResponseEntity.ok(Map.of("updatedCount", updatedCount));
    }

    /**
     * Block a chat room
     */
    @PutMapping("/rooms/{chatRoomId}/block/{userId}")
    public ResponseEntity<ChatRoomResponse> blockChatRoom(
            @PathVariable Long chatRoomId,
            @PathVariable Long userId) {
        ChatRoomResponse chatRoom = chatService.blockChatRoom(chatRoomId, userId);
        if (chatRoom == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(chatRoom);
    }

    /**
     * Report a chat room
     */
    @PutMapping("/rooms/{chatRoomId}/report/{userId}")
    public ResponseEntity<ChatRoomResponse> reportChatRoom(
            @PathVariable Long chatRoomId,
            @PathVariable Long userId) {
        ChatRoomResponse chatRoom = chatService.reportChatRoom(chatRoomId, userId);
        if (chatRoom == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(chatRoom);
    }

    /**
     * Unblock a chat room
     */
    @PutMapping("/rooms/{chatRoomId}/unblock/{userId}")
    public ResponseEntity<ChatRoomResponse> unblockChatRoom(
            @PathVariable Long chatRoomId,
            @PathVariable Long userId) {
        ChatRoomResponse chatRoom = chatService.unblockChatRoom(chatRoomId, userId);
        if (chatRoom == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(chatRoom);
    }
}
