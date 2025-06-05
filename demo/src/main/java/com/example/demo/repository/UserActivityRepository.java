package com.example.demo.repository;

import com.example.demo.entity.ActivityType;
import com.example.demo.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    
    // Get user's most viewed categories
    @Query("SELECT l.categoryId, COUNT(*) as count FROM UserActivity ua " +
           "JOIN Listing l ON ua.listingId = l.id " +
           "WHERE ua.userId = :userId AND ua.activityType = :activityType " +
           "AND ua.createdAt >= :since " +
           "GROUP BY l.categoryId ORDER BY count DESC")
    List<Object[]> findMostViewedCategories(
        @Param("userId") Long userId,
        @Param("activityType") ActivityType activityType,
        @Param("since") LocalDateTime since
    );
    
    // Check if user has viewed a listing recently
    boolean existsByUserIdAndListingIdAndActivityTypeAndCreatedAtAfter(
        Long userId, Long listingId, ActivityType activityType, LocalDateTime since
    );
}