package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.TransactionStatus;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {
      @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    /**
     * Create transaction from direct purchase (buy now)
     */
    @PostMapping("/direct")
    public ResponseEntity<ApiResponse> createDirectTransaction(
            @RequestParam Long listingId,
            @RequestParam Long buyerId) {
        try {
            TransactionResponse transaction = transactionService.createDirectTransaction(listingId, buyerId);
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Transaction được tạo thành công!",
                transaction
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    /**
     * Complete a transaction
     */
    @PostMapping("/{transactionId}/complete")
    public ResponseEntity<ApiResponse> completeTransaction(
            @PathVariable Long transactionId,
            @RequestParam Long userId) {
        try {
            TransactionResponse transaction = transactionService.completeTransaction(transactionId, userId);
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Transaction được hoàn thành!",
                transaction
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    /**
     * Cancel a transaction
     */
    @PostMapping("/{transactionId}/cancel")
    public ResponseEntity<ApiResponse> cancelTransaction(
            @PathVariable Long transactionId,
            @RequestParam Long userId,
            @RequestParam(required = false) String reason) {
        try {
            TransactionResponse transaction = transactionService.cancelTransaction(transactionId, userId, reason);
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Transaction đã được hủy!",
                transaction
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    /**
     * Get user's purchase history (as buyer)
     */
    @GetMapping("/purchases/{userId}")
    public ResponseEntity<PagedApiResponse<TransactionResponse>> getUserPurchaseHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponse> transactions = transactionService.getUserPurchaseHistory(userId, pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lịch sử mua hàng thành công!",
                transactions.getContent(),
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages()
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
     * Get user's sales history (as seller)
     */
    @GetMapping("/sales/{userId}")
    public ResponseEntity<PagedApiResponse<TransactionResponse>> getUserSalesHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponse> transactions = transactionService.getUserSalesHistory(userId, pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lịch sử bán hàng thành công!",
                transactions.getContent(),
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages()
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
     * Get all transactions for a user (both buying and selling)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedApiResponse<TransactionResponse>> getUserAllTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponse> transactions = transactionService.getUserAllTransactions(userId, pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lịch sử giao dịch thành công!",
                transactions.getContent(),
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages()
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
     * Get transactions by status for a user
     */
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<PagedApiResponse<TransactionResponse>> getUserTransactionsByStatus(
            @PathVariable Long userId,
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            TransactionStatus transactionStatus = TransactionStatus.valueOf(status.toUpperCase());
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponse> transactions = transactionService.getUserTransactionsByStatus(userId, transactionStatus, pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lấy giao dịch theo trạng thái thành công!",
                transactions.getContent(),
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new PagedApiResponse<>(
                false,
                "Trạng thái không hợp lệ!",
                null, 0, 0, 0, 0
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
     * Get transaction statistics for a user
     */
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<ApiResponse> getUserTransactionStats(@PathVariable Long userId) {
        try {
            TransactionService.TransactionStatsResponse stats = transactionService.getUserTransactionStats(userId);
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Thống kê giao dịch thành công!",
                stats
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    /**
     * Get transaction details by ID
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse> getTransactionById(@PathVariable Long transactionId) {
        try {
            // This would require a getTransactionById method in TransactionService
            // For now, we'll return a simple response
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Chi tiết transaction",
                null // TODO: Implement getTransactionById in service
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    /**
     * Get completed transactions for rating (buyer and seller can rate each other)
     */
    @GetMapping("/completed/for-rating")
    public ResponseEntity<ApiResponse> getCompletedTransactionsForRating(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponse> transactions = transactionService.getCompletedTransactionsForUser(userId, pageable);
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Danh sách transaction hoàn thành lấy thành công!",
                transactions
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    /**
     * Find transaction by listing and buyer for rating
     */
    @GetMapping("/find-by-listing")
    public ResponseEntity<ApiResponse> findTransactionByListing(
            @RequestParam Long listingId,
            @RequestParam Long buyerId) {
        try {
            // Find completed transaction for this listing and buyer
            Optional<Transaction> transactionOpt = transactionRepository.findByListingIdAndStatus(listingId, TransactionStatus.COMPLETED);
            
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                // Verify this buyer was involved
                if (transaction.getBuyerId().equals(buyerId)) {
                    return ResponseEntity.ok(new ApiResponse(
                        true,
                        "Transaction tìm thấy!",
                        Map.of("transactionId", transaction.getId())
                    ));
                } else {
                    return ResponseEntity.badRequest().body(new ApiResponse(
                        false,
                        "Bạn không phải người mua trong giao dịch này!"
                    ));
                }
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(
                    false,
                    "Không tìm thấy giao dịch hoàn thành cho listing này!"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Lỗi khi tìm transaction: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Debug endpoint to list all transactions
     */
    @GetMapping("/debug/all")
    public ResponseEntity<?> getAllTransactions() {
        try {
            List<Transaction> transactions = transactionRepository.findAll();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "transactions", transactions,
                "count", transactions.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Error: " + e.getMessage()
            ));
        }
    }
      /**
     * Debug endpoint to check transactions by listing
     */
    @GetMapping("/debug/by-listing/{listingId}")
    public ResponseEntity<?> getTransactionsByListing(@PathVariable Long listingId) {
        try {
            List<Transaction> transactions = transactionRepository.findByListingIdOrderByTransactionDateDesc(listingId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "listingId", listingId,
                "transactions", transactions,
                "count", transactions.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Debug endpoint to manually complete a transaction for testing
     */
    @PostMapping("/debug/complete/{transactionId}")
    public ResponseEntity<?> completeTransaction(@PathVariable Long transactionId) {
        try {
            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                transaction.markAsCompleted();
                transactionRepository.save(transaction);
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Transaction completed successfully",
                    "transactionId", transactionId,
                    "status", transaction.getStatus()
                ));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(
                    false,
                    "Transaction not found"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Debug endpoint to create a test transaction for rating
     */
    @PostMapping("/debug/create-test")
    public ResponseEntity<ApiResponse> createTestTransaction(
            @RequestParam Long listingId,
            @RequestParam Long buyerId,
            @RequestParam Long sellerId,
            @RequestParam(defaultValue = "100000") Double amount) {
        try {
            // Create a new transaction
            Transaction transaction = new Transaction(listingId, buyerId, sellerId, BigDecimal.valueOf(amount));
            transaction.markAsCompleted(); // Mark as completed immediately for testing
            Transaction savedTransaction = transactionRepository.save(transaction);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Test transaction created and marked as completed",
                Map.of(
                    "transactionId", savedTransaction.getId(),
                    "listingId", listingId,
                    "buyerId", buyerId,
                    "sellerId", sellerId,
                    "status", "COMPLETED"
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Error creating test transaction: " + e.getMessage()
            ));
        }
    }    /**
     * Get transaction by offer ID
     */
    @GetMapping("/offer/{offerId}")
    public ResponseEntity<ApiResponse> getTransactionByOfferId(@PathVariable Long offerId) {
        try {
            // Find transaction by offer ID
            Optional<Transaction> transactionOpt = transactionRepository.findByOfferId(offerId);
            
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                
                // Simple conversion to TransactionResponse
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
                
                return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Lấy thông tin giao dịch thành công!",
                    response
                ));
            } else {
                return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Chưa có giao dịch cho offer này",
                    null
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Lỗi khi lấy thông tin giao dịch: " + e.getMessage()
            ));
        }
    }
}
