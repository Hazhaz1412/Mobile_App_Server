package com.example.demo.service;

import com.example.demo.entity.UserProfile;
import com.example.demo.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class UserProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private StorageService storageService;
    
    @Value("${app.base-url}")
    private String baseUrl;

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
        return userProfileRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User profile not found"));
    }
    
    // Other existing methods...
}