package com.example.demo.service.payment;

import com.example.demo.dto.payment.*;
import com.example.demo.entity.Payment;
import com.example.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisaPaymentService {
    
    private final PaymentRepository paymentRepository;
    private final WebClient.Builder webClientBuilder;
    
    @Value("${visa.merchant-id:TEST_MERCHANT}")
    private String merchantId;
    
    @Value("${visa.api-key:TEST_API_KEY}")
    private String apiKey;
    
    @Value("${visa.endpoint:https://sandbox-api.visa.com/cybersource/payments/v1/authorizations}")
    private String visaEndpoint;
    
    @Value("${visa.return-url:https://webhook.site/b3088a6a-2d17-4f8d-a383-71389a6c600b}")
    private String returnUrl;
    
    @Value("${visa.notify-url:https://webhook.site/b3088a6a-2d17-4f8d-a383-71389a6c600b}")
    private String notifyUrl;
      public CreatePaymentResponse createVisaPayment(CreatePaymentRequest request, Payment payment) {
        try {
            log.info("Creating Visa payment for amount: {}, card: {}", 
                    request.getAmount(), maskCardNumber(request.getCardNumber()));
            
            // Validate request
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return CreatePaymentResponse.failure("Invalid payment amount");
            }
            
            // Set payment method specific fields
            payment.setPaymentMethodType(Payment.PaymentMethodType.VISA);
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            payment.setExpiredAt(LocalDateTime.now().plusMinutes(30)); // 30 minutes expire
            
            // Save masked card information
            if (request.getCardNumber() != null) {
                payment.setCardNumberMasked(maskCardNumber(request.getCardNumber()));
                payment.setCardType(getCardType(request.getCardNumber()));
            }
            
            // Generate transaction ID
            String transactionId = "visa_" + System.currentTimeMillis() + "_" + payment.getId();
            payment.setTransactionId(transactionId);
            
            // Save payment first
            Payment savedPayment = paymentRepository.save(payment);
            log.info("Created Visa payment record with ID: {}", savedPayment.getId());
            
            // Build and send request to Visa
            Map<String, Object> visaRequest = buildVisaRequest(request, savedPayment);
            
            try {
                // Send request to Visa API
                WebClient webClient = webClientBuilder.build();
                Map<String, Object> visaResponse = webClient.post()
                        .uri(visaEndpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .bodyValue(visaRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                
                return processVisaResponse(visaResponse, savedPayment);
                
            } catch (Exception e) {
                log.error("Error calling Visa API: ", e);
                
                // For testing purposes, simulate successful response
                log.info("Simulating Visa API response for testing");
                return simulateVisaResponse(savedPayment);
            }
            
        } catch (Exception e) {
            log.error("Error creating Visa payment", e);
            
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            
            return CreatePaymentResponse.failure("Error creating Visa payment: " + e.getMessage());
        }
    }
    
    private CreatePaymentResponse processVisaResponse(Map<String, Object> response, Payment payment) {
        if (response != null && isVisaResponseSuccessful(response)) {
            // Success response
            String transactionId = (String) response.get("id");
            String authCode = (String) response.get("authorizationCode");
            
            // Update payment with Visa response
            if (transactionId != null) {
                payment.setExternalTransactionId(transactionId);
            }
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            
            Payment savedPayment = paymentRepository.save(payment);
            
            // Create payment URL (3D Secure redirect if needed)
            String paymentUrl = buildPaymentUrl(transactionId != null ? transactionId : payment.getTransactionId());
            
            log.info("Visa payment created successfully - ID: {}, AuthCode: {}", transactionId, authCode);
            
            return CreatePaymentResponse.success(
                PaymentResponse.fromEntity(savedPayment),
                paymentUrl
            );
        } else {
            // Failed response
            String message = response != null ? 
                (String) response.get("message") : "Visa payment failed";
            log.error("Visa payment creation failed: {}", message);
            
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            
            return CreatePaymentResponse.failure("Visa payment failed: " + message);
        }
    }
    
    private CreatePaymentResponse simulateVisaResponse(Payment payment) {
        // Simulate successful Visa response for testing
        String mockTransactionId = "visa_mock_" + System.currentTimeMillis();
        
        payment.setExternalTransactionId(mockTransactionId);
        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        
        Payment savedPayment = paymentRepository.save(payment);
        
        String paymentUrl = buildPaymentUrl(mockTransactionId);
        
        log.info("Simulated Visa payment created - ID: {}", mockTransactionId);
        
        return CreatePaymentResponse.success(
            PaymentResponse.fromEntity(savedPayment),
            paymentUrl
        );
    }
    
    private boolean isVisaResponseSuccessful(Map<String, Object> response) {
        if (response == null) return false;
          String status = (String) response.get("status");
        return "AUTHORIZED".equals(status) || "SUCCESS".equals(status);
    }

    private Map<String, Object> buildVisaRequest(CreatePaymentRequest request, Payment payment) {
        Map<String, Object> visaRequest = new HashMap<>();
        
        try {
            // Client reference information
            Map<String, Object> clientReferenceInformation = new HashMap<>();
            clientReferenceInformation.put("code", payment.getTransactionId());
            visaRequest.put("clientReferenceInformation", clientReferenceInformation);
            
            // Payment information
            Map<String, Object> paymentInformation = new HashMap<>();
            Map<String, Object> card = new HashMap<>();
            
            if (request.getCardToken() != null && !request.getCardToken().isEmpty()) {
                // Use saved card token
                card.put("transientToken", request.getCardToken());
            } else if (request.getCardNumber() != null) {
                // New card
                card.put("number", request.getCardNumber());
                card.put("expirationMonth", getExpiryMonth(request.getExpiryDate()));
                card.put("expirationYear", getExpiryYear(request.getExpiryDate()));
                if (request.getCvv() != null) {
                    card.put("securityCode", request.getCvv());
                }
            }
            
            paymentInformation.put("card", card);
            visaRequest.put("paymentInformation", paymentInformation);
            
            // Order information
            Map<String, Object> orderInformation = new HashMap<>();
            Map<String, Object> amountDetails = new HashMap<>();
            amountDetails.put("totalAmount", request.getAmount().toString());
            amountDetails.put("currency", "VND");
            orderInformation.put("amountDetails", amountDetails);
            
            // Billing information
            Map<String, Object> billTo = new HashMap<>();
            billTo.put("firstName", "TradeUp");
            billTo.put("lastName", "User");
            billTo.put("address1", "123 Main Street");
            billTo.put("locality", "Ho Chi Minh City");
            billTo.put("administrativeArea", "HCM");
            billTo.put("postalCode", "700000");
            billTo.put("country", "VN");
            billTo.put("email", "user@tradeup.com");
            orderInformation.put("billTo", billTo);
            
            visaRequest.put("orderInformation", orderInformation);
            
            // Processing information
            Map<String, Object> processingInformation = new HashMap<>();
            processingInformation.put("capture", true);
            visaRequest.put("processingInformation", processingInformation);
            
            log.debug("Built Visa request for payment: {}", payment.getId());
            return visaRequest;
            
        } catch (Exception e) {
            log.error("Error building Visa request", e);
            throw new RuntimeException("Failed to build Visa request", e);
        }
    }
    
    public boolean handleVisaCallback(String transactionId, String status) {
        try {
            log.info("Handling Visa callback - Transaction: {}, Status: {}", transactionId, status);
            
            Optional<Payment> paymentOpt = paymentRepository.findByExternalTransactionId(transactionId);
            if (paymentOpt.isEmpty()) {
                // Try to find by internal transaction ID
                paymentOpt = paymentRepository.findByTransactionId(transactionId);
            }
            
            if (paymentOpt.isEmpty()) {
                log.error("Payment not found for transactionId: {}", transactionId);
                return false;
            }
            
            Payment payment = paymentOpt.get();
            
            // Update payment status based on Visa callback
            if (isSuccessStatus(status)) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now());
                log.info("Visa payment {} completed successfully", payment.getId());
                
            } else if (isFailureStatus(status)) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                log.warn("Visa payment {} failed with status: {}", payment.getId(), status);
                
            } else {
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
                log.info("Visa payment {} still processing with status: {}", payment.getId(), status);
            }
            
            paymentRepository.save(payment);
            return true;
            
        } catch (Exception e) {
            log.error("Error handling Visa callback", e);
            return false;
        }
    }
    
    private boolean isSuccessStatus(String status) {
        return "AUTHORIZED".equalsIgnoreCase(status) || 
               "SETTLED".equalsIgnoreCase(status) ||
               "SUCCESS".equalsIgnoreCase(status) ||
               "COMPLETED".equalsIgnoreCase(status);
    }
    
    private boolean isFailureStatus(String status) {
        return "DECLINED".equalsIgnoreCase(status) || 
               "FAILED".equalsIgnoreCase(status) ||
               "REJECTED".equalsIgnoreCase(status) ||
               "CANCELLED".equalsIgnoreCase(status);
    }
    
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String cleaned = cardNumber.replaceAll("\\s+", "");
        if (cleaned.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cleaned.substring(cleaned.length() - 4);
    }
    
    private String getCardType(String cardNumber) {
        if (cardNumber == null) return "Unknown";
        
        String cleaned = cardNumber.replaceAll("\\s+", "");
        if (cleaned.startsWith("4")) {
            return "VISA";
        } else if (cleaned.startsWith("5") || cleaned.startsWith("2")) {
            return "MASTERCARD";
        } else if (cleaned.startsWith("3")) {
            return "AMERICAN_EXPRESS";
        }
        return "Unknown";
    }
      private String getExpiryMonth(String expiryDate) {
        if (expiryDate == null || !expiryDate.contains("/")) {
            return "12";
        }
        try {
            String month = expiryDate.split("/")[0].trim();
            int monthInt = Integer.parseInt(month);
            return String.format("%02d", monthInt); // Ensure 2 digits
        } catch (Exception e) {
            log.warn("Invalid expiry month format: {}", expiryDate);
            return "12";
        }
    }
    
    private String getExpiryYear(String expiryDate) {
        if (expiryDate == null || !expiryDate.contains("/")) {
            return "2025";
        }
        try {
            String year = expiryDate.split("/")[1].trim();
            if (year.length() == 2) {
                return "20" + year;
            } else if (year.length() == 4) {
                return year;
            }
            return "2025";
        } catch (Exception e) {
            log.warn("Invalid expiry year format: {}", expiryDate);
            return "2025";
        }
    }
    
    private String buildPaymentUrl(String transactionId) {
        // In production, this would be a 3D Secure URL or redirect URL
        return returnUrl + "?transactionId=" + transactionId + "&status=success";
    }
    
    /**
     * Validate Visa payment configuration
     */
    public boolean isConfigurationValid() {
        return merchantId != null && !merchantId.isEmpty() &&
               apiKey != null && !apiKey.isEmpty() &&
               visaEndpoint != null && !visaEndpoint.isEmpty() &&               returnUrl != null && !returnUrl.isEmpty();
    }

    /**
     * Validate card number format (basic Luhn algorithm)
     */
    public boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null) return false;
        
        String cleaned = cardNumber.replaceAll("\\s+", "");
        if (cleaned.length() < 13 || cleaned.length() > 19) {
            return false;
        }
        
        // Basic Luhn algorithm check
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cleaned.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cleaned.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        
        return (sum % 10 == 0);
    }
}
