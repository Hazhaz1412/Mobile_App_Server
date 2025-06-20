package com.example.demo.service.payment;

import com.example.demo.dto.payment.*;
import com.example.demo.entity.Payment;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.util.MoMoSignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoMoPaymentService {
      private final PaymentRepository paymentRepository;
    private final WebClient.Builder webClientBuilder;
    private final MoMoSignatureUtil momoSignatureUtil;
    private final PaymentCompletionService paymentCompletionService;
    
    // MoMo API Configuration theo official docs
    @Value("${momo.partner-code:MOMO}")
    private String partnerCode;
    
    @Value("${momo.partner-name:Test}")
    private String partnerName;
    
    @Value("${momo.store-id:MomoTestStore}")
    private String storeId;
    
    @Value("${momo.access-key:F8BBA842ECF85}")
    private String accessKey;
    
    @Value("${momo.secret-key:K951B6PE1waDMi640xX08PD3vg6EkVlz}")
    private String secretKey;
    
    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;
      @Value("${momo.return-url:https://zn8vnhrf-8080.asse.devtunnels.ms/payment-return}")
    private String returnUrl;
      @Value("${momo.notify-url:https://zn8vnhrf-8080.asse.devtunnels.ms/api/v1/payments/callback/momo}")
    private String notifyUrl;
    
    @Value("${momo.mock-mode:false}")
    private boolean mockMode;    /**
     * Tạo MoMo payment theo official API specification
     */
    public CreatePaymentResponse createMoMoPayment(CreatePaymentRequest request, Payment payment) {
        try {
            if (mockMode) {
                // Tạo mock response cho MoMo để testing
                log.info("MOCK MODE: Creating mock MoMo payment for payment ID: {}", payment.getId());
                
                // Tạo các URL giả lập
                String baseUrl = "https://momo-simulator.tradup.vn/pay";
                String payUrl = baseUrl + "?orderId=" + payment.getTransactionId() + "&amount=" + request.getAmount();
                String qrCodeUrl = baseUrl + "/qr?orderId=" + payment.getTransactionId();
                String deeplink = "momo://app?action=paymentSuccess&orderId=" + payment.getTransactionId();
                String deeplinkMiniApp = deeplink + "&miniApp=true";
                
                // Cập nhật payment với thông tin mock
                payment.setExternalTransactionId("MOCK_" + payment.getTransactionId());
                payment.setPaymentUrl(payUrl);
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
                payment.setExpiredAt(LocalDateTime.now().plusMinutes(15)); // 15 phút expire
                payment.setUpdatedAt(LocalDateTime.now());
                
                Payment savedPayment = paymentRepository.save(payment);
                
                log.info("MOCK MODE: Successfully created mock MoMo payment: {}", payUrl);
                
                return CreatePaymentResponse.builder()
                    .success(true)
                    .message("Tạo giao dịch MoMo (giả lập) thành công")
                    .payment(PaymentResponse.fromEntity(savedPayment))
                    .paymentUrl(payUrl)
                    .qrCodeUrl(qrCodeUrl)
                    .deeplink(deeplink)
                    .deeplinkMiniApp(deeplinkMiniApp)
                    .build();
            }
            
            // Tạo request theo MoMo API format
            Map<String, Object> momoRequest = buildMoMoRequest(request, payment);
            
            log.info("Sending MoMo payment request: {}", momoRequest);
            
            // Gửi request tới MoMo
            WebClient webClient = webClientBuilder.build();
            Map<String, Object> response = webClient.post()
                    .uri(momoEndpoint)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(momoRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            log.info("MoMo response: {}", response);
            
            if (response != null && Integer.valueOf(0).equals(response.get("resultCode"))) {
                // Thành công
                String payUrl = (String) response.get("payUrl");
                String qrCodeUrl = (String) response.get("qrCodeUrl");
                String deeplink = (String) response.get("deeplink");
                String deeplinkMiniApp = (String) response.get("deeplinkMiniApp");
                
                // Cập nhật payment với thông tin từ MoMo
                payment.setExternalTransactionId((String) response.get("orderId"));
                payment.setPaymentUrl(payUrl);
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
                payment.setExpiredAt(LocalDateTime.now().plusMinutes(15)); // 15 phút expire
                payment.setUpdatedAt(LocalDateTime.now());
                
                Payment savedPayment = paymentRepository.save(payment);
                
                return CreatePaymentResponse.builder()
                    .success(true)
                    .message("Tạo giao dịch MoMo thành công")
                    .payment(PaymentResponse.fromEntity(savedPayment))
                    .paymentUrl(payUrl)
                    .qrCodeUrl(qrCodeUrl)
                    .deeplink(deeplink)
                    .deeplinkMiniApp(deeplinkMiniApp)
                    .build();
            } else {
                // Thất bại
                String message = response != null ? (String) response.get("message") : "Lỗi kết nối MoMo";
                Integer resultCode = response != null ? (Integer) response.get("resultCode") : -1;
                log.error("MoMo payment creation failed: resultCode={}, message={}", resultCode, message);
                
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                
                return CreatePaymentResponse.builder()
                    .success(false)
                    .message("Tạo thanh toán MoMo thất bại: " + message)
                    .payment(PaymentResponse.fromEntity(payment))
                    .build();
            }            
        } catch (Exception e) {
            log.error("Error creating MoMo payment", e);
            
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            
            return CreatePaymentResponse.builder()
                .success(false)
                .message("Lỗi hệ thống: " + e.getMessage())
                .payment(PaymentResponse.fromEntity(payment))
                .build();
        }
    }
    
    /**
     * Build MoMo request theo official API format
     */
    private Map<String, Object> buildMoMoRequest(CreatePaymentRequest request, Payment payment) 
            throws NoSuchAlgorithmException, InvalidKeyException {
          String orderId = payment.getTransactionId();
        String requestId = orderId; // Có thể giống orderId hoặc unique
        long amount = request.getAmount().longValue();
        
        // Validate amount (MoMo yêu cầu >= 1000 VND)
        if (amount < 1000) {
            throw new IllegalArgumentException("Amount must be at least 1000 VND");
        }
        
        String orderInfo = request.getDescription() != null ? 
            request.getDescription() : "Thanh toán đơn hàng " + request.getListingId();
        String extraData = ""; // Base64 encoded JSON, default empty
        String requestType = "captureWallet"; // Fixed theo API
        String lang = "vi"; // Vietnamese
        
        // Override URLs nếu có trong request
        String redirectUrl = request.getReturnUrl() != null ? request.getReturnUrl() : returnUrl;
        String ipnUrl = request.getNotifyUrl() != null ? request.getNotifyUrl() : notifyUrl;        // Build signature theo MoMo documentation mới nhất
        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;
        String signature = momoSignatureUtil.hmacSHA256(rawSignature, secretKey);
        
        log.info("MoMo signature data: {}", rawSignature);
        log.info("MoMo signature: {}", signature);
        
        // Build request theo official MoMo API format
        Map<String, Object> momoRequest = new HashMap<>();
        momoRequest.put("partnerCode", partnerCode);
        momoRequest.put("partnerName", partnerName);
        momoRequest.put("storeId", storeId);
        momoRequest.put("requestType", requestType);
        momoRequest.put("ipnUrl", ipnUrl);
        momoRequest.put("redirectUrl", redirectUrl);
        momoRequest.put("orderId", orderId);
        momoRequest.put("amount", amount);
        momoRequest.put("lang", lang);
        momoRequest.put("orderInfo", orderInfo);
        momoRequest.put("requestId", requestId);
        momoRequest.put("extraData", extraData);
        momoRequest.put("signature", signature);
        // NOTE: accessKey không gửi trong request body, chỉ dùng để tạo signature
        
        return momoRequest;
    }
    
    /**
     * Xử lý callback IPN từ MoMo
     */
    public boolean handleMoMoCallback(Map<String, Object> callbackData) {
        try {
            log.info("Received MoMo IPN callback: {}", callbackData);
            
            String orderId = (String) callbackData.get("orderId");
            Integer resultCode = (Integer) callbackData.get("resultCode");
            String transId = String.valueOf(callbackData.get("transId"));
            String signature = (String) callbackData.get("signature");
            
            // Verify signature để đảm bảo tính toàn vẹn dữ liệu
            if (!verifyMoMoSignature(callbackData)) {
                log.error("Invalid MoMo signature for orderId: {}", orderId);
                return false;
            }
            
            Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(orderId);
            if (paymentOpt.isEmpty()) {
                log.error("Payment not found for orderId: {}", orderId);
                return false;
            }
            
            Payment payment = paymentOpt.get();
              if (Integer.valueOf(0).equals(resultCode)) {
                // Thanh toán thành công
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now());
                payment.setExternalTransactionId(transId);
                log.info("Payment completed successfully: orderId={}, transId={}", orderId, transId);
                
                // Save payment first
                payment.setUpdatedAt(LocalDateTime.now());
                payment = paymentRepository.save(payment);
                  // Handle payment completion (lock offers, create transactions)
                paymentCompletionService.handlePaymentCompletion(payment);
                
            } else {
                // Thanh toán thất bại
                payment.setStatus(Payment.PaymentStatus.FAILED);
                log.warn("Payment failed: orderId={}, resultCode={}", orderId, resultCode);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
            return true;
            
        } catch (Exception e) {
            log.error("Error handling MoMo callback", e);
            return false;
        }
    }
    
    /**
     * Verify MoMo IPN signature
     */
    private boolean verifyMoMoSignature(Map<String, Object> callbackData) {
        try {
            String receivedSignature = (String) callbackData.get("signature");
            
            // Build signature string theo format MoMo IPN
            String rawSignature = "accessKey=" + accessKey +
                    "&amount=" + callbackData.get("amount") +
                    "&extraData=" + callbackData.get("extraData") +
                    "&message=" + callbackData.get("message") +
                    "&orderId=" + callbackData.get("orderId") +
                    "&orderInfo=" + callbackData.get("orderInfo") +
                    "&orderType=" + callbackData.get("orderType") +
                    "&partnerCode=" + callbackData.get("partnerCode") +
                    "&payType=" + callbackData.get("payType") +
                    "&requestId=" + callbackData.get("requestId") +
                    "&responseTime=" + callbackData.get("responseTime") +
                    "&resultCode=" + callbackData.get("resultCode") +
                    "&transId=" + callbackData.get("transId");
            
            String calculatedSignature = momoSignatureUtil.hmacSHA256(rawSignature, secretKey);
            
            return Objects.equals(receivedSignature, calculatedSignature);
            
        } catch (Exception e) {
            log.error("Error verifying MoMo signature", e);
            return false;
        }
    }
}
