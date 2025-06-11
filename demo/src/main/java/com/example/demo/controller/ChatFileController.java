package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.service.FileStorageService;

@RestController
@RequestMapping("/api/chat-files")
public class ChatFileController {
    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadChatImage(@RequestParam("file") MultipartFile file) {
        // Validate and upload image
        String url = fileStorageService.uploadImage(file, "chat");
        return ResponseEntity.ok(url);
    }
}
