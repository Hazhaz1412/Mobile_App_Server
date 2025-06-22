package com.example.demo.service;

import com.example.demo.entity.AccountVerificationCode;
import com.example.demo.entity.VerificationAction;
import com.example.demo.repository.AccountVerificationCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AccountVerificationService {
    
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_MINUTES = 15;
    private static final int MAX_CODES_PER_HOUR = 5;
    
    @Autowired
    private AccountVerificationCodeRepository verificationCodeRepository;
    
    @Autowired
    private JavaMailSender mailSender;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Generate and send verification code
     */
    @Transactional
    public void sendVerificationCode(String email, VerificationAction action) {
        // Rate limiting check
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentCodesCount = verificationCodeRepository.countByEmailAndCreatedAtAfter(email, oneHourAgo);
        
        if (recentCodesCount >= MAX_CODES_PER_HOUR) {
            throw new RuntimeException("Too many verification codes requested. Please try again later.");
        }
        
        // Invalidate all existing codes for this email and action
        verificationCodeRepository.markAllAsUsedByEmailAndAction(email, action);
        
        // Generate new code
        String verificationCode = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES);
        
        AccountVerificationCode code = new AccountVerificationCode(email, verificationCode, action, expiresAt);
        verificationCodeRepository.save(code);
        
        // Send email
        try {
            sendVerificationEmail(email, verificationCode, action);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    /**
     * Validate verification code
     */
    @Transactional
    public boolean validateVerificationCode(String email, String code, VerificationAction action) {
        Optional<AccountVerificationCode> verificationCodeOpt = verificationCodeRepository
                .findByEmailAndVerificationCodeAndActionAndUsedFalse(email, code, action);
        
        if (verificationCodeOpt.isEmpty()) {
            return false;
        }
        
        AccountVerificationCode verificationCode = verificationCodeOpt.get();
        
        if (!verificationCode.isValid()) {
            return false;
        }
        
        // Mark as used
        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);
        
        return true;
    }
    
    /**
     * Clean up expired codes (can be called by scheduled task)
     */
    @Transactional
    public void cleanupExpiredCodes() {
        verificationCodeRepository.deleteExpiredCodes(LocalDateTime.now());
    }
    
    /**
     * Generate a secure 6-digit verification code
     */
    private String generateVerificationCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * Send verification email
     */
    private void sendVerificationEmail(String to, String verificationCode, VerificationAction action) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        helper.setTo(to);
        
        String actionText = action == VerificationAction.DEACTIVATE ? "tạm ngưng" : "xóa vĩnh viễn";
        String subject = action == VerificationAction.DEACTIVATE ? "Xác nhận tạm ngưng tài khoản" : "Xác nhận xóa tài khoản";
        
        helper.setSubject(subject);
        
        String htmlContent = generateEmailTemplate(verificationCode, actionText, action);
        helper.setText(htmlContent, true);
        
        mailSender.send(mimeMessage);
    }
    
    /**
     * Generate email template
     */
    private String generateEmailTemplate(String verificationCode, String actionText, VerificationAction action) {
        String warningText = action == VerificationAction.DELETE 
            ? "<div style='background-color: #ffebee; border-left: 4px solid #f44336; padding: 15px; margin: 20px 0;'>" +
              "<strong style='color: #c62828;'>⚠️ CẢNH BÁO:</strong><br>" +
              "Việc xóa tài khoản là không thể hoàn tác. Tất cả dữ liệu của bạn sẽ bị xóa vĩnh viễn!" +
              "</div>"
            : "<div style='background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0;'>" +
              "<strong style='color: #ef6c00;'>ℹ️ LƯU Ý:</strong><br>" +
              "Tài khoản tạm ngưng có thể được kích hoạt lại bằng cách đăng nhập." +
              "</div>";
        
        return "<html>" +
               "<head>" +
               "  <style>" +
               "    .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }" +
               "    .code-container { background-color: #f5f5f5; padding: 20px; margin: 20px 0; border-radius: 8px; text-align: center; }" +
               "    .verification-code { font-family: monospace; font-size: 32px; font-weight: bold; color: #333; letter-spacing: 8px; }" +
               "    .instructions { line-height: 1.6; margin: 20px 0; }" +
               "    .expiry-info { color: #666; font-size: 14px; margin-top: 15px; }" +
               "    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #999; }" +
               "  </style>" +
               "</head>" +
               "<body>" +
               "  <div class='container'>" +
               "    <h2>Xác nhận " + actionText + " tài khoản</h2>" +
               "    <p>Chúng tôi đã nhận được yêu cầu " + actionText + " tài khoản của bạn.</p>" +
               "    " + warningText +
               "    <div class='instructions'>" +
               "      <p>Để xác nhận hành động này, vui lòng nhập mã xác nhận bên dưới vào ứng dụng:</p>" +
               "    </div>" +
               "    <div class='code-container'>" +
               "      <div class='verification-code'>" + verificationCode + "</div>" +
               "      <div class='expiry-info'>Mã này sẽ hết hạn sau 15 phút</div>" +
               "    </div>" +
               "    <div class='instructions'>" +
               "      <p><strong>Nếu bạn không yêu cầu hành động này:</strong></p>" +
               "      <ul>" +
               "        <li>Bỏ qua email này</li>" +
               "        <li>Mã xác nhận sẽ tự động hết hạn</li>" +
               "        <li>Đổi mật khẩu tài khoản để đảm bảo an toàn</li>" +
               "      </ul>" +
               "    </div>" +
               "    <div class='footer'>" +
               "      <p>Email này được gửi từ TradeUp App. Vui lòng không trả lời email này.</p>" +
               "    </div>" +
               "  </div>" +
               "</body>" +
               "</html>";
    }
}
