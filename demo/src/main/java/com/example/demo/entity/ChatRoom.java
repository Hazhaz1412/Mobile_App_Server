package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long user1Id;

    @Column(nullable = false)
    private Long user2Id;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean isBlocked = false;

    @Column
    private Long blockBy; // userId

    @Column
    private Long reportBy; // userId

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUser1Id() { return user1Id; }
    public void setUser1Id(Long user1Id) { this.user1Id = user1Id; }
    public Long getUser2Id() { return user2Id; }
    public void setUser2Id(Long user2Id) { this.user2Id = user2Id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getIsBlocked() { return isBlocked; }
    public void setIsBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; }
    public Long getBlockBy() { return blockBy; }
    public void setBlockBy(Long blockBy) { this.blockBy = blockBy; }
    public Long getReportBy() { return reportBy; }
    public void setReportBy(Long reportBy) { this.reportBy = reportBy; }
}
