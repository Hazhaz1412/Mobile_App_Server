package com.example.demo.dto.payment;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentResponse {
    
    private boolean success;
    private String message;
    private PaymentResponse payment;
    
    // MoMo specific fields theo official API
    private String paymentUrl; // URL để redirect đến cổng thanh toán
    private String qrCodeUrl; // URL data để tạo QR code (không phải image URL)
    private String deeplink; // Deep link để mở app MoMo
    private String deeplinkMiniApp; // Deep link cho MoMo mini app
    
    // Legacy fields for backward compatibility
    private String qrCode; // Deprecated: use qrCodeUrl
    private String deepLink; // Deprecated: use deeplink
    
    // Static factory methods
    public static CreatePaymentResponse success(PaymentResponse payment, String paymentUrl) {
        return CreatePaymentResponse.builder()
                .success(true)
                .message("Tạo giao dịch thành công")
                .payment(payment)
                .paymentUrl(paymentUrl)
                .build();
    }
    
    public static CreatePaymentResponse success(PaymentResponse payment, String paymentUrl, String qrCodeUrl, String deeplink) {
        return CreatePaymentResponse.builder()
                .success(true)
                .message("Tạo giao dịch thành công")
                .payment(payment)
                .paymentUrl(paymentUrl)
                .qrCodeUrl(qrCodeUrl)
                .deeplink(deeplink)
                // Legacy support
                .qrCode(qrCodeUrl)
                .deepLink(deeplink)
                .build();
    }
    
    public static CreatePaymentResponse failure(String message) {
        return CreatePaymentResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
