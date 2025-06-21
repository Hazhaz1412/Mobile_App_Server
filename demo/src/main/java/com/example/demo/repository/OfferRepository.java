package com.example.demo.repository;

import com.example.demo.entity.Offer;
import com.example.demo.entity.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    
    // Find offers for a specific listing
    Page<Offer> findByListingIdOrderByCreatedAtDesc(Long listingId, Pageable pageable);
    
    // Find offers made by a buyer
    Page<Offer> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);
    
    // Find offers received by a seller
    Page<Offer> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);
    
    // Find offers by status
    Page<Offer> findByStatusOrderByCreatedAtDesc(OfferStatus status, Pageable pageable);
    
    // Find offers by listing and buyer (to prevent duplicate offers)
    Optional<Offer> findByListingIdAndBuyerIdAndStatus(Long listingId, Long buyerId, OfferStatus status);
    
    // Find pending offers for a listing
    List<Offer> findByListingIdAndStatus(Long listingId, OfferStatus status);
    
    // Find offers by buyer and status
    Page<Offer> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, OfferStatus status, Pageable pageable);
    
    // Find offers by seller and status
    Page<Offer> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, OfferStatus status, Pageable pageable);
    
    // Count pending offers for a seller
    @Query("SELECT COUNT(o) FROM Offer o WHERE o.sellerId = :sellerId AND o.status = 'PENDING'")
    long countPendingOffersBySeller(@Param("sellerId") Long sellerId);
    
    // Count pending offers for a buyer
    @Query("SELECT COUNT(o) FROM Offer o WHERE o.buyerId = :buyerId AND o.status = 'PENDING'")
    long countPendingOffersByBuyer(@Param("buyerId") Long buyerId);
    
    // Find expired offers that need to be updated
    @Query("SELECT o FROM Offer o WHERE o.status = 'PENDING' AND o.expiresAt < :currentTime")
    List<Offer> findExpiredOffers(@Param("currentTime") LocalDateTime currentTime);
    
    // Check if buyer has any pending offer for a listing
    boolean existsByListingIdAndBuyerIdAndStatus(Long listingId, Long buyerId, OfferStatus status);
    
    // Get offer statistics for a listing
    @Query("SELECT COUNT(o) FROM Offer o WHERE o.listingId = :listingId")
    long countOffersByListing(@Param("listingId") Long listingId);
    
    // Get average offer amount for a listing
    @Query("SELECT AVG(o.offerAmount) FROM Offer o WHERE o.listingId = :listingId")
    Double getAverageOfferAmountByListing(@Param("listingId") Long listingId);
    
    // Get highest offer for a listing
    @Query("SELECT MAX(o.offerAmount) FROM Offer o WHERE o.listingId = :listingId")
    Double getHighestOfferAmountByListing(@Param("listingId") Long listingId);
      // Check if listing has completed offer (already sold)
    @Query("SELECT COUNT(o) > 0 FROM Offer o WHERE o.listingId = :listingId AND o.status = 'COMPLETED'")
    boolean existsCompletedOfferByListingId(@Param("listingId") Long listingId);
    
    // Find offers for listing excluding specific status
    Page<Offer> findByListingIdAndStatusNotOrderByCreatedAtDesc(Long listingId, OfferStatus status, Pageable pageable);
}
