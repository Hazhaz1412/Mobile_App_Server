package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.JwtAuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.PasswordResetRequest;
import com.example.demo.dto.PasswordUpdateRequest;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.GoogleAuthRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.VerificationToken;
import com.example.demo.repository.VerificationTokenRepository;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.security.RefreshTokenService;
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
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private RefreshTokenService refreshTokenService;
    
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
            
            // Generate JWT tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
            
            // Create response with tokens
            JwtAuthResponse authResponse = new JwtAuthResponse(accessToken, refreshToken);
            
            return ResponseEntity.ok(new ApiResponse(
                true, 
                "Đăng nhập thành công!",
                authResponse
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false, 
                e.getMessage()
            ));
        }
    }
      @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            
            // Check if token is invalidated (due to logout)
            if (refreshTokenService.isTokenInvalidated(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponse(false, "Refresh token has been invalidated", null)
                );
            }
            
            // Validate refresh token
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponse(false, "Invalid refresh token", null)
                );
            }
            
            // Get user ID from token
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            
            // Generate new tokens
            String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);
            
            // Create response
            JwtAuthResponse authResponse = new JwtAuthResponse(newAccessToken, newRefreshToken);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Token refreshed successfully",
                authResponse
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ApiResponse(false, "Failed to refresh token: " + e.getMessage(), null)
            );
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestBody RefreshTokenRequest request) {
        try {
            // Invalidate the refresh token
            refreshTokenService.invalidateToken(request.getRefreshToken());
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Logged out successfully",
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse(false, "Failed to logout: " + e.getMessage(), null)
            );
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
    public ResponseEntity<String> verifyAccount(@RequestParam String token) {
        try {
            // Call the service method which will handle the transaction
            User user = userService.verifyUserAccount(token);
            
            // Return HTML success page for mobile app
            String htmlResponse = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Xác thực thành công</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            margin: 0;
                            padding: 20px;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 15px;
                            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                            text-align: center;
                            max-width: 400px;
                            width: 100%;
                            animation: slideIn 0.5s ease-out;
                        }
                        @keyframes slideIn {
                            from { opacity: 0; transform: translateY(-20px); }
                            to { opacity: 1; transform: translateY(0); }
                        }
                        .success-icon {
                            color: #4CAF50;
                            font-size: 80px;
                            margin-bottom: 20px;
                            animation: bounce 1s ease-in-out;
                        }
                        @keyframes bounce {
                            0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
                            40% { transform: translateY(-10px); }
                            60% { transform: translateY(-5px); }
                        }
                        h1 {
                            color: #333;
                            margin-bottom: 15px;
                            font-size: 28px;
                        }
                        p {
                            color: #666;
                            line-height: 1.6;
                            margin-bottom: 30px;
                            font-size: 16px;
                        }
                        .mobile-instruction {
                            background: #f8f9fa;
                            padding: 20px;
                            border-radius: 8px;
                            margin: 20px 0;
                            border-left: 4px solid #4CAF50;
                        }
                        .countdown {
                            color: #888;
                            font-size: 14px;
                            margin-top: 20px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="success-icon">✓</div>
                        <h1>Xác thực thành công!</h1>
                        <p>Tài khoản của bạn đã được kích hoạt thành công.</p>
                        
                        <div class="mobile-instruction">
                            <strong>📱 Hướng dẫn tiếp theo:</strong><br>
                            Hãy quay lại ứng dụng di động và đăng nhập với email đã đăng ký.
                        </div>
                        
                        <div class="countdown">
                            <span id="countdown">Tự động đóng sau 5 giây...</span>
                        </div>
                    </div>
                    
                    <script>
                        let countdown = 5;
                        const countdownElement = document.getElementById('countdown');
                        
                        const timer = setInterval(() => {
                            countdown--;
                            countdownElement.textContent = `Tự động đóng sau ${countdown} giây...`;
                            
                            if (countdown <= 0) {
                                clearInterval(timer);
                                countdownElement.textContent = 'Đang đóng...';
                                
                                // Try different methods to close/navigate
                                if (window.ReactNativeWebView) {
                                    window.ReactNativeWebView.postMessage('verification_success');
                                }
                                
                                // For Android WebView
                                if (window.Android) {
                                    window.Android.onVerificationComplete();
                                }
                                
                                // Try to close the window
                                setTimeout(() => {
                                    window.close();
                                    // If close doesn't work, try to navigate back
                                    if (window.history.length > 1) {
                                        window.history.back();
                                    } else {
                                        // Last resort - show message
                                        countdownElement.textContent = 'Vui lòng đóng tab này và quay lại ứng dụng';
                                    }
                                }, 1000);
                            }
                        }, 1000);
                    </script>
                </body>
                </html>
                """;
    
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(htmlResponse);
        
        } catch (Exception e) {
            e.printStackTrace();
            
            // Return HTML error page
            String errorHtml = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Xác thực thất bại</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background: linear-gradient(135deg, #ff6b6b 0%, #ee5a52 100%);
                            margin: 0;
                            padding: 20px;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 15px;
                            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                            text-align: center;
                            max-width: 400px;
                            width: 100%;
                        }
                        .error-icon {
                            color: #f44336;
                            font-size: 80px;
                            margin-bottom: 20px;
                        }
                        h1 {
                            color: #333;
                            margin-bottom: 15px;
                            font-size: 28px;
                        }
                        p {
                            color: #666;
                            line-height: 1.6;
                            margin-bottom: 30px;
                            font-size: 16px;
                        }
                        .error-details {
                            background: #fff3f3;
                            padding: 15px;
                            border-radius: 8px;
                            border-left: 4px solid #f44336;
                            color: #d32f2f;
                            font-size: 14px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="error-icon">✗</div>
                        <h1>Xác thực thất bại!</h1>
                        <p>Có lỗi xảy ra trong quá trình xác thực tài khoản.</p>
                        <div class="error-details">
                            """ + e.getMessage() + """
                        </div>
                        <p style="margin-top: 20px;">
                            Vui lòng thử đăng ký lại hoặc liên hệ hỗ trợ.
                        </p>
                    </div>
                </body>
                </html>
                """;
    
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(errorHtml);
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
            
            // Generate JWT tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
            
            // Create response with tokens
            JwtAuthResponse authResponse = new JwtAuthResponse(accessToken, refreshToken);
            
            return ResponseEntity.ok(new ApiResponse(true, "Google login successful", authResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }
}