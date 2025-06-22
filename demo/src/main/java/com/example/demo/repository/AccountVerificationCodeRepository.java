package com.example.demo.repository;

import com.example.demo.entity.AccountVerificationCode;
import com.example.demo.entity.VerificationAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountVerificationCodeRepository extends JpaRepository<AccountVerificationCode, Long> {
    
    /**
     * Find a valid verification code by email, code, and action
     */
    Optional<AccountVerificationCode> findByEmailAndVerificationCodeAndActionAndUsedFalse(
            String email, String verificationCode, VerificationAction action);
    
    /**
     * Find all valid codes for an email and action
     */
    List<AccountVerificationCode> findByEmailAndActionAndUsedFalse(String email, VerificationAction action);
    
    /**
     * Mark all unused codes as used for a specific email and action
     */
    @Modifying
    @Transactional
    @Query("UPDATE AccountVerificationCode avc SET avc.used = true WHERE avc.email = :email AND avc.action = :action AND avc.used = false")
    int markAllAsUsedByEmailAndAction(@Param("email") String email, @Param("action") VerificationAction action);
    
    /**
     * Delete expired codes
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AccountVerificationCode avc WHERE avc.expiresAt < :now")
    int deleteExpiredCodes(@Param("now") LocalDateTime now);
    
    /**
     * Count non-expired codes for an email within a time window (for rate limiting)
     */
    @Query("SELECT COUNT(avc) FROM AccountVerificationCode avc WHERE avc.email = :email AND avc.createdAt > :since")
    long countByEmailAndCreatedAtAfter(@Param("email") String email, @Param("since") LocalDateTime since);
}
