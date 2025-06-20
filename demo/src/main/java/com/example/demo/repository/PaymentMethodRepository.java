package com.example.demo.repository;

import com.example.demo.entity.PaymentMethod;
import com.example.demo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    
    // Tìm tất cả payment methods của một user
    List<PaymentMethod> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(Long userId);
    
    // Tìm tất cả payment methods theo type của một user
    List<PaymentMethod> findByUserIdAndTypeAndIsActiveTrueOrderByCreatedAtDesc(Long userId, Payment.PaymentMethodType type);
    
    // Tìm default payment method của một user
    Optional<PaymentMethod> findByUserIdAndIsDefaultTrueAndIsActiveTrue(Long userId);
    
    // Tìm default payment method theo type của một user
    Optional<PaymentMethod> findByUserIdAndTypeAndIsDefaultTrueAndIsActiveTrue(Long userId, Payment.PaymentMethodType type);
    
    // Tìm payment method theo phone number (MoMo)
    Optional<PaymentMethod> findByUserIdAndPhoneNumberAndTypeAndIsActiveTrue(Long userId, String phoneNumber, Payment.PaymentMethodType type);
    
    // Tìm payment method theo card token
    Optional<PaymentMethod> findByUserIdAndCardTokenAndIsActiveTrue(Long userId, String cardToken);
    
    // Kiểm tra xem user có payment method default không
    boolean existsByUserIdAndIsDefaultTrueAndIsActiveTrue(Long userId);
    
    // Kiểm tra xem user có payment method theo type không
    boolean existsByUserIdAndTypeAndIsActiveTrue(Long userId, Payment.PaymentMethodType type);
    
    // Set tất cả payment methods của user thành not default
    @Modifying
    @Transactional
    @Query("UPDATE PaymentMethod pm SET pm.isDefault = false WHERE pm.userId = :userId")
    void unsetAllDefaultForUser(@Param("userId") Long userId);
    
    // Set tất cả payment methods của user theo type thành not default
    @Modifying
    @Transactional
    @Query("UPDATE PaymentMethod pm SET pm.isDefault = false WHERE pm.userId = :userId AND pm.type = :type")
    void unsetAllDefaultForUserAndType(@Param("userId") Long userId, @Param("type") Payment.PaymentMethodType type);
    
    // Deactivate payment method
    @Modifying
    @Transactional
    @Query("UPDATE PaymentMethod pm SET pm.isActive = false WHERE pm.id = :id")
    void deactivatePaymentMethod(@Param("id") Long id);
    
    // Count active payment methods by user
    long countByUserIdAndIsActiveTrue(Long userId);
}
