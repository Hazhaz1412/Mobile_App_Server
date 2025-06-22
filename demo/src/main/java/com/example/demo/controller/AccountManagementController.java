package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.SendVerificationRequest;
import com.example.demo.dto.VerifyCodeRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.VerificationAction;
import com.example.demo.service.AccountVerificationService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Validated
public class AccountManagementController {
    
    @Autowired
    private AccountVerificationService verificationService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Send verification code for account deactivation or deletion
     */
    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponse> sendVerificationCode(@Valid @RequestBody SendVerificationRequest request) {
        try {
            // Get current user ID from JWT token
            Long currentUserId = getCurrentUserId();
            
            // Get user to verify email matches
            User user = userService.getUserById(currentUserId);
            
            // Validate email matches the authenticated user's email
            if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Email does not match your account"));
            }
            
            // Validate user status
            if (user.getStatus().name().equals("DELETED")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Account has already been deleted"));
            }
            
            // Send verification code
            verificationService.sendVerificationCode(request.getEmail(), request.getAction());
            
            String actionText = request.getAction() == VerificationAction.DEACTIVATE ? "tạm ngưng" : "xóa";
            return ResponseEntity.ok(new ApiResponse(
                true, 
                String.format("Mã xác nhận đã được gửi đến email của bạn để %s tài khoản", actionText),
                null
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Có lỗi xảy ra khi gửi mã xác nhận"));
        }
    }
    
    /**
     * Deactivate user account with verification
     */
    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<ApiResponse> deactivateAccount(
            @PathVariable Long userId,
            @Valid @RequestBody VerifyCodeRequest request) {
        try {
            // Get current user ID from JWT token
            Long currentUserId = getCurrentUserId();
            
            // Validate user can only deactivate their own account
            userService.validateUserForAccountOperation(currentUserId, userId);
            
            // Get user email for verification
            User user = userService.getUserById(userId);
            
            // Validate verification code
            boolean isValid = verificationService.validateVerificationCode(
                user.getEmail(), request.getVerificationCode(), VerificationAction.DEACTIVATE);
            
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Mã xác nhận không hợp lệ hoặc đã hết hạn"));
            }
            
            // Deactivate the account
            User deactivatedUser = userService.deactivateUser(userId);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Tài khoản đã được tạm ngưng thành công. Bạn có thể kích hoạt lại bằng cách đăng nhập.",
                deactivatedUser.getId()
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Có lỗi xảy ra khi tạm ngưng tài khoản"));
        }
    }
    
    /**
     * Permanently delete user account with verification
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deleteAccount(
            @PathVariable Long userId,
            @Valid @RequestBody VerifyCodeRequest request) {
        try {
            // Get current user ID from JWT token
            Long currentUserId = getCurrentUserId();
            
            // Validate user can only delete their own account
            userService.validateUserForAccountOperation(currentUserId, userId);
            
            // Get user email for verification
            User user = userService.getUserById(userId);
            
            // Validate verification code
            boolean isValid = verificationService.validateVerificationCode(
                user.getEmail(), request.getVerificationCode(), VerificationAction.DELETE);
            
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Mã xác nhận không hợp lệ hoặc đã hết hạn"));
            }
            
            // Delete the account
            userService.deleteUser(userId);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Tài khoản đã được xóa vĩnh viễn thành công.",
                null
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Có lỗi xảy ra khi xóa tài khoản"));
        }
    }
    
    /**
     * Get current user ID from JWT authentication context
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long)) {
            throw new RuntimeException("Invalid authentication token");
        }
        
        return (Long) principal;
    }
}
