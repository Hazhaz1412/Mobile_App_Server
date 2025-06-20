package com.example.demo.security;

import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;

@Configuration
public class PaymentSecurityConfiguration {
    
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;
    
    @Value("${visa.api-key}")
    private String visaApiKey;
    
    @Value("${app.payment.encryption.password:TradeUpSecurePayment2024}")
    private String encryptionPassword;
    
    @Value("${app.payment.encryption.salt:TradeUpSalt}")
    private String encryptionSalt;
    
    /**
     * Initialize Stripe API key on startup
     */    @PostConstruct
    public void initStripe() {
        if (stripeSecretKey != null && !stripeSecretKey.isEmpty() && 
            !stripeSecretKey.contains("AAAA") && isValidStripeKey(stripeSecretKey)) {
            Stripe.apiKey = stripeSecretKey;
            System.out.println("✅ Stripe API initialized successfully with real key");
        } else {
            System.out.println("⚠️ Stripe API key not configured or using mock key - will use mock mode");
        }
    }/**
     * Payment data encryption for sensitive information
     */
    @Bean
    public BytesEncryptor paymentDataEncryptor() {
        // Use hex-encoded salt (32 characters for proper encoding)
        String hexSalt = "5472616465557053616c743230323434454"  + "E4954"; // "TradeUpSalt2024INIT" in hex
        return new AesBytesEncryptor(encryptionPassword, hexSalt, 
                KeyGenerators.secureRandom(16));
    }
      /**
     * Validate Stripe configuration
     */
    public boolean isStripeConfigured() {
        return stripeSecretKey != null && 
               !stripeSecretKey.isEmpty() && 
               !stripeSecretKey.contains("AAAA") &&
               isValidStripeKey(stripeSecretKey);
    }
    
    /**
     * Check if Stripe key is valid format
     */
    private boolean isValidStripeKey(String key) {
        return key != null && (key.startsWith("sk_test_") || key.startsWith("sk_live_")) && key.length() > 20;
    }
    
    /**
     * Validate Visa configuration
     */
    public boolean isVisaConfigured() {
        return visaApiKey != null && !visaApiKey.isEmpty();
    }
    
    /**
     * Mask sensitive payment data for logging
     */
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String cleaned = cardNumber.replaceAll("\\s+", "");
        if (cleaned.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cleaned.substring(cleaned.length() - 4);
    }
    
    /**
     * Mask API keys for logging
     */
    public String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
