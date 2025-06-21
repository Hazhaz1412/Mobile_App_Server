package com.example.demo.controller;

import com.example.demo.dto.payment.*;
import com.example.demo.entity.Payment;
import com.example.demo.service.payment.PaymentService;
import com.example.demo.service.payment.StripePaymentService;
import com.example.demo.service.EscrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {
      private final PaymentService paymentService;
    private final StripePaymentService stripePaymentService;
    private final EscrowService escrowService;
    
    @PostMapping
    public ResponseEntity<CreatePaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        try {
            log.info("Creating payment for listing: {}, buyer: {}, amount: {}", 
                    request.getListingId(), request.getBuyerId(), request.getAmount());
            
            CreatePaymentResponse response = paymentService.createPayment(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error creating payment", e);
            return ResponseEntity.internalServerError()
                    .body(CreatePaymentResponse.failure("Lỗi hệ thống: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        try {
            PaymentResponse payment = paymentService.getPayment(paymentId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Error getting payment: {}", paymentId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(@PathVariable String transactionId) {
        try {
            PaymentResponse payment = paymentService.getPaymentByTransactionId(transactionId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Error getting payment by transaction ID: {}", transactionId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<PaymentResponse>> getUserPaymentHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("Getting payment history for user: {}, page: {}, size: {}", userId, page, size);
            List<PaymentResponse> payments = paymentService.getPaymentHistory(userId, "BUYER");
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("Error getting payment history for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @RequestParam Long userId,
            @RequestParam String role) {
        try {
            List<PaymentResponse> payments = paymentService.getPaymentHistory(userId, role);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("Error getting payment history for user: {}, role: {}", userId, role, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/listing/{listingId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByListing(@PathVariable Long listingId) {
        try {
            List<PaymentResponse> payments = paymentService.getPaymentsByListing(listingId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("Error getting payments for listing: {}", listingId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/user/{userId}/listing/{listingId}/pending")
    public ResponseEntity<PaymentResponse> getPendingPaymentForListing(
            @PathVariable Long userId,
            @PathVariable Long listingId) {
        try {
            log.info("Checking pending payment for user: {}, listing: {}", userId, listingId);
            PaymentResponse pendingPayment = paymentService.getPendingPaymentForListing(userId, listingId);
            
            if (pendingPayment != null) {
                return ResponseEntity.ok(pendingPayment);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting pending payment for user: {}, listing: {}", userId, listingId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{paymentId}/confirm-cash")
    public ResponseEntity<Map<String, Object>> confirmCashPayment(
            @PathVariable Long paymentId,
            @RequestParam Long sellerId) {
        try {
            boolean success = paymentService.confirmCashPayment(paymentId, sellerId);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", "Xác nhận thanh toán tiền mặt thành công"
            ));
        } catch (Exception e) {
            log.error("Error confirming cash payment: {}", paymentId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/{paymentId}/release-escrow")
    public ResponseEntity<Map<String, Object>> releaseEscrow(
            @PathVariable Long paymentId,
            @RequestParam Long buyerId) {
        try {
            boolean success = paymentService.releaseEscrow(paymentId, buyerId);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", "Thả escrow thành công"
            ));
        } catch (Exception e) {
            log.error("Error releasing escrow: {}", paymentId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelPayment(
            @PathVariable Long paymentId,
            @RequestParam Long userId) {
        try {
            boolean success = paymentService.cancelPayment(paymentId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", "Hủy giao dịch thành công"
            ));
        } catch (Exception e) {
            log.error("Error canceling payment: {}", paymentId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * MoMo IPN (Instant Payment Notification) Callback
     * POST /api/v1/payments/callback/momo
     */
    @PostMapping("/callback/momo")
    public ResponseEntity<Map<String, Object>> handleMoMoCallback(@RequestBody Map<String, Object> callbackData) {
        try {
            log.info("Received MoMo IPN callback: {}", callbackData);
            
            boolean result = paymentService.handleMoMoCallback(callbackData);
            
            Map<String, Object> response = Map.of(
                "partnerCode", callbackData.get("partnerCode"),
                "requestId", callbackData.get("requestId"),
                "orderId", callbackData.get("orderId"),
                "resultCode", result ? 0 : 1,
                "message", result ? "Xử lý thành công" : "Xử lý thất bại",
                "responseTime", System.currentTimeMillis(),
                "extraData", ""
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error handling MoMo callback", e);
            
            Map<String, Object> errorResponse = Map.of(
                "partnerCode", callbackData.getOrDefault("partnerCode", ""),
                "requestId", callbackData.getOrDefault("requestId", ""),
                "orderId", callbackData.getOrDefault("orderId", ""),
                "resultCode", 1,
                "message", "Lỗi hệ thống",
                "responseTime", System.currentTimeMillis(),
                "extraData", ""
            );
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * MoMo Return URL - Xử lý redirect từ MoMo về app
     * GET /api/v1/payments/return/momo
     */
    @GetMapping("/return/momo")
    public ResponseEntity<String> handleMoMoReturn(@RequestParam Map<String, String> params) {
        try {
            log.info("Received MoMo return params: {}", params);
            
            String orderId = params.get("orderId");
            String resultCode = params.get("resultCode");
            
            if ("0".equals(resultCode)) {
                return ResponseEntity.ok(
                    "<html><body><h2>Thanh toán thành công!</h2>" +
                    "<p>Mã đơn hàng: " + orderId + "</p>" +
                    "<script>setTimeout(() => window.close(), 3000);</script>" +
                    "</body></html>");
            } else {
                return ResponseEntity.ok(
                    "<html><body><h2>Thanh toán thất bại!</h2>" +
                    "<p>Mã đơn hàng: " + orderId + "</p>" +
                    "<p>Mã lỗi: " + resultCode + "</p>" +
                    "<script>setTimeout(() => window.close(), 3000);</script>" +
                    "</body></html>");
            }
            
        } catch (Exception e) {
            log.error("Error handling MoMo return", e);
            return ResponseEntity.ok(
                "<html><body><h2>Có lỗi xảy ra!</h2>" +
                "<script>setTimeout(() => window.close(), 3000);</script>" +
                "</body></html>");
        }
    }
    
    /**
     * Manual update payment status - FOR TESTING ONLY
     * PUT /api/v1/payments/{paymentId}/status
     */
    @PutMapping("/{paymentId}/status")
    public ResponseEntity<Map<String, Object>> updatePaymentStatus(
            @PathVariable Long paymentId,
            @RequestParam String status,
            @RequestParam(required = false) String transactionId) {
        try {
            log.info("Manual update payment status: paymentId={}, status={}, transactionId={}", 
                    paymentId, status, transactionId);
            
            boolean success = paymentService.updatePaymentStatus(paymentId, status, transactionId);
            
            // Nếu payment chuyển sang COMPLETED, tự động setup escrow
            if (success && "COMPLETED".equals(status)) {
                try {
                    escrowService.setupEscrow(paymentId);
                    log.info("Auto-setup escrow for completed payment: {}", paymentId);
                } catch (Exception e) {
                    log.warn("Failed to setup escrow for payment {}: {}", paymentId, e.getMessage());
                    // Không throw exception để không ảnh hưởng đến việc cập nhật status
                }
            }
            
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "Cập nhật trạng thái thành công" : "Cập nhật trạng thái thất bại",
                    "paymentId", paymentId,
                    "newStatus", status
            ));
            
        } catch (Exception e) {
            log.error("Error updating payment status: paymentId={}, status={}", paymentId, status, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi cập nhật trạng thái: " + e.getMessage()
            ));
        }
    }    // Visa callback endpoint
    @PostMapping("/callback/visa")
    public ResponseEntity<Map<String, Object>> handleVisaCallback(@RequestBody Map<String, Object> callbackData) {
        try {
            log.info("Received Visa callback: {}", callbackData);
            
            String transactionId = (String) callbackData.get("id");
            String status = (String) callbackData.get("status");
            
            if (transactionId == null || status == null) {
                log.error("Invalid Visa callback - missing transactionId or status");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid callback data"
                ));
            }
            
            boolean success = paymentService.handleVisaCallback(transactionId, status);
            
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "Callback processed successfully" : "Failed to process callback",
                    "transactionId", transactionId,
                    "status", status
            ));
            
        } catch (Exception e) {
            log.error("Error handling Visa callback", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Internal server error: " + e.getMessage(),
                    "error_code", "VISA_CALLBACK_ERROR"
            ));
        }
    }    // ==================== STRIPE PAYMENT ENDPOINTS ====================
    
    /**
     * Get Stripe configuration status
     * GET /api/v1/payments/stripe/status
     */
    @GetMapping("/stripe/status")
    public ResponseEntity<Map<String, Object>> getStripeStatus() {
        try {
            Map<String, Object> status = stripePaymentService.getConfigurationStatus();
            return ResponseEntity.ok(status);        } catch (Exception e) {
            log.error("Error getting Stripe status", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error getting Stripe status: " + e.getMessage()
            ));
        }
    }
      /**
     * Create Stripe payment session
     * POST /api/v1/payments/stripe/create
     */
    @PostMapping("/stripe/create")
    public ResponseEntity<Map<String, Object>> createStripePayment(@Valid @RequestBody CreatePaymentRequest request) {
        try {
            log.info("Creating Stripe payment for listing: {}, offer: {}, buyer: {}, amount: {}", 
                    request.getListingId(), request.getOfferId(), request.getBuyerId(), request.getAmount());
            
            // CRITICAL: Check if offer can be purchased (prevent multiple purchases)
            if (request.getOfferId() != null && !paymentService.canPurchaseOffer(request.getOfferId())) {
                log.warn("BLOCKED: Attempt to purchase already completed offer: {}", request.getOfferId());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Offer này đã được thanh toán và hoàn thành",
                    "error_code", "OFFER_ALREADY_COMPLETED"
                ));
            }
            
            // Validate request
            if (request.getAmount() == null || request.getAmount().doubleValue() <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid payment amount"
                ));
            }
            
            // Use the standard payment service to ensure all validation logic is applied
            request.setPaymentMethodType(Payment.PaymentMethodType.STRIPE);
            CreatePaymentResponse paymentResponse = paymentService.createPayment(request);
            
            if (!paymentResponse.isSuccess()) {
                log.warn("Payment creation failed: {}", paymentResponse.getMessage());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", paymentResponse.getMessage(),
                    "error_code", "PAYMENT_CREATION_FAILED"
                ));
            }
              // If payment creation succeeded, create Stripe session
            Map<String, Object> response = stripePaymentService.createPaymentSession(
                request.getListingId(),
                request.getAmount().doubleValue(),
                request.getDescription(),
                request.getBuyerId(),
                request.getSellerId(),
                paymentResponse.getPayment().getId()
            );
            
            // Add payment info to response
            if ((Boolean) response.get("success")) {
                response.put("payment", paymentResponse.getPayment());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error creating Stripe payment", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error creating Stripe payment: " + e.getMessage(),
                    "error_code", "STRIPE_CREATE_ERROR"
            ));
        }
    }/**
     * Stripe real checkout page (query param version)
     * GET /api/v1/payments/stripe/checkout?paymentId=123
     */
    @GetMapping("/stripe/checkout")
    public ResponseEntity<String> stripeRealCheckout(
            @RequestParam Long paymentId,
            @RequestParam(required = false) String mode) {
        
        log.info("Opening Stripe real checkout - Payment: {}, Mode: {}", paymentId, mode);
        return loadStripeCheckoutPage(paymentId);
    }
    
    /**
     * Stripe real checkout page (RESTful path version) 
     * GET /api/v1/payments/stripe/checkout/123
     */
    @GetMapping("/stripe/checkout/{paymentId}")
    public ResponseEntity<String> stripeRealCheckoutPath(@PathVariable Long paymentId) {
        log.info("Opening Stripe real checkout (RESTful) - Payment: {}", paymentId);
        return loadStripeCheckoutPage(paymentId);
    }
      private ResponseEntity<String> loadStripeCheckoutPage(Long paymentId) {
        try {
            // Load the real checkout HTML template
            String template = loadTemplate("stripe-real-checkout.html");
            
            // Get payment details
            PaymentResponse payment = paymentService.getPayment(paymentId);
            if (payment == null) {
                return ResponseEntity.notFound().build();
            }

            // Get product name and seller name from database
            String productName = getProductName(payment.getListingId());
            String sellerName = getSellerName(payment.getSellerId());
            
            // Replace placeholders in template
            template = template.replace("{{PAYMENT_ID}}", paymentId.toString());
            template = template.replace("{{AMOUNT}}", String.format("%.0f", payment.getAmount().doubleValue()));
            template = template.replace("{{PRODUCT_NAME}}", productName != null ? productName : "Sản phẩm TradeUp");
            template = template.replace("{{SELLER_NAME}}", sellerName != null ? sellerName : "Người bán TradeUp");
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(template);
            
        } catch (Exception e) {
            log.error("Error loading Stripe real checkout", e);
            return ResponseEntity.status(500).body("Error loading checkout page");
        }
    }
    
    /**
     * Confirm Stripe payment with payment method
     * POST /api/v1/payments/stripe/confirm
     */
    @PostMapping("/stripe/confirm")
    public ResponseEntity<Map<String, Object>> confirmStripePayment(@RequestBody Map<String, Object> request) {
        log.info("Confirming Stripe payment with payment method");
        
        try {
            String paymentMethodId = (String) request.get("payment_method");
            Long paymentId = Long.valueOf(request.get("payment_id").toString());
            
            Map<String, Object> result = stripePaymentService.confirmPayment(paymentId, paymentMethodId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error confirming Stripe payment", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error confirming payment: " + e.getMessage()
            ));
        }
    }
      /**
     * Handle Stripe payment success
     * GET /api/v1/payments/success/stripe
     */
    @GetMapping("/success/stripe")
    public ResponseEntity<String> stripePaymentSuccess(
            @RequestParam("session_id") String sessionId,
            @RequestParam("payment_id") Long paymentId) {
        
        log.info("🎉 Stripe payment success - Session: {}, Payment: {}", sessionId, paymentId);
        
        try {
            // Use StripePaymentService to handle completion properly
            boolean success = stripePaymentService.handleSuccessCallback(sessionId, paymentId.toString());
            
            if (success) {
                log.info("✅ Payment {} completed successfully via success callback", paymentId);
                
                String html = "<html><body style='font-family: Arial, sans-serif; text-align: center; padding: 50px;'>" +
                        "<h1 style='color: #28a745;'>✅ Thanh toán thành công!</h1>" +
                        "<p><strong>Payment ID:</strong> " + paymentId + "</p>" +
                        "<p><strong>Session ID:</strong> " + sessionId + "</p>" +
                        "<p style='color: #28a745; font-weight: bold;'>🎉 Đơn hàng của bạn đã được xác nhận!</p>" +
                        "<p>Cảm ơn bạn đã sử dụng TradeUp!</p>" +
                        "<script>setTimeout(() => { window.close(); }, 3000);</script>" +
                        "</body></html>";
                
                return ResponseEntity.ok()
                        .header("Content-Type", "text/html; charset=UTF-8")
                        .body(html);
            } else {
                log.error("❌ Failed to process payment completion for payment {}", paymentId);
                
                String html = "<html><body style='font-family: Arial, sans-serif; text-align: center; padding: 50px;'>" +
                        "<h1 style='color: #dc3545;'>⚠️ Lỗi xử lý thanh toán</h1>" +
                        "<p>Payment ID: " + paymentId + "</p>" +
                        "<p>Vui lòng liên hệ hỗ trợ để được giúp đỡ.</p>" +
                        "<script>setTimeout(() => { window.close(); }, 3000);</script>" +
                        "</body></html>";
                
                return ResponseEntity.status(500)
                        .header("Content-Type", "text/html; charset=UTF-8")
                        .body(html);
            }
            
        } catch (Exception e) {
            log.error("💥 Error processing Stripe success for payment {}: ", paymentId, e);
            
            String html = "<html><body style='font-family: Arial, sans-serif; text-align: center; padding: 50px;'>" +
                    "<h1 style='color: #dc3545;'>💥 Lỗi hệ thống</h1>" +
                    "<p>Payment ID: " + paymentId + "</p>" +
                    "<p>Đã xảy ra lỗi khi xử lý thanh toán. Vui lòng liên hệ hỗ trợ.</p>" +
                    "<script>setTimeout(() => { window.close(); }, 3000);</script>" +
                    "</body></html>";
            
            return ResponseEntity.status(500)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        }
    }
    
    /**
     * Handle Stripe payment success (Payment Intent flow - only paymentId needed)
     * GET /api/v1/payments/stripe/success?paymentId=123
     */
    @GetMapping("/stripe/success")
    public ResponseEntity<String> stripePaymentSuccessById(@RequestParam("paymentId") Long paymentId) {
        
        log.info("🎯 Stripe payment success (Payment Intent) - Payment: {}", paymentId);
        
        try {
            // Use StripePaymentService to handle completion properly (no session_id for Payment Intent)
            boolean success = stripePaymentService.handleSuccessCallback(null, paymentId.toString());
            
            if (success) {
                log.info("✅ Payment {} completed successfully via Payment Intent callback", paymentId);
                
                String html = generateSuccessPage(paymentId, null);
                
                return ResponseEntity.ok()
                        .header("Content-Type", "text/html; charset=UTF-8")
                        .body(html);
            } else {
                log.error("❌ Failed to process Payment Intent completion for payment {}", paymentId);
                
                String html = generateErrorPage(paymentId, "Lỗi xử lý thanh toán");
                
                return ResponseEntity.status(500)
                        .header("Content-Type", "text/html; charset=UTF-8")
                        .body(html);
            }
            
        } catch (Exception e) {
            log.error("💥 Error processing Payment Intent success for payment {}: ", paymentId, e);
            
            String html = generateErrorPage(paymentId, "Lỗi hệ thống");
            
            return ResponseEntity.status(500)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        }
    }
    
    private String generateSuccessPage(Long paymentId, String sessionId) {
        return "<html><head>" +
                "<title>Payment Successful</title>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }" +
                ".success-container { background: white; padding: 40px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); max-width: 500px; margin: 0 auto; }" +
                ".success-icon { color: #28a745; font-size: 60px; margin-bottom: 20px; }" +
                "h1 { color: #28a745; margin-bottom: 20px; }" +
                "p { color: #666; margin-bottom: 30px; }" +
                ".details { text-align: left; background: #f8f9fa; padding: 20px; border-radius: 5px; margin: 20px 0; }" +
                ".close-btn { background: #007bff; color: white; border: none; padding: 12px 30px; border-radius: 5px; cursor: pointer; font-size: 16px; }" +
                ".close-btn:hover { background: #0056b3; }" +
                "</style>" +
                "</head><body>" +
                "<div class=\"success-container\">" +
                "<div class=\"success-icon\">✅</div>" +
                "<h1>Payment Successful!</h1>" +
                "<p>Your payment has been processed successfully and your order has been confirmed.</p>" +
                "<div class=\"details\">" +
                "<strong>Session ID:</strong> " + (sessionId != null ? sessionId : "null") + "<br>" +
                "<strong>Payment ID:</strong> " + paymentId + "<br>" +
                "<strong>Status:</strong> Completed" +
                "</div>" +
                "<button class=\"close-btn\" onclick=\"window.close()\">Close Window</button>" +
                "<script>" +
                "// Auto close after 5 seconds" +
                "setTimeout(function() {" +
                "    window.close();" +
                "}, 5000);" +
                "</script>" +
                "</div></body></html>";
    }
    
    private String generateErrorPage(Long paymentId, String errorTitle) {
        return "<html><head>" +
                "<title>Payment Error</title>" +
                "<meta charset=\"UTF-8\">" +
                "<style>" +
                "body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }" +
                ".error-container { background: white; padding: 40px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); max-width: 500px; margin: 0 auto; }" +
                ".error-icon { color: #dc3545; font-size: 60px; margin-bottom: 20px; }" +
                "h1 { color: #dc3545; margin-bottom: 20px; }" +
                "p { color: #666; margin-bottom: 30px; }" +
                "</style>" +
                "</head><body>" +
                "<div class=\"error-container\">" +
                "<div class=\"error-icon\">❌</div>" +
                "<h1>" + errorTitle + "</h1>" +
                "<p>Payment ID: " + paymentId + "</p>" +
                "<p>Đã xảy ra lỗi khi xử lý thanh toán. Vui lòng liên hệ hỗ trợ.</p>" +
                "<script>setTimeout(() => { window.close(); }, 3000);</script>" +
                "</div></body></html>";
    }
    
    /**
     * Load HTML template from resources
     */
    private String loadTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/" + templateName);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error loading template: " + templateName, e);
            return "<html><body><h1>Error loading template</h1></body></html>";
        }
    }
    
    /**
     * Get product name from listing
     */
    private String getProductName(Long listingId) {
        try {
            // In a real app, you would query listing database
            // For now, return a formatted name
            return "Sản phẩm ID #" + listingId;
        } catch (Exception e) {
            log.error("Error getting product name for listing: {}", listingId, e);
            return "Sản phẩm TradeUp";
        }
    }
    
    /**
     * Get seller name from user
     */
    private String getSellerName(Long sellerId) {
        try {
            // In a real app, you would query user database
            // For now, return a formatted name
            return "Người bán ID #" + sellerId;
        } catch (Exception e) {
            log.error("Error getting seller name for user: {}", sellerId, e);
            return "Người bán TradeUp";
        }
    }
    
    // ==================== DEBUG ENDPOINTS ====================
    
    @GetMapping("/debug/offer/{offerId}")
    public ResponseEntity<Map<String, Object>> getPaymentsByOffer(@PathVariable Long offerId) {
        try {
            log.info("Getting payments for offer: {}", offerId);
            List<PaymentResponse> payments = paymentService.getPaymentsByOfferId(offerId);
            
            Map<String, Object> debug = Map.of(
                "offerId", offerId,
                "paymentCount", payments.size(),
                "payments", payments
            );
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            log.error("Error getting payments for offer: {}", offerId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/debug/complete-offer/{offerId}")
    public ResponseEntity<Map<String, String>> debugCompleteOffer(@PathVariable Long offerId) {
        try {
            log.info("DEBUG: Manually completing offer: {}", offerId);
            paymentService.debugCompleteOffer(offerId);
            return ResponseEntity.ok(Map.of("message", "Offer " + offerId + " marked as completed"));
        } catch (Exception e) {
            log.error("Error completing offer: {}", offerId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/debug/fix-offer-id/{paymentId}/{offerId}")
    public ResponseEntity<Map<String, Object>> debugFixOfferIdAndComplete(
            @PathVariable Long paymentId, 
            @PathVariable Long offerId) {
        try {
            log.info("DEBUG: Fixing offerId for payment {} -> offer {}", paymentId, offerId);
            Map<String, Object> result = paymentService.debugFixOfferIdAndComplete(paymentId, offerId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fixing offerId for payment {}: ", paymentId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    // ==================== STRIPE WEBHOOK ENDPOINT ====================
    
    /**
     * Handle Stripe webhook events
     * POST /api/v1/payments/webhook/stripe
     */
    @PostMapping("/webhook/stripe")
    public ResponseEntity<Map<String, Object>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        try {
            log.info("🔔 Received Stripe webhook - Payload length: {}", payload != null ? payload.length() : 0);
            
            Map<String, Object> result = stripePaymentService.handleWebhook(payload, signature);
            
            if ((Boolean) result.getOrDefault("success", false)) {
                log.info("✅ Stripe webhook processed successfully");
                return ResponseEntity.ok(result);
            } else {
                log.error("❌ Stripe webhook processing failed: {}", result.get("message"));
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("💥 Error processing Stripe webhook: ", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Internal error processing webhook: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Test Stripe webhook endpoint
     * GET /api/v1/payments/webhook/stripe/test
     */
    @GetMapping("/webhook/stripe/test")
    public ResponseEntity<Map<String, Object>> testStripeWebhook() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Stripe webhook endpoint is working",
            "endpoint", "/api/v1/payments/webhook/stripe",
            "devtunnel_url", "https://zn8vnhrf-8080.asse.devtunnels.ms/api/v1/payments/webhook/stripe",
            "instructions", "Cấu hình URL này trong Stripe Dashboard → Developers → Webhooks"
        ));
    }
    
    @GetMapping("/debug/payment/{paymentId}")
    public ResponseEntity<Map<String, Object>> debugGetPayment(@PathVariable Long paymentId) {
        try {
            log.info("DEBUG: Getting payment details for payment: {}", paymentId);
            Payment payment = paymentService.findById(paymentId);
            
            if (payment == null) {
                return ResponseEntity.notFound().build();
            }
              Map<String, Object> debug = new HashMap<>();
            debug.put("paymentId", payment.getId());
            debug.put("offerId", payment.getOfferId());
            debug.put("listingId", payment.getListingId());
            debug.put("buyerId", payment.getBuyerId());
            debug.put("sellerId", payment.getSellerId());
            debug.put("amount", payment.getAmount());
            debug.put("status", payment.getStatus().toString());
            debug.put("paymentMethod", payment.getPaymentMethodType().toString());
            debug.put("transactionId", payment.getTransactionId());
            debug.put("createdAt", payment.getCreatedAt());
            debug.put("hasOfferId", payment.getOfferId() != null);
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            log.error("Error getting payment details: {}", paymentId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/debug/status")
    public ResponseEntity<Map<String, Object>> debugGetAllPaymentsStatus() {
        try {
            log.info("DEBUG: Getting all payments status");
            // Get recent payments (limit to last 20)
            List<Payment> payments = paymentService.getAllPayments(); // Need to implement this
            
            Map<String, Object> debug = Map.of(
                "totalPayments", payments.size(),
                "payments", payments.stream().map(p -> Map.of(
                    "id", p.getId(),
                    "offerId", p.getOfferId(),
                    "listingId", p.getListingId(),
                    "status", p.getStatus().toString(),
                    "paymentMethod", p.getPaymentMethodType().toString(),
                    "hasOfferId", p.getOfferId() != null
                )).toList()
            );
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            log.error("Error getting payments status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * TEST ENDPOINT: Force complete a payment (no auth required for testing)
     */
    @PostMapping("/test/force-complete/{paymentId}")
    public ResponseEntity<Map<String, Object>> testForceCompletePayment(@PathVariable Long paymentId) {
        try {
            log.info("🧪 TEST: Force completing payment {}", paymentId);
            
            // Find payment
            Payment payment = paymentService.findById(paymentId);
            if (payment == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Call force completion
            stripePaymentService.forceCompleteOfferFromPayment(payment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Force completion triggered successfully");
            response.put("paymentId", paymentId);
            response.put("testMode", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in test force completion", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to force complete: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}