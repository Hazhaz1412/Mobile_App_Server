package com.example.demo.service.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.model.Price;
import com.stripe.param.checkout.SessionCreateParams;
import com.example.demo.entity.Payment;
import com.example.demo.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    private boolean mockMode;

    private final PaymentRepository paymentRepository;
    private final PaymentMockService paymentMockService;

    public StripePaymentService(PaymentRepository paymentRepository, PaymentMockService paymentMockService) {
        this.paymentRepository = paymentRepository;
        this.paymentMockService = paymentMockService;
    }    public Map<String, Object> createPaymentSession(Long listingId, Double amount, String description, Long buyerId, Long sellerId) {
        logger.info("Creating Stripe payment session for listing: {}, amount: {}, mockMode: {}", listingId, amount, mockMode);

        try {
            // Validate input parameters
            if (listingId == null || amount == null || amount <= 0 || buyerId == null || sellerId == null) {
                logger.error("Invalid payment parameters: listingId={}, amount={}, buyerId={}, sellerId={}", 
                           listingId, amount, buyerId, sellerId);
                return createErrorResponse("Invalid payment parameters");
            }

            // Use mock service if in mock mode
            if (mockMode) {
                logger.info("Using mock mode for Stripe payment");
                return paymentMockService.createMockStripeSession(listingId, amount, description, buyerId, sellerId, successUrl);
            }

            // Real Stripe mode - validate configuration first
            if (!isConfigurationValid()) {
                logger.error("Invalid Stripe configuration - missing or invalid keys");
                return createErrorResponse("Stripe configuration is invalid. Please check your API keys.");
            }

            // Create payment record in database first
            Payment payment = new Payment();
            payment.setListingId(listingId);
            payment.setAmount(BigDecimal.valueOf(amount));
            payment.setDescription(description != null ? description : "TradeUp Purchase");
            payment.setBuyerId(buyerId);
            payment.setSellerId(sellerId);
            payment.setPaymentMethodType(Payment.PaymentMethodType.STRIPE);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            
            Payment savedPayment = paymentRepository.save(payment);
            logger.info("Created payment record with ID: {}", savedPayment.getId());            // Initialize Stripe with secret key
            Stripe.apiKey = stripeSecretKey;            // WORKAROUND: Stripe thêm 3 số 0 vào cuối số tiền VND, vậy ta cắt bớt 3 số 0            // Convert VND to USD to meet Stripe's minimum requirements
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
    }
    
    /**
     * Handle Stripe webhook events (for production use)
     */
    public Map<String, Object> handleWebhook(String payload, String sigHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // In production, verify webhook signature here
            logger.info("Processing Stripe webhook");
            
            response.put("success", true);
            response.put("message", "Webhook processed successfully");
            
        } catch (Exception e) {
            logger.error("Error processing Stripe webhook: ", e);
            response.put("success", false);
            response.put("message", "Error processing webhook: " + e.getMessage());
        }
        
        return response;
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
    
    private Map<String, Object> handleMockConfirmPayment(Long paymentId) {        logger.info("Mock confirm payment: {}", paymentId);
        
        // Simply mark as completed for mock mode
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionId("mock_pi_" + System.currentTimeMillis());
        paymentRepository.save(payment);
        
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
}
