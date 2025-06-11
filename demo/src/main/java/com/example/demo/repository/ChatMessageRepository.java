package com.example.demo.repository;

import com.example.demo.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // Get all messages in a chat room ordered by time
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);
    
    // Get all unread messages in a chat room
    List<ChatMessage> findByChatRoomIdAndIsReadFalse(Long chatRoomId);
    
    // Get unread messages sent by a specific user in a chat room
    List<ChatMessage> findByChatRoomIdAndSenderIdAndIsReadFalse(Long chatRoomId, Long senderId);
    
    // Count unread messages in a chat room
    long countByChatRoomIdAndIsReadFalse(Long chatRoomId);
    
    // Count unread messages for a specific recipient
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chatRoomId = :chatRoomId AND cm.senderId != :userId AND cm.isRead = false")
    long countUnreadMessagesForUser(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
    
    // Mark all messages in a room as read for a recipient
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.isRead = true WHERE cm.chatRoomId = :chatRoomId AND cm.senderId != :userId AND cm.isRead = false")
    int markAllMessagesAsRead(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
    
    // Get the latest message from each chat room
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.id IN (SELECT MAX(cm2.id) FROM ChatMessage cm2 GROUP BY cm2.chatRoomId) AND cm.chatRoomId IN :chatRoomIds")
    List<ChatMessage> findLatestMessagesByRoomIds(@Param("chatRoomIds") List<Long> chatRoomIds);
}
