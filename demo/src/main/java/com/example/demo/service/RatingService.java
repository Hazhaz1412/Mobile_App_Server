package com.example.demo.service;

import com.example.demo.dto.CreateRatingRequest;
import com.example.demo.dto.RatingResponse;
import com.example.demo.dto.UserRatingStatsResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RatingService {
    
    @Autowired
    private RatingRepository ratingRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ListingRepository listingRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Transactional
    public RatingResponse createRating(Long raterUserId, CreateRatingRequest request) {
        // Validate transaction exists
        Optional<Transaction> transactionOpt = transactionRepository.findById(request.getTransactionId());
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transaction không tồn tại!");
        }
        
        Transaction transaction = transactionOpt.get();
        
        // Validate that rater is involved in the transaction
        if (!transaction.getBuyerId().equals(raterUserId) && !transaction.getSellerId().equals(raterUserId)) {
            throw new RuntimeException("Bạn không có quyền đánh giá transaction này!");
        }
        
        // Validate that transaction is completed
        if (!transaction.getStatus().equals(TransactionStatus.COMPLETED)) {
            throw new RuntimeException("Chỉ có thể đánh giá sau khi giao dịch hoàn thành!");
        }
        
        // Check if already rated
        if (ratingRepository.existsByTransactionIdAndRaterUserId(request.getTransactionId(), raterUserId)) {
            throw new RuntimeException("Bạn đã đánh giá cho giao dịch này rồi!");
        }
        
        // Validate rated user
        if (!userRepository.existsById(request.getRatedUserId())) {
            throw new RuntimeException("User được đánh giá không tồn tại!");
        }
        
        // Validate that rater is not rating themselves
        if (raterUserId.equals(request.getRatedUserId())) {
            throw new RuntimeException("Không thể tự đánh giá bản thân!");
        }
        
        // Validate that rated user is the other party in transaction
        Long expectedRatedUserId = transaction.getBuyerId().equals(raterUserId) ? 
                                   transaction.getSellerId() : transaction.getBuyerId();
        if (!request.getRatedUserId().equals(expectedRatedUserId)) {
            throw new RuntimeException("Chỉ có thể đánh giá người bán/mua trong giao dịch này!");
        }
        
        // Create rating
        Rating rating = new Rating(
            request.getTransactionId(),
            raterUserId,
            request.getRatedUserId(),
            transaction.getListingId(),
            request.getRating(),
            request.getComment()
        );
        
        Rating savedRating = ratingRepository.save(rating);
        
        return convertToResponse(savedRating);
    }
    
    public List<RatingResponse> getRatingsForUser(Long userId) {
        List<Rating> ratings = ratingRepository.findByRatedUserIdOrderByCreatedAtDesc(userId);
        return ratings.stream()
                     .map(this::convertToResponse)
                     .collect(Collectors.toList());
    }
    
    public List<RatingResponse> getRatingsByUser(Long userId) {
        List<Rating> ratings = ratingRepository.findByRaterUserIdOrderByCreatedAtDesc(userId);
        return ratings.stream()
                     .map(this::convertToResponse)
                     .collect(Collectors.toList());
    }
    
    public UserRatingStatsResponse getUserRatingStats(Long userId) {
        // Validate user exists
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User không tồn tại!");
        }
        
        User user = userOpt.get();
        
        // Get average rating
        Double averageRating = ratingRepository.getAverageRatingForUser(userId);
        if (averageRating == null) {
            averageRating = 0.0;
        }
        
        // Get total ratings
        Long totalRatings = ratingRepository.countByRatedUserId(userId);
        
        // Get rating distribution
        List<Object[]> distributionData = ratingRepository.getRatingDistributionForUser(userId);
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        
        // Initialize all ratings to 0
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }
        
        // Fill actual data
        for (Object[] data : distributionData) {
            Integer rating = (Integer) data[0];
            Long count = (Long) data[1];
            ratingDistribution.put(rating, count);
        }
          return new UserRatingStatsResponse(
            userId,
            user.getEmail(),
            Math.round(averageRating * 10.0) / 10.0, // Round to 1 decimal place
            totalRatings,
            ratingDistribution
        );
    }
    
    public boolean canUserRateTransaction(Long userId, Long transactionId) {
        // Check if transaction exists and user is involved
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isEmpty()) {
            return false;
        }
        
        Transaction transaction = transactionOpt.get();
        
        // Check if user is buyer or seller
        if (!transaction.getBuyerId().equals(userId) && !transaction.getSellerId().equals(userId)) {
            return false;
        }
        
        // Check if transaction is completed
        if (!transaction.getStatus().equals(TransactionStatus.COMPLETED)) {
            return false;
        }
        
        // Check if already rated
        return !ratingRepository.existsByTransactionIdAndRaterUserId(transactionId, userId);
    }
    
    public Optional<RatingResponse> getRatingForTransaction(Long userId, Long transactionId) {
        Optional<Rating> ratingOpt = ratingRepository.findByTransactionIdAndRaterUserId(transactionId, userId);
        return ratingOpt.map(this::convertToResponse);
    }
      private RatingResponse convertToResponse(Rating rating) {
        // Get user names
        String raterUserName = userRepository.findById(rating.getRaterUserId())
                                            .map(User::getEmail)
                                            .orElse("Unknown");
        
        String ratedUserName = userRepository.findById(rating.getRatedUserId())
                                            .map(User::getEmail)
                                            .orElse("Unknown");
        
        // Get listing title
        String listingTitle = listingRepository.findById(rating.getListingId())
                                              .map(Listing::getTitle)
                                              .orElse("Unknown");
        
        return new RatingResponse(
            rating.getId(),
            rating.getTransactionId(),
            rating.getRaterUserId(),
            raterUserName,
            rating.getRatedUserId(),
            ratedUserName,
            rating.getListingId(),
            listingTitle,
            rating.getRating(),
            rating.getComment(),
            rating.getCreatedAt()
        );
    }
}
