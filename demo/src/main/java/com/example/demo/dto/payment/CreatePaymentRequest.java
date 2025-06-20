package com.example.demo.dto.payment;

import com.example.demo.entity.Payment;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {
      @NotNull(message = "Listing ID không được để trống")
    private Long listingId;
    
    private Long offerId; // Optional: for payments from accepted offers
    
    @NotNull(message = "Buyer ID không được để trống")
    private Long buyerId;
    
    @NotNull(message = "Seller ID không được để trống")
    private Long sellerId;
    
    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;
    
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private Payment.PaymentMethodType paymentMethodType;
    
    private String description;
    
    // MoMo specific
    private String momoPhoneNumber;
    
    // Card specific
    private String cardToken; // Token của thẻ đã lưu
    private String cardNumber; // Hoặc số thẻ mới (sẽ được tokenize)
    private String cardHolderName;
    private String expiryDate;
    private String cvv;
      // Escrow
    @Builder.Default
    private Boolean useEscrow = false;
    
    // Return URLs
    private String returnUrl;
    private String cancelUrl;
    private String notifyUrl;
    
    // Additional info
    private String userAgent;
    private String clientIp;
}
