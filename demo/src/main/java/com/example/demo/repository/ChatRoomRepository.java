package com.example.demo.repository;

import com.example.demo.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);
    
    // Find user's chat rooms
    List<ChatRoom> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);
    
    // Find all active (not blocked) chat rooms for a user
    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.user1Id = :userId OR cr.user2Id = :userId) AND cr.isBlocked = false")
    List<ChatRoom> findActiveRoomsByUserId(@Param("userId") Long userId);
    
    // Alternative lookup method for finding a room between two users
    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.user1Id = :user1Id AND cr.user2Id = :user2Id) OR (cr.user1Id = :user2Id AND cr.user2Id = :user1Id)")
    Optional<ChatRoom> findRoomByUserIds(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
