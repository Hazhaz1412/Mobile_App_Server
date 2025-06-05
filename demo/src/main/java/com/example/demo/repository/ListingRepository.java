package com.example.demo.repository;

import com.example.demo.entity.Listing;
import com.example.demo.entity.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    
    // Basic queries
    Page<Listing> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ListingStatus status, Pageable pageable);
    Page<Listing> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, ListingStatus status, Pageable pageable);
    Page<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status, Pageable pageable);
    Page<Listing> findByCategoryIdAndStatusOrderByCreatedAtDesc(Long categoryId, ListingStatus status, Pageable pageable);
    Optional<Listing> findByIdAndUserId(Long id, Long userId);
    
    // Advanced search with multiple criteria
    @Query("SELECT l FROM Listing l WHERE l.status = :status " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "    LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "    LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR l.categoryId = :categoryId) " +
           "AND (:conditionId IS NULL OR l.conditionId = :conditionId) " +
           "AND (:minPrice IS NULL OR l.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR l.price <= :maxPrice)")
    Page<Listing> searchWithCriteria(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("conditionId") Long conditionId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("status") ListingStatus status,
        Pageable pageable
    );
    
    // Search by distance using Haversine formula
    @Query("SELECT l FROM Listing l WHERE l.status = :status " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "    LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "    LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR l.categoryId = :categoryId) " +
           "AND (:conditionId IS NULL OR l.conditionId = :conditionId) " +
           "AND (:minPrice IS NULL OR l.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR l.price <= :maxPrice) " +
           "AND (l.latitude IS NOT NULL AND l.longitude IS NOT NULL) " +
           "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) * " +
           "    cos(radians(l.longitude) - radians(:longitude)) + " +
           "    sin(radians(:latitude)) * sin(radians(l.latitude)))) <= :maxDistance")
    Page<Listing> searchWithDistance(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("conditionId") Long conditionId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("latitude") BigDecimal latitude,
        @Param("longitude") BigDecimal longitude,
        @Param("maxDistance") Double maxDistance,
        @Param("status") ListingStatus status,
        Pageable pageable
    );
    
    // Get popular listings (high view count and interactions)
    @Query("SELECT l FROM Listing l WHERE l.status = :status " +
           "ORDER BY (l.viewCount + l.interactionCount * 2) DESC")
    Page<Listing> findPopularListings(@Param("status") ListingStatus status, Pageable pageable);
    
    // Get listings by category with popularity
    @Query("SELECT l FROM Listing l WHERE l.status = :status AND l.categoryId = :categoryId " +
           "ORDER BY (l.viewCount + l.interactionCount * 2) DESC")
    Page<Listing> findPopularListingsByCategory(
        @Param("categoryId") Long categoryId,
        @Param("status") ListingStatus status,
        Pageable pageable
    );
    
    // Get listings by user preferences (categories they viewed most)
    @Query("SELECT l FROM Listing l WHERE l.status = :status " +
           "AND l.categoryId IN :preferredCategories " +
           "ORDER BY l.createdAt DESC")
    Page<Listing> findByPreferredCategories(
        @Param("preferredCategories") List<Long> preferredCategories,
        @Param("status") ListingStatus status,
        Pageable pageable
    );
    
    // Get nearby listings
    @Query("SELECT l FROM Listing l WHERE l.status = :status " +
           "AND (l.latitude IS NOT NULL AND l.longitude IS NOT NULL) " +
           "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) * " +
           "    cos(radians(l.longitude) - radians(:longitude)) + " +
           "    sin(radians(:latitude)) * sin(radians(l.latitude)))) <= :maxDistance " +
           "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) * " +
           "    cos(radians(l.longitude) - radians(:longitude)) + " +
           "    sin(radians(:latitude)) * sin(radians(l.latitude)))) ASC")
    Page<Listing> findNearbyListings(
        @Param("latitude") BigDecimal latitude,
        @Param("longitude") BigDecimal longitude,
        @Param("maxDistance") Double maxDistance,
        @Param("status") ListingStatus status,
        Pageable pageable
    );
}