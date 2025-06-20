package com.example.demo.service;

import com.example.demo.dto.TransactionResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private ListingRepository listingRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private ListingImageRepository listingImageRepository;
    
    @Autowired
    private OfferRepository offerRepository;
    
    /**
     * Create transaction from direct purchase (buy now)
     */
    @Transactional
    public TransactionResponse createDirectTransaction(Long listingId, Long buyerId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new RuntimeException("Listing không tồn tại!"));
            
        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw new RuntimeException("Listing không còn khả dụng!");
        }
        
        if (listing.getUserId().equals(buyerId)) {
            throw new RuntimeException("Bạn không thể mua listing của chính mình!");
        }
        
        // Check if there's already a completed transaction for this listing
        if (transactionRepository.existsByListingIdAndStatus(listingId, TransactionStatus.COMPLETED)) {
            throw new RuntimeException("Listing này đã được bán!");
        }
        
        // Create transaction
        Transaction transaction = new Transaction(
            listingId,
            buyerId,
            listing.getUserId(),
            listing.getPrice()
        );
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Mark listing as sold
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);
        
        // Reject all pending offers for this listing
        rejectAllPendingOffers(listingId);
        
        return convertToTransactionResponse(savedTransaction);
    }
    
    /**
     * Create transaction from accepted offer
     */
    @Transactional
    public TransactionResponse createTransactionFromOffer(Offer offer) {
        Listing listing = listingRepository.findById(offer.getListingId())
            .orElseThrow(() -> new RuntimeException("Listing không tồn tại!"));
        
        Transaction transaction = new Transaction(
            offer.getListingId(),
            offer.getBuyerId(),
            offer.getSellerId(),
            offer.getOfferAmount(),
            offer.getId()
        );
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        return convertToTransactionResponse(savedTransaction);
    }
    
    /**
     * Complete a transaction
     */
    @Transactional
    public TransactionResponse completeTransaction(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction không tồn tại!"));
            
        // Only buyer or seller can complete the transaction
        if (!transaction.getBuyerId().equals(userId) && !transaction.getSellerId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hoàn thành transaction này!");
        }
        
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Transaction không thể hoàn thành!");
        }
        
        transaction.markAsCompleted();
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        return convertToTransactionResponse(savedTransaction);
    }
    
    /**
     * Cancel a transaction
     */
    @Transactional
    public TransactionResponse cancelTransaction(Long transactionId, Long userId, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction không tồn tại!"));
            
        // Only buyer or seller can cancel the transaction
        if (!transaction.getBuyerId().equals(userId) && !transaction.getSellerId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy transaction này!");
        }
        
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Transaction không thể hủy!");
        }
        
        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setNotes(reason);
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Mark listing as available again
        Listing listing = listingRepository.findById(transaction.getListingId())
            .orElseThrow(() -> new RuntimeException("Listing không tồn tại!"));
        listing.setStatus(ListingStatus.AVAILABLE);
        listingRepository.save(listing);
        
        return convertToTransactionResponse(savedTransaction);
    }
    
    /**
     * Get user's purchase history (as buyer)
     */
    public Page<TransactionResponse> getUserPurchaseHistory(Long userId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findUserPurchaseHistory(userId, pageable);
        return transactions.map(this::convertToTransactionResponse);
    }
    
    /**
     * Get user's sales history (as seller)
     */
    public Page<TransactionResponse> getUserSalesHistory(Long userId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findUserSalesHistory(userId, pageable);
        return transactions.map(this::convertToTransactionResponse);
    }
    
    /**
     * Get all transactions for a user (both buying and selling)
     */
    public Page<TransactionResponse> getUserAllTransactions(Long userId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findRecentTransactionsByUser(userId, pageable);
        return transactions.map(this::convertToTransactionResponse);
    }
    
    /**
     * Get transactions by status for a user
     */
    public Page<TransactionResponse> getUserTransactionsByStatus(Long userId, TransactionStatus status, Pageable pageable) {
        // Get both buyer and seller transactions
        Page<Transaction> buyerTransactions = transactionRepository.findByBuyerIdAndStatusOrderByTransactionDateDesc(userId, status, pageable);
        Page<Transaction> sellerTransactions = transactionRepository.findBySellerIdAndStatusOrderByTransactionDateDesc(userId, status, pageable);
        
        // For simplicity, return buyer transactions first
        // In a real implementation, you might want to merge and sort both results
        return buyerTransactions.map(this::convertToTransactionResponse);
    }
    
    /**
     * Get transaction statistics for a user
     */
    public TransactionStatsResponse getUserTransactionStats(Long userId) {
        long totalTransactions = transactionRepository.countCompletedTransactionsByUser(userId);
        Double totalSales = transactionRepository.getTotalSalesAmountBySeller(userId);
        Double totalPurchases = transactionRepository.getTotalPurchaseAmountByBuyer(userId);
        long pendingTransactions = transactionRepository.countPendingTransactionsByUser(userId);
        
        TransactionStatsResponse stats = new TransactionStatsResponse();
        stats.setTotalCompletedTransactions(totalTransactions);
        stats.setTotalSalesAmount(totalSales != null ? BigDecimal.valueOf(totalSales) : BigDecimal.ZERO);
        stats.setTotalPurchaseAmount(totalPurchases != null ? BigDecimal.valueOf(totalPurchases) : BigDecimal.ZERO);
        stats.setPendingTransactions(pendingTransactions);
        
        return stats;
    }
    
    /**
     * Get completed transactions for a user (for rating purposes)
     */
    public Page<TransactionResponse> getCompletedTransactionsForUser(Long userId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findByUserIdAndStatusOrderByCompletedAtDesc(
            userId, TransactionStatus.COMPLETED, pageable);
        return transactions.map(this::convertToTransactionResponse);
    }
    
    /**
     * Reject all pending offers for a listing when it's sold
     */
    private void rejectAllPendingOffers(Long listingId) {
        List<Offer> pendingOffers = offerRepository.findByListingIdAndStatus(listingId, OfferStatus.PENDING);
        for (Offer offer : pendingOffers) {
            offer.setStatus(OfferStatus.REJECTED);
            offerRepository.save(offer);
        }
    }
    
    /**
     * Convert Transaction entity to TransactionResponse DTO
     */
    private TransactionResponse convertToTransactionResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setListingId(transaction.getListingId());
        response.setBuyerId(transaction.getBuyerId());
        response.setSellerId(transaction.getSellerId());
        response.setFinalPrice(transaction.getFinalPrice());
        response.setOfferId(transaction.getOfferId());
        response.setStatus(transaction.getStatus());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setCompletionDate(transaction.getCompletionDate());
        response.setNotes(transaction.getNotes());
        
        // Get listing details
        listingRepository.findById(transaction.getListingId()).ifPresent(listing -> {
            response.setListingTitle(listing.getTitle());
            
            // Get primary image
            List<ListingImage> images = listingImageRepository.findByListingIdAndIsPrimaryTrue(listing.getId());
            if (!images.isEmpty()) {
                response.setListingImageUrl(images.get(0).getImageUrl());
            }
        });
        
        // Get buyer profile
        userProfileRepository.findByUserId(transaction.getBuyerId()).ifPresent(profile -> {
            response.setBuyerName(profile.getDisplayName());
            response.setBuyerProfilePic(profile.getProfilePictureUrl());
        });
        
        // Get seller profile
        userProfileRepository.findByUserId(transaction.getSellerId()).ifPresent(profile -> {
            response.setSellerName(profile.getDisplayName());
            response.setSellerProfilePic(profile.getProfilePictureUrl());
        });
        
        return response;
    }
    
    // Inner class for transaction statistics
    public static class TransactionStatsResponse {
        private long totalCompletedTransactions;
        private BigDecimal totalSalesAmount;
        private BigDecimal totalPurchaseAmount;
        private long pendingTransactions;
        
        // Getters and setters
        public long getTotalCompletedTransactions() { return totalCompletedTransactions; }
        public void setTotalCompletedTransactions(long totalCompletedTransactions) { this.totalCompletedTransactions = totalCompletedTransactions; }
        
        public BigDecimal getTotalSalesAmount() { return totalSalesAmount; }
        public void setTotalSalesAmount(BigDecimal totalSalesAmount) { this.totalSalesAmount = totalSalesAmount; }
        
        public BigDecimal getTotalPurchaseAmount() { return totalPurchaseAmount; }
        public void setTotalPurchaseAmount(BigDecimal totalPurchaseAmount) { this.totalPurchaseAmount = totalPurchaseAmount; }
        
        public long getPendingTransactions() { return pendingTransactions; }
        public void setPendingTransactions(long pendingTransactions) { this.pendingTransactions = pendingTransactions; }
    }
}
