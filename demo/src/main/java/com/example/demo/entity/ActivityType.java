package com.example.demo.entity;

public enum ActivityType {
    VIEW, SEARCH, INTERACT, FAVORITE,
    // Chat related activity types
    CHAT_MESSAGE_SENT, 
    CHAT_MESSAGE_READ,
    CHAT_ROOM_CREATED,
    CHAT_ROOM_BLOCKED,
    CHAT_ROOM_REPORTED
}