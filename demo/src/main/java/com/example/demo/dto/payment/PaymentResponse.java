package com.example.demo.dto.payment;

import com.example.demo.entity.Payment;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    
    private Long id;
    private Long listingId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal amount;
    private Payment.PaymentMethodType paymentMethodType;
    private Payment.PaymentStatus status;
    private String transactionId;
    private String externalTransactionId;
    private String description;
    private String paymentUrl;
    
    // MoMo specific
    private String momoPhoneNumber;
    
    // Card specific
    private String cardNumberMasked;
    private String cardType;
    
    // Escrow
    private Boolean useEscrow;
    private Payment.EscrowStatus escrowStatus;
    private LocalDateTime escrowReleasedAt;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime expiredAt;
    
    // Convert from entity
    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .listingId(payment.getListingId())
                .buyerId(payment.getBuyerId())
                .sellerId(payment.getSellerId())
                .amount(payment.getAmount())
                .paymentMethodType(payment.getPaymentMethodType())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .externalTransactionId(payment.getExternalTransactionId())
                .description(payment.getDescription())
                .paymentUrl(payment.getPaymentUrl())
                .momoPhoneNumber(payment.getMomoPhoneNumber())
                .cardNumberMasked(payment.getCardNumberMasked())
                .cardType(payment.getCardType())
                .useEscrow(payment.getUseEscrow())
                .escrowStatus(payment.getEscrowStatus())
                .escrowReleasedAt(payment.getEscrowReleasedAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .paidAt(payment.getPaidAt())
                .expiredAt(payment.getExpiredAt())
                .build();
    }
}
