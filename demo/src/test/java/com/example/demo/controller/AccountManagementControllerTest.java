package com.example.demo.controller;

import com.example.demo.dto.SendVerificationRequest;
import com.example.demo.dto.VerifyCodeRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.entity.VerificationAction;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.mail.host=localhost",
    "spring.mail.port=25",
    "spring.mail.properties.mail.smtp.auth=false",
    "spring.mail.properties.mail.smtp.starttls.enable=false"
})
@Transactional
public class AccountManagementControllerTest {
    
    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private String validToken;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
        
        objectMapper = new ObjectMapper();
        
        // Create test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);
        
        // Generate valid JWT token
        validToken = jwtTokenProvider.generateAccessToken(testUser.getId());
    }
    
    @Test
    void testSendVerificationCode_Success() throws Exception {
        SendVerificationRequest request = new SendVerificationRequest();
        request.setEmail("test@example.com");
        request.setAction(VerificationAction.DEACTIVATE);
        
        mockMvc.perform(post("/api/users/send-verification")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mã xác nhận đã được gửi đến email của bạn để tạm ngưng tài khoản"));
    }
    
    @Test
    void testSendVerificationCode_EmailMismatch() throws Exception {
        SendVerificationRequest request = new SendVerificationRequest();
        request.setEmail("wrong@example.com");
        request.setAction(VerificationAction.DEACTIVATE);
        
        mockMvc.perform(post("/api/users/send-verification")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email does not match your account"));
    }
    
    @Test
    void testSendVerificationCode_Unauthorized() throws Exception {
        SendVerificationRequest request = new SendVerificationRequest();
        request.setEmail("test@example.com");
        request.setAction(VerificationAction.DEACTIVATE);
        
        mockMvc.perform(post("/api/users/send-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testDeactivateAccount_InvalidCode() throws Exception {
        VerifyCodeRequest request = new VerifyCodeRequest();
        request.setVerificationCode("123456");
        
        mockMvc.perform(post("/api/users/" + testUser.getId() + "/deactivate")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Mã xác nhận không hợp lệ hoặc đã hết hạn"));
    }
    
    @Test
    void testDeleteAccount_DifferentUser() throws Exception {
        // Create another user
        User anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setPasswordHash("hashedpassword");
        anotherUser.setStatus(UserStatus.ACTIVE);
        anotherUser = userRepository.save(anotherUser);
        
        VerifyCodeRequest request = new VerifyCodeRequest();
        request.setVerificationCode("123456");
        
        mockMvc.perform(delete("/api/users/" + anotherUser.getId())
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You can only manage your own account"));
    }
}
