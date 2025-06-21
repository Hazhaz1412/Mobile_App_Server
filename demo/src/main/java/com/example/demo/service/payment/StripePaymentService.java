package com.example.demo.service.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.model.Price;
import com.stripe.param.checkout.SessionCreateParams;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Offer;
import com.example.demo.entity.OfferStatus;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.OfferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StripePaymentService {
    private static final Logger logger = LoggerFactory.getLogger(StripePaymentService.class);

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;
    
    @Value("${stripe.mock-mode:false}")
    private boolean mockMode;    private final PaymentRepository paymentRepository;
    private final PaymentMockService paymentMockService;
    private final PaymentCompletionService paymentCompletionService;
    private final OfferRepository offerRepository;

    public StripePaymentService(PaymentRepository paymentRepository, PaymentMockService paymentMockService, PaymentCompletionService paymentCompletionService, OfferRepository offerRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMockService = paymentMockService;
        this.paymentCompletionService = paymentCompletionService;
        this.offerRepository = offerRepository;
    }    public Map<String, Object> createPaymentSession(Long listingId, Double amount, String description, Long buyerId, Long sellerId, Long paymentId) {
        logger.info("Creating Stripe payment session for listing: {}, amount: {}, paymentId: {}, mockMode: {}", listingId, amount, paymentId, mockMode);

        try {
            // Validate input parameters
            if (listingId == null || amount == null || amount <= 0 || buyerId == null || sellerId == null) {
                logger.error("Invalid payment parameters: listingId={}, amount={}, buyerId={}, sellerId={}", 
                           listingId, amount, buyerId, sellerId);
                return createErrorResponse("Invalid payment parameters");            }

            // Get the payment record that was already created by PaymentService (need this for offerId)
            Payment savedPayment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment record not found: " + paymentId));

            // Use mock service if in mock mode
            if (mockMode) {
                logger.info("Using mock mode for Stripe payment - offerId: {}", savedPayment.getOfferId());
                return paymentMockService.createMockStripeSession(listingId, amount, description, buyerId, sellerId, successUrl, savedPayment.getOfferId());
            }

            // Real Stripe mode - validate configuration first
            if (!isConfigurationValid()) {
                logger.error("Invalid Stripe configuration - missing or invalid keys");
                return createErrorResponse("Stripe configuration is invalid. Please check your API keys.");
            }

            // Initialize Stripe with secret key
            Stripe.apiKey = stripeSecretKey;// WORKAROUND: Stripe thêm 3 số 0 vào cuối số tiền VND, vậy ta cắt bớt 3 số 0            // Convert VND to USD to meet Stripe's minimum requirements
            long originalAmountVND = Math.round(amount);
            
            // VND to USD conversion (approximate rate: 1 USD = 24,000 VND)
            double usdAmount = originalAmountVND / 24000.0;
            long usdAmountCents = Math.max(50, Math.round(usdAmount * 100)); // Minimum 50 cents
            
            logger.info("=== STRIPE VND TO USD CONVERSION ===");
            logger.info("Original VND amount: {}", amount);
            logger.info("Original amount (long): {}", originalAmountVND);
            logger.info("USD equivalent: ${:.2f}", usdAmount);
            logger.info("USD amount in cents for Stripe: {} cents", usdAmountCents);
            logger.info("USD amount for display: ${:.2f}", usdAmountCents / 100.0);            // Create Price object with USD amount
            com.stripe.model.Price priceObject;
            try {
                Map<String, Object> priceParams = new HashMap<>();
                priceParams.put("currency", "usd"); // Use USD instead of VND
                priceParams.put("unit_amount", usdAmountCents); // USD amount in cents
                
                Map<String, Object> productData = new HashMap<>();
                productData.put("name", "TradeUp Purchase: " + (description != null ? description : "Item") + " (Originally " + String.format("%,d VND", originalAmountVND) + ")");
                productData.put("metadata", Map.of(
                    "listing_id", listingId.toString(),
                    "original_amount_vnd", String.valueOf(originalAmountVND),
                    "usd_amount_cents", String.valueOf(usdAmountCents),
                    "conversion_note", "VND converted to USD for Stripe compatibility"
                ));
                priceParams.put("product_data", productData);
                
                priceObject = com.stripe.model.Price.create(priceParams);                logger.info("Created Stripe Price: {} with USD amount: {} cents (${:.2f}) for original {} VND", 
                           priceObject.getId(), usdAmountCents, usdAmountCents / 100.0, originalAmountVND);
                
            } catch (StripeException e) {
                logger.error("Failed to create Stripe Price object: ", e);
                throw new RuntimeException("Failed to create price object: " + e.getMessage());
            }

            // Create Stripe checkout session using the Price object
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}&payment_id=" + savedPayment.getId())
                    .setCancelUrl(cancelUrl + "?payment_id=" + savedPayment.getId())
                    .addLineItem(
                        SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPrice(priceObject.getId()) // Use the adjusted Price object
                            .build()
                    )
                    .putMetadata("payment_id", savedPayment.getId().toString())
                    .putMetadata("listing_id", listingId.toString())
                    .putMetadata("buyer_id", buyerId.toString())
                    .putMetadata("seller_id", sellerId.toString())
                    .putMetadata("original_amount_vnd", String.valueOf(originalAmountVND))
                    .putMetadata("usd_amount_cents", String.valueOf(usdAmountCents))
                    .putMetadata("price_object_id", priceObject.getId())
                    .setExpiresAt(System.currentTimeMillis() / 1000 + 1800) // 30 minutes
                    .build();

            Session session = Session.create(params);
            logger.info("Created Stripe session: {} with expiry: {}", session.getId(), session.getExpiresAt());

            // Update payment with Stripe session ID
            savedPayment.setTransactionId(session.getId());
            savedPayment.setExpiredAt(LocalDateTime.now().plusMinutes(30));
            paymentRepository.save(savedPayment);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stripe session created successfully");
            response.put("sessionId", session.getId());
            response.put("checkoutUrl", session.getUrl());
            response.put("paymentId", savedPayment.getId());
            response.put("status", "pending");
            response.put("amount", amount);
            response.put("currency", "VND");
            response.put("expiresAt", session.getExpiresAt());
            response.put("mockMode", false);
            
            return response;
              } catch (IllegalArgumentException e) {
            logger.error("Invalid payment parameters: {}", e.getMessage());
            return createErrorResponse("Invalid payment parameters: " + e.getMessage());
            
        } catch (StripeException e) {
            logger.error("Stripe API error: ", e);
            return createErrorResponse("Stripe error: " + e.getMessage());
            
        } catch (Exception e) {
            logger.error("Error creating Stripe session: ", e);
            return createErrorResponse("Error creating Stripe payment: " + e.getMessage());
        }
    }public Map<String, Object> handlePaymentSuccess(String sessionId, Long paymentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Processing Stripe payment success - Session: {}, Payment: {}", sessionId, paymentId);
            
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                logger.error("Payment not found: {}", paymentId);
                response.put("success", false);
                response.put("message", "Payment not found");
                return response;
            }
            
            if (mockMode) {
                // Use mock service for handling success
                logger.info("Mock mode: Using PaymentMockService for payment success");
                return paymentMockService.simulatePaymentSuccess(paymentId, sessionId);
            }
            
            // Real Stripe mode
            if (!isConfigurationValid()) {
                logger.error("Invalid Stripe configuration");
                response.put("success", false);
                response.put("message", "Stripe configuration is invalid");
                return response;
            }
            
            Stripe.apiKey = stripeSecretKey;
            
            // Retrieve session from Stripe
            Session session = Session.retrieve(sessionId);
            logger.info("Retrieved Stripe session: {}, status: {}, payment_status: {}", 
                       sessionId, session.getStatus(), session.getPaymentStatus());            // Update payment status based on Stripe session
            if ("paid".equals(session.getPaymentStatus()) || "complete".equals(session.getStatus())) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now());
                payment.setUpdatedAt(LocalDateTime.now());
                
                // Get payment intent ID for transaction reference
                if (session.getPaymentIntent() != null) {
                    payment.setTransactionId(session.getPaymentIntent());
                }
                
                // Save payment first
                payment = paymentRepository.save(payment);
                
                // CRITICAL: Handle payment completion (lock offers, create transactions)
                logger.info("STRIPE_COMPLETION: Triggering completion handler for payment {}", paymentId);
                paymentCompletionService.handlePaymentCompletion(payment);
                
                logger.info("Payment {} completed successfully", paymentId);
                response.put("status", "completed");
                response.put("message", "Payment completed successfully");
                
            } else if ("unpaid".equals(session.getPaymentStatus())) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setUpdatedAt(LocalDateTime.now());
                logger.warn("Payment {} failed - Stripe status: {}", paymentId, session.getPaymentStatus());
                response.put("status", "failed");
                response.put("message", "Payment was not completed");
                
            } else {
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
                payment.setUpdatedAt(LocalDateTime.now());
                logger.info("Payment {} is still processing - Stripe status: {}", paymentId, session.getPaymentStatus());
                response.put("status", "processing");
                response.put("message", "Payment is being processed");
            }
            
            paymentRepository.save(payment);
            
            response.put("success", true);
            response.put("paymentId", paymentId);
            response.put("sessionId", sessionId);
            response.put("transactionId", payment.getTransactionId());
            response.put("amount", payment.getAmount());
            response.put("mockMode", false);
            
        } catch (StripeException e) {
            logger.error("Stripe API error handling payment success: ", e);
            response.put("success", false);
            response.put("message", "Stripe error: " + e.getMessage());
            response.put("error_code", "STRIPE_SUCCESS_ERROR");
            
        } catch (Exception e) {
            logger.error("Error handling Stripe payment success: ", e);
            response.put("success", false);
            response.put("message", "Error processing payment success: " + e.getMessage());
            response.put("error_code", "STRIPE_SUCCESS_ERROR");
        }
        
        return response;
    }public Map<String, Object> handlePaymentCancel(Long paymentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Processing Stripe payment cancellation - Payment: {}", paymentId);
            
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment != null) {
                // Check if payment is already completed
                if (Payment.PaymentStatus.COMPLETED.equals(payment.getStatus())) {
                    logger.warn("Cannot cancel completed payment: {}", paymentId);
                    response.put("success", false);
                    response.put("message", "Cannot cancel completed payment");
                    return response;
                }
                  payment.setStatus(Payment.PaymentStatus.CANCELLED);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                
                logger.info("Payment {} cancelled by user", paymentId);
                
                response.put("success", true);
                response.put("status", "cancelled");
                response.put("message", "Payment cancelled successfully");
                response.put("paymentId", paymentId);
                
            } else {
                logger.error("Payment not found: {}", paymentId);
                response.put("success", false);
                response.put("message", "Payment not found");
            }
            
        } catch (Exception e) {
            logger.error("Error cancelling payment: ", e);
            response.put("success", false);
            response.put("message", "Error cancelling payment: " + e.getMessage());
            response.put("error_code", "STRIPE_CANCEL_ERROR");
        }
        
        return response;
    }    /**
     * Handle Stripe webhook events (for production use)
     */
    public Map<String, Object> handleWebhook(String payload, String sigHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("🔔 Processing Stripe webhook - Payload length: {}", payload != null ? payload.length() : 0);
            
            if (mockMode) {
                logger.info("🧪 Mock mode: Simulating webhook payment completion");
                response.put("success", true);
                response.put("message", "Mock webhook processed successfully");
                return response;
            }
            
            // Initialize Stripe
            Stripe.apiKey = stripeSecretKey;
            
            // Parse the JSON payload
            Map<String, Object> payloadMap = parseWebhookPayload(payload);
            String eventType = (String) payloadMap.get("type");
            
            logger.info("🎯 Webhook event type: {}", eventType);
            
            // Handle checkout.session.completed event
            if ("checkout.session.completed".equals(eventType)) {
                Map<String, Object> eventData = (Map<String, Object>) payloadMap.get("data");
                Map<String, Object> sessionData = (Map<String, Object>) eventData.get("object");
                
                String sessionId = (String) sessionData.get("id");
                String paymentStatus = (String) sessionData.get("payment_status");
                
                logger.info("🎉 Checkout session completed - Session: {}, Payment Status: {}", sessionId, paymentStatus);
                
                if ("paid".equals(paymentStatus)) {
                    // Find payment by session ID
                    Payment payment = paymentRepository.findByTransactionId(sessionId).orElse(null);
                    
                    if (payment != null) {
                        logger.info("💰 Found payment {} for webhook completion", payment.getId());
                        
                        // Update payment status to COMPLETED
                        payment.setStatus(Payment.PaymentStatus.COMPLETED);
                        payment.setPaidAt(LocalDateTime.now());
                        payment.setUpdatedAt(LocalDateTime.now());
                        
                        // Store payment intent ID if available
                        String paymentIntentId = (String) sessionData.get("payment_intent");
                        if (paymentIntentId != null) {
                            payment.setExternalTransactionId(paymentIntentId);
                        }
                        
                        paymentRepository.save(payment);
                        logger.info("✅ Payment {} marked as COMPLETED via webhook", payment.getId());
                        
                        // 🚀 CRITICAL: Trigger offer completion logic
                        logger.info("🚀 WEBHOOK TRIGGERING AUTOMATIC COMPLETION for payment {} with offer {}", payment.getId(), payment.getOfferId());
                        paymentCompletionService.handlePaymentCompletion(payment);
                        logger.info("✨ Webhook payment completion handling COMPLETED for payment {}", payment.getId());
                        
                        response.put("success", true);
                        response.put("message", "Payment completed successfully via webhook");
                        response.put("paymentId", payment.getId());
                    } else {
                        logger.warn("⚠️ Payment not found for session ID: {}", sessionId);
                        response.put("success", false);
                        response.put("message", "Payment not found for session ID: " + sessionId);
                    }
                } else {
                    logger.warn("⚠️ Session {} payment status is not 'paid': {}", sessionId, paymentStatus);
                    response.put("success", true);
                    response.put("message", "Session not paid, no action taken");
                }
            } else {
                logger.info("ℹ️ Ignoring webhook event type: {}", eventType);
                response.put("success", true);
                response.put("message", "Event type ignored: " + eventType);
            }
            
        } catch (Exception e) {
            logger.error("💥 Error processing Stripe webhook: ", e);
            response.put("success", false);
            response.put("message", "Error processing webhook: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Parse webhook payload JSON
     */
    private Map<String, Object> parseWebhookPayload(String payload) {
        try {
            // Simple JSON parsing using basic string manipulation
            // In production, use a proper JSON library like Jackson or Gson
            
            // For now, we'll use a basic approach to extract key information
            Map<String, Object> result = new HashMap<>();
            
            if (payload.contains("\"type\":")) {
                String type = extractJsonValue(payload, "type");
                result.put("type", type);
                logger.info("📋 Extracted event type: {}", type);
            }
            
            if (payload.contains("\"data\":")) {
                // Extract session ID if present
                if (payload.contains("\"id\":")) {
                    String sessionId = extractSessionId(payload);
                    if (sessionId != null) {
                        Map<String, Object> data = new HashMap<>();
                        Map<String, Object> object = new HashMap<>();
                        object.put("id", sessionId);
                        
                        // Extract payment_status
                        if (payload.contains("\"payment_status\":")) {
                            String paymentStatus = extractJsonValue(payload, "payment_status");
                            object.put("payment_status", paymentStatus);
                            logger.info("💳 Extracted payment status: {}", paymentStatus);
                        }
                        
                        // Extract payment_intent
                        if (payload.contains("\"payment_intent\":")) {
                            String paymentIntent = extractJsonValue(payload, "payment_intent");
                            object.put("payment_intent", paymentIntent);
                            logger.info("🔗 Extracted payment intent: {}", paymentIntent);
                        }
                        
                        data.put("object", object);
                        result.put("data", data);
                    }
                }
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing webhook payload", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Extract JSON value using simple string manipulation
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return null;
        
        startIndex += searchKey.length();
        
        // Skip whitespace and quotes
        while (startIndex < json.length() && (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '"')) {
            startIndex++;
        }
        
        // Find end of value
        int endIndex = startIndex;
        boolean inQuotes = json.charAt(startIndex - 1) == '"';
        
        if (inQuotes) {
            // Find closing quote
            while (endIndex < json.length() && json.charAt(endIndex) != '"') {
                endIndex++;
            }
        } else {
            // Find comma or closing brace
            while (endIndex < json.length() && json.charAt(endIndex) != ',' && json.charAt(endIndex) != '}') {
                endIndex++;
            }
        }
        
        if (endIndex > startIndex) {
            return json.substring(startIndex, endIndex).trim();
        }
        
        return null;
    }
    
    /**
     * Extract session ID from webhook payload
     */
    private String extractSessionId(String payload) {
        // Look for session ID pattern (cs_test_... or cs_live_...)
        String[] patterns = {"\"id\":\"cs_test_", "\"id\":\"cs_live_", "\"id\":\"cs_"};
        
        for (String pattern : patterns) {
            int startIndex = payload.indexOf(pattern);
            if (startIndex != -1) {
                startIndex += pattern.length();
                int endIndex = payload.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    String sessionId = "cs_" + payload.substring(startIndex, endIndex);
                    logger.info("🔍 Found session ID: {}", sessionId);
                    return sessionId;
                }
            }
        }
        
        return null;
    }
      /**
     * Validate Stripe configuration
     */
    public boolean isConfigurationValid() {
        boolean secretKeyValid = stripeSecretKey != null && 
                                !stripeSecretKey.isEmpty() && 
                                stripeSecretKey.startsWith("sk_");
        
        boolean urlsValid = successUrl != null && !successUrl.isEmpty() &&
                           cancelUrl != null && !cancelUrl.isEmpty();
        
        boolean isValid = secretKeyValid && urlsValid;
        
        if (!isValid) {
            logger.warn("Stripe configuration validation failed - SecretKey valid: {}, URLs valid: {}", 
                       secretKeyValid, urlsValid);
            if (!secretKeyValid) {
                logger.warn("Invalid Stripe secret key: {}", 
                           stripeSecretKey != null ? stripeSecretKey.substring(0, Math.min(10, stripeSecretKey.length())) + "..." : "null");
            }
        }
        
        return isValid;
    }
    
    /**
     * Get current configuration status for debugging
     */
    public Map<String, Object> getConfigurationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("mockMode", mockMode);
        status.put("secretKeyConfigured", stripeSecretKey != null && !stripeSecretKey.isEmpty());
        status.put("secretKeyValid", stripeSecretKey != null && stripeSecretKey.startsWith("sk_"));
        status.put("successUrlConfigured", successUrl != null && !successUrl.isEmpty());
        status.put("cancelUrlConfigured", cancelUrl != null && !cancelUrl.isEmpty());
        status.put("configurationValid", isConfigurationValid());
        
        // Don't expose actual secret key
        status.put("secretKeyPrefix", stripeSecretKey != null && stripeSecretKey.length() > 10 ? 
                                     stripeSecretKey.substring(0, 10) + "..." : "not configured");          return status;
    }
    
    /**
     * Confirm payment with payment method (for real Stripe integration)
     */
    public Map<String, Object> confirmPayment(Long paymentId, String paymentMethodId) {
        logger.info("Confirming Stripe payment: {} with payment method: {}", paymentId, paymentMethodId);
        
        try {
            if (mockMode) {
                return handleMockConfirmPayment(paymentId);
            }
            
            // Get payment from database
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
            
            // Initialize Stripe
            Stripe.apiKey = stripeSecretKey;              // Create PaymentIntent with USD conversion (same as checkout session)
            
            // Convert VND to USD for PaymentIntent
            long originalAmountVND = payment.getAmount().longValue();
            double usdAmount = originalAmountVND / 24000.0;
            long usdAmountCents = Math.max(50, Math.round(usdAmount * 100));
            
            logger.info("=== PAYMENT INTENT VND TO USD CONVERSION ===");
            logger.info("Original VND amount: {}", originalAmountVND);
            logger.info("USD equivalent: ${:.2f}", usdAmount);
            logger.info("USD amount in cents for PaymentIntent: {} cents", usdAmountCents);
            
            Map<String, Object> paymentIntentParams = new HashMap<>();
            paymentIntentParams.put("amount", usdAmountCents); // Use USD cents
            paymentIntentParams.put("currency", "usd"); // Use USD currency
            paymentIntentParams.put("payment_method", paymentMethodId);
            paymentIntentParams.put("confirm", true);
            paymentIntentParams.put("return_url", successUrl + "?paymentId=" + paymentId);
              // Add metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("payment_id", paymentId.toString());
            metadata.put("listing_id", payment.getListingId().toString());
            metadata.put("buyer_id", payment.getBuyerId().toString());
            metadata.put("original_amount_vnd", String.valueOf(originalAmountVND));
            metadata.put("usd_amount_cents", String.valueOf(usdAmountCents));
            metadata.put("conversion_note", "VND converted to USD for Stripe compatibility");
            paymentIntentParams.put("metadata", metadata);
            
            com.stripe.model.PaymentIntent paymentIntent = com.stripe.model.PaymentIntent.create(paymentIntentParams);
              // Update payment with Stripe payment intent ID
            payment.setTransactionId(paymentIntent.getId());
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            paymentRepository.save(payment);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("payment_intent", paymentIntent.toJson());
            result.put("client_secret", paymentIntent.getClientSecret());
            
            if ("succeeded".equals(paymentIntent.getStatus())) {
                // Payment succeeded immediately
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
                result.put("status", "succeeded");
            } else if ("requires_action".equals(paymentIntent.getStatus())) {
                // Requires additional authentication
                result.put("status", "requires_action");
                result.put("next_action", paymentIntent.getNextAction());
            }
            
            logger.info("Stripe payment confirmed successfully: {}", paymentId);
            return result;
            
        } catch (StripeException e) {
            logger.error("Stripe error confirming payment: {}", paymentId, e);
            return createErrorResponse("Stripe error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error confirming Stripe payment: {}", paymentId, e);
            return createErrorResponse("Payment confirmation failed: " + e.getMessage());
        }
    }
      private Map<String, Object> handleMockConfirmPayment(Long paymentId) {        
        logger.info("Mock confirm payment: {}", paymentId);
        
        // Simply mark as completed for mock mode
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionId("mock_pi_" + System.currentTimeMillis());
        payment = paymentRepository.save(payment);
        
        // CRITICAL: Handle payment completion for mock mode too
        logger.info("STRIPE_MOCK_COMPLETION: Triggering completion handler for payment {}", paymentId);
        paymentCompletionService.handlePaymentCompletion(payment);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", "succeeded");
        result.put("message", "Mock payment confirmed successfully");
        
        return result;
    }
    
    /**
     * Create error response map
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("error_code", "STRIPE_ERROR");
        return response;
    }

    /**
     * Handle Stripe success callback and verify payment
     */    public boolean handleSuccessCallback(String sessionId, String paymentIdStr) {
        logger.info("🔥 STRIPE SUCCESS CALLBACK - SessionId: {}, PaymentId: {}", sessionId, paymentIdStr);
        
        try {
            if (mockMode) {
                logger.info("🧪 Mock mode: Simulating successful payment verification");
                return handleMockSuccessCallback(sessionId, paymentIdStr);
            }
            
            // Handle case where only paymentId is provided (Payment Intent flow)
            if ((sessionId == null || sessionId.isEmpty()) && paymentIdStr != null) {
                logger.info("💳 No session ID provided, attempting direct payment verification with paymentId: {}", paymentIdStr);
                return handlePaymentIntentSuccess(paymentIdStr);
            }
            
            // Initialize Stripe
            Stripe.apiKey = stripeSecretKey;
            
            // Retrieve the session from Stripe
            Session session = Session.retrieve(sessionId);
            logger.info("✅ Retrieved Stripe session: {} with payment_status: {}", session.getId(), session.getPaymentStatus());
            
            if ("paid".equals(session.getPaymentStatus())) {
                // Find payment by session ID or payment ID
                Payment payment = findPaymentBySessionOrId(sessionId, paymentIdStr);
                
                if (payment != null) {
                    logger.info("💰 Found payment {} for completion - Offer ID: {}", payment.getId(), payment.getOfferId());
                    
                    // Update payment status to COMPLETED
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setUpdatedAt(LocalDateTime.now());
                    
                    // Store Stripe payment intent ID if available
                    if (session.getPaymentIntent() != null) {
                        payment.setExternalTransactionId(session.getPaymentIntent());
                    }                    paymentRepository.save(payment);
                    logger.info("✅ Payment {} marked as COMPLETED", payment.getId());
                    
                    // 🚀 CRITICAL: Force complete offer (handles missing offerId automatically)
                    logger.info("🚀 FORCE COMPLETING offer for payment {}", payment.getId());
                    forceCompleteOfferFromPayment(payment);
                    logger.info("✨ Force completion FINISHED for payment {}", payment.getId());
                    
                    return true;
                } else {
                    logger.error("❌ Payment not found for session {} or payment ID {}", sessionId, paymentIdStr);
                    return false;
                }
            } else {
                logger.warn("⚠️ Session {} payment status is not 'paid': {}", sessionId, session.getPaymentStatus());
                return false;
            }
            
        } catch (StripeException e) {
            logger.error("🔥 Stripe error while verifying session {}: ", sessionId, e);
            return false;
        } catch (Exception e) {
            logger.error("💥 Error handling Stripe success callback for session {}: ", sessionId, e);
            return false;
        }
    }

    /**
     * Handle mock success callback for testing
     */
    private boolean handleMockSuccessCallback(String sessionId, String paymentIdStr) {
        logger.info("Mock success callback - SessionId: {}, PaymentId: {}", sessionId, paymentIdStr);
        
        try {
            Payment payment = findPaymentBySessionOrId(sessionId, paymentIdStr);
            
            if (payment != null) {
                // Update payment status to COMPLETED
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now());
                payment.setUpdatedAt(LocalDateTime.now());
                payment.setExternalTransactionId("mock_" + sessionId);                paymentRepository.save(payment);
                logger.info("Mock payment {} marked as COMPLETED", payment.getId());
                
                // 🚀 Force complete offer (handles missing offerId automatically)  
                logger.info("🚀 MOCK: Force completing offer for payment {}", payment.getId());
                forceCompleteOfferFromPayment(payment);
                logger.info("Mock payment completion handling triggered for payment {}", payment.getId());
                
                return true;
            } else {
                logger.error("Mock: Payment not found for session {} or payment ID {}", sessionId, paymentIdStr);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error handling mock success callback: ", e);
            return false;
        }
    }

    /**
     * Find payment by session ID or payment ID
     */
    private Payment findPaymentBySessionOrId(String sessionId, String paymentIdStr) {
        Payment payment = null;
        
        // Try to find by session ID (transaction_id)
        if (sessionId != null) {
            payment = paymentRepository.findByTransactionId(sessionId).orElse(null);
            if (payment != null) {
                logger.info("Found payment by session ID: {}", payment.getId());
                return payment;
            }
        }
        
        // Try to find by payment ID
        if (paymentIdStr != null) {
            try {
                Long paymentId = Long.parseLong(paymentIdStr);
                payment = paymentRepository.findById(paymentId).orElse(null);
                if (payment != null) {
                    logger.info("Found payment by payment ID: {}", payment.getId());
                    return payment;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid payment ID format: {}", paymentIdStr);
            }
        }
        
        logger.warn("Payment not found for session {} or payment ID {}", sessionId, paymentIdStr);
        return null;
    }
    
    /**
     * Handle Payment Intent success when only paymentId is provided
     */    private boolean handlePaymentIntentSuccess(String paymentIdStr) {
        logger.info("Handling Payment Intent success for paymentId: {}", paymentIdStr);
        
        try {
            Long paymentId = Long.parseLong(paymentIdStr);
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            
            if (payment == null) {
                logger.error("Payment not found for ID: {}", paymentId);
                return false;
            }
            
            logger.info("💡 PAYMENT_INTENT: Found payment {} with status: {}", paymentId, payment.getStatus());
            
            // Check if payment is already completed
            if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
                logger.info("⚠️ Payment {} is already completed, STILL calling force completion!", paymentId);
                
                // 🚀 CRITICAL FIX: Always call force completion even if payment is already completed
                logger.info("🚀 PAYMENT_INTENT: Force completing offer for payment {} (already completed)", payment.getId());
                forceCompleteOfferFromPayment(payment);
                logger.info("✅ Payment completion handling triggered for payment {}", payment.getId());
                
                return true;
            }
            
            // Update payment status to COMPLETED
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            
            logger.info("Payment {} marked as COMPLETED via Payment Intent flow", payment.getId());
            
            // 🚀 Force complete offer (handles missing offerId automatically)
            logger.info("🚀 PAYMENT_INTENT: Force completing offer for payment {}", payment.getId());
            forceCompleteOfferFromPayment(payment);
            logger.info("✅ Payment completion handling triggered for payment {}", payment.getId());
            
            return true;
            
        } catch (NumberFormatException e) {
            logger.error("Invalid payment ID format: {}", paymentIdStr);
            return false;
        } catch (Exception e) {
            logger.error("Error handling Payment Intent success: ", e);
            return false;
        }
    }
    
    /**
     * Auto-check and complete successful Stripe payments that weren't properly completed
     */
    public Map<String, Object> autoCompleteSuccessfulPayments() {
        logger.info("🔄 Starting auto-completion check for Stripe payments...");
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> completedPayments = new ArrayList<>();
        int totalChecked = 0;
        int totalCompleted = 0;
        
        try {
            // Find all COMPLETED Stripe payments that might not have completed offers
            List<Payment> stripePayments = paymentRepository.findAll().stream()
                .filter(p -> "STRIPE".equals(p.getPaymentMethodType()))
                .filter(p -> Payment.PaymentStatus.COMPLETED.equals(p.getStatus()))
                .filter(p -> p.getTransactionId() != null && p.getTransactionId().startsWith("pi_"))
                .toList();
            
            logger.info("📋 Found {} completed Stripe payments to check", stripePayments.size());
            
            for (Payment payment : stripePayments) {
                totalChecked++;
                
                try {
                    // Check if offer needs completion
                    if (payment.getOfferId() != null) {
                        Optional<Offer> offerOpt = offerRepository.findById(payment.getOfferId());
                        
                        if (offerOpt.isPresent()) {
                            Offer offer = offerOpt.get();
                            
                            // If offer is not COMPLETED or hasPaidTransaction is false, trigger completion
                            if (offer.getStatus() != OfferStatus.COMPLETED || 
                                (offer.getHasPaidTransaction() == null || !offer.getHasPaidTransaction())) {
                                
                                logger.info("🚀 AUTO-COMPLETING: Payment {} with offer {} - Current offer status: {}, hasPaidTransaction: {}", 
                                    payment.getId(), offer.getId(), offer.getStatus(), offer.getHasPaidTransaction());
                                
                                // Trigger completion
                                paymentCompletionService.handlePaymentCompletion(payment);
                                totalCompleted++;
                                
                                completedPayments.add(Map.of(
                                    "paymentId", payment.getId(),
                                    "offerId", offer.getId(),
                                    "transactionId", payment.getTransactionId(),
                                    "amount", payment.getAmount(),
                                    "previousStatus", offer.getStatus().toString(),
                                    "newStatus", "COMPLETED"
                                ));
                                
                                logger.info("✅ AUTO-COMPLETED: Payment {} and offer {}", payment.getId(), offer.getId());
                            } else {
                                logger.info("ℹ️ SKIP: Payment {} offer {} already properly completed", payment.getId(), offer.getId());
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    logger.error("❌ Error auto-completing payment {}: ", payment.getId(), e);
                }
            }
            
            result.put("success", true);
            result.put("message", String.format("Auto-completion check completed. Checked: %d, Completed: %d", totalChecked, totalCompleted));
            result.put("totalChecked", totalChecked);
            result.put("totalCompleted", totalCompleted);
            result.put("completedPayments", completedPayments);
            
            logger.info("🎉 Auto-completion finished: {}/{} payments completed", totalCompleted, totalChecked);
            
        } catch (Exception e) {
            logger.error("💥 Error during auto-completion: ", e);
            result.put("success", false);
            result.put("message", "Error during auto-completion: " + e.getMessage());
        }
        
        return result;
    }
      /**
     * Force complete offer for a payment (auto-update offer status regardless of offerId)
     * This is called from payment success callback to ensure offer is always updated
     */
    public void forceCompleteOfferFromPayment(Payment payment) {
        try {
            logger.info("🔄 FORCE COMPLETING offer from payment {}", payment.getId());
            logger.info("Payment details - Listing: {}, Buyer: {}, Seller: {}, Amount: {}", 
                    payment.getListingId(), payment.getBuyerId(), payment.getSellerId(), payment.getAmount());
            
            // Strategy 1: If payment has offerId, use normal completion
            if (payment.getOfferId() != null) {
                logger.info("✅ Payment has offerId {}, using normal completion", payment.getOfferId());
                paymentCompletionService.handlePaymentCompletion(payment);
                return;
            }              // Strategy 2: Find offer by payment details (listingId + buyerId + status ACCEPTED)
            logger.info("🔍 Payment has no offerId, searching for matching offer...");
            Optional<Offer> matchingOfferOpt = offerRepository.findByListingIdAndBuyerIdAndStatus(
                payment.getListingId(), 
                payment.getBuyerId(), 
                OfferStatus.ACCEPTED
            );
            
            if (matchingOfferOpt.isPresent()) {
                Offer offer = matchingOfferOpt.get();
                logger.info("🎯 Found matching offer {} for payment {}", offer.getId(), payment.getId());
                
                // Update payment with correct offerId
                payment.setOfferId(offer.getId());
                paymentRepository.save(payment);
                logger.info("💾 Updated payment {} with offerId {}", payment.getId(), offer.getId());
                
                // Now complete normally
                paymentCompletionService.handlePaymentCompletion(payment);
                logger.info("✅ FORCE COMPLETION SUCCESS for payment {} and offer {}", payment.getId(), offer.getId());
                return;
            }
              // Strategy 3: Fallback - manual offer status update if no completion service works
            logger.warn("⚠️ No matching offer found or normal completion failed");
            logger.info("💡 Attempting direct offer status update for listing {}, buyer {}", 
                    payment.getListingId(), payment.getBuyerId());
            
            // Find any ACCEPTED offer for this listing and buyer (try different status)
            List<OfferStatus> statusesToCheck = List.of(OfferStatus.ACCEPTED, OfferStatus.PENDING);
            
            for (OfferStatus status : statusesToCheck) {
                Optional<Offer> offerOpt = offerRepository.findByListingIdAndBuyerIdAndStatus(
                    payment.getListingId(), 
                    payment.getBuyerId(),
                    status
                );
                
                if (offerOpt.isPresent()) {
                    Offer offer = offerOpt.get();
                    logger.info("🔧 DIRECT UPDATE: Setting offer {} (status: {}) to COMPLETED", offer.getId(), status);
                    offer.setStatus(OfferStatus.COMPLETED);
                    offer.setHasPaidTransaction(true);
                    offer.setUpdatedAt(LocalDateTime.now());
                    offerRepository.save(offer);
                    
                    // Also update payment with offerId
                    payment.setOfferId(offer.getId());
                    paymentRepository.save(payment);
                    
                    logger.info("✅ DIRECT UPDATE SUCCESS: offer {} now COMPLETED", offer.getId());
                    return;
                }
            }
            
            logger.error("❌ No suitable offer found for payment {}", payment.getId());
            
        } catch (Exception e) {
            logger.error("💥 Error force completing offer from payment {}: ", payment.getId(), e);
        }
    }
}
