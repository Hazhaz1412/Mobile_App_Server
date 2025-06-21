package com.example.demo.service.payment;

import com.example.demo.entity.Payment;
import com.example.demo.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class PaymentMockService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentMockService.class);
    
    private final PaymentRepository paymentRepository;
    private final PaymentCompletionService paymentCompletionService;
    private final Random random = new Random();
    
    public PaymentMockService(PaymentRepository paymentRepository, PaymentCompletionService paymentCompletionService) {
        this.paymentRepository = paymentRepository;
        this.paymentCompletionService = paymentCompletionService;
    }
      /**
     * Create a mock Stripe payment session for testing
     */
    public Map<String, Object> createMockStripeSession(Long listingId, Double amount, String description, 
                                                      Long buyerId, Long sellerId, String successUrl, Long offerId) {
        logger.info("Creating mock Stripe payment session for listing: {}, amount: {}, offerId: {}", listingId, amount, offerId);
        
        try {            // Create payment record in database
            Payment payment = new Payment();
            payment.setListingId(listingId);
            payment.setOfferId(offerId); // 🔥 CRITICAL FIX: Set offerId!
            payment.setAmount(BigDecimal.valueOf(amount));
            payment.setDescription(description);
            payment.setBuyerId(buyerId);
            payment.setSellerId(sellerId);
            payment.setPaymentMethodType(Payment.PaymentMethodType.STRIPE);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            
            Payment savedPayment = paymentRepository.save(payment);
            logger.info("Created mock payment record with ID: {}", savedPayment.getId());
            
            // Generate mock session data
            String mockSessionId = "cs_test_mock_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
            String mockCheckoutUrl = createMockCheckoutUrl(mockSessionId, savedPayment.getId(), successUrl);
            
            // Update payment with mock session ID
            savedPayment.setTransactionId(mockSessionId);
            savedPayment.setExpiredAt(LocalDateTime.now().plusMinutes(30));
            paymentRepository.save(savedPayment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mock Stripe session created successfully");
            response.put("sessionId", mockSessionId);
            response.put("checkoutUrl", mockCheckoutUrl);
            response.put("paymentId", savedPayment.getId());
            response.put("status", "pending");
            response.put("amount", amount);
            response.put("currency", "VND");
            response.put("expiresAt", System.currentTimeMillis() / 1000 + 1800); // 30 minutes
            response.put("mockMode", true);
            
            logger.info("Mock Stripe session created successfully: {}", mockSessionId);
            return response;
            
        } catch (Exception e) {
            logger.error("Error creating mock Stripe session", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create mock payment session: " + e.getMessage());
            return errorResponse;
        }
    }
    
    /**
     * Create a mock MoMo payment session for testing
     */
    public Map<String, Object> createMockMoMoSession(Long listingId, Double amount, String description, 
                                                    Long buyerId, Long sellerId) {
        logger.info("Creating mock MoMo payment session for listing: {}, amount: {}", listingId, amount);
        
        try {
            // Create payment record in database
            Payment payment = new Payment();
            payment.setListingId(listingId);
            payment.setAmount(BigDecimal.valueOf(amount));
            payment.setDescription(description);
            payment.setBuyerId(buyerId);
            payment.setSellerId(sellerId);
            payment.setPaymentMethodType(Payment.PaymentMethodType.MOMO);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            
            Payment savedPayment = paymentRepository.save(payment);
            logger.info("Created mock MoMo payment record with ID: {}", savedPayment.getId());
            
            // Generate mock MoMo data
            String mockTransactionId = "MM" + System.currentTimeMillis() + random.nextInt(1000);
            String mockOrderId = "ORDER_" + System.currentTimeMillis();
            String mockPayUrl = "https://test-payment.momo.vn/gw_payment/transactionProcessor";
            String mockQrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + mockPayUrl;
            String mockDeeplink = "momo://app?action=payWithApp&isScanQR=true&hash=" + mockTransactionId;
            
            // Update payment with mock transaction ID
            savedPayment.setTransactionId(mockTransactionId);
            savedPayment.setExpiredAt(LocalDateTime.now().plusMinutes(15));
            paymentRepository.save(savedPayment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mock MoMo session created successfully");
            response.put("transactionId", mockTransactionId);
            response.put("orderId", mockOrderId);
            response.put("payUrl", mockPayUrl);
            response.put("qrCodeUrl", mockQrCodeUrl);
            response.put("deeplink", mockDeeplink);
            response.put("paymentId", savedPayment.getId());
            response.put("status", "pending");
            response.put("amount", amount);
            response.put("currency", "VND");
            response.put("mockMode", true);
            
            logger.info("Mock MoMo session created successfully: {}", mockTransactionId);
            return response;
            
        } catch (Exception e) {
            logger.error("Error creating mock MoMo session", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create mock MoMo session: " + e.getMessage());
            return errorResponse;
        }
    }
    
    /**
     * Simulate payment completion for testing
     */
    public Map<String, Object> simulatePaymentSuccess(Long paymentId, String transactionId) {
        logger.info("Simulating payment success for payment: {}, transaction: {}", paymentId, transactionId);
        
        try {
            Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));            // Update payment status to completed
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            
            // Save payment first
            payment = paymentRepository.save(payment);
            
            // CRITICAL: Handle payment completion (lock offers, create transactions)
            logger.info("MOCK_COMPLETION: Triggering completion handler for payment {}", paymentId);
            paymentCompletionService.handlePaymentCompletion(payment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment completed successfully (Mock)");
            response.put("paymentId", paymentId);
            response.put("transactionId", transactionId);
            response.put("status", "completed");
            response.put("mockMode", true);
            
            logger.info("Mock payment completed successfully: {}", paymentId);
            return response;
            
        } catch (Exception e) {
            logger.error("Error simulating payment success", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to simulate payment success: " + e.getMessage());
            return errorResponse;
        }
    }
    
    private String createMockCheckoutUrl(String sessionId, Long paymentId, String successUrl) {
        // Create a mock checkout URL that will redirect to success after some time
        return successUrl.replace("/success/stripe", "/stripe/mock-checkout") + 
               "?session_id=" + sessionId + "&payment_id=" + paymentId;
    }
}
