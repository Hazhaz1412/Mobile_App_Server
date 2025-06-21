package com.example.demo.controller;

import com.example.demo.entity.Offer;
import com.example.demo.entity.OfferStatus;
import com.example.demo.entity.Listing;
import com.example.demo.entity.ListingStatus;
import com.example.demo.repository.OfferRepository;
import com.example.demo.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")
public class DebugController {
    
    @Autowired
    private OfferRepository offerRepository;
    
    @Autowired
    private ListingRepository listingRepository;
    
    @PostMapping("/fix-offer/{offerId}")
    public ResponseEntity<Map<String, Object>> fixOfferStatus(@PathVariable Long offerId) {
        try {
            // Get the offer
            Optional<Offer> offerOpt = offerRepository.findById(offerId);
            if (offerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Offer offer = offerOpt.get();
            String oldStatus = offer.getStatus().toString();
            
            // Update offer status to COMPLETED
            offer.setStatus(OfferStatus.COMPLETED);
            offer.setUpdatedAt(LocalDateTime.now());
            offerRepository.save(offer);
            
            // Update listing status to SOLD
            Optional<Listing> listingOpt = listingRepository.findById(offer.getListingId());
            String listingOldStatus = "NOT_FOUND";
            if (listingOpt.isPresent()) {
                Listing listing = listingOpt.get();
                listingOldStatus = listing.getStatus().toString();
                listing.setStatus(ListingStatus.SOLD);
                listing.setUpdatedAt(LocalDateTime.now());
                listingRepository.save(listing);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "offerId", offerId,
                "offerStatus", Map.of("old", oldStatus, "new", "COMPLETED"),
                "listingStatus", Map.of("old", listingOldStatus, "new", "SOLD")
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @GetMapping("/offer-status/{offerId}")
    public ResponseEntity<Map<String, Object>> getOfferStatus(@PathVariable Long offerId) {
        try {
            Optional<Offer> offerOpt = offerRepository.findById(offerId);
            if (offerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Offer offer = offerOpt.get();
            return ResponseEntity.ok(Map.of(
                "offerId", offerId,
                "status", offer.getStatus().toString(),
                "listingId", offer.getListingId(),
                "updatedAt", offer.getUpdatedAt()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
