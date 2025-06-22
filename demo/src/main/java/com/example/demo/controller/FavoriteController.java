package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PagedApiResponse;
import com.example.demo.entity.Favorite;
import com.example.demo.entity.Listing;
import com.example.demo.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing user favorites
 */
@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {
    
    @Autowired
    private FavoriteService favoriteService;
    
    /**
     * Add a listing to user's favorites
     * POST /api/favorites
     */
    @PostMapping
    public ResponseEntity<ApiResponse> addToFavorites(
            @RequestParam Long userId,
            @RequestParam Long listingId) {
        try {
            Favorite favorite = favoriteService.addToFavorites(userId, listingId);
            return ResponseEntity.ok(new ApiResponse(true, "Đã thêm vào danh sách yêu thích", favorite));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Remove a listing from user's favorites
     * DELETE /api/favorites
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse> removeFromFavorites(
            @RequestParam Long userId,
            @RequestParam Long listingId) {
        try {
            favoriteService.removeFromFavorites(userId, listingId);
            return ResponseEntity.ok(new ApiResponse(true, "Đã xóa khỏi danh sách yêu thích"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Toggle favorite status (add/remove)
     * POST /api/favorites/toggle
     */
    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse> toggleFavorite(
            @RequestParam Long userId,
            @RequestParam Long listingId) {
        try {
            boolean isAdded = favoriteService.toggleFavorite(userId, listingId);
            String message = isAdded ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích";
            return ResponseEntity.ok(new ApiResponse(true, message, isAdded));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Check if a listing is favorited by user
     * GET /api/favorites/check
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse> checkFavorite(
            @RequestParam Long userId,
            @RequestParam Long listingId) {
        try {
            boolean isFavorited = favoriteService.isFavorited(userId, listingId);
            return ResponseEntity.ok(new ApiResponse(true, "OK", isFavorited));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }
      /**
     * Get all favorite listings for a user (simple version)
     * GET /api/favorites/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getUserFavoriteListings(@PathVariable Long userId) {
        try {
            List<Listing> favoriteListings = favoriteService.getUserFavoriteListings(userId);
            
            // Initialize lazy fields to prevent serialization issues
            for (Listing listing : favoriteListings) {
                if (listing.getUser() != null) {
                    listing.getUser().getId(); // Force initialization
                }
                if (listing.getCategory() != null) {
                    listing.getCategory().getId(); // Force initialization
                }
                if (listing.getItemCondition() != null) {
                    listing.getItemCondition().getId(); // Force initialization
                }
            }
            
            return ResponseEntity.ok(new ApiResponse(true, "Danh sách yêu thích", favoriteListings));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Get paginated favorite listings for a user
     * GET /api/favorites/user/{userId}/paged
     */
    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<PagedApiResponse<Listing>> getUserFavoriteListingsPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Listing> favoriteListings = favoriteService.getUserFavoriteListings(userId, pageable);
              return ResponseEntity.ok(new PagedApiResponse<>(
                    true, 
                    "Danh sách yêu thích", 
                    favoriteListings.getContent(),
                    favoriteListings.getNumber(),
                    favoriteListings.getSize(),
                    favoriteListings.getTotalElements(),
                    favoriteListings.getTotalPages()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new PagedApiResponse<>(false, "Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Get user's favorite metadata (includes favorite IDs and timestamps)
     * GET /api/favorites/user/{userId}/metadata
     */
    @GetMapping("/user/{userId}/metadata")
    public ResponseEntity<ApiResponse> getUserFavoriteMetadata(@PathVariable Long userId) {
        try {
            List<Favorite> favorites = favoriteService.getUserFavorites(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Metadata yêu thích", favorites));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Get paginated user's favorite metadata
     * GET /api/favorites/user/{userId}/metadata/paged
     */
    @GetMapping("/user/{userId}/metadata/paged")
    public ResponseEntity<PagedApiResponse<Favorite>> getUserFavoriteMetadataPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Favorite> favorites = favoriteService.getUserFavorites(userId, pageable);
              return ResponseEntity.ok(new PagedApiResponse<>(
                    true, 
                    "Metadata yêu thích", 
                    favorites.getContent(),
                    favorites.getNumber(),
                    favorites.getSize(),
                    favorites.getTotalElements(),
                    favorites.getTotalPages()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new PagedApiResponse<>(false, "Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Count user's favorites
     * GET /api/favorites/user/{userId}/count
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<ApiResponse> countUserFavorites(@PathVariable Long userId) {
        try {
            long count = favoriteService.countUserFavorites(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Số lượng yêu thích", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Count favorites for a listing
     * GET /api/favorites/listing/{listingId}/count
     */
    @GetMapping("/listing/{listingId}/count")
    public ResponseEntity<ApiResponse> countListingFavorites(@PathVariable Long listingId) {
        try {
            long count = favoriteService.countListingFavorites(listingId);
            return ResponseEntity.ok(new ApiResponse(true, "Số người yêu thích", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Get users who favorited a listing
     * GET /api/favorites/listing/{listingId}/users
     */
    @GetMapping("/listing/{listingId}/users")
    public ResponseEntity<ApiResponse> getListingFavorites(@PathVariable Long listingId) {
        try {
            List<Favorite> favorites = favoriteService.getListingFavorites(listingId);
            return ResponseEntity.ok(new ApiResponse(true, "Danh sách người yêu thích", favorites));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }
}
