package com.example.demo.dto.payment;

import com.example.demo.entity.PaymentMethod;
import com.example.demo.entity.Payment;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodResponse {
    
    private Long id;
    private Long userId;
    private Payment.PaymentMethodType type;
    private String displayName;
    private String description;
    
    // MoMo fields
    private String phoneNumber;
    
    // Card fields (masked for security)
    private String cardNumberMasked;
    private String cardHolderName;
    private String expiryDate;
    private String cardType;
    
    private Boolean isDefault;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Convert from entity
    public static PaymentMethodResponse fromEntity(PaymentMethod paymentMethod) {
        return PaymentMethodResponse.builder()
                .id(paymentMethod.getId())
                .userId(paymentMethod.getUserId())
                .type(paymentMethod.getType())
                .displayName(paymentMethod.getDisplayName())
                .description(paymentMethod.getDescription())
                .phoneNumber(paymentMethod.getPhoneNumber())
                .cardNumberMasked(paymentMethod.getCardNumberMasked())
                .cardHolderName(paymentMethod.getCardHolderName())
                .expiryDate(paymentMethod.getExpiryDate())
                .isDefault(paymentMethod.getIsDefault())
                .isActive(paymentMethod.getIsActive())
                .createdAt(paymentMethod.getCreatedAt())
                .updatedAt(paymentMethod.getUpdatedAt())
                .build();
    }
}
