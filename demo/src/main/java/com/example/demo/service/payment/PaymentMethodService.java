package com.example.demo.service.payment;

import com.example.demo.dto.payment.PaymentMethodResponse;
import com.example.demo.dto.payment.SavePaymentMethodRequest;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentMethod;
import com.example.demo.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodService {
    
    private final PaymentMethodRepository paymentMethodRepository;
    
    @Transactional
    public PaymentMethodResponse savePaymentMethod(SavePaymentMethodRequest request) {
        try {
            PaymentMethod paymentMethod = PaymentMethod.builder()
                    .userId(request.getUserId())
                    .type(request.getType())
                    .displayName(request.getDisplayName())
                    .description(request.getDescription())
                    .isDefault(request.getIsDefault())
                    .isActive(true)
                    .build();
            
            // Set specific fields based on payment method type
            switch (request.getType()) {
                case MOMO:
                    paymentMethod.setPhoneNumber(request.getPhoneNumber());
                    break;
                case VISA:
                case MASTERCARD:
                    if (request.getCardNumber() != null) {
                        paymentMethod.setCardNumberMasked(maskCardNumber(request.getCardNumber()));
                        paymentMethod.setCardToken(generateCardToken()); // Sinh token an toàn
                    }
                    paymentMethod.setCardHolderName(request.getCardHolderName());
                    paymentMethod.setExpiryDate(request.getExpiryDate());
                    break;
                default:
                    throw new RuntimeException("Loại phương thức thanh toán không được hỗ trợ");
            }
            
            // Nếu set làm default, unset tất cả default khác của user
            if (request.getIsDefault()) {
                paymentMethodRepository.unsetAllDefaultForUserAndType(request.getUserId(), request.getType());
            }
            
            PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
            return PaymentMethodResponse.fromEntity(savedPaymentMethod);
            
        } catch (Exception e) {
            log.error("Error saving payment method", e);
            throw new RuntimeException("Lỗi lưu phương thức thanh toán: " + e.getMessage());
        }
    }
    
    public List<PaymentMethodResponse> getUserPaymentMethods(Long userId) {
        List<PaymentMethod> paymentMethods = paymentMethodRepository
                .findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
        
        return paymentMethods.stream()
                .map(PaymentMethodResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<PaymentMethodResponse> getUserPaymentMethodsByType(Long userId, Payment.PaymentMethodType type) {
        List<PaymentMethod> paymentMethods = paymentMethodRepository
                .findByUserIdAndTypeAndIsActiveTrueOrderByCreatedAtDesc(userId, type);
        
        return paymentMethods.stream()
                .map(PaymentMethodResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    public PaymentMethodResponse getDefaultPaymentMethod(Long userId) {
        PaymentMethod paymentMethod = paymentMethodRepository
                .findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức thanh toán mặc định"));
        
        return PaymentMethodResponse.fromEntity(paymentMethod);
    }
    
    public PaymentMethodResponse getDefaultPaymentMethodByType(Long userId, Payment.PaymentMethodType type) {
        PaymentMethod paymentMethod = paymentMethodRepository
                .findByUserIdAndTypeAndIsDefaultTrueAndIsActiveTrue(userId, type)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức thanh toán mặc định cho loại này"));
        
        return PaymentMethodResponse.fromEntity(paymentMethod);
    }
    
    @Transactional
    public boolean setDefaultPaymentMethod(Long paymentMethodId, Long userId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức thanh toán"));
        
        if (!paymentMethod.getUserId().equals(userId)) {
            throw new RuntimeException("Không có quyền thay đổi phương thức thanh toán này");
        }
        
        if (!paymentMethod.getIsActive()) {
            throw new RuntimeException("Không thể set phương thức thanh toán đã bị vô hiệu hóa làm mặc định");
        }
        
        // Unset all default for this user and type
        paymentMethodRepository.unsetAllDefaultForUserAndType(userId, paymentMethod.getType());
        
        // Set this one as default
        paymentMethod.setIsDefault(true);
        paymentMethodRepository.save(paymentMethod);
        
        return true;
    }
    
    @Transactional
    public boolean deletePaymentMethod(Long paymentMethodId, Long userId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức thanh toán"));
        
        if (!paymentMethod.getUserId().equals(userId)) {
            throw new RuntimeException("Không có quyền xóa phương thức thanh toán này");
        }
        
        // Soft delete - set as inactive
        paymentMethodRepository.deactivatePaymentMethod(paymentMethodId);
        
        return true;
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
    
    private String generateCardToken() {
        // Trong thực tế, đây sẽ là token từ payment gateway
        return "TK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
