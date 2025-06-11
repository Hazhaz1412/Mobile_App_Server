package com.example.demo.controller;

import com.example.demo.entity.ChatRoom;
import com.example.demo.repository.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat-rooms")
public class ChatRoomController {
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @GetMapping("/user/{userId}")
    public List<ChatRoom> getUserChatRooms(@PathVariable Long userId) {
        return chatRoomRepository.findByUser1IdOrUser2Id(userId, userId);
    }

    @PostMapping
    public ChatRoom createRoom(@RequestBody ChatRoom room) {
        return chatRoomRepository.save(room);
    }

    @PostMapping("/{roomId}/block")
    public ResponseEntity<?> blockRoom(@PathVariable Long roomId, @RequestParam Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        room.setIsBlocked(true);
        room.setBlockBy(userId);
        chatRoomRepository.save(room);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/report")
    public ResponseEntity<?> reportRoom(@PathVariable Long roomId, @RequestParam Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        room.setReportBy(userId);
        chatRoomRepository.save(room);
        return ResponseEntity.ok().build();
    }
}
