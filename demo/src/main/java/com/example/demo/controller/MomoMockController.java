package com.example.demo.controller;

import com.example.demo.entity.Payment;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/momo")
@RequiredArgsConstructor
@Slf4j
public class MomoMockController {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * Hiển thị trang mock checkout cho MoMo
     */
    @GetMapping("/mock-checkout")
    public String showMockCheckout(
            @RequestParam String orderId,
            @RequestParam(defaultValue = "10000") Long amount,
            @RequestParam(defaultValue = "Thanh toán đơn hàng") String orderInfo,
            @RequestParam(defaultValue = "TradeUp") String partnerName,
            Model model) {
        
        log.info("Showing MoMo mock checkout page for orderId: {}, amount: {}", orderId, amount);
        
        model.addAttribute("orderId", orderId);
        model.addAttribute("amount", amount);
        model.addAttribute("orderInfo", orderInfo);
        model.addAttribute("partnerName", partnerName);
        
        return "momo-mock-checkout";
    }
    
    /**
     * Xử lý kết quả từ trang mock checkout
     */
    @PostMapping("/mock-process")
    public String processMockCheckout(
            @RequestParam String orderId,
            @RequestParam String status,
            Model model) {
        
        log.info("Processing MoMo mock checkout for orderId: {}, status: {}", orderId, status);
        
        // Tìm payment theo orderId (transactionId)
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(orderId);
        
        if (paymentOpt.isEmpty()) {
            log.error("Payment not found for orderId: {}", orderId);
            model.addAttribute("error", "Không tìm thấy giao dịch");
            model.addAttribute("redirectUrl", "/");
            return "payment-result";
        }
        
        Payment payment = paymentOpt.get();
        
        // Cập nhật trạng thái payment dựa vào kết quả
        if ("success".equals(status)) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            model.addAttribute("success", true);
            model.addAttribute("message", "Thanh toán thành công");
        } else {
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
            model.addAttribute("success", false);
            model.addAttribute("message", "Thanh toán đã bị hủy");
        }
        
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        // Xử lý escrow nếu payment thành công
        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            try {
                paymentService.handlePaymentCompleted(payment);
                log.info("Successfully processed payment completion for orderId: {}", orderId);
            } catch (Exception e) {
                log.error("Error handling payment completion", e);
            }
        }
        
        // Redirect URL to show success/failure page
        String redirectUrl = "/";
        model.addAttribute("redirectUrl", redirectUrl);
        
        return "payment-result";
    }
    
    /**
     * API để cập nhật trạng thái payment từ mobile app
     */
    @PostMapping("/mock-update")
    @ResponseBody
    public ResponseEntity<?> updatePaymentStatus(
            @RequestParam String orderId,
            @RequestParam String status) {
        
        log.info("Updating payment status via API: orderId={}, status={}", orderId, status);
        
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(orderId);
        
        if (paymentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Payment not found"
            ));
        }
        
        Payment payment = paymentOpt.get();
        
        // Cập nhật trạng thái payment
        if ("success".equals(status)) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
        } else if ("cancel".equals(status)) {
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid status"
            ));
        }
        
        payment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        
        // Xử lý escrow nếu payment thành công
        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            try {
                paymentService.handlePaymentCompleted(payment);
            } catch (Exception e) {
                log.error("Error handling payment completion", e);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Payment status updated successfully");
        result.put("paymentId", payment.getId());
        result.put("status", payment.getStatus().toString());
        
        return ResponseEntity.ok(result);
    }
}
