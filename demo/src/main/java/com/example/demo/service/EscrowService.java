package com.example.demo.service;

import com.example.demo.entity.Payment;
import com.example.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowService {
    
    private final PaymentRepository paymentRepository;
    
    /**
     * Thiết lập escrow cho một payment
     */
    @Transactional
    public Payment setupEscrow(Long paymentId) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            throw new IllegalArgumentException("Payment not found: " + paymentId);
        }
        
        Payment payment = paymentOpt.get();
        
        // Chỉ thiết lập escrow nếu payment đã hoàn tất và chưa có escrow
        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Payment must be completed to setup escrow");
        }
        
        if (payment.getUseEscrow() && payment.getEscrowStatus() != Payment.EscrowStatus.NONE) {
            throw new IllegalStateException("Escrow already setup for this payment");
        }
        
        // Thiết lập escrow
        payment.setUseEscrow(true);
        payment.setEscrowStatus(Payment.EscrowStatus.HOLDING);
        payment.setEscrowHoldUntil(LocalDateTime.now().plusDays(7)); // Giữ tiền 7 ngày
        payment.setAutoReleaseScheduled(true);
        
        Payment savedPayment = paymentRepository.save(payment);
        
        log.info("Escrow setup for payment {}: holding until {} (7 days)", 
                paymentId, savedPayment.getEscrowHoldUntil());
        
        return savedPayment;
    }
    
    /**
     * Người mua chấp nhận thanh toán - giải phóng tiền cho người bán
     */
    @Transactional
    public Payment releaseEscrowByBuyer(Long paymentId, Long buyerId) {
        Payment payment = validateEscrowPayment(paymentId);
        
        // Kiểm tra quyền của buyer
        if (!payment.getBuyerId().equals(buyerId)) {
            throw new IllegalArgumentException("Only buyer can release escrow payment");
        }
        
        // Kiểm tra trạng thái
        if (payment.getEscrowStatus() != Payment.EscrowStatus.HOLDING) {
            throw new IllegalStateException("Payment is not in holding status");
        }
        
        // Giải phóng tiền
        payment.setEscrowStatus(Payment.EscrowStatus.RELEASED);
        payment.setEscrowReleasedAt(LocalDateTime.now());
        payment.setAutoReleaseScheduled(false);
        
        Payment savedPayment = paymentRepository.save(payment);
        
        log.info("Escrow released by buyer {} for payment {}", buyerId, paymentId);
        
        return savedPayment;
    }
    
    /**
     * Báo cáo tranh chấp
     */
    @Transactional
    public Payment reportDispute(Long paymentId, Long reporterId, String reason) {
        Payment payment = validateEscrowPayment(paymentId);
        
        // Kiểm tra quyền báo cáo (chỉ buyer hoặc seller)
        if (!payment.getBuyerId().equals(reporterId) && !payment.getSellerId().equals(reporterId)) {
            throw new IllegalArgumentException("Only buyer or seller can report dispute");
        }
        
        // Kiểm tra trạng thái
        if (payment.getEscrowStatus() != Payment.EscrowStatus.HOLDING) {
            throw new IllegalStateException("Can only report dispute for holding payments");
        }
        
        // Kiểm tra thời hạn báo cáo (trong vòng 7 ngày)
        if (LocalDateTime.now().isAfter(payment.getEscrowHoldUntil())) {
            throw new IllegalStateException("Dispute reporting period has expired");
        }
        
        // Thiết lập tranh chấp
        payment.setEscrowStatus(Payment.EscrowStatus.DISPUTED);
        payment.setDisputeReportedAt(LocalDateTime.now());
        payment.setDisputeReporterId(reporterId);
        payment.setDisputeReason(reason);
        payment.setAutoReleaseScheduled(false); // Dừng tự động giải phóng
        
        Payment savedPayment = paymentRepository.save(payment);
        
        log.info("Dispute reported by user {} for payment {}: {}", reporterId, paymentId, reason);
        
        return savedPayment;
    }
    
    /**
     * Admin giải quyết tranh chấp - hoàn tiền cho buyer
     */
    @Transactional
    public Payment refundDisputedPayment(Long paymentId, String adminNote) {
        Payment payment = validateEscrowPayment(paymentId);
        
        // Kiểm tra trạng thái
        if (payment.getEscrowStatus() != Payment.EscrowStatus.DISPUTED) {
            throw new IllegalStateException("Payment is not in disputed status");
        }
        
        // Hoàn tiền
        payment.setEscrowStatus(Payment.EscrowStatus.REFUNDED);
        payment.setEscrowReleasedAt(LocalDateTime.now());
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        if (adminNote != null) {
            payment.setDisputeReason(payment.getDisputeReason() + " | Admin decision: " + adminNote);
        }
        
        Payment savedPayment = paymentRepository.save(payment);
        
        log.info("Disputed payment {} refunded to buyer. Admin note: {}", paymentId, adminNote);
        
        return savedPayment;
    }
    
    /**
     * Tự động giải phóng tiền sau 7 ngày (scheduled task)
     */
    @Scheduled(fixedRate = 3600000) // Chạy mỗi giờ
    @Transactional
    public void autoReleaseExpiredEscrows() {
        LocalDateTime now = LocalDateTime.now();
        
        // Tìm các payment cần tự động giải phóng
        List<Payment> expiredEscrows = paymentRepository.findAll().stream()
                .filter(p -> p.getUseEscrow() && 
                            p.getEscrowStatus() == Payment.EscrowStatus.HOLDING &&
                            p.getAutoReleaseScheduled() &&
                            p.getEscrowHoldUntil() != null && 
                            now.isAfter(p.getEscrowHoldUntil()))
                .toList();
        
        for (Payment payment : expiredEscrows) {
            try {
                payment.setEscrowStatus(Payment.EscrowStatus.RELEASED);
                payment.setEscrowReleasedAt(now);
                payment.setAutoReleaseScheduled(false);
                
                paymentRepository.save(payment);
                
                log.info("Auto-released escrow for payment {} after 7 days", payment.getId());
            } catch (Exception e) {
                log.error("Error auto-releasing escrow for payment {}", payment.getId(), e);
            }
        }
        
        if (!expiredEscrows.isEmpty()) {
            log.info("Auto-released {} expired escrow payments", expiredEscrows.size());
        }
    }
    
    /**
     * Lấy thông tin escrow của payment
     */
    public EscrowInfo getEscrowInfo(Long paymentId) {
        Payment payment = validateEscrowPayment(paymentId);
        
        return EscrowInfo.builder()
                .paymentId(payment.getId())
                .useEscrow(payment.getUseEscrow())
                .escrowStatus(payment.getEscrowStatus())
                .holdUntil(payment.getEscrowHoldUntil())
                .releasedAt(payment.getEscrowReleasedAt())
                .disputeReportedAt(payment.getDisputeReportedAt())
                .disputeReporterId(payment.getDisputeReporterId())
                .disputeReason(payment.getDisputeReason())
                .daysRemaining(payment.getEscrowHoldUntil() != null ? 
                        Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), payment.getEscrowHoldUntil())) : 0)
                .build();
    }
    
    private Payment validateEscrowPayment(Long paymentId) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            throw new IllegalArgumentException("Payment not found: " + paymentId);
        }
        
        Payment payment = paymentOpt.get();
        if (!payment.getUseEscrow()) {
            throw new IllegalStateException("Payment does not use escrow");
        }
        
        return payment;
    }
    
    /**
     * DTO cho thông tin escrow
     */
    @lombok.Data
    @lombok.Builder
    public static class EscrowInfo {
        private Long paymentId;
        private Boolean useEscrow;
        private Payment.EscrowStatus escrowStatus;
        private LocalDateTime holdUntil;
        private LocalDateTime releasedAt;
        private LocalDateTime disputeReportedAt;
        private Long disputeReporterId;
        private String disputeReason;
        private long daysRemaining;
    }
}
