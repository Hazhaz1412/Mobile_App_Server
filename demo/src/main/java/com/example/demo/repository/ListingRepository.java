package com.example.demo.repository;

import com.example.demo.entity.Listing;
import com.example.demo.entity.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    
    // Find by user and status
    Page<Listing> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ListingStatus status, Pageable pageable);
    
    // Find by user (all statuses except DELETED)
    Page<Listing> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, ListingStatus status, Pageable pageable);
    
    // Find available listings
    Page<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status, Pageable pageable);
    
    // Find by category
    Page<Listing> findByCategoryIdAndStatusOrderByCreatedAtDesc(Long categoryId, ListingStatus status, Pageable pageable);
    
    // Search listings
    @Query("SELECT l FROM Listing l WHERE l.status = :status AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY l.createdAt DESC")
    Page<Listing> searchByKeyword(@Param("keyword") String keyword, 
                                 @Param("status") ListingStatus status, 
                                 Pageable pageable);
    
    // Find by user and id (for security check)
    Optional<Listing> findByIdAndUserId(Long id, Long userId);
    
    // Count user listings
    long countByUserIdAndStatus(Long userId, ListingStatus status);
}