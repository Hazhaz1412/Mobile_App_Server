package com.example.demo.service.payment;

import com.example.demo.entity.Offer;
import com.example.demo.entity.OfferStatus;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.Listing;
import com.example.demo.entity.ListingStatus;
import com.example.demo.repository.OfferRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletionService {
      private final OfferRepository offerRepository;
    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    
    /**
     * Handle payment completion - lock offers and create transaction for rating
     */    @Transactional
    public void handlePaymentCompletion(Payment payment) {
        try {
            log.info("=== PAYMENT COMPLETION START === Payment ID: {}", payment.getId());
            log.info("Payment details - Offer ID: {}, Buyer ID: {}, Amount: {}, Status: {}", 
                    payment.getOfferId(), payment.getBuyerId(), payment.getAmount(), payment.getStatus());
            
            // 🔥 CRITICAL DEBUG: Check if offerId is null
            if (payment.getOfferId() == null) {
                log.error("❌ COMPLETION FAILED: Payment {} has NULL offerId! Cannot complete offer.", payment.getId());
                log.error("💡 SOLUTION: Need to manually set offerId for this payment first!");
                return; // Skip completion
            }
            
            // If this payment is for an offer, lock the offer
            if (payment.getOfferId() != null) {
                log.info("Processing offer completion for offer ID: {}", payment.getOfferId());
                Optional<Offer> offerOpt = offerRepository.findById(payment.getOfferId());
                if (offerOpt.isPresent()) {
                    Offer offer = offerOpt.get();
                    log.info("Found offer {} with current status: {}", offer.getId(), offer.getStatus());
                    
                    // Update offer status to COMPLETED to prevent further purchases
                    OfferStatus oldStatus = offer.getStatus();
                    offer.setStatus(OfferStatus.COMPLETED);
                    offer.setHasPaidTransaction(true);
                    offer.setUpdatedAt(LocalDateTime.now());
                    offerRepository.save(offer);
                    
                    log.info("🚀 OFFER UPDATED: {} status changed from {} to COMPLETED, hasPaidTransaction = true", 
                            offer.getId(), oldStatus);
                    
                    // CRITICAL: Mark listing as SOLD to prevent new offers
                    Optional<Listing> listingOpt = listingRepository.findById(offer.getListingId());
                    if (listingOpt.isPresent()) {
                        Listing listing = listingOpt.get();
                        listing.setStatus(ListingStatus.SOLD);
                        listing.setUpdatedAt(LocalDateTime.now());
                        listingRepository.save(listing);
                        log.info("📋 LISTING UPDATED: {} marked as SOLD after payment", listing.getId());
                    }
                    
                    // Create transaction record from the offer
                    Transaction transaction = new Transaction(
                        offer.getListingId(),
                        offer.getBuyerId(),
                        offer.getSellerId(),
                        payment.getAmount(),
                        offer.getId()
                    );
                    transaction.markAsCompleted();
                    Transaction savedTransaction = transactionRepository.save(transaction);
                    
                    log.info("Transaction {} created for completed offer payment", savedTransaction.getId());
                    
                    // TODO: Send notification to both buyer and seller to rate each other
                    log.info("RATING_NOTIFICATION: Transaction {} completed. Buyer {} and Seller {} can now rate each other", 
                        savedTransaction.getId(), offer.getBuyerId(), offer.getSellerId());
                }
            } else {
                // Direct purchase - create transaction
                Transaction transaction = new Transaction(
                    payment.getListingId(),
                    payment.getBuyerId(),
                    payment.getSellerId(),
                    payment.getAmount()
                );
                transaction.markAsCompleted();
                Transaction savedTransaction = transactionRepository.save(transaction);
                
                log.info("Transaction {} created for direct purchase payment", savedTransaction.getId());
                
                // TODO: Send notification to both buyer and seller to rate each other  
                log.info("RATING_NOTIFICATION: Transaction {} completed. Buyer {} and Seller {} can now rate each other", 
                    savedTransaction.getId(), payment.getBuyerId(), payment.getSellerId());
            }
            
        } catch (Exception e) {
            log.error("Error handling payment completion for payment ID: {}", payment.getId(), e);
            throw new RuntimeException("Failed to handle payment completion: " + e.getMessage());
        }
    }
      /**
     * Check if offer can be purchased (not already completed)
     */
    public boolean canPurchaseOffer(Long offerId) {
        return offerRepository.findById(offerId)
            .map(offer -> {
                boolean canPurchase = offer.getStatus() != OfferStatus.COMPLETED;
                log.info("OFFER_STATUS_CHECK: Offer {} has status {}, canPurchase: {}", 
                        offerId, offer.getStatus(), canPurchase);
                return canPurchase;
            })
            .orElseThrow(() -> new RuntimeException("Offer không tồn tại: " + offerId));
    }
}
