package com.example.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for MoMo payment processing
 */
@Component
@Slf4j
public class MoMoSignatureUtil {

    /**
     * Create HMAC SHA256 signature for MoMo API request
     *
     * @param data      Map of parameters to be included in the signature
     * @param secretKey The secret key provided by MoMo
     * @return Generated signature string
     */
    public String generateSignature(Map<String, Object> data, String secretKey) {
        // Sort parameters by key to ensure consistent order
        Map<String, Object> sortedParams = new TreeMap<>(data);
        
        // Build raw signature string (key=value&key=value&...)
        StringBuilder rawSignature = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
            if (entry.getValue() != null && !entry.getKey().equals("signature")) {
                if (rawSignature.length() > 0) {
                    rawSignature.append("&");
                }
                rawSignature.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        
        log.debug("Raw signature before HMAC: {}", rawSignature);
        
        try {
            // Create HMAC SHA256 signature
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            byte[] hmacBytes = hmacSha256.doFinal(rawSignature.toString().getBytes(StandardCharsets.UTF_8));
            
            // Convert bytes to hex string
            return bytesToHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating MoMo signature", e);
            throw new RuntimeException("Failed to generate MoMo signature", e);
        }
    }
    
    /**
     * Alternative method for generating signature from raw string directly
     */
    public String generateSignature(String rawSignature, String secretKey) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            byte[] hmacBytes = hmacSha256.doFinal(rawSignature.getBytes(StandardCharsets.UTF_8));
            
            return bytesToHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating MoMo signature", e);
            throw new RuntimeException("Failed to generate MoMo signature", e);
        }
    }
    
    /**
     * Validate a signature received from MoMo
     */
    public boolean validateSignature(Map<String, Object> data, String receivedSignature, String secretKey) {
        // Make a copy and remove the signature field
        Map<String, Object> dataForValidation = new TreeMap<>(data);
        dataForValidation.remove("signature");
        
        // Generate our signature
        String calculatedSignature = generateSignature(dataForValidation, secretKey);
        
        // Compare
        boolean isValid = calculatedSignature.equals(receivedSignature);
        if (!isValid) {
            log.warn("MoMo signature validation failed. Expected: {}, Received: {}", 
                    calculatedSignature, receivedSignature);
        }
        
        return isValid;
    }
    
    /**
     * Convert bytes to hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
    
    /**
     * Simple method for generating HMAC SHA256 signature
     * To be compatible with existing code
     */
    public String hmacSHA256(String data, String key) {
        return generateSignature(data, key);
    }
}
