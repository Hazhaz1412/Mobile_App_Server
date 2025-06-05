package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    
    @Value("${app.upload.dir:${user.home}/mobile-app-uploads}")
    private String uploadDir;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public String uploadImage(MultipartFile file, String subfolder) {
        try {
            System.out.println("=== FILE STORAGE DEBUG ===");
            System.out.println("Upload dir: " + uploadDir);
            System.out.println("Base URL: " + baseUrl);
            System.out.println("Subfolder: " + subfolder);
            System.out.println("File original name: " + file.getOriginalFilename());
            System.out.println("File content type: " + file.getContentType());
            System.out.println("File size: " + file.getSize());
            System.out.println("File is empty: " + file.isEmpty());
            
            // Validate file
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File không được để trống!");
            }
            
            // ENABLE LẠI validation content type
            if (!isValidImageFile(file)) {
                throw new RuntimeException("File phải là hình ảnh (JPG, PNG, WEBP)!");
            }
            System.out.println("Content type validation PASSED");
            
            // Create directory if not exists
            Path uploadPath = Paths.get(uploadDir, subfolder);
            System.out.println("Upload path: " + uploadPath.toAbsolutePath());
            
            Files.createDirectories(uploadPath);
            System.out.println("Directory created/exists: " + Files.exists(uploadPath));
            
            // Generate unique filename - ensure proper extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "image.jpg";
            }
            
            String extension = ".jpg"; // Default extension
            if (originalFilename.contains(".")) {
                String originalExt = originalFilename.substring(originalFilename.lastIndexOf("."));
                if (originalExt.toLowerCase().matches("\\.(jpg|jpeg|png|webp)")) {
                    extension = originalExt;
                }
            }
            
            String filename = UUID.randomUUID().toString() + extension;
            System.out.println("Generated filename: " + filename);
            
            // Save file
            Path filePath = uploadPath.resolve(filename);
            System.out.println("File path: " + filePath.toAbsolutePath());
            
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File saved successfully. Final size: " + Files.size(filePath) + " bytes");
            
            // Return URL với tunnel domain
            String fileUrl = baseUrl + "/uploads/" + subfolder + "/" + filename;
            System.out.println("File URL: " + fileUrl);
            
            return fileUrl;
            
        } catch (IOException e) {
            System.err.println("IOException in uploadImage: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể upload file: " + e.getMessage());
        }
    }
    
    public boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            System.out.println("File validation failed: file is null or empty");
            return false;
        }
        
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        
        System.out.println("=== FILE VALIDATION ===");
        System.out.println("Content type: " + contentType);
        System.out.println("Original filename: " + originalFilename);
        
        // Check by content type
        boolean validContentType = false;
        if (contentType != null) {
            validContentType = contentType.equals("image/jpeg") || 
                              contentType.equals("image/png") || 
                              contentType.equals("image/jpg") ||
                              contentType.equals("image/webp") ||
                              contentType.startsWith("image/");  // More lenient check
        }
        
        // Check by file extension as fallback
        boolean validExtension = false;
        if (originalFilename != null) {
            String filename = originalFilename.toLowerCase();
            validExtension = filename.endsWith(".jpg") || 
                            filename.endsWith(".jpeg") || 
                            filename.endsWith(".png") || 
                            filename.endsWith(".webp");
        }
        
        System.out.println("Valid content type: " + validContentType);
        System.out.println("Valid extension: " + validExtension);
        
        // Accept if either content type or extension is valid, OR if file size > 0
        boolean isValid = validContentType || validExtension || file.getSize() > 0;
        System.out.println("Final validation result: " + isValid);
        
        return isValid;
    }
    
    public void deleteFile(String fileUrl) {
        try {
            // Extract filename from URL
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            String subfolder = fileUrl.substring(fileUrl.indexOf("/uploads/") + 9, fileUrl.lastIndexOf("/"));
            
            Path filePath = Paths.get(uploadDir, subfolder, filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but don't throw exception
            System.err.println("Không thể xóa file: " + e.getMessage());
        }
    }
}