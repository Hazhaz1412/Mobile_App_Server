package com.example.demo.repository;

import com.example.demo.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    
    /**
     * Kiểm tra xem đã có rating cho transaction này chưa
     */
    boolean existsByTransactionIdAndRaterUserId(Long transactionId, Long raterUserId);
    
    /**
     * Lấy tất cả ratings cho một user (người được đánh giá)
     */
    List<Rating> findByRatedUserIdOrderByCreatedAtDesc(Long ratedUserId);
    
    /**
     * Lấy tất cả ratings mà user đã đánh giá cho người khác
     */
    List<Rating> findByRaterUserIdOrderByCreatedAtDesc(Long raterUserId);
    
    /**
     * Lấy ratings cho một listing cụ thể
     */
    List<Rating> findByListingIdOrderByCreatedAtDesc(Long listingId);
    
    /**
     * Tính điểm trung bình cho user
     */
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.ratedUserId = :userId")
    Double getAverageRatingForUser(@Param("userId") Long userId);
    
    /**
     * Đếm tổng số ratings cho user
     */
    Long countByRatedUserId(Long ratedUserId);
    
    /**
     * Lấy rating cho transaction cụ thể
     */
    Optional<Rating> findByTransactionIdAndRaterUserId(Long transactionId, Long raterUserId);
    
    /**
     * Lấy ratings theo từng mức sao cho user
     */
    @Query("SELECT r.rating, COUNT(r) FROM Rating r WHERE r.ratedUserId = :userId GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> getRatingDistributionForUser(@Param("userId") Long userId);
}
