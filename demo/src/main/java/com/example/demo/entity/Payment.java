package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
      @Column(name = "listing_id", nullable = false)
    private Long listingId;
    
    @Column(name = "offer_id")
    private Long offerId; // Null for direct purchases, set for offer-based purchases
    
    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;
    
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
      @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type", nullable = false, length = 20)
    private PaymentMethodType paymentMethodType;
      @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "external_transaction_id")
    private String externalTransactionId;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;
    
    // MoMo specific fields
    @Column(name = "momo_phone_number")
    private String momoPhoneNumber;
    
    // Card specific fields
    @Column(name = "card_number_masked")
    private String cardNumberMasked;
    
    @Column(name = "card_type")
    private String cardType;    // Escrow fields
    @Builder.Default
    @Column(name = "use_escrow")
    private Boolean useEscrow = false;
      @Enumerated(EnumType.STRING)
    @Column(name = "escrow_status", length = 20)
    private EscrowStatus escrowStatus;
    
    @Column(name = "escrow_released_at")
    private LocalDateTime escrowReleasedAt;
    
    @Column(name = "escrow_hold_until")
    private LocalDateTime escrowHoldUntil; // Thời gian giữ tiền (7 ngày từ khi thanh toán)
    
    @Column(name = "dispute_reported_at")
    private LocalDateTime disputeReportedAt; // Thời điểm báo cáo tranh chấp
    
    @Column(name = "dispute_reporter_id")
    private Long disputeReporterId; // ID người báo cáo (buyer hoặc seller)
    
    @Column(name = "dispute_reason", columnDefinition = "TEXT")
    private String disputeReason; // Lý do tranh chấp
      @Builder.Default
    @Column(name = "auto_release_scheduled")
    private Boolean autoReleaseScheduled = false; // Đã lên lịch tự động giải phóng tiền
    
    // Timestamps
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.PENDING;
        }        if (autoReleaseScheduled == null) {
            autoReleaseScheduled = false;
        }
        
        // Nếu sử dụng escrow, thiết lập thời gian giữ tiền (7 ngày)
        if (useEscrow && escrowHoldUntil == null) {
            escrowHoldUntil = LocalDateTime.now().plusDays(7);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
      // Payment method types
    public enum PaymentMethodType {
        MOMO, VISA, MASTERCARD, CASH, STRIPE
    }
    
    // Payment status
    public enum PaymentStatus {
        PENDING,      // Chờ thanh toán
        PROCESSING,   // Đang xử lý
        COMPLETED,    // Hoàn thành
        FAILED,       // Thất bại
        CANCELLED,    // Đã hủy
        EXPIRED,      // Hết hạn
        REFUNDED      // Đã hoàn tiền
    }
      // Escrow status
    public enum EscrowStatus {
        NONE,         // Không sử dụng escrow
        HOLDING,      // Đang giữ tiền
        DISPUTED,     // Có tranh chấp
        RELEASED,     // Đã chuyển cho người bán
        REFUNDED      // Đã hoàn lại cho người mua
    }
}
