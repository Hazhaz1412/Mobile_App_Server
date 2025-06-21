package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.UserProfileUpdateRequest;
import com.example.demo.entity.UserProfile;
import com.example.demo.entity.Offer;
import com.example.demo.entity.OfferStatus;
import com.example.demo.repository.OfferRepository;
import com.example.demo.service.StorageService;
import com.example.demo.service.UserProfileService;
import com.example.demo.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.example.demo.dto.UserProfileResponse;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {    @Autowired
    private UserProfileService userProfileService;
    
    @Autowired
    private StorageService storageService;
      @Autowired
    private RatingService ratingService;
    
    @Autowired
    private OfferRepository offerRepository;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    @Value("${app.upload.dir:${user.home}/uploads}")
    private String uploadDir;
    
    @PostMapping("/{userId}/profile-image")
    public ResponseEntity<ApiResponse> uploadProfileImage(
            @PathVariable Long userId,
            @RequestParam("image") MultipartFile file) {
        
        try {
            UserProfile profile = userProfileService.updateProfilePicture(userId, file);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Profile image updated successfully",
                profile.getProfilePictureUrl()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Error uploading profile image: " + e.getMessage()
            ));
        }
    }

    @GetMapping(value = "/{userId}/profile-image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> getProfileImage(@PathVariable Long userId) {
        try {
            UserProfile profile = userProfileService.getUserProfile(userId);
            if (profile.getProfilePictureUrl() == null) {
                return ResponseEntity.notFound().build();
            }
            
            String imageUrl = profile.getProfilePictureUrl();
            String filename = Paths.get(imageUrl).getFileName().toString();
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "inline")
                .body(storageService.loadFileAsResource(filename));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        try {
            UserProfile profile = userProfileService.getUserProfile(userId);
            if (profile == null) {
                return ResponseEntity.notFound().build();
            }
            UserProfileResponse response = convertToResponse(profile);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Handle "User profile not found" specifically
            if (e.getMessage().contains("User profile not found")) {
                return ResponseEntity.notFound().build();
            }
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private UserProfileResponse convertToResponse(UserProfile profile) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(profile.getUserId());
        response.setDisplayName(profile.getDisplayName());
        
        // Xử lý an toàn nếu user là null
        if (profile.getUser() != null) {
            response.setEmail(profile.getUser().getEmail());
        } else {
            response.setEmail(null);
        }
        
        response.setBio(profile.getBio());
        response.setContactInfo(profile.getContactInfo());
        
        // Convert Double to BigDecimal
        if (profile.getRatingAvg() != null) {
            response.setRatingAvg(new BigDecimal(profile.getRatingAvg().toString()));
        }
        
        response.setRatingCount(profile.getRatingCount());
        response.setProfilePictureUrl(profile.getProfilePictureUrl());
        return response;
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody UserProfileUpdateRequest request) {
        try {
            UserProfile updatedProfile = userProfileService.updateUserProfile(userId, request);
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Profile updated successfully",
                updatedProfile.getUserId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Error updating profile: " + e.getMessage()
            ));
        }
    }

    // Debug endpoint to list all user profiles
    @GetMapping("/debug/all-profiles")
    public ResponseEntity<?> getAllUserProfiles() {
        try {
            return ResponseEntity.ok(userProfileService.getAllUserProfiles());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }    // Fix endpoint to create missing user profiles
    @PostMapping("/fix/create-missing-profiles")
    public ResponseEntity<?> createMissingUserProfiles() {
        try {
            int created = userProfileService.createMissingUserProfiles();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Created " + created + " missing user profiles",
                "created_count", created
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // Debug endpoint to show ratings table schema
    @GetMapping("/debug/ratings-schema")
    public ResponseEntity<?> getRatingsSchema() {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Check database schema for ratings table manually",
                "note", "Run: DESCRIBE ratings; in MySQL"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Error: " + e.getMessage()
            ));
        }
    }    // Debug endpoint to recalculate all user rating stats
    @PostMapping("/debug/recalculate-ratings")
    public ResponseEntity<?> recalculateAllUserRatings() {
        try {
            ratingService.recalculateAllUserRatingStats();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "All user rating stats have been recalculated successfully!"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Error: " + e.getMessage()
            ));
        }
    }

    // Debug endpoint to check profile response format
    @GetMapping("/debug/profile-format/{userId}")
    public ResponseEntity<?> debugProfileFormat(@PathVariable Long userId) {
        try {
            UserProfile profile = userProfileService.getUserProfile(userId);            if (profile == null) {
                return ResponseEntity.notFound().build();
            }
            UserProfileResponse response = convertToResponse(profile);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "original_profile", Map.of(
                    "userId", profile.getUserId(),
                    "displayName", profile.getDisplayName(),
                    "ratingAvg", profile.getRatingAvg(),
                    "ratingCount", profile.getRatingCount()
                ),
                "response_format", Map.of(
                    "userId", response.getUserId(),
                    "displayName", response.getDisplayName(),
                    "ratingAvg", response.getRatingAvg(),
                    "ratingCount", response.getRatingCount()
                )
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    // Test endpoint to manually complete offer for testing UI
    @PostMapping("/debug/complete-offer/{offerId}")
    public ResponseEntity<?> debugCompleteOffer(@PathVariable Long offerId) {
        try {
            // This is for testing purposes only - simulate payment completion
            Offer offer = offerRepository.findById(offerId).orElse(null);
            if (offer == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Update offer status
            offer.setStatus(OfferStatus.COMPLETED);
            offer.setHasPaidTransaction(true);
            offer.setUpdatedAt(LocalDateTime.now());
            offerRepository.save(offer);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Offer " + offerId + " marked as COMPLETED for testing",
                "offer_id", offerId,
                "new_status", "COMPLETED",
                "has_paid_transaction", true
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }    }
    
    // Comprehensive test endpoint to verify payment completion flow
    @PostMapping("/debug/test-payment-completion/{offerId}")
    public ResponseEntity<?> testPaymentCompletionFlow(@PathVariable Long offerId) {
        try {
            // This tests the automatic payment completion system
            Offer offer = offerRepository.findById(offerId).orElse(null);
            if (offer == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Show current offer status
            Map<String, Object> beforeStatus = Map.of(
                "offer_id", offer.getId(),
                "current_status", offer.getStatus().toString(),
                "has_paid_transaction", offer.getHasPaidTransaction() != null ? offer.getHasPaidTransaction() : false,
                "buyer_id", offer.getBuyerId(),
                "listing_id", offer.getListingId()
            );
            
            // Update offer status to simulate payment completion
            String oldStatus = offer.getStatus().toString();
            offer.setStatus(OfferStatus.COMPLETED);
            offer.setHasPaidTransaction(true);
            offer.setUpdatedAt(LocalDateTime.now());
            offerRepository.save(offer);
            
            Map<String, Object> afterStatus = Map.of(
                "offer_id", offer.getId(),
                "new_status", offer.getStatus().toString(),
                "has_paid_transaction", offer.getHasPaidTransaction(),
                "updated_at", offer.getUpdatedAt().toString()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Offer completion flow tested - này chỉ là test, hệ thống thật sẽ tự động sau khi thanh toán",
                "before", beforeStatus,
                "after", afterStatus,
                "instructions", Map.of(
                    "real_flow", "Sau khi thanh toán Stripe, PaymentCompletionService sẽ tự động cập nhật",
                    "check_android", "Bây giờ mở Android app để xem nút đã đổi thành 'ĐÃ HOÀN THÀNH'"
                )
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    // Endpoint to check automatic payment completion flow
    @GetMapping("/debug/check-payment-flow/{offerId}")
    public ResponseEntity<?> checkPaymentCompletionFlow(@PathVariable Long offerId) {
        try {
            // Check current offer status
            Offer offer = offerRepository.findById(offerId).orElse(null);
            if (offer == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if there are any payments for this offer
            // Note: You'd need to inject PaymentRepository to do this properly
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "offer_id", offerId,
                "current_status", offer.getStatus().toString(),
                "has_paid_transaction", offer.getHasPaidTransaction() != null ? offer.getHasPaidTransaction() : false,
                "buyer_id", offer.getBuyerId(),
                "listing_id", offer.getListingId(),
                "explanation", Map.of(
                    "automatic_flow", "PaymentCompletionService.handlePaymentCompletion() tự động gọi sau Stripe payment success",
                    "what_happens", "1. Payment status -> COMPLETED, 2. Offer status -> COMPLETED, 3. hasPaidTransaction -> true, 4. Listing -> SOLD",
                    "ui_effect", "Android app sẽ hiển thị 'ĐÃ HOÀN THÀNH' thay vì 'MUA NGAY'",
                    "problem_check", "Nếu offer vẫn ACCEPTED sau payment, có thể Stripe callback không gọi được hoặc payment không có offerId"
                )
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    // Debug endpoint to list all offers
    @GetMapping("/debug/all-offers")
    public ResponseEntity<?> getAllOffers() {
        try {
            List<Offer> offers = offerRepository.findAll();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "total_offers", offers.size(),
                "offers", offers.stream().map(offer -> Map.of(
                    "id", offer.getId(),
                    "listing_id", offer.getListingId(),
                    "buyer_id", offer.getBuyerId(),
                    "seller_id", offer.getSellerId(),
                    "status", offer.getStatus().toString(),
                    "has_paid_transaction", offer.getHasPaidTransaction() != null ? offer.getHasPaidTransaction() : false,
                    "amount", offer.getOfferAmount(),
                    "created_at", offer.getCreatedAt(),
                    "updated_at", offer.getUpdatedAt()
                )).toList()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}