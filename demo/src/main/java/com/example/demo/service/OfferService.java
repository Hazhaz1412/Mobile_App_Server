package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OfferService {
    
    @Autowired
    private OfferRepository offerRepository;
    
    @Autowired
    private ListingRepository listingRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private ListingImageRepository listingImageRepository;
    
    @Autowired
    private TransactionService transactionService;
    
    /**
     * Create a new offer for a listing
     */
    @Transactional
    public OfferResponse createOffer(Long buyerId, CreateOfferRequest request) {        // Validate listing exists and is available
        Listing listing = listingRepository.findById(request.getListingId())
            .orElseThrow(() -> new RuntimeException("Listing không tồn tại!"));
            
        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            if (listing.getStatus() == ListingStatus.SOLD) {
                throw new RuntimeException("Sản phẩm này đã được bán!");
            } else {
                throw new RuntimeException("Listing không còn khả dụng!");
            }
        }
        
        if (!listing.getIsNegotiable()) {
            throw new RuntimeException("Listing này không thể thương lượng giá!");
        }
        
        // Check if buyer is not the seller
        if (listing.getUserId().equals(buyerId)) {
            throw new RuntimeException("Bạn không thể đưa ra offer cho listing của chính mình!");
        }
        
        // Check if buyer already has a pending offer for this listing
        if (offerRepository.existsByListingIdAndBuyerIdAndStatus(
                request.getListingId(), buyerId, OfferStatus.PENDING)) {
            throw new RuntimeException("Bạn đã có offer đang chờ phản hồi cho listing này!");
        }
        
        // Validate offer amount is reasonable (not less than 10% of listing price)
        BigDecimal minOffer = listing.getPrice().multiply(new BigDecimal("0.1"));
        if (request.getOfferAmount().compareTo(minOffer) < 0) {
            throw new RuntimeException("Offer quá thấp! Tối thiểu 10% giá niêm yết.");
        }
        
        // Create the offer
        Offer offer = new Offer(
            request.getListingId(),
            buyerId,
            listing.getUserId(),
            request.getOfferAmount(),
            request.getMessage()
        );
        
        Offer savedOffer = offerRepository.save(offer);
        
        // Track user activity
        // TODO: Add notification to seller about new offer
        
        return convertToOfferResponse(savedOffer);
    }
    
    /**
     * Respond to an offer (accept, reject, or counter)
     */
    @Transactional
    public OfferResponse respondToOffer(Long offerId, Long sellerId, RespondToOfferRequest request) {
        Offer offer = offerRepository.findById(offerId)
            .orElseThrow(() -> new RuntimeException("Offer không tồn tại!"));
            
        // Validate seller owns the listing
        if (!offer.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Bạn không có quyền phản hồi offer này!");
        }
        
        // Check if offer can be responded to
        if (!offer.canBeAccepted()) {
            throw new RuntimeException("Offer không thể phản hồi (đã hết hạn hoặc đã được phản hồi)!");
        }
        
        // Validate request
        if (!request.isValid()) {
            throw new RuntimeException("Request không hợp lệ!");
        }
        
        String action = request.getAction().toUpperCase();
        
        switch (action) {
            case "ACCEPT":
                return acceptOffer(offer, request.getMessage());
            case "REJECT":
                return rejectOffer(offer, request.getMessage());
            case "COUNTER":
                return counterOffer(offer, request.getCounterAmount(), request.getMessage());
            default:
                throw new RuntimeException("Action không được hỗ trợ!");
        }
    }
    
    /**
     * Accept an offer and create transaction
     */
    private OfferResponse acceptOffer(Offer offer, String message) {
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setUpdatedAt(LocalDateTime.now());
        
        // Create transaction
        transactionService.createTransactionFromOffer(offer);
        
        // Mark listing as sold
        Listing listing = listingRepository.findById(offer.getListingId())
            .orElseThrow(() -> new RuntimeException("Listing không tồn tại!"));
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);
        
        // Reject all other pending offers for this listing
        List<Offer> otherOffers = offerRepository.findByListingIdAndStatus(
            offer.getListingId(), OfferStatus.PENDING);
        for (Offer otherOffer : otherOffers) {
            if (!otherOffer.getId().equals(offer.getId())) {
                otherOffer.setStatus(OfferStatus.REJECTED);
                offerRepository.save(otherOffer);
            }
        }
        
        Offer savedOffer = offerRepository.save(offer);
        // TODO: Send notification to buyer about accepted offer
        
        return convertToOfferResponse(savedOffer);
    }
    
    /**
     * Reject an offer
     */
    private OfferResponse rejectOffer(Offer offer, String message) {
        offer.setStatus(OfferStatus.REJECTED);
        offer.setUpdatedAt(LocalDateTime.now());
        
        Offer savedOffer = offerRepository.save(offer);
        // TODO: Send notification to buyer about rejected offer
        
        return convertToOfferResponse(savedOffer);
    }
    
    /**
     * Counter an offer with a new amount
     */
    private OfferResponse counterOffer(Offer offer, BigDecimal counterAmount, String message) {
        // Mark original offer as countered
        offer.setStatus(OfferStatus.COUNTERED);
        offer.setUpdatedAt(LocalDateTime.now());
        offerRepository.save(offer);
        
        // Create new counter offer (seller becomes buyer, buyer becomes seller)
        Offer counterOffer = new Offer(
            offer.getListingId(),
            offer.getSellerId(), // Seller becomes the new "buyer" for the counter offer
            offer.getBuyerId(),  // Original buyer becomes the new "seller"
            counterAmount,
            message
        );
        
        Offer savedCounterOffer = offerRepository.save(counterOffer);
        // TODO: Send notification to original buyer about counter offer
        
        return convertToOfferResponse(savedCounterOffer);
    }
    
    /**
     * Withdraw an offer (buyer cancels their offer)
     */
    @Transactional
    public OfferResponse withdrawOffer(Long offerId, Long buyerId) {
        Offer offer = offerRepository.findById(offerId)
            .orElseThrow(() -> new RuntimeException("Offer không tồn tại!"));
            
        if (!offer.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("Bạn không có quyền rút lại offer này!");
        }
        
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể rút lại offer đang chờ phản hồi!");
        }
        
        offer.setStatus(OfferStatus.WITHDRAWN);
        offer.setUpdatedAt(LocalDateTime.now());
        
        Offer savedOffer = offerRepository.save(offer);
        return convertToOfferResponse(savedOffer);
    }
    
    /**
     * Get offers for a listing
     */
    public Page<OfferResponse> getOffersForListing(Long listingId, Pageable pageable) {
        Page<Offer> offers = offerRepository.findByListingIdOrderByCreatedAtDesc(listingId, pageable);
        return offers.map(this::convertToOfferResponse);
    }
    
    /**
     * Get offers made by a buyer
     */
    public Page<OfferResponse> getOffersByBuyer(Long buyerId, Pageable pageable) {
        Page<Offer> offers = offerRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable);
        return offers.map(this::convertToOfferResponse);
    }
    
    /**
     * Get offers received by a seller
     */
    public Page<OfferResponse> getOffersBySeller(Long sellerId, Pageable pageable) {
        Page<Offer> offers = offerRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
        return offers.map(this::convertToOfferResponse);
    }
      /**
     * Get active (non-completed) offers for a listing - for browsing
     */
    public Page<OfferResponse> getActiveOffersForListing(Long listingId, Pageable pageable) {
        // Only return offers that are not COMPLETED (i.e., still available for purchase)
        Page<Offer> offers = offerRepository.findByListingIdAndStatusNotOrderByCreatedAtDesc(listingId, OfferStatus.COMPLETED, pageable);
        return offers.map(this::convertToOfferResponse);
    }
    
    /**
     * Check if listing is available for purchase (no completed offers)
     */
    public boolean isListingAvailableForPurchase(Long listingId) {
        return !offerRepository.existsCompletedOfferByListingId(listingId);
    }
    
    /**
     * Get pending offers count for a seller
     */
    public long getPendingOffersCount(Long sellerId) {
        return offerRepository.countPendingOffersBySeller(sellerId);
    }
    
    /**
     * Convert Offer entity to OfferResponse DTO
     */
    private OfferResponse convertToOfferResponse(Offer offer) {
        OfferResponse response = new OfferResponse();
        response.setId(offer.getId());
        response.setListingId(offer.getListingId());
        response.setBuyerId(offer.getBuyerId());
        response.setSellerId(offer.getSellerId());
        response.setOfferAmount(offer.getOfferAmount());
        response.setMessage(offer.getMessage());
        response.setStatus(offer.getStatus());
        response.setCreatedAt(offer.getCreatedAt());
        response.setUpdatedAt(offer.getUpdatedAt());
        response.setExpiresAt(offer.getExpiresAt());
        response.setExpired(offer.isExpired());        response.setCanBeAccepted(offer.canBeAccepted());
        response.setCanBeRejected(offer.canBeRejected());
        response.setCanBeCountered(offer.canBeCountered());
        response.setHasPaidTransaction(offer.getHasPaidTransaction() != null ? offer.getHasPaidTransaction() : false);
        
        // Get listing details
        listingRepository.findById(offer.getListingId()).ifPresent(listing -> {
            response.setListingTitle(listing.getTitle());
            response.setListingPrice(listing.getPrice());
            
            // Get primary image
            List<ListingImage> images = listingImageRepository.findByListingIdAndIsPrimaryTrue(listing.getId());
            if (!images.isEmpty()) {
                response.setListingImageUrl(images.get(0).getImageUrl());
            }
        });
        
        // Get buyer profile
        userProfileRepository.findByUserId(offer.getBuyerId()).ifPresent(profile -> {
            response.setBuyerName(profile.getDisplayName());
            response.setBuyerProfilePic(profile.getProfilePictureUrl());
        });
        
        // Get seller profile
        userProfileRepository.findByUserId(offer.getSellerId()).ifPresent(profile -> {
            response.setSellerName(profile.getDisplayName());
            response.setSellerProfilePic(profile.getProfilePictureUrl());
        });
        
        return response;
    }
    
    /**
     * Update expired offers (to be called by scheduled task)
     */
    @Transactional
    public void updateExpiredOffers() {
        List<Offer> expiredOffers = offerRepository.findExpiredOffers(LocalDateTime.now());
        for (Offer offer : expiredOffers) {
            offer.setStatus(OfferStatus.EXPIRED);
            offerRepository.save(offer);
        }
    }
}
