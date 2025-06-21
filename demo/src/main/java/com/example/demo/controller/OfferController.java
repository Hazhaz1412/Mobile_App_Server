package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.Offer;
import com.example.demo.entity.OfferStatus;
import com.example.demo.repository.OfferRepository;
import com.example.demo.service.OfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/offers")
@CrossOrigin(origins = "*")
public class OfferController {
      @Autowired
    private OfferService offerService;
    
    @Autowired
    private OfferRepository offerRepository;
    
    /**
     * Create a new offer
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createOffer(
            @RequestParam Long buyerId,
            @Valid @RequestBody CreateOfferRequest request) {
        try {
            OfferResponse offer = offerService.createOffer(buyerId, request);
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Offer được tạo thành công!",
                offer
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    /**
     * Respond to an offer (accept, reject, counter)
     */
    @PostMapping("/{offerId}/respond")
    public ResponseEntity<ApiResponse> respondToOffer(
            @PathVariable Long offerId,
            @RequestParam Long sellerId,
            @Valid @RequestBody RespondToOfferRequest request) {
        try {
            OfferResponse offer = offerService.respondToOffer(offerId, sellerId, request);
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Phản hồi offer thành công!",
                offer
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    /**
     * Withdraw an offer
     */
    @PostMapping("/{offerId}/withdraw")
    public ResponseEntity<ApiResponse> withdrawOffer(
            @PathVariable Long offerId,
            @RequestParam Long buyerId) {
        try {
            OfferResponse offer = offerService.withdrawOffer(offerId, buyerId);
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Rút offer thành công!",
                offer
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    /**
     * Get offers for a specific listing
     */
    @GetMapping("/listing/{listingId}")
    public ResponseEntity<PagedApiResponse<OfferResponse>> getOffersForListing(
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<OfferResponse> offers = offerService.getOffersForListing(listingId, pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lấy danh sách offers thành công!",
                offers.getContent(),
                offers.getNumber(),
                offers.getSize(),
                offers.getTotalElements(),
                offers.getTotalPages()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new PagedApiResponse<>(
                false,
                e.getMessage(),
                null,
                0, 0, 0L, 0
            ));
        }
    }
    
    /**
     * Get active (purchasable) offers for a specific listing - for browsing
     */
    @GetMapping("/listing/{listingId}/active")
    public ResponseEntity<PagedApiResponse<OfferResponse>> getActiveOffersForListing(
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<OfferResponse> offers = offerService.getActiveOffersForListing(listingId, pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lấy danh sách offers khả dụng thành công!",
                offers.getContent(),
                offers.getNumber(),
                offers.getSize(),
                offers.getTotalElements(),
                offers.getTotalPages()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new PagedApiResponse<>(
                false,
                e.getMessage(),
                null,
                0, 0, 0L, 0
            ));
        }
    }
    
    /**
     * Check if listing is available for purchase
     */
    @GetMapping("/listing/{listingId}/available")
    public ResponseEntity<Map<String, Object>> checkListingAvailability(@PathVariable Long listingId) {
        try {
            boolean available = offerService.isListingAvailableForPurchase(listingId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "available", available,
                "message", available ? "Sản phẩm còn khả dụng" : "Sản phẩm đã được bán"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Check specific offer status and purchasability
     */
    @GetMapping("/{offerId}/status")
    public ResponseEntity<Map<String, Object>> checkOfferStatus(@PathVariable Long offerId) {
        try {
            Optional<Offer> offerOpt = offerRepository.findById(offerId);
            if (offerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Offer offer = offerOpt.get();
            boolean canPurchase = offer.getStatus() != OfferStatus.COMPLETED;
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "offer_id", offerId,
                "status", offer.getStatus().toString(),
                "can_purchase", canPurchase,
                "message", canPurchase ? "Có thể mua" : "Đã được mua"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get offer details with updated status
     */
    @GetMapping("/{offerId}/details")
    public ResponseEntity<Map<String, Object>> getOfferDetails(@PathVariable Long offerId) {
        try {
            Optional<Offer> offerOpt = offerRepository.findById(offerId);
            if (offerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Offer offer = offerOpt.get();
            boolean canPurchase = offer.getStatus() != OfferStatus.COMPLETED;
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "offer", Map.of(
                    "id", offer.getId(),
                    "listing_id", offer.getListingId(),
                    "buyer_id", offer.getBuyerId(),
                    "seller_id", offer.getSellerId(),
                    "offer_amount", offer.getOfferAmount(),
                    "status", offer.getStatus().toString(),
                    "message", offer.getMessage(),
                    "created_at", offer.getCreatedAt(),
                    "updated_at", offer.getUpdatedAt()
                ),
                "can_purchase", canPurchase,
                "ui_message", canPurchase ? "MUA NGAY" : "ĐÃ BÁN"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get offers made by a buyer
     */
    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<PagedApiResponse<OfferResponse>> getOffersByBuyer(
            @PathVariable Long buyerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<OfferResponse> offers = offerService.getOffersByBuyer(buyerId, pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lấy danh sách offers của buyer thành công!",
                offers.getContent(),
                offers.getNumber(),
                offers.getSize(),
                offers.getTotalElements(),
                offers.getTotalPages()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new PagedApiResponse<>(
                false,
                e.getMessage(),
                null, 0, 0, 0, 0
            ));
        }
    }
    
    /**
     * Get offers received by a seller
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<PagedApiResponse<OfferResponse>> getOffersBySeller(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<OfferResponse> offers = offerService.getOffersBySeller(sellerId, pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lấy danh sách offers của seller thành công!",
                offers.getContent(),
                offers.getNumber(),
                offers.getSize(),
                offers.getTotalElements(),
                offers.getTotalPages()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new PagedApiResponse<>(
                false,
                e.getMessage(),
                null, 0, 0, 0, 0
            ));
        }
    }
    
    /**
     * Get pending offers count for a seller
     */
    @GetMapping("/seller/{sellerId}/pending-count")
    public ResponseEntity<ApiResponse> getPendingOffersCount(@PathVariable Long sellerId) {
        try {
            long count = offerService.getPendingOffersCount(sellerId);
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Lấy số lượng pending offers thành công!",
                count
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    /**
     * Get offer details by ID
     */
    @GetMapping("/{offerId}")
    public ResponseEntity<ApiResponse> getOfferById(@PathVariable Long offerId) {
        try {
            // This would require a getOfferById method in OfferService
            // For now, we'll return a simple response
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Chi tiết offer",
                null // TODO: Implement getOfferById in service
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    /**
     * DEBUG: Manually mark an offer as completed (for testing)
     */
    @PostMapping("/{offerId}/debug/mark-completed")
    public ResponseEntity<Map<String, Object>> debugMarkOfferCompleted(@PathVariable Long offerId) {
        try {
            Optional<Offer> offerOpt = offerRepository.findById(offerId);
            if (offerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Offer offer = offerOpt.get();
            String oldStatus = offer.getStatus().toString();
              // Mark offer as completed
            offer.setStatus(OfferStatus.COMPLETED);
            offer.setHasPaidTransaction(true);
            offer.setUpdatedAt(java.time.LocalDateTime.now());
            offerRepository.save(offer);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Offer marked as completed",
                "offer_id", offerId,
                "old_status", oldStatus,
                "new_status", "COMPLETED"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * DEBUG: Reset offer status for testing
     */
    @PostMapping("/{offerId}/debug/reset-status")
    public ResponseEntity<Map<String, Object>> debugResetOfferStatus(
            @PathVariable Long offerId,
            @RequestParam String status) {
        try {
            Optional<Offer> offerOpt = offerRepository.findById(offerId);
            if (offerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Offer offer = offerOpt.get();
            String oldStatus = offer.getStatus().toString();
            
            // Set new status
            OfferStatus newStatus = OfferStatus.valueOf(status.toUpperCase());
            offer.setStatus(newStatus);
            offer.setUpdatedAt(java.time.LocalDateTime.now());
            offerRepository.save(offer);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Offer status reset",
                "offer_id", offerId,
                "old_status", oldStatus,
                "new_status", status.toUpperCase()
            ));
        } catch (Exception e) {            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * DEBUG: Manually complete an offer (for testing)
     */
    @PostMapping("/debug/complete/{offerId}")
    public ResponseEntity<Map<String, Object>> debugCompleteOffer(@PathVariable Long offerId) {
        try {
            Optional<Offer> offerOpt = offerRepository.findById(offerId);
            if (offerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Offer offer = offerOpt.get();
            String oldStatus = offer.getStatus().toString();
            
            // Update offer status to COMPLETED
            offer.setStatus(OfferStatus.COMPLETED);
            offer.setUpdatedAt(java.time.LocalDateTime.now());
            offerRepository.save(offer);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "offerId", offerId,
                "oldStatus", oldStatus,
                "newStatus", "COMPLETED",
                "message", "Offer manually completed"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
