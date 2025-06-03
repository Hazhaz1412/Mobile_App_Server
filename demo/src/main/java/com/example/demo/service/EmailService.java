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
        // Your existing code...
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