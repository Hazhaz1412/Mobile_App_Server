package com.example.demo.service;

import com.example.demo.entity.Favorite;
import com.example.demo.entity.Listing;
import com.example.demo.repository.FavoriteRepository;
import com.example.demo.repository.ListingRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing user favorites
 */
@Service
@Transactional
public class FavoriteService {
    
    @Autowired
    private FavoriteRepository favoriteRepository;
    
    @Autowired
    private ListingRepository listingRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Add a listing to user's favorites
     */
    public Favorite addToFavorites(Long userId, Long listingId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        
        // Validate listing exists
        if (!listingRepository.existsById(listingId)) {
            throw new RuntimeException("Listing not found with id: " + listingId);
        }
        
        // Check if already favorited
        if (favoriteRepository.existsByUserIdAndListingId(userId, listingId)) {
            throw new RuntimeException("Listing is already in favorites");
        }
        
        // Create and save favorite
        Favorite favorite = new Favorite(userId, listingId);
        return favoriteRepository.save(favorite);
    }
    
    /**
     * Remove a listing from user's favorites
     */
    public void removeFromFavorites(Long userId, Long listingId) {
        favoriteRepository.deleteByUserIdAndListingId(userId, listingId);
    }
    
    /**
     * Check if a listing is in user's favorites
     */
    public boolean isFavorited(Long userId, Long listingId) {
        return favoriteRepository.existsByUserIdAndListingId(userId, listingId);
    }
    
    /**
     * Get all favorite listings for a user
     */
    public List<Listing> getUserFavoriteListings(Long userId) {
        return favoriteRepository.findFavoriteListingsByUserId(userId);
    }
    
    /**
     * Get paginated favorite listings for a user
     */
    public Page<Listing> getUserFavoriteListings(Long userId, Pageable pageable) {
        return favoriteRepository.findFavoriteListingsByUserId(userId, pageable);
    }
    
    /**
     * Get all favorites (metadata) for a user
     */
    public List<Favorite> getUserFavorites(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get paginated favorites (metadata) for a user
     */
    public Page<Favorite> getUserFavorites(Long userId, Pageable pageable) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * Count user's favorites
     */
    public long countUserFavorites(Long userId) {
        return favoriteRepository.countByUserId(userId);
    }
    
    /**
     * Count favorites for a listing
     */
    public long countListingFavorites(Long listingId) {
        return favoriteRepository.countByListingId(listingId);
    }
    
    /**
     * Get users who favorited a listing
     */
    public List<Favorite> getListingFavorites(Long listingId) {
        return favoriteRepository.findByListingIdOrderByCreatedAtDesc(listingId);
    }
    
    /**
     * Toggle favorite status (add if not exists, remove if exists)
     */
    public boolean toggleFavorite(Long userId, Long listingId) {
        if (favoriteRepository.existsByUserIdAndListingId(userId, listingId)) {
            // Remove from favorites
            favoriteRepository.deleteByUserIdAndListingId(userId, listingId);
            return false; // Removed
        } else {
            // Add to favorites
            addToFavorites(userId, listingId);
            return true; // Added
        }
    }
    
    /**
     * Clean up favorites for a deleted user
     */
    public void cleanupUserFavorites(Long userId) {
        favoriteRepository.deleteByUserId(userId);
    }
    
    /**
     * Clean up favorites for a deleted listing
     */
    public void cleanupListingFavorites(Long listingId) {
        favoriteRepository.deleteByListingId(listingId);
    }
    
    /**
     * Get favorite by user and listing
     */
    public Optional<Favorite> getFavorite(Long userId, Long listingId) {
        return favoriteRepository.findByUserIdAndListingId(userId, listingId);
    }
}
