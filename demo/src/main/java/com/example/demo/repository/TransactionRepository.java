package com.example.demo.repository;

import com.example.demo.entity.Transaction;
import com.example.demo.entity.TransactionStatus;
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
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Find transactions by buyer
    Page<Transaction> findByBuyerIdOrderByTransactionDateDesc(Long buyerId, Pageable pageable);
    
    // Find transactions by seller
    Page<Transaction> findBySellerIdOrderByTransactionDateDesc(Long sellerId, Pageable pageable);
    
    // Find transactions by listing
    List<Transaction> findByListingIdOrderByTransactionDateDesc(Long listingId);
    
    // Find transactions by status
    Page<Transaction> findByStatusOrderByTransactionDateDesc(TransactionStatus status, Pageable pageable);
    
    // Find buyer's transactions by status
    Page<Transaction> findByBuyerIdAndStatusOrderByTransactionDateDesc(Long buyerId, TransactionStatus status, Pageable pageable);
    
    // Find seller's transactions by status
    Page<Transaction> findBySellerIdAndStatusOrderByTransactionDateDesc(Long sellerId, TransactionStatus status, Pageable pageable);
    
    // Find transaction by listing (should be unique for COMPLETED status)
    Optional<Transaction> findByListingIdAndStatus(Long listingId, TransactionStatus status);
    
    // Check if listing has completed transaction
    boolean existsByListingIdAndStatus(Long listingId, TransactionStatus status);
      // Check if offer has completed transaction
    boolean existsByOfferIdAndStatus(Long offerId, TransactionStatus status);
      // Get user's purchase history (as buyer)
    @Query("SELECT t FROM Transaction t WHERE t.buyerId = :userId AND t.status = 'COMPLETED' ORDER BY t.completionDate DESC")
    Page<Transaction> findUserPurchaseHistory(@Param("userId") Long userId, Pageable pageable);
    
    // Get user's sales history (as seller)
    @Query("SELECT t FROM Transaction t WHERE t.sellerId = :userId AND t.status = 'COMPLETED' ORDER BY t.completionDate DESC")
    Page<Transaction> findUserSalesHistory(@Param("userId") Long userId, Pageable pageable);
    
    // Get transaction statistics for a user
    @Query("SELECT COUNT(t) FROM Transaction t WHERE (t.buyerId = :userId OR t.sellerId = :userId) AND t.status = 'COMPLETED'")
    long countCompletedTransactionsByUser(@Param("userId") Long userId);
    
    // Get total sales amount for a seller
    @Query("SELECT COALESCE(SUM(t.finalPrice), 0) FROM Transaction t WHERE t.sellerId = :sellerId AND t.status = 'COMPLETED'")
    Double getTotalSalesAmountBySeller(@Param("sellerId") Long sellerId);
    
    // Get total purchase amount for a buyer
    @Query("SELECT COALESCE(SUM(t.finalPrice), 0) FROM Transaction t WHERE t.buyerId = :buyerId AND t.status = 'COMPLETED'")
    Double getTotalPurchaseAmountByBuyer(@Param("buyerId") Long buyerId);
    
    // Find recent transactions for dashboard
    @Query("SELECT t FROM Transaction t WHERE (t.buyerId = :userId OR t.sellerId = :userId) ORDER BY t.transactionDate DESC")
    Page<Transaction> findRecentTransactionsByUser(@Param("userId") Long userId, Pageable pageable);
    
    // Find transactions in date range
    @Query("SELECT t FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    Page<Transaction> findTransactionsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate, 
                                                 Pageable pageable);
    
    // Find transactions from offers
    List<Transaction> findByOfferIdIsNotNull();
    
    // Find transaction by specific offer ID
    Optional<Transaction> findByOfferId(Long offerId);
    
    // Count pending transactions for user
    @Query("SELECT COUNT(t) FROM Transaction t WHERE (t.buyerId = :userId OR t.sellerId = :userId) AND t.status = 'PENDING'")
    long countPendingTransactionsByUser(@Param("userId") Long userId);      // Find completed transactions for a user (as buyer or seller) for rating
    @Query("SELECT t FROM Transaction t WHERE (t.buyerId = :userId OR t.sellerId = :userId) AND t.status = :status ORDER BY t.transactionDate DESC")
    Page<Transaction> findByUserIdAndStatusOrderByCompletedAtDesc(@Param("userId") Long userId, @Param("status") TransactionStatus status, Pageable pageable);
}
