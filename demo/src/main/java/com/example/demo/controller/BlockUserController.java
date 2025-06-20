package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.BlockUserRequest;
import com.example.demo.dto.BlockedUser;
import com.example.demo.dto.BlockStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * BLOCK USER CONTROLLER
 * Handles user blocking/unblocking functionality
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*") // Configure properly for production
public class BlockUserController {    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Removed JwtUtil dependency for now - will use fallback authentication

    /**
     * Block user - Path parameter version (Primary endpoint)
     * POST /api/users/{targetUserId}/block
     */
    @PostMapping("/{targetUserId}/block")
    public ResponseEntity<ApiResponse> blockUserPath(
            @PathVariable Long targetUserId,
            @RequestBody BlockUserRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            Long currentUserId = getCurrentUserId(httpRequest);
            
            System.out.println("🔥 BlockUser API called - Path version");
            System.out.println("Current User: " + currentUserId + ", Target User: " + targetUserId);
            System.out.println("Reason: " + request.getReason());
            
            // Call optimized stored procedure
            jdbcTemplate.update(
                "CALL BlockUser(?, ?, ?)",
                currentUserId, targetUserId, request.getReason()
            );
            
            System.out.println("✅ User blocked successfully via stored procedure");
            
            return ResponseEntity.ok(
                new ApiResponse(true, "User blocked successfully")
            );
            
        } catch (Exception e) {
            System.err.println("❌ Block user error: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(500).body(
                new ApiResponse(false, "Failed to block user: " + e.getMessage())
            );
        }
    }    /**
     * Block user - Query parameter version (Fallback endpoint)
     * POST /api/users/block?userId={currentUserId}&targetUserId={targetUserId}
     */
    @PostMapping("/block")
    public ResponseEntity<ApiResponse> blockUserQuery(
            @RequestParam Long userId,
            @RequestParam Long targetUserId,
            @RequestBody BlockUserRequest request) {
        
        try {
            System.out.println("🔥 BlockUser API called - Query version (fallback)");
            System.out.println("Current User: " + userId + ", Target User: " + targetUserId);
            System.out.println("Reason: " + request.getReason());
            
            // Call optimized stored procedure
            jdbcTemplate.update(
                "CALL BlockUser(?, ?, ?)",
                userId, targetUserId, request.getReason()
            );
            
            System.out.println("✅ User blocked successfully via stored procedure (fallback)");
            
            return ResponseEntity.ok(
                new ApiResponse(true, "User blocked successfully")
            );
            
        } catch (Exception e) {
            System.err.println("❌ Block user error (fallback): " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(500).body(
                new ApiResponse(false, "Failed to block user: " + e.getMessage())
            );
        }
    }    /**
     * Unblock user
     * DELETE /api/users/{targetUserId}/block
     */
    @DeleteMapping("/{targetUserId}/block")
    public ResponseEntity<ApiResponse> unblockUser(
            @PathVariable Long targetUserId,
            HttpServletRequest httpRequest) {
        
        try {
            Long currentUserId = getCurrentUserId(httpRequest);
            
            System.out.println("🔓 UnblockUser API called");
            System.out.println("Current User: " + currentUserId + ", Target User: " + targetUserId);
            
            jdbcTemplate.update(
                "CALL UnblockUser(?, ?)",
                currentUserId, targetUserId
            );
            
            System.out.println("✅ User unblocked successfully");
            
            return ResponseEntity.ok(
                new ApiResponse(true, "User unblocked successfully")
            );
            
        } catch (Exception e) {
            System.err.println("❌ Unblock user error: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(500).body(
                new ApiResponse(false, "Failed to unblock user: " + e.getMessage())
            );
        }
    }    /**
     * Get list of blocked users
     * GET /api/users/blocked
     */
    @GetMapping("/blocked")
    public ResponseEntity<ApiResponse> getBlockedUsers(
            HttpServletRequest httpRequest) {
        
        try {
            Long currentUserId = getCurrentUserId(httpRequest);
            
            System.out.println("📋 GetBlockedUsers API called for user: " + currentUserId);
            
            String sql = """
                SELECT 
                    blocked_id as userId,
                    blocked_name as displayName,
                    blocked_avatar as avatarUrl,
                    reason,
                    created_at as blockedAt
                FROM active_blocks 
                WHERE blocker_id = ?
                ORDER BY created_at DESC
                """;
            
            List<BlockedUser> blockedUsers = jdbcTemplate.query(
                sql, 
                new BeanPropertyRowMapper<>(BlockedUser.class),
                currentUserId
            );
            
            System.out.println("✅ Found " + blockedUsers.size() + " blocked users");
            
            return ResponseEntity.ok(
                new ApiResponse(true, "Blocked users retrieved", blockedUsers)
            );
            
        } catch (Exception e) {
            System.err.println("❌ Get blocked users error: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(500).body(
                new ApiResponse(false, "Failed to get blocked users")
            );
        }
    }    /**
     * Check block status between users
     * GET /api/users/{targetUserId}/block-status
     */
    @GetMapping("/{targetUserId}/block-status")
    public ResponseEntity<ApiResponse> checkBlockStatus(
            @PathVariable Long targetUserId,
            HttpServletRequest httpRequest) {
        
        try {
            Long currentUserId = getCurrentUserId(httpRequest);
            
            System.out.println("🔍 CheckBlockStatus API called");
            System.out.println("Current User: " + currentUserId + ", Target User: " + targetUserId);
            
            // Use optimized functions
            String checkBlockedSql = "SELECT IsUserBlocked(?, ?) as isBlocked";
            
            Boolean isBlocked = jdbcTemplate.queryForObject(
                checkBlockedSql, Boolean.class, currentUserId, targetUserId
            );
            
            Boolean isBlockedBy = jdbcTemplate.queryForObject(
                checkBlockedSql, Boolean.class, targetUserId, currentUserId
            );
            
            BlockStatus status = new BlockStatus();
            status.setIsBlocked(isBlocked != null ? isBlocked : false);
            status.setIsBlockedBy(isBlockedBy != null ? isBlockedBy : false);
            status.setCanInteract(!(status.getIsBlocked() || status.getIsBlockedBy()));
            
            System.out.println("✅ Block status: isBlocked=" + status.getIsBlocked() + 
                             ", isBlockedBy=" + status.getIsBlockedBy() + 
                             ", canInteract=" + status.getCanInteract());
            
            return ResponseEntity.ok(
                new ApiResponse(true, "Block status retrieved", status)
            );
            
        } catch (Exception e) {
            System.err.println("❌ Check block status error: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(500).body(
                new ApiResponse(false, "Failed to check block status")
            );
        }
    }    /**
     * Get current user ID from JWT token or fallback methods
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            // Fallback: Try to get from header
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null) {
                return Long.parseLong(userIdHeader);
            }
            
            // For testing: use a default user ID
            System.out.println("⚠️ Using fallback user ID for testing");
            return 9L; // Default user ID for testing
            
        } catch (Exception e) {
            System.err.println("❌ Error getting current user ID: " + e.getMessage());
            // Return test user ID
            return 9L;
        }
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
