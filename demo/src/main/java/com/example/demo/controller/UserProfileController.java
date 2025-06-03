package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.UserProfile;
import com.example.demo.service.StorageService;
import com.example.demo.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.example.demo.dto.UserProfileResponse;
import java.math.BigDecimal;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;
    
    @Autowired
    private StorageService storageService;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    @Value("${app.upload.dir:${user.home}/uploads}")
    private String uploadDir;
    
    @PostMapping("/{userId}/profile-image")
    public ResponseEntity<ApiResponse> uploadProfileImage(
            @PathVariable Long userId,
            @RequestParam("image") MultipartFile file) {
        
        try {
            UserProfile profile = userProfileService.updateProfilePicture(userId, file);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Profile image updated successfully",
                profile.getProfilePictureUrl()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Error uploading profile image: " + e.getMessage()
            ));
        }
    }

    @GetMapping(value = "/{userId}/profile-image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> getProfileImage(@PathVariable Long userId) {
        try {
            UserProfile profile = userProfileService.getUserProfile(userId);
            if (profile.getProfilePictureUrl() == null) {
                return ResponseEntity.notFound().build();
            }
            
            // The URL should be in the format baseUrl/uploads/filename
            String imageUrl = profile.getProfilePictureUrl();
            String filename = Paths.get(imageUrl).getFileName().toString();
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "inline")
                .body(storageService.loadFileAsResource(filename));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        try {
            UserProfile profile = userProfileService.getUserProfile(userId);
            UserProfileResponse response = convertToResponse(profile);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private UserProfileResponse convertToResponse(UserProfile profile) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(profile.getUserId());
        response.setDisplayName(profile.getDisplayName());
        
        // Xử lý an toàn nếu user là null
        if (profile.getUser() != null) {
            response.setEmail(profile.getUser().getEmail());
        } else {
            response.setEmail(null);
        }
        
        response.setBio(profile.getBio());
        response.setContactInfo(profile.getContactInfo());
        
        // Convert Double to BigDecimal
        if (profile.getRatingAvg() != null) {
            response.setRatingAvg(new BigDecimal(profile.getRatingAvg().toString()));
        }
        
        response.setRatingCount(profile.getRatingCount());
        response.setProfilePictureUrl(profile.getProfilePictureUrl());
        return response;
    }
}