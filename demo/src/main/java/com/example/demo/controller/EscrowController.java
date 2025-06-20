package com.example.demo.controller;

import com.example.demo.entity.Payment;
import com.example.demo.service.EscrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/escrow")
@RequiredArgsConstructor
@Slf4j
public class EscrowController {
    
    private final EscrowService escrowService;
    
    /**
     * API: Người mua chấp nhận thanh toán
     * POST /api/v1/escrow/{paymentId}/release
     */
    @PostMapping("/{paymentId}/release")
    public ResponseEntity<?> releaseEscrow(
            @PathVariable Long paymentId,
            @RequestBody Map<String, Object> request) {
        
        try {
            Long buyerId = Long.valueOf(request.get("buyerId").toString());
            
            log.info("Buyer {} releasing escrow for payment {}", buyerId, paymentId);
            
            Payment payment = escrowService.releaseEscrowByBuyer(paymentId, buyerId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thanh toán đã được chấp nhận! Tiền đã chuyển cho người bán.",
                    "paymentId", payment.getId(),
                    "escrowStatus", payment.getEscrowStatus(),
                    "releasedAt", payment.getEscrowReleasedAt()
            ));
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error releasing escrow for payment {}: {}", paymentId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error releasing escrow for payment {}", paymentId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API: Báo cáo tranh chấp
     * POST /api/v1/escrow/{paymentId}/dispute
     */
    @PostMapping("/{paymentId}/dispute")
    public ResponseEntity<?> reportDispute(
            @PathVariable Long paymentId,
            @RequestBody Map<String, Object> request) {
        
        try {
            Long reporterId = Long.valueOf(request.get("reporterId").toString());
            String reason = request.get("reason").toString();
            
            log.info("User {} reporting dispute for payment {}: {}", reporterId, paymentId, reason);
            
            Payment payment = escrowService.reportDispute(paymentId, reporterId, reason);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tranh chấp đã được ghi nhận. Hệ thống sẽ xem xét và xử lý trong 24-48 giờ.",
                    "paymentId", payment.getId(),
                    "escrowStatus", payment.getEscrowStatus(),
                    "disputeReportedAt", payment.getDisputeReportedAt()
            ));
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error reporting dispute for payment {}: {}", paymentId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error reporting dispute for payment {}", paymentId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API: Lấy thông tin escrow
     * GET /api/v1/escrow/{paymentId}/info
     */    @GetMapping("/{paymentId}/info")
    public ResponseEntity<?> getEscrowInfo(@PathVariable Long paymentId) {
        try {
            EscrowService.EscrowInfo info = escrowService.getEscrowInfo(paymentId);
            
            // Return EscrowInfo directly for Android compatibility
            return ResponseEntity.ok(info);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error getting escrow info for payment {}: {}", paymentId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error getting escrow info for payment {}", paymentId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API: Admin - Giải quyết tranh chấp bằng hoàn tiền
     * POST /api/v1/escrow/{paymentId}/admin/refund
     */
    @PostMapping("/{paymentId}/admin/refund")
    public ResponseEntity<?> adminRefundDispute(
            @PathVariable Long paymentId,
            @RequestBody Map<String, Object> request) {
        
        try {
            String adminNote = request.getOrDefault("adminNote", "").toString();
            
            log.info("Admin refunding disputed payment {}: {}", paymentId, adminNote);
            
            Payment payment = escrowService.refundDisputedPayment(paymentId, adminNote);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tranh chấp đã được giải quyết. Tiền đã hoàn lại cho người mua.",
                    "paymentId", payment.getId(),
                    "escrowStatus", payment.getEscrowStatus(),
                    "refundedAt", payment.getEscrowReleasedAt()
            ));
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error refunding disputed payment {}: {}", paymentId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error refunding disputed payment {}", paymentId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống: " + e.getMessage()
            ));
        }
    }
}
