package com.example.demo.security;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to manage refresh tokens.
 * In a production application, you might want to store these in a database.
 */
@Service
public class RefreshTokenService {
    // In-memory store of invalidated tokens (for logout)
    private final Map<String, Boolean> invalidatedTokens = new HashMap<>();
    
    /**
     * Invalidate a refresh token (used for logout)
     */
    public void invalidateToken(String token) {
        invalidatedTokens.put(token, true);
    }
    
    /**
     * Check if a token is invalidated
     */
    public boolean isTokenInvalidated(String token) {
        return invalidatedTokens.getOrDefault(token, false);
    }
}
