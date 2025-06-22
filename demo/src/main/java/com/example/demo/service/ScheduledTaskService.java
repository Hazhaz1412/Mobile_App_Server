package com.example.demo.service;

import com.example.demo.service.AccountVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTaskService {
    
    @Autowired
    private AccountVerificationService verificationService;
    
    /**
     * Clean up expired verification codes every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 milliseconds
    public void cleanupExpiredVerificationCodes() {
        verificationService.cleanupExpiredCodes();
    }
}
