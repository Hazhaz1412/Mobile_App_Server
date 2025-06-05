package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ListingService {
    
    @Autowired
    private ListingRepository listingRepository;
    
    @Autowired
    private ListingImageRepository listingImageRepository;
    
    @Autowired
    private ListingTagRepository listingTagRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ItemConditionRepository itemConditionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository; // Thêm dòng này
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Transactional
    public Listing createListing(Long userId, CreateListingRequest request) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User không tồn tại!");
        }
        
        // Validate category exists
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new RuntimeException("Danh mục không tồn tại!");
        }
        
        // Validate condition exists
        if (!itemConditionRepository.existsById(request.getConditionId())) {
            throw new RuntimeException("Tình trạng không tồn tại!");
        }
        
        // Create listing
        Listing listing = new Listing();
        listing.setUserId(userId);
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setPrice(request.getPrice());
        listing.setCategoryId(request.getCategoryId());
        listing.setConditionId(request.getConditionId());
        listing.setLocationText(request.getLocationText());
        listing.setLatitude(request.getLatitude());
        listing.setLongitude(request.getLongitude());
        listing.setStatus(ListingStatus.AVAILABLE);
        
        Listing savedListing = listingRepository.save(listing);
        
        // Save tags if provided
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<ListingTag> tags = request.getTags().stream()
                .map(tagName -> new ListingTag(savedListing.getId(), tagName))
                .collect(Collectors.toList());
            listingTagRepository.saveAll(tags);
        }
        
        return savedListing;
    }
    
    @Transactional
    public Listing addImagesToListing(Long listingId, Long userId, List<MultipartFile> images) {
        // Validate listing exists and belongs to user
        Optional<Listing> listingOptional = listingRepository.findByIdAndUserId(listingId, userId);
        if (listingOptional.isEmpty()) {
            throw new RuntimeException("Listing không tồn tại hoặc bạn không có quyền chỉnh sửa!");
        }
        
        Listing listing = listingOptional.get();
        
        // Check if already has images
        List<ListingImage> existingImages = listingImageRepository.findByListingIdOrderByDisplayOrder(listingId);
        
        // Validate total images count (max 10)
        if (existingImages.size() + images.size() > 10) {
            throw new RuntimeException("Tối đa 10 hình ảnh cho một sản phẩm!");
        }
        
        // Upload and save images
        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            
            // Validate image
            if (!fileStorageService.isValidImageFile(image)) {
                throw new RuntimeException("File " + image.getOriginalFilename() + " không phải là hình ảnh hợp lệ!");
            }
            
            // Upload image
            String imageUrl = fileStorageService.uploadImage(image, "listings");
            
            // Save image record
            ListingImage listingImage = new ListingImage();
            listingImage.setListingId(listingId);
            listingImage.setImageUrl(imageUrl);
            listingImage.setDisplayOrder(existingImages.size() + i);
            listingImage.setIsPrimary(existingImages.isEmpty() && i == 0); // First image is primary
            
            listingImageRepository.save(listingImage);
        }
        
        return listing;
    }
    
    @Transactional
    public Listing updateListing(Long listingId, Long userId, UpdateListingRequest request) {
        Optional<Listing> listingOptional = listingRepository.findByIdAndUserId(listingId, userId);
        if (listingOptional.isEmpty()) {
            throw new RuntimeException("Listing không tồn tại hoặc bạn không có quyền chỉnh sửa!");
        }
        
        Listing listing = listingOptional.get();
        
        // Update fields if provided
        if (request.getTitle() != null) {
            listing.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            listing.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            listing.setPrice(request.getPrice());
        }
        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw new RuntimeException("Danh mục không tồn tại!");
            }
            listing.setCategoryId(request.getCategoryId());
        }
        if (request.getConditionId() != null) {
            if (!itemConditionRepository.existsById(request.getConditionId())) {
                throw new RuntimeException("Tình trạng không tồn tại!");
            }
            listing.setConditionId(request.getConditionId());
        }
        if (request.getLocationText() != null) {
            listing.setLocationText(request.getLocationText());
        }
        if (request.getLatitude() != null) {
            listing.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            listing.setLongitude(request.getLongitude());
        }
        if (request.getStatus() != null) {
            try {
                ListingStatus status = ListingStatus.valueOf(request.getStatus().toUpperCase());
                listing.setStatus(status);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Status không hợp lệ!");
            }
        }
        
        Listing savedListing = listingRepository.save(listing);
        
        // Update tags if provided
        if (request.getTags() != null) {
            // Delete old tags
            listingTagRepository.deleteByListingId(listingId);
            
            // Save new tags
            if (!request.getTags().isEmpty()) {
                List<ListingTag> tags = request.getTags().stream()
                    .map(tagName -> new ListingTag(listingId, tagName))
                    .collect(Collectors.toList());
                listingTagRepository.saveAll(tags);
            }
        }
        
        return savedListing;
    }
    
    @Transactional
    public void deleteListing(Long listingId, Long userId) {
        Optional<Listing> listingOptional = listingRepository.findByIdAndUserId(listingId, userId);
        if (listingOptional.isEmpty()) {
            throw new RuntimeException("Listing không tồn tại hoặc bạn không có quyền xóa!");
        }
        
        Listing listing = listingOptional.get();
        listing.setStatus(ListingStatus.DELETED);
        listingRepository.save(listing);
    }
    
    public Page<ListingResponse> getUserListings(Long userId, String status, Pageable pageable) {
        Page<Listing> listings;
        
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            try {
                ListingStatus listingStatus = ListingStatus.valueOf(status.toUpperCase());
                listings = listingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, listingStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Status không hợp lệ!");
            }
        } else {
            // Get all except DELETED
            listings = listingRepository.findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, ListingStatus.DELETED, pageable);
        }
        
        return listings.map(this::convertToResponse);
    }
    
    public Page<ListingResponse> getAvailableListings(Pageable pageable) {
        Page<Listing> listings = listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.AVAILABLE, pageable);
        return listings.map(this::convertToResponse);
    }
    
    public Page<ListingResponse> getListingsByCategory(Long categoryId, Pageable pageable) {
        Page<Listing> listings = listingRepository.findByCategoryIdAndStatusOrderByCreatedAtDesc(categoryId, ListingStatus.AVAILABLE, pageable);
        return listings.map(this::convertToResponse);
    }
    
    public Page<ListingResponse> searchListings(String keyword, Pageable pageable) {
        Page<Listing> listings = listingRepository.searchByKeyword(keyword, ListingStatus.AVAILABLE, pageable);
        return listings.map(this::convertToResponse);
    }
    
    public ListingResponse getListingById(Long listingId) {
        Optional<Listing> listingOptional = listingRepository.findById(listingId);
        if (listingOptional.isEmpty()) {
            throw new RuntimeException("Listing không tồn tại!");
        }
        
        Listing listing = listingOptional.get();
        
        // Increment view count
        listing.setViewCount(listing.getViewCount() + 1);
        listingRepository.save(listing);
        
        return convertToResponse(listing);
    }
    
    public List<Category> getAllCategories() {
        return categoryRepository.findByIsActiveTrueOrderByName();
    }
    
    public List<ItemCondition> getAllConditions() {
        return itemConditionRepository.findAllByOrderByDisplayOrder();
    }
    
    @Transactional
    public void incrementInteraction(Long listingId) {
        Optional<Listing> listingOptional = listingRepository.findById(listingId);
        if (listingOptional.isPresent()) {
            Listing listing = listingOptional.get();
            listing.setInteractionCount(listing.getInteractionCount() + 1);
            listingRepository.save(listing);
        }
    }
    
    private ListingResponse convertToResponse(Listing listing) {
        ListingResponse response = new ListingResponse();
        response.setId(listing.getId());
        response.setUserId(listing.getUserId());
        response.setTitle(listing.getTitle());
        response.setDescription(listing.getDescription());
        response.setPrice(listing.getPrice());
        response.setCategoryId(listing.getCategoryId());
        response.setConditionId(listing.getConditionId());
        response.setLocationText(listing.getLocationText());
        response.setLatitude(listing.getLatitude());
        response.setLongitude(listing.getLongitude());
        response.setStatus(listing.getStatus());
        response.setViewCount(listing.getViewCount());
        response.setInteractionCount(listing.getInteractionCount());
        response.setCreatedAt(listing.getCreatedAt());
        response.setUpdatedAt(listing.getUpdatedAt());
        
        // Get category name
        if (listing.getCategory() != null) {
            response.setCategoryName(listing.getCategory().getName());
        } else {
            categoryRepository.findById(listing.getCategoryId())
                .ifPresent(category -> response.setCategoryName(category.getName()));
        }
        
        // Get condition name
        if (listing.getItemCondition() != null) {
            response.setConditionName(listing.getItemCondition().getName());
        } else {
            itemConditionRepository.findById(listing.getConditionId())
                .ifPresent(condition -> response.setConditionName(condition.getName()));
        }
        
        // Get user display name
        userProfileRepository.findByUserId(listing.getUserId())
            .ifPresent(profile -> response.setUserDisplayName(profile.getDisplayName()));
        
        // Get images
        List<ListingImage> images = listingImageRepository.findByListingIdOrderByDisplayOrder(listing.getId());
        response.setImageUrls(images.stream().map(ListingImage::getImageUrl).collect(Collectors.toList()));
        
        // Get primary image
        images.stream()
            .filter(ListingImage::getIsPrimary)
            .findFirst()
            .ifPresent(img -> response.setPrimaryImageUrl(img.getImageUrl()));
        
        if (response.getPrimaryImageUrl() == null && !images.isEmpty()) {
            response.setPrimaryImageUrl(images.get(0).getImageUrl());
        }
        
        // Get tags
        List<ListingTag> tags = listingTagRepository.findByListingId(listing.getId());
        response.setTags(tags.stream().map(ListingTag::getTagName).collect(Collectors.toList()));
        
        return response;
    }
}