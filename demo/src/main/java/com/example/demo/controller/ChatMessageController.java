package com.example.demo.controller;

import com.example.demo.entity.ChatMessage;
import com.example.demo.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat-messages")
public class ChatMessageController {
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @GetMapping("/room/{roomId}")
    public List<ChatMessage> getMessages(@PathVariable Long roomId) {
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId);
    }
}
