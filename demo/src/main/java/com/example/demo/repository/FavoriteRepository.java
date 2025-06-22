package com.example.demo.repository;

import com.example.demo.entity.Favorite;
import com.example.demo.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    /**
     * Find favorite by user and listing
     */
    Optional<Favorite> findByUserIdAndListingId(Long userId, Long listingId);
    
    /**
     * Check if a listing is favorited by a user
     */
    boolean existsByUserIdAndListingId(Long userId, Long listingId);
    
    /**
     * Get all favorites for a user
     */
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Get paginated favorites for a user
     */
    Page<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Count favorites for a user
     */
    long countByUserId(Long userId);
    
    /**
     * Count favorites for a listing
     */
    long countByListingId(Long listingId);
    
    /**
     * Get all users who favorited a listing
     */
    List<Favorite> findByListingIdOrderByCreatedAtDesc(Long listingId);
    
    /**
     * Delete favorite by user and listing
     */
    void deleteByUserIdAndListingId(Long userId, Long listingId);
    
    /**
     * Get favorite listings with full listing details (JOIN query)
     */
    @Query("SELECT f.listing FROM Favorite f " +
           "WHERE f.userId = :userId " +
           "AND f.listing.status = 'AVAILABLE' " +
           "ORDER BY f.createdAt DESC")
    List<Listing> findFavoriteListingsByUserId(@Param("userId") Long userId);
    
    /**
     * Get paginated favorite listings with full listing details
     */
    @Query("SELECT f.listing FROM Favorite f " +
           "WHERE f.userId = :userId " +
           "AND f.listing.status = 'AVAILABLE' " +
           "ORDER BY f.createdAt DESC")
    Page<Listing> findFavoriteListingsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Get favorite listings with all statuses (for admin/debug)
     */
    @Query("SELECT f.listing FROM Favorite f " +
           "WHERE f.userId = :userId " +
           "ORDER BY f.createdAt DESC")
    List<Listing> findAllFavoriteListingsByUserId(@Param("userId") Long userId);
    
    /**
     * Delete all favorites for a user (for cleanup)
     */
    void deleteByUserId(Long userId);
    
    /**
     * Delete all favorites for a listing (for cleanup when listing is deleted)
     */
    void deleteByListingId(Long listingId);
}
