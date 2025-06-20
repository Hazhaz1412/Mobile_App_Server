package com.example.demo.service;

import com.example.demo.entity.UserProfile;
import com.example.demo.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.dto.UserProfileUpdateRequest;
import java.util.Optional;

@Service
public class UserProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private StorageService storageService;
    
    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private com.example.demo.repository.UserRepository userRepository;

    @Transactional
    public UserProfile updateProfilePicture(Long userId, MultipartFile file) {
        UserProfile profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User profile not found"));
            
        // Delete old image if exists
        String oldImageUrl = profile.getProfilePictureUrl();
        if (oldImageUrl != null && oldImageUrl.startsWith(baseUrl + "/uploads/")) {
            String oldFileName = oldImageUrl.substring(oldImageUrl.lastIndexOf("/") + 1);
            storageService.deleteFile(oldFileName);
        }
        
        // Store new image
        String fileName = storageService.storeFile(file);
        String imageUrl = baseUrl + "/uploads/" + fileName;
        
        profile.setProfilePictureUrl(imageUrl);
        return userProfileRepository.save(profile);
    }
      public UserProfile getUserProfile(Long userId) {
        Optional<UserProfile> profile = userProfileRepository.findById(userId);
        return profile.orElse(null);
    }
    
    @Transactional
    public UserProfile updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User profile not found"));
        
        if (request.getDisplayName() != null && !request.getDisplayName().trim().isEmpty()) {
            profile.setDisplayName(request.getDisplayName().trim());
        }
        
        if (request.getBio() != null) {
            profile.setBio(request.getBio().trim());
        }
        
        if (request.getContactInfo() != null) {
            profile.setContactInfo(request.getContactInfo().trim());
        }
        
        return userProfileRepository.save(profile);
    }
    
    public java.util.List<UserProfile> getAllUserProfiles() {
        return userProfileRepository.findAll();
    }    // Simple method để tạo user profiles chính
    @Transactional
    public int createMissingUserProfiles() {
        int created = 0;
        // Tạo profiles cho các tài khoản chính
        Long[] mainUserIds = {9L, 10L, 21L, 22L, 23L};
        
        for (Long userId : mainUserIds) {
            if (!userProfileRepository.existsByUserId(userId)) {
                UserProfile profile = new UserProfile();
                profile.setUserId(userId);
                profile.setDisplayName("Test User " + userId);
                profile.setBio("");
                profile.setContactInfo("");
                profile.setRatingAvg(0.0);
                profile.setRatingCount(0);
                profile.setProfilePictureUrl(null);
                
                try {
                    userProfileRepository.save(profile);
                    created++;
                } catch (Exception e) {
                    System.err.println("Failed to create profile for user " + userId + ": " + e.getMessage());
                }
            }
        }
        
        return created;
    }
}