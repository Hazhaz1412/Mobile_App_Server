package com.example.demo.repository;

import com.example.demo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // Tìm payment theo transaction ID
    Optional<Payment> findByTransactionId(String transactionId);
    
    // Tìm payment theo external transaction ID (MoMo, Visa)
    Optional<Payment> findByExternalTransactionId(String externalTransactionId);
    
    // Tìm tất cả payment của một buyer
    List<Payment> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    
    // Tìm tất cả payment của một seller
    List<Payment> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    
    // Tìm payment theo listing ID
    List<Payment> findByListingIdOrderByCreatedAtDesc(Long listingId);
    
    // Tìm payment theo status
    List<Payment> findByStatusOrderByCreatedAtDesc(Payment.PaymentStatus status);
    
    // Tìm payment theo buyer và status
    List<Payment> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, Payment.PaymentStatus status);
    
    // Tìm payment theo seller và status
    List<Payment> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, Payment.PaymentStatus status);
    
    // Tìm payment theo payment method type
    List<Payment> findByPaymentMethodTypeOrderByCreatedAtDesc(Payment.PaymentMethodType paymentMethodType);
    
    // Tìm escrow payments
    List<Payment> findByUseEscrowTrueOrderByCreatedAtDesc();
    
    // Tìm escrow payments theo status
    List<Payment> findByUseEscrowTrueAndEscrowStatusOrderByCreatedAtDesc(Payment.EscrowStatus escrowStatus);
    
    // Kiểm tra xem có payment nào đang pending cho listing này không
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.listingId = :listingId AND p.status IN ('PENDING', 'PROCESSING')")
    boolean existsPendingPaymentForListing(@Param("listingId") Long listingId);
    
    // Tìm payment đang pending của một buyer cho một listing
    Optional<Payment> findByListingIdAndBuyerIdAndStatusIn(Long listingId, Long buyerId, List<Payment.PaymentStatus> statuses);
    
    // Tìm payment theo listingId, buyerId và status cụ thể
    List<Payment> findByListingIdAndBuyerIdAndStatus(Long listingId, Long buyerId, Payment.PaymentStatus status);
}
