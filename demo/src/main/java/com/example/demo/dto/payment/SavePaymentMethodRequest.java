package com.example.demo.dto.payment;

import com.example.demo.entity.Payment;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavePaymentMethodRequest {
    
    @NotNull(message = "User ID không được để trống")
    private Long userId;
    
    @NotNull(message = "Loại phương thức thanh toán không được để trống")
    private Payment.PaymentMethodType type;
    
    @NotBlank(message = "Tên hiển thị không được để trống")
    private String displayName;
    
    private String description;
    
    // MoMo fields
    private String phoneNumber;
    
    // Card fields
    private String cardNumber; // Sẽ được tokenize và mask
    private String cardHolderName;
    private String expiryDate;    private String cvv;
    
    @Builder.Default
    private Boolean isDefault = false;
}
