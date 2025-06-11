package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.ActivityType;
import com.example.demo.entity.Listing; // THÊM import này
import com.example.demo.service.FileStorageService;
import com.example.demo.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
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
    
    /**
     * Advanced search endpoint
     */
    @GetMapping("/search")
    public ResponseEntity<PagedApiResponse<ListingResponse>> searchListings(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long conditionId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(defaultValue = "NEWEST") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId) {
        try {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setKeyword(keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null);
            criteria.setCategoryId(categoryId);
            criteria.setConditionId(conditionId);
            criteria.setMinPrice(minPrice);
            criteria.setMaxPrice(maxPrice);
            criteria.setLatitude(latitude);
            criteria.setLongitude(longitude);
            criteria.setMaxDistance(maxDistance);
            criteria.setSortBy(sortBy);
            criteria.setPage(page);
            criteria.setSize(size);
            
            // Track search activity if user is logged in
            if (userId != null && keyword != null && !keyword.trim().isEmpty()) {
                // Note: We'll track this as a general search activity
                // You might want to create a separate table for search history
            }
            
            Page<ListingResponse> listings = listingService.searchListings(criteria);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Tìm kiếm thành công!",
                listings.getContent(),
                listings.getNumber(),
                listings.getSize(),
                listings.getTotalElements(),
                listings.getTotalPages()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new PagedApiResponse<>(
                false,
                "Lỗi tìm kiếm: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get personalized recommendations
     */
    @GetMapping("/recommendations")
    public ResponseEntity<PagedApiResponse<ListingResponse>> getRecommendations(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ListingResponse> listings = listingService.getRecommendations(userId, pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lấy danh sách đề xuất thành công!",
                listings.getContent(),
                listings.getNumber(),
                listings.getSize(),
                listings.getTotalElements(),
                listings.getTotalPages()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new PagedApiResponse<>(
                false,
                "Lỗi lấy danh sách đề xuất: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get popular listings
     */
    @GetMapping("/popular")
    public ResponseEntity<PagedApiResponse<ListingResponse>> getPopularListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ListingResponse> listings = listingService.getPopularListings(pageable);
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lấy danh sách phổ biến thành công!",
                listings.getContent(),
                listings.getNumber(),
                listings.getSize(),
                listings.getTotalElements(),
                listings.getTotalPages()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new PagedApiResponse<>(
                false,
                "Lỗi lấy danh sách phổ biến: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get nearby listings
     */
    @GetMapping("/nearby")
    public ResponseEntity<PagedApiResponse<ListingResponse>> getNearbyListings(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "10.0") Double maxDistance,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ListingResponse> listings = listingService.getNearbyListings(
                latitude, longitude, maxDistance, pageable
            );
            
            return ResponseEntity.ok(new PagedApiResponse<>(
                true,
                "Lấy danh sách gần đây thành công!",
                listings.getContent(),
                listings.getNumber(),
                listings.getSize(),
                listings.getTotalElements(),
                listings.getTotalPages()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new PagedApiResponse<>(
                false,
                "Lỗi lấy danh sách gần đây: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get listing detail with activity tracking
     */
    @GetMapping("/{listingId}")
    public ResponseEntity<ApiResponse> getListingDetail(
            @PathVariable Long listingId,
            @RequestParam(required = false) Long userId) {
        try {
            ListingResponse listing = listingService.getListingById(listingId, userId);
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
    
    /**
     * Track interaction (like, share, contact, etc.)
     */
    @PostMapping("/{listingId}/interact")
    public ResponseEntity<ApiResponse> trackInteraction(
            @PathVariable Long listingId,
            @RequestParam(required = false) Long userId) {
        try {
            listingService.incrementInteraction(listingId);
            
            if (userId != null) {
                listingService.trackUserActivity(userId, listingId, ActivityType.INTERACT);
            }
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Cập nhật tương tác thành công!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Lỗi cập nhật tương tác: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Home recommendations: categories with recommended listings for each
     */
    @GetMapping("/home-recommendations")
    public ResponseEntity<ApiResponse> getHomeRecommendations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false, defaultValue = "10.0") Double maxDistance,
            @RequestParam(required = false, defaultValue = "5") Integer listingsPerCategory
    ) {
        try {
            List<CategoryWithListingsResponse> data = listingService.getHomeRecommendations(userId, latitude, longitude, maxDistance, listingsPerCategory);
            return ResponseEntity.ok(new ApiResponse(true, "Lấy gợi ý trang chủ thành công!", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Lỗi lấy gợi ý trang chủ: " + e.getMessage()));
        }
    }
}