package com.example.demo.controller;

import com.example.demo.dto.payment.PaymentMethodResponse;
import com.example.demo.dto.payment.SavePaymentMethodRequest;
import com.example.demo.entity.Payment;
import com.example.demo.service.payment.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentMethodController {
    
    private final PaymentMethodService paymentMethodService;
    
    @PostMapping
    public ResponseEntity<PaymentMethodResponse> savePaymentMethod(@Valid @RequestBody SavePaymentMethodRequest request) {
        try {
            log.info("Saving payment method for user: {}, type: {}", request.getUserId(), request.getType());
            
            PaymentMethodResponse response = paymentMethodService.savePaymentMethod(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error saving payment method", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentMethodResponse>> getUserPaymentMethods(@PathVariable Long userId) {
        try {
            List<PaymentMethodResponse> paymentMethods = paymentMethodService.getUserPaymentMethods(userId);
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            log.error("Error getting payment methods for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<PaymentMethodResponse>> getUserPaymentMethodsByType(
            @PathVariable Long userId,
            @PathVariable Payment.PaymentMethodType type) {
        try {
            List<PaymentMethodResponse> paymentMethods = paymentMethodService.getUserPaymentMethodsByType(userId, type);
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            log.error("Error getting payment methods for user: {}, type: {}", userId, type, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/user/{userId}/default")
    public ResponseEntity<PaymentMethodResponse> getDefaultPaymentMethod(@PathVariable Long userId) {
        try {
            PaymentMethodResponse paymentMethod = paymentMethodService.getDefaultPaymentMethod(userId);
            return ResponseEntity.ok(paymentMethod);
        } catch (Exception e) {
            log.error("Error getting default payment method for user: {}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/user/{userId}/default/type/{type}")
    public ResponseEntity<PaymentMethodResponse> getDefaultPaymentMethodByType(
            @PathVariable Long userId,
            @PathVariable Payment.PaymentMethodType type) {
        try {
            PaymentMethodResponse paymentMethod = paymentMethodService.getDefaultPaymentMethodByType(userId, type);
            return ResponseEntity.ok(paymentMethod);
        } catch (Exception e) {
            log.error("Error getting default payment method for user: {}, type: {}", userId, type, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{paymentMethodId}/set-default")
    public ResponseEntity<Map<String, Object>> setDefaultPaymentMethod(
            @PathVariable Long paymentMethodId,
            @RequestParam Long userId) {
        try {
            boolean success = paymentMethodService.setDefaultPaymentMethod(paymentMethodId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", "Đặt phương thức thanh toán mặc định thành công"
            ));
        } catch (Exception e) {
            log.error("Error setting default payment method: {}", paymentMethodId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    @DeleteMapping("/{paymentMethodId}")
    public ResponseEntity<Map<String, Object>> deletePaymentMethod(
            @PathVariable Long paymentMethodId,
            @RequestParam Long userId) {
        try {
            boolean success = paymentMethodService.deletePaymentMethod(paymentMethodId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", "Xóa phương thức thanh toán thành công"
            ));
        } catch (Exception e) {
            log.error("Error deleting payment method: {}", paymentMethodId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
