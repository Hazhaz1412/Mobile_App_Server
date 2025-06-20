package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.OfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/offers")
@CrossOrigin(origins = "*")
public class OfferController {
    
    @Autowired
    private OfferService offerService;
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
                null, 0, 0, 0, 0
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
}
