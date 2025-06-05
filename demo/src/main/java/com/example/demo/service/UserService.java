package com.example.demo.service;

import com.example.demo.entity.PasswordResetToken;
import com.example.demo.entity.User;
import com.example.demo.entity.UserProfile;
import com.example.demo.entity.UserStatus;
import com.example.demo.entity.VerificationToken;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;
    
    @Value("${app.base-url:https://zn8vnhrf-8080.asse.devtunnels.ms}")
    private String baseUrl;
    
    // filepath: src/main/java/com/example/demo/service/UserService.java
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    // Add these methods to your existing UserService class
    public void requestPasswordReset(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Email không tồn tại trong hệ thống!");
        }
        
        User user = userOptional.get();
        
        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUserId(user.getId());
        
        // Create new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUserId(user.getId());
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);
        
        // Send email ASYNCHRONOUSLY
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmailAsync(email, resetUrl);
    }

    public User resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            throw new RuntimeException("Token không hợp lệ!");
        }
        
        PasswordResetToken resetToken = tokenOptional.get();
        
        // Check if token is expired
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("Token đã hết hạn!");
        }
        
        // Get user and update password
        Optional<User> userOptional = userRepository.findById(resetToken.getUserId());
        
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Người dùng không tồn tại!");
        }
        
        User user = userOptional.get();
        user.setPasswordHash(newPassword);
        user = userRepository.save(user);
        
        // Delete used token
        passwordResetTokenRepository.delete(resetToken);
        
        return user;
    }


    @Transactional
    public User registerUser(String email, String password, String displayName) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại!");
        }
        
        User user = new User(email, password);
        user.setStatus(UserStatus.PENDING);
        User savedUser = userRepository.save(user);
        UserProfile profile = new UserProfile(savedUser.getId(), displayName);
        userProfileRepository.save(profile);
        
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUserId(savedUser.getId());
        verificationToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        tokenRepository.save(verificationToken);
        
        String verificationUrl = baseUrl + "/api/auth/verify?token=" + token;
        // Call the local method instead of emailService
        emailService.sendVerificationEmail(email, verificationUrl);
        
        return savedUser;
    }
    
    public User loginUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmailAndStatus(email, UserStatus.ACTIVE);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getPasswordHash().equals(password)) {
                return user;
            }
        }
        
        throw new RuntimeException("Email hoặc password không đúng!");
    }
    
    public User activateUser(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setStatus(UserStatus.ACTIVE);
            return userRepository.save(user);
        }
        throw new RuntimeException("User không tồn tại!");
    }
    
    public void sendVerificationEmail(String to, String verificationUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Account Verification");
        message.setText("Please verify your account by clicking this link: " + verificationUrl);
        mailSender.send(message);
    }
    
    @Transactional
    public User processGoogleLogin(String idToken, String email, String displayName) {
        // In a production app, you would verify the Google ID token
        // For now, we'll trust the token and use the email from the request
        
        // Check if user exists
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        
        if (userOptional.isPresent()) {
            // User exists - ensure they're active
            user = userOptional.get();
            if (user.getStatus() != UserStatus.ACTIVE) {
                user.setStatus(UserStatus.ACTIVE);
                user = userRepository.save(user);
            }
        } else {
            // Create new user
            user = new User(email, null); // No password for Google users
            user.setStatus(UserStatus.ACTIVE);
            user = userRepository.save(user);
            
            // Create user profile
            UserProfile profile = new UserProfile(user.getId(), displayName);
            userProfileRepository.save(profile);
        }
        
        return user;
    }

    @Transactional
    public User verifyUserAccount(String token) {
        Optional<VerificationToken> tokenOptional = tokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            throw new RuntimeException("Invalid verification token");
        }
        
        VerificationToken verificationToken = tokenOptional.get();
        
        // Check if token is expired
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired");
        }
        
        // Get user and activate
        Optional<User> userOptional = userRepository.findById(verificationToken.getUserId());
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOptional.get();
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        
        // Delete the verification token
        tokenRepository.deleteByUserId(user.getId());
        
        return user;
    }
}