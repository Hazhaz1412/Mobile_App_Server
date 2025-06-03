package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.PasswordResetRequest;
import com.example.demo.dto.PasswordUpdateRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.GoogleAuthRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.VerificationToken;
import com.example.demo.repository.VerificationTokenRepository;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private VerificationTokenRepository tokenRepository;
    
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            userService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Email đặt lại mật khẩu đã được gửi!",
                null
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody PasswordUpdateRequest request) {
        try {
            User user = userService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Mật khẩu đã được đặt lại thành công!",
                user.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(
                request.getEmail(), 
                request.getPassword(), 
                request.getDisplayName()
            );
            
            return ResponseEntity.ok(new ApiResponse(
                true, 
                "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.", 
                user.getId()
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false, 
                e.getMessage()
            ));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = userService.loginUser(
                request.getEmail(), 
                request.getPassword()
            );
            
            return ResponseEntity.ok(new ApiResponse(
                true, 
                "Đăng nhập thành công!", 
                user.getId()
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false, 
                e.getMessage()
            ));
        }
    }
    
    // Endpoint tạm thời để activate user (thay thế email verification)
    @PostMapping("/activate/{userId}")
    public ResponseEntity<ApiResponse> activateUser(@PathVariable Long userId) {
        try {
            User user = userService.activateUser(userId);
            return ResponseEntity.ok(new ApiResponse(
                true, 
                "Tài khoản đã được kích hoạt thành công!", 
                user.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false, 
                e.getMessage()
            ));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse> verifyAccount(@RequestParam String token) {
        try {
            Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(token);
            
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid verification token"));
            }
            
            VerificationToken verificationToken = tokenOpt.get();
            
            if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Verification token has expired"));
            }
            
            User user = userService.activateUser(verificationToken.getUserId());
            tokenRepository.deleteByUserId(user.getId());
            
            return ResponseEntity.ok(new ApiResponse(true, "Account successfully verified", user.getId()));
        } catch (Exception e) {
            // Log the exception
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error processing verification: " + e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse> googleLogin(@RequestBody GoogleAuthRequest request) {
        try {
            User user = userService.processGoogleLogin(
                request.getIdToken(),
                request.getEmail(),
                request.getDisplayName()
            );
            
            return ResponseEntity.ok(new ApiResponse(true, "Google login successful", user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }
}