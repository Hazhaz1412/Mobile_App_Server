package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendPasswordResetEmail(String to, String resetUrl) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("Đặt lại mật khẩu");
            
            String token = resetUrl.substring(resetUrl.indexOf("token=") + 6);
            
            String htmlContent = 
                "<html>" +
                "<head>" +
                "  <style>" +
                "    .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "    .token-container { background-color: #f5f5f5; padding: 15px; margin: 15px 0; border-radius: 5px; position: relative; }" +
                "    .token { font-family: monospace; font-size: 16px; word-break: break-all; margin-right: 50px; }" +
                "    .copy-btn { position: absolute; top: 10px; right: 10px; background-color: #4CAF50; color: white; " +
                "      border: none; padding: 5px 10px; border-radius: 3px; cursor: pointer; }" +
                "    .instructions { line-height: 1.6; }" +
                "    .web-link { margin-top: 20px; }" +
                "    .note { margin-top: 20px; font-style: italic; color: #666; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class='container'>" +
                "    <h2>Đặt lại mật khẩu</h2>" +
                "    <div class='instructions'>" +
                "      <p>Để đặt lại mật khẩu, vui lòng:</p>" +
                "      <ol>" +
                "        <li>Mở ứng dụng di động và chọn 'Quên mật khẩu'</li>" +
                "        <li>Sau khi nhập email, nhấn 'Tiếp tục đến đặt lại mật khẩu'</li>" +
                "        <li>Nhập mã token bên dưới vào ứng dụng:</li>" +
                "      </ol>" +
                "    </div>" +
                "    <div class='token-container'>" +
                "      <div class='token' id='token' style='background-color: #f0f0f0; padding: 10px; border: 1px dashed #999; border-radius: 4px; text-align: center; margin: 15px 0;'>" + token + "</div>" +
                "      <p><em>Chọn và sao chép mã trên để sử dụng trong ứng dụng</em></p>" +
                "    </div>" +
                "    <div class='web-link'>" +
                "      <p>Hoặc nếu bạn đang sử dụng máy tính, hãy truy cập đường dẫn này:</p>" +
                "      <a href='" + resetUrl + "'>" + resetUrl + "</a>" +
                "    </div>" +
                "    <div class='note'>" +
                "      <p>Lưu ý: Mã token này sẽ hết hạn sau 1 giờ.</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
            
            helper.setText(createPlainTextVersion(token, resetUrl), htmlContent);
            
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
    
    @Async("taskExecutor")
    public void sendPasswordResetEmailAsync(String to, String resetUrl) {
        sendPasswordResetEmail(to, resetUrl);
    }
    
    @Async("taskExecutor")
    public void sendVerificationEmail(String to, String verificationUrl) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("Xác thực tài khoản - Ứng dụng di động");
            
            String htmlContent = 
                "<html>" +
                "<head>" +
                "  <style>" +
                "    .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }" +
                "    .content { background-color: #f9f9f9; padding: 20px; border-radius: 0 0 5px 5px; }" +
                "    .verification-btn { background-color: #4CAF50; color: white; padding: 12px 24px; " +
                "      text-decoration: none; border-radius: 5px; display: inline-block; margin: 15px 0; }" +
                "    .instructions { line-height: 1.6; margin: 20px 0; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class='container'>" +
                "    <div class='header'>" +
                "      <h2>Chào mừng bạn đến với ứng dụng!</h2>" +
                "    </div>" +
                "    <div class='content'>" +
                "      <p>Cảm ơn bạn đã đăng ký tài khoản. Để hoàn tất quá trình đăng ký, vui lòng xác thực email của bạn.</p>" +
                "      <div style='text-align: center;'>" +
                "        <a href='" + verificationUrl + "' class='verification-btn'>XÁC THỰC TÀI KHOẢN</a>" +
                "      </div>" +
                "      <div class='instructions'>" +
                "        <p><strong>Sau khi xác thực thành công:</strong></p>" +
                "        <ol>" +
                "          <li>Quay lại ứng dụng di động</li>" +
                "          <li>Đăng nhập với email và mật khẩu đã đăng ký</li>" +
                "          <li>Bắt đầu sử dụng ứng dụng!</li>" +
                "        </ol>" +
                "      </div>" +
                "      <p><em>Liên kết xác thực sẽ hết hạn sau 24 giờ.</em></p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
            
            helper.setText("Vui lòng xác thực tài khoản bằng cách truy cập: " + verificationUrl, htmlContent);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi gửi email xác thực: " + e.getMessage(), e);
        }
    }
    
    private String createPlainTextVersion(String token, String resetUrl) {
        return "Để đặt lại mật khẩu, vui lòng:\n\n" +
               "1. Mở ứng dụng di động và chọn 'Quên mật khẩu'\n" +
               "2. Sau khi nhập email, nhấn 'Tiếp tục đến đặt lại mật khẩu'\n" +
               "3. Nhập mã token này vào ứng dụng:\n\n" + 
               token + "\n\n" +
               "Hoặc nếu bạn đang sử dụng máy tính, hãy truy cập đường dẫn này:\n" + 
               resetUrl + "\n\n" +
               "Lưu ý: Mã token này sẽ hết hạn sau 1 giờ.";
    }
}