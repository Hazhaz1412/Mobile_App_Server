package com.example.demo.service.payment;

import com.example.demo.dto.payment.*;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentMethod;
import com.example.demo.entity.Offer;
import com.example.demo.entity.OfferStatus;
import com.example.demo.entity.Transaction;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.PaymentMethodRepository;
import com.example.demo.repository.OfferRepository;
import com.example.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OfferRepository offerRepository;
    private final TransactionRepository transactionRepository;
    private final MoMoPaymentService moMoPaymentService;
    private final VisaPaymentService visaPaymentService;
    private final PaymentCompletionService paymentCompletionService;
      @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {        
        // CRITICAL: Synchronized payment creation for same offer to prevent race condition
        String lockKey = request.getOfferId() != null ? "offer_" + request.getOfferId() : "listing_" + request.getListingId();
        
        log.info("PAYMENT_VALIDATION: Creating payment for offerId={}, listingId={}, amount={}, method={}", 
                request.getOfferId(), request.getListingId(), request.getAmount(), request.getPaymentMethodType());
        
        synchronized (lockKey.intern()) {
            try {
                // Double-check offer status inside synchronized block
                if (request.getOfferId() != null) {
                    boolean canPurchase = canPurchaseOffer(request.getOfferId());
                    log.info("PAYMENT_VALIDATION: Offer {} canPurchase check result: {}", request.getOfferId(), canPurchase);
                    
                    if (!canPurchase) {
                        log.warn("PAYMENT_BLOCKED: Offer {} already completed, preventing payment creation", request.getOfferId());
                        return CreatePaymentResponse.failure("Offer này đã được thanh toán và hoàn thành");
                    }
                }
                
                // Kiểm tra xem có payment pending nào cho listing này không
                if (paymentRepository.existsPendingPaymentForListing(request.getListingId())) {
                    log.warn("PAYMENT_BLOCKED: Pending payment already exists for listing {}", request.getListingId());
                    return CreatePaymentResponse.failure("Đã có giao dịch đang chờ xử lý cho sản phẩm này");
                }
                
                // Kiểm tra nếu đã có payment COMPLETED cho offer này
                if (request.getOfferId() != null) {
                    List<Payment> existingPayments = paymentRepository.findByOfferIdOrderByCreatedAtDesc(request.getOfferId());
                    boolean hasCompletedPayment = existingPayments.stream()
                        .anyMatch(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED);
                    
                    log.info("PAYMENT_VALIDATION: Offer {} has {} existing payments, hasCompletedPayment: {}", 
                            request.getOfferId(), existingPayments.size(), hasCompletedPayment);
                    
                    if (hasCompletedPayment) {
                        log.warn("PAYMENT_BLOCKED: Offer {} already has completed payment", request.getOfferId());
                        return CreatePaymentResponse.failure("Offer này đã có giao dịch hoàn thành");
                    }
                }                
                
                log.info("PAYMENT_VALIDATION: All checks passed, creating payment for offer {}", request.getOfferId());
                
                // Tạo payment entity
                Payment payment = Payment.builder()
                        .listingId(request.getListingId())
                        .offerId(request.getOfferId())
                        .buyerId(request.getBuyerId())
                        .sellerId(request.getSellerId())
                        .amount(request.getAmount())
                        .paymentMethodType(request.getPaymentMethodType())
                        .status(Payment.PaymentStatus.PENDING)
                        .transactionId(generateTransactionId())
                        .description(request.getDescription())
                        .useEscrow(request.getUseEscrow() != null ? request.getUseEscrow() : false)
                        .build();
                
                // Set escrow status nếu sử dụng escrow
                if (payment.getUseEscrow()) {
                    payment.setEscrowStatus(Payment.EscrowStatus.HOLDING);
                } else {
                    payment.setEscrowStatus(Payment.EscrowStatus.NONE);
                }
                
                // Lưu MoMo phone number nếu có
                if (request.getMomoPhoneNumber() != null) {
                    payment.setMomoPhoneNumber(request.getMomoPhoneNumber());
                }
                
                // Lưu payment trước khi gọi payment gateway
                payment = paymentRepository.save(payment);                  // Gọi payment gateway tương ứng
                CreatePaymentResponse response;
                switch (request.getPaymentMethodType()) {
                    case MOMO:
                        response = moMoPaymentService.createMoMoPayment(request, payment);
                        break;
                    case VISA:
                    case MASTERCARD:
                        response = visaPaymentService.createVisaPayment(request, payment);
                        break;
                    case STRIPE:
                        // For Stripe, payment record is already created, just return success
                        // The actual Stripe session will be created by PaymentController
                        response = CreatePaymentResponse.success(
                            PaymentResponse.fromEntity(payment),
                            null
                        );
                        break;
                    case CASH:
                        // COD - chỉ tạo payment record
                        payment.setStatus(Payment.PaymentStatus.PENDING);
                        payment = paymentRepository.save(payment);
                        response = CreatePaymentResponse.success(
                            PaymentResponse.fromEntity(payment),
                            null
                        );
                        break;
                    default:
                        return CreatePaymentResponse.failure("Phương thức thanh toán không được hỗ trợ");
                }
                
                return response;
                
            } catch (Exception e) {
                log.error("Error creating payment", e);
                return CreatePaymentResponse.failure("Lỗi hệ thống: " + e.getMessage());
            }
        } // End synchronized block
    }
    
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
        return PaymentResponse.fromEntity(payment);
    }
    
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
        return PaymentResponse.fromEntity(payment);
    }
    
    public List<PaymentResponse> getPaymentHistory(Long userId, String role) {
        List<Payment> payments;
        if ("buyer".equalsIgnoreCase(role)) {
            payments = paymentRepository.findByBuyerIdOrderByCreatedAtDesc(userId);
        } else if ("seller".equalsIgnoreCase(role)) {
            payments = paymentRepository.findBySellerIdOrderByCreatedAtDesc(userId);
        } else {
            throw new RuntimeException("Vai trò không hợp lệ");
        }
        
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<PaymentResponse> getPaymentsByListing(Long listingId) {
        List<Payment> payments = paymentRepository.findByListingIdOrderByCreatedAtDesc(listingId);
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<PaymentResponse> getPaymentsByOfferId(Long offerId) {
        List<Payment> payments = paymentRepository.findByOfferIdOrderByCreatedAtDesc(offerId);
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public boolean confirmCashPayment(Long paymentId, Long sellerId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
        
        // Chỉ seller mới có thể confirm COD
        if (!payment.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Không có quyền xác nhận giao dịch này");
        }
        
        if (payment.getPaymentMethodType() != Payment.PaymentMethodType.CASH) {
            throw new RuntimeException("Chỉ có thể xác nhận giao dịch tiền mặt");
        }
        
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new RuntimeException("Giao dịch không ở trạng thái chờ xác nhận");
        }
          payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        
        // Handle payment completion (lock offers, create transactions)
        handlePaymentCompletion(payment);
        
        return true;
    }
    
    @Transactional
    public boolean releaseEscrow(Long paymentId, Long buyerId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
        
        // Chỉ buyer mới có thể release escrow
        if (!payment.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("Không có quyền thả escrow cho giao dịch này");
        }
        
        if (!payment.getUseEscrow()) {
            throw new RuntimeException("Giao dịch không sử dụng escrow");
        }
        
        if (payment.getEscrowStatus() != Payment.EscrowStatus.HOLDING) {
            throw new RuntimeException("Escrow không ở trạng thái holding");
        }
        
        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Giao dịch chưa hoàn thành");
        }
        
        payment.setEscrowStatus(Payment.EscrowStatus.RELEASED);
        payment.setEscrowReleasedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        return true;
    }
    
    @Transactional
    public boolean cancelPayment(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
        
        // Chỉ buyer hoặc seller mới có thể cancel
        if (!payment.getBuyerId().equals(userId) && !payment.getSellerId().equals(userId)) {
            throw new RuntimeException("Không có quyền hủy giao dịch này");
        }
        
        if (payment.getStatus() != Payment.PaymentStatus.PENDING && 
            payment.getStatus() != Payment.PaymentStatus.PROCESSING) {
            throw new RuntimeException("Không thể hủy giao dịch ở trạng thái hiện tại");
        }
        
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        paymentRepository.save(payment);        
        return true;
    }
    
    /**
     * Xử lý MoMo IPN callback
     */
    public boolean handleMoMoCallback(java.util.Map<String, Object> callbackData) {
        return moMoPaymentService.handleMoMoCallback(callbackData);
    }
    
    public boolean handleMoMoCallback(String orderId, String resultCode, String transId) {
        try {
            java.util.Map<String, Object> callbackData = new java.util.HashMap<>();
            callbackData.put("orderId", orderId);
            callbackData.put("resultCode", resultCode);
            callbackData.put("transId", transId);
            
            return moMoPaymentService.handleMoMoCallback(callbackData);
        } catch (Exception e) {
            log.error("Error handling MoMo callback", e);
            return false;
        }
    }
      public boolean handleVisaCallback(String transactionId, String status) {
        try {
            return visaPaymentService.handleVisaCallback(transactionId, status);
        } catch (Exception e) {
            log.error("Error handling Visa callback", e);
            return false;
        }
    }
    
    public PaymentResponse getPendingPaymentForListing(Long userId, Long listingId) {
        try {
            List<Payment> payments = paymentRepository.findByListingIdAndBuyerIdAndStatus(
                listingId, userId, Payment.PaymentStatus.PENDING);
            
            if (payments.isEmpty()) {
                // Also check for PROCESSING status
                payments = paymentRepository.findByListingIdAndBuyerIdAndStatus(
                    listingId, userId, Payment.PaymentStatus.PROCESSING);
            }
              if (!payments.isEmpty()) {
                Payment payment = payments.get(0); // Get the first one
                return PaymentResponse.fromEntity(payment);
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error getting pending payment for user: {}, listing: {}", userId, listingId, e);
            return null;
        }
    }
    
    /**
     * Manual update payment status - FOR TESTING ONLY
     */
    public boolean updatePaymentStatus(Long paymentId, String status, String transactionId) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (paymentOpt.isEmpty()) {
                log.error("Payment not found: {}", paymentId);
                return false;
            }
            
            Payment payment = paymentOpt.get();
            Payment.PaymentStatus paymentStatus;
            
            try {
                paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid payment status: {}", status);
                return false;
            }
            
            payment.setStatus(paymentStatus);
            payment.setUpdatedAt(LocalDateTime.now());
            
            if (transactionId != null && !transactionId.trim().isEmpty()) {
                payment.setExternalTransactionId(transactionId);
            }
              if (paymentStatus == Payment.PaymentStatus.COMPLETED) {
                payment.setPaidAt(LocalDateTime.now());
            }
            
            payment = paymentRepository.save(payment);
            log.info("Payment status updated: paymentId={}, status={}", paymentId, status);
            
            // CRITICAL: Handle payment completion when status is set to COMPLETED
            if (paymentStatus == Payment.PaymentStatus.COMPLETED) {
                log.info("PAYMENT_COMPLETION: Triggering completion handler for payment {}", paymentId);
                handlePaymentCompletion(payment);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error updating payment status", e);
            return false;
        }
    }
    
    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Xử lý logic khi payment hoàn thành thành công
     * - Nếu sử dụng escrow, thiết lập trạng thái HOLDING
     * - Cập nhật các thông tin liên quan
     */
    @Transactional
    public void handlePaymentCompleted(Payment payment) {
        log.info("Handling completed payment: id={}, transactionId={}", payment.getId(), payment.getTransactionId());
        
        // Đảm bảo payment ở trạng thái COMPLETED
        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
        }
        
        // Xử lý escrow nếu được yêu cầu
        if (payment.getUseEscrow()) {
            payment.setEscrowStatus(Payment.EscrowStatus.HOLDING);
            
            // Thiết lập thời gian giữ tiền (7 ngày)
            if (payment.getEscrowHoldUntil() == null) {
                payment.setEscrowHoldUntil(LocalDateTime.now().plusDays(7));
            }
            
            // Lên lịch tự động giải phóng
            payment.setAutoReleaseScheduled(true);
            
            log.info("Escrow set to HOLDING for payment {}: holding until {} (7 days)", 
                payment.getId(), payment.getEscrowHoldUntil());
        } else if (payment.getEscrowStatus() == null) {
            payment.setEscrowStatus(Payment.EscrowStatus.NONE);
        }
        
        // Lưu payment
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        log.info("Payment successfully processed: id={}, status={}, escrowStatus={}", 
            payment.getId(), payment.getStatus(), payment.getEscrowStatus());
    }
      /**
     * Handle payment completion - delegate to PaymentCompletionService
     */
    @Transactional
    public void handlePaymentCompletion(Payment payment) {
        paymentCompletionService.handlePaymentCompletion(payment);
    }
    
    /**
     * Check if offer can be purchased (not already completed)
     */
    public boolean canPurchaseOffer(Long offerId) {
        return paymentCompletionService.canPurchaseOffer(offerId);
    }
      public void debugCompleteOffer(Long offerId) {
        log.info("DEBUG: Manually completing offer: {}", offerId);
        // Create mock payment to trigger completion
        Payment mockPayment = Payment.builder()
                .offerId(offerId)
                .build();
        paymentCompletionService.handlePaymentCompletion(mockPayment);
    }
    
    /**
     * Find payment by ID (for debug purposes)
     */
    public Payment findById(Long paymentId) {
        return paymentRepository.findById(paymentId).orElse(null);
    }
    
    /**
     * Save payment (for debug purposes)
     */
    @Transactional
    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }
    
    /**
     * Debug: Fix offerId for a payment and trigger completion
     */
    @Transactional
    public Map<String, Object> debugFixOfferIdAndComplete(Long paymentId, Long offerId) {
        log.info("DEBUG: Fixing offerId for payment {} -> offer {}", paymentId, offerId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                result.put("success", false);
                result.put("message", "Payment not found: " + paymentId);
                return result;
            }
            
            // Set offerId
            payment.setOfferId(offerId);
            payment = paymentRepository.save(payment);
            
            log.info("✅ Updated payment {} with offerId: {}", paymentId, offerId);
            
            // Trigger completion if payment is completed
            if (Payment.PaymentStatus.COMPLETED.equals(payment.getStatus())) {
                log.info("🚀 Triggering completion handler for payment {}", paymentId);
                paymentCompletionService.handlePaymentCompletion(payment);
                
                result.put("success", true);
                result.put("message", "Fixed offerId and triggered completion");
                result.put("completionTriggered", true);
            } else {
                result.put("success", true);
                result.put("message", "Fixed offerId but payment not completed yet");
                result.put("completionTriggered", false);
            }
            
            result.put("paymentId", paymentId);
            result.put("offerId", offerId);
            result.put("paymentStatus", payment.getStatus().toString());
            
        } catch (Exception e) {
            log.error("💥 Error fixing offerId for payment {}: ", paymentId, e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Get all payments (for debug purposes)
     */
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
    
    /**
     * Force complete offer for a payment (auto-update offer status regardless of offerId)
     * This is called from payment success callback to ensure offer is always updated
     */
    @Transactional
    public void forceCompleteOfferFromPayment(Payment payment) {
        try {
            log.info("🔄 FORCE COMPLETING offer from payment {}", payment.getId());
            log.info("Payment details - Listing: {}, Buyer: {}, Seller: {}, Amount: {}", 
                    payment.getListingId(), payment.getBuyerId(), payment.getSellerId(), payment.getAmount());
            
            // Strategy 1: If payment has offerId, use normal completion
            if (payment.getOfferId() != null) {
                log.info("✅ Payment has offerId {}, using normal completion", payment.getOfferId());
                paymentCompletionService.handlePaymentCompletion(payment);
                return;
            }
              // Strategy 2: Find offer by payment details (listingId + buyerId + status ACCEPTED)            log.info("🔍 Payment has no offerId, searching for matching offer...");
            Optional<Offer> matchingOfferOpt = offerRepository.findByListingIdAndBuyerIdAndStatus(
                payment.getListingId(), 
                payment.getBuyerId(), 
                OfferStatus.ACCEPTED
            );
            
            if (matchingOfferOpt.isPresent()) {
                Offer offer = matchingOfferOpt.get();
                log.info("🎯 Found matching offer {} for payment {}", offer.getId(), payment.getId());
                
                // Update payment with correct offerId
                payment.setOfferId(offer.getId());
                paymentRepository.save(payment);
                log.info("💾 Updated payment {} with offerId {}", payment.getId(), offer.getId());
                
                // Now complete normally
                paymentCompletionService.handlePaymentCompletion(payment);
                log.info("✅ FORCE COMPLETION SUCCESS for payment {} and offer {}", payment.getId(), offer.getId());
                return;
            }
            
            // Strategy 3: Fallback - Create transaction for direct purchase (no offer)
            log.warn("⚠️ No matching offer found, treating as direct purchase");
            Transaction transaction = new Transaction(
                payment.getListingId(),
                payment.getBuyerId(),
                payment.getSellerId(),
                payment.getAmount()
            );
            transaction.markAsCompleted();
            Transaction savedTransaction = transactionRepository.save(transaction);
            log.info("📝 Created direct purchase transaction {}", savedTransaction.getId());
            
        } catch (Exception e) {
            log.error("💥 Error force completing offer from payment {}: ", payment.getId(), e);
        }
    }
}
