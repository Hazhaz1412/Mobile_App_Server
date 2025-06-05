package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.service.ListingService;
import com.example.demo.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
@CrossOrigin(origins = "*")
@Validated
public class ListingController {
    
    @Autowired
    private ListingService listingService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    // Create new listing
    @PostMapping
    @CrossOrigin(origins = "*")
    public ResponseEntity<ApiResponse> createListing(
            @RequestParam Long userId,
            @Valid @RequestBody CreateListingRequest request) {
        try {
            Listing listing = listingService.createListing(userId, request);
            return ResponseEntity.ok(new ApiResponse(
                true, 
                "Tạo listing thành công!", 
                listing.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false, 
                e.getMessage()
            ));
        }
    }
    
    // SỬA: Upload images endpoint để match với Android
    @PostMapping("/{listingId}/images")
    @CrossOrigin(origins = "*")
    public ResponseEntity<ApiResponse> uploadImages(
            @PathVariable Long listingId,
            @RequestParam Long userId,
            @RequestParam("images") MultipartFile[] images) { // SỬA: Dùng array và @RequestParam
        try {
            System.out.println("=== UPLOAD IMAGES DEBUG ===");
            System.out.println("ListingId: " + listingId);
            System.out.println("UserId: " + userId);
            System.out.println("Images count: " + (images != null ? images.length : 0));
            
            if (images == null || images.length == 0) {
                return ResponseEntity.badRequest().body(new ApiResponse(
                    false, 
                    "Vui lòng chọn ít nhất một hình ảnh!"
                ));
            }
            
            // Validate each image
            for (int i = 0; i < images.length; i++) {
                MultipartFile image = images[i];
                System.out.println("Image " + i + " - Name: " + image.getOriginalFilename() + 
                                 ", Size: " + image.getSize() + 
                                 ", ContentType: " + image.getContentType());
                
                if (image.isEmpty()) {
                    return ResponseEntity.badRequest().body(new ApiResponse(
                        false, 
                        "Hình ảnh thứ " + (i + 1) + " bị trống!"
                    ));
                }
                
                if (!fileStorageService.isValidImageFile(image)) {
                    return ResponseEntity.badRequest().body(new ApiResponse(
                        false, 
                        "File " + image.getOriginalFilename() + " không phải là hình ảnh hợp lệ!"
                    ));
                }
            }
            
            // Convert array to List
            List<MultipartFile> imageList = Arrays.asList(images);
            listingService.addImagesToListing(listingId, userId, imageList);
            
            return ResponseEntity.ok(new ApiResponse(
                true, 
                "Upload hình ảnh thành công!"
            ));
        } catch (RuntimeException e) {
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse(
                false, 
                "Lỗi upload: " + e.getMessage()
            ));
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse(
                false, 
                "Lỗi không xác định: " + e.getMessage()
            ));
        }
    }
    
    // Test endpoint để upload single image
    @PostMapping("/test-upload")
    @CrossOrigin(origins = "*")
    public ResponseEntity<ApiResponse> testUpload(
            @RequestParam("image") MultipartFile image) {
        try {
            System.out.println("=== TEST UPLOAD DEBUG ===");
            System.out.println("Image name: " + image.getOriginalFilename());
            System.out.println("Image size: " + image.getSize());
            System.out.println("Content type: " + image.getContentType());
            
            if (image.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(
                    false, 
                    "File không được để trống!"
                ));
            }
            
            String imageUrl = fileStorageService.uploadImage(image, "test");
            
            return ResponseEntity.ok(new ApiResponse(
                true, 
                "Upload thành công!",
                imageUrl
            ));
        } catch (Exception e) {
            System.err.println("Test upload error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse(
                false, 
                "Lỗi upload: " + e.getMessage()
            ));
        }
    }
    
    // Update listing
    @PutMapping("/{listingId}")
    public ResponseEntity<ApiResponse> updateListing(
            @PathVariable Long listingId,
            @RequestParam Long userId,
            @Valid @RequestBody UpdateListingRequest request) {
        try {
            Listing listing = listingService.updateListing(listingId, userId, request);
            return ResponseEntity.ok(new ApiResponse(
                true, 
                "Cập nhật listing thành công!", 
                listing.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false, 
                e.getMessage()
            ));
        }
    }
    
    // Delete listing
    @DeleteMapping("/{listingId}")
    public ResponseEntity<ApiResponse> deleteListing(
            @PathVariable Long listingId,
            @RequestParam Long userId) {
        try {
            listingService.deleteListing(listingId, userId);
            return ResponseEntity.ok(new ApiResponse(
                true, 
                "Xóa listing thành công!"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false, 
                e.getMessage()
            ));
        }
    }
    
    // Get user's listings
    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedApiResponse<ListingResponse>> getUserListings(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ListingResponse> listings = listingService.getUserListings(userId, status, pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true, 
                "Lấy danh sách listing thành công!",
                listings.getContent(),
                listings.getNumber(),
                listings.getSize(),
                listings.getTotalElements(),
                listings.getTotalPages()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new PagedApiResponse<>(
                false, 
                e.getMessage()
            ));
        }
    }
    
    // Get all available listings
    @GetMapping
    public ResponseEntity<PagedApiResponse<ListingResponse>> getAvailableListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ListingResponse> listings = listingService.getAvailableListings(pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lấy danh sách listing thành công!",
                listings.getContent(),
                listings.getNumber(),
                listings.getSize(),
                listings.getTotalElements(),
                listings.getTotalPages()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new PagedApiResponse<>(
                false,
                e.getMessage()
            ));
        }
    }
    
    // Get listing detail
    @GetMapping("/{listingId}")
    public ResponseEntity<ApiResponse> getListingDetail(@PathVariable Long listingId) {
        try {
            ListingResponse listing = listingService.getListingById(listingId);
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Lấy thông tin listing thành công!",
                listing
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    // Get all categories
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse> getAllCategories() {
        try {
            List<Category> categories = listingService.getAllCategories();
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Lấy danh sách danh mục thành công!",
                categories
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
    
    // Get all conditions
    @GetMapping("/conditions")
    public ResponseEntity<ApiResponse> getAllConditions() {
        try {
            List<ItemCondition> conditions = listingService.getAllConditions();
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Lấy danh sách tình trạng thành công!",
                conditions
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                e.getMessage()
            ));
        }
    }
}