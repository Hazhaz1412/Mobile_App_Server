package com.example.demo.controller;

import com.example.demo.dto.payment.*;
import com.example.demo.service.payment.PaymentService;
import com.example.demo.service.payment.StripePaymentService;
import com.example.demo.service.EscrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
            log.info("Creating Stripe payment for listing: {}, buyer: {}, amount: {}", 
                    request.getListingId(), request.getBuyerId(), request.getAmount());
            
            // Validate request
            if (request.getAmount() == null || request.getAmount().doubleValue() <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid payment amount"
                ));
            }
            
            Map<String, Object> response = stripePaymentService.createPaymentSession(
                request.getListingId(),
                request.getAmount().doubleValue(),
                request.getDescription(),
                request.getBuyerId(),
                request.getSellerId()
            );
            
            if ((Boolean) response.get("success")) {
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
    }    /**
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
        
        log.info("Stripe payment success - Session: {}, Payment: {}", sessionId, paymentId);
        
        try {
            // Update payment status to COMPLETED
            paymentService.updatePaymentStatus(paymentId, "COMPLETED", null);
            
            String html = "<html><body style='font-family: Arial, sans-serif; text-align: center; padding: 50px;'>" +
                    "<h1 style='color: #28a745;'>✅ Thanh toán thành công!</h1>" +
                    "<p>Payment ID: " + paymentId + "</p>" +
                    "<p>Session ID: " + sessionId + "</p>" +
                    "<p>Cảm ơn bạn đã sử dụng TradeUp!</p>" +
                    "<script>setTimeout(() => { window.close(); }, 3000);</script>" +
                    "</body></html>";
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
            
        } catch (Exception e) {
            log.error("Error processing Stripe success", e);
            return ResponseEntity.status(500).body("Error processing payment");
        }
    }
    
    @GetMapping("/cancel/stripe")
    public ResponseEntity<String> stripePaymentCancel(@RequestParam("payment_id") Long paymentId) {
        
        log.info("Stripe payment cancelled - Payment: {}", paymentId);
        
        try {
            // Update payment status to CANCELLED
            paymentService.updatePaymentStatus(paymentId, "CANCELLED", null);
            
            String html = "<html><body style='font-family: Arial, sans-serif; text-align: center; padding: 50px;'>" +
                    "<h1 style='color: #dc3545;'>❌ Thanh toán đã bị hủy</h1>" +
                    "<p>Payment ID: " + paymentId + "</p>" +
                    "<p>Bạn có thể thử lại thanh toán bất cứ lúc nào.</p>" +
                    "<script>setTimeout(() => { window.close(); }, 3000);</script>" +
                    "</body></html>";
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
            
        } catch (Exception e) {
            log.error("Error processing Stripe cancellation", e);
            return ResponseEntity.status(500).body("Error processing cancellation");
        }
    }
    
    /**
     * Handle Stripe webhooks
     * POST /api/v1/payments/stripe/webhook
     */
    @PostMapping("/stripe/webhook")
    public ResponseEntity<Map<String, Object>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        try {
            log.info("Received Stripe webhook");
            
            Map<String, Object> response = stripePaymentService.handleWebhook(payload, sigHeader);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error handling Stripe webhook", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error processing webhook: " + e.getMessage()
            ));
        }
    }
      // ========== STRIPE MOCK CHECKOUT PAGE ==========
      @GetMapping("/stripe/mock-checkout")
    public ResponseEntity<String> stripeMockCheckout(
            @RequestParam("session_id") String sessionId,
            @RequestParam("payment_id") Long paymentId) {
        
        log.info("Displaying Stripe mock checkout page - Session: {}, Payment: {}", sessionId, paymentId);
        
        try {
            // Get payment details for display
            PaymentResponse payment = paymentService.getPayment(paymentId);
            
            String html = generateMockCheckoutHtml(sessionId, paymentId, payment);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
            
        } catch (Exception e) {
            log.error("Error loading mock checkout page", e);
            
            String html = generateMockCheckoutHtml(sessionId, paymentId, null);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        }
    }
    
    private String generateMockCheckoutHtml(String sessionId, Long paymentId, PaymentResponse payment) {
        String amount = payment != null ? payment.getAmount() + " VND" : "150,000 VND";
        String description = payment != null && payment.getDescription() != null ? payment.getDescription() : "TradeUp Purchase";
        
        return "<!DOCTYPE html>\n" +
                "<html lang=\"vi\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>TradeUp - Mock Stripe Checkout</title>\n" +
                "    <style>\n" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "        body {\n" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
                "            min-height: 100vh;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .checkout-container {\n" +
                "            background: white;\n" +
                "            border-radius: 12px;\n" +
                "            box-shadow: 0 20px 40px rgba(0,0,0,0.1);\n" +
                "            padding: 40px;\n" +
                "            max-width: 500px;\n" +
                "            width: 100%;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .logo {\n" +
                "            color: #635bff;\n" +
                "            font-size: 24px;\n" +
                "            font-weight: bold;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .mock-badge {\n" +
                "            background: #ff6b6b;\n" +
                "            color: white;\n" +
                "            padding: 4px 12px;\n" +
                "            border-radius: 20px;\n" +
                "            font-size: 12px;\n" +
                "            font-weight: bold;\n" +
                "            display: inline-block;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .payment-details {\n" +
                "            background: #f8f9fa;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 20px;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .payment-item {\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            margin-bottom: 10px;\n" +
                "            font-size: 14px;\n" +
                "        }\n" +
                "        .payment-item:last-child {\n" +
                "            font-weight: bold;\n" +
                "            font-size: 16px;\n" +
                "            border-top: 1px solid #dee2e6;\n" +
                "            padding-top: 10px;\n" +
                "            margin-bottom: 0;\n" +
                "        }\n" +
                "        .mock-card {\n" +
                "            background: #635bff;\n" +
                "            color: white;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 20px;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .card-number {\n" +
                "            font-size: 18px;\n" +
                "            font-weight: bold;\n" +
                "            letter-spacing: 2px;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .card-details {\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            font-size: 14px;\n" +
                "        }\n" +
                "        .button-group {\n" +
                "            display: flex;\n" +
                "            gap: 15px;\n" +
                "            margin-top: 30px;\n" +
                "        }\n" +
                "        .btn {\n" +
                "            flex: 1;\n" +
                "            padding: 15px 20px;\n" +
                "            border: none;\n" +
                "            border-radius: 8px;\n" +
                "            font-size: 16px;\n" +
                "            font-weight: 600;\n" +
                "            cursor: pointer;\n" +
                "            transition: all 0.3s ease;\n" +
                "        }\n" +
                "        .btn-success {\n" +
                "            background: #28a745;\n" +
                "            color: white;\n" +
                "        }\n" +
                "        .btn-success:hover {\n" +
                "            background: #218838;\n" +
                "            transform: translateY(-2px);\n" +
                "        }\n" +
                "        .btn-danger {\n" +
                "            background: #dc3545;\n" +
                "            color: white;\n" +
                "        }\n" +
                "        .btn-danger:hover {\n" +
                "            background: #c82333;\n" +
                "            transform: translateY(-2px);\n" +
                "        }\n" +
                "        .processing {\n" +
                "            display: none;\n" +
                "            color: #6c757d;\n" +
                "            font-style: italic;\n" +
                "            margin-top: 20px;\n" +
                "        }\n" +
                "        .footer {\n" +
                "            margin-top: 30px;\n" +
                "            font-size: 12px;\n" +
                "            color: #6c757d;\n" +
                "            border-top: 1px solid #e9ecef;\n" +
                "            padding-top: 20px;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"checkout-container\">\n" +
                "        <div class=\"logo\">🔒 Stripe Checkout</div>\n" +
                "        <div class=\"mock-badge\">MOCK MODE</div>\n" +
                "        \n" +
                "        <div class=\"payment-details\">\n" +
                "            <div class=\"payment-item\">\n" +
                "                <span>Sản phẩm:</span>\n" +
                "                <span>" + description + "</span>\n" +
                "            </div>\n" +
                "            <div class=\"payment-item\">\n" +
                "                <span>Số tiền:</span>\n" +
                "                <span>" + amount + "</span>\n" +
                "            </div>\n" +
                "            <div class=\"payment-item\">\n" +
                "                <span>Phí xử lý:</span>\n" +
                "                <span>0 VND</span>\n" +
                "            </div>\n" +
                "            <div class=\"payment-item\">\n" +
                "                <span>Tổng cộng:</span>\n" +
                "                <span>" + amount + "</span>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"mock-card\">\n" +
                "            <div class=\"card-number\">4242 4242 4242 4242</div>\n" +
                "            <div class=\"card-details\">\n" +
                "                <span>Hết hạn: 12/28</span>\n" +
                "                <span>CVC: 123</span>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"button-group\">\n" +
                "            <button class=\"btn btn-success\" onclick=\"processPayment(true)\">\n" +
                "                ✓ Thanh toán thành công\n" +
                "            </button>\n" +
                "            <button class=\"btn btn-danger\" onclick=\"processPayment(false)\">\n" +
                "                ✗ Hủy thanh toán\n" +
                "            </button>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"processing\" id=\"processing\">\n" +
                "            Đang xử lý thanh toán...\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"footer\">\n" +
                "            <p><strong>Chế độ Mock:</strong> Đây là trang thanh toán giả lập cho mục đích test.</p>\n" +
                "            <p>Session ID: " + sessionId + "</p>\n" +
                "            <p>Payment ID: " + paymentId + "</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        function processPayment(success) {\n" +
                "            document.getElementById('processing').style.display = 'block';\n" +
                "            document.querySelectorAll('.btn').forEach(btn => btn.disabled = true);\n" +
                "            \n" +
                "            setTimeout(() => {\n" +
                "                if (success) {\n" +
                "                    window.location.href = '/api/v1/payments/success/stripe?session_id=" + sessionId + "&payment_id=" + paymentId + "';\n" +
                "                } else {\n" +
                "                    window.location.href = '/api/v1/payments/cancel/stripe?payment_id=" + paymentId + "';\n" +
                "                }\n" +
                "            }, 2000);\n" +
                "        }\n" +
                "        \n" +
                "        setTimeout(() => {\n" +
                "            if (document.getElementById('processing').style.display === 'none' || document.getElementById('processing').style.display === '') {\n" +
                "                alert('Phiên thanh toán sẽ hết hạn trong 5 giây...');\n" +
                "                setTimeout(() => {\n" +
                "                    processPayment(false);\n" +
                "                }, 5000);\n" +
                "            }\n" +                "        }, 55000);\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
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
}
