package com.example.demo.service.payment;

import com.example.demo.entity.Offer;
import com.example.demo.entity.OfferStatus;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Transaction;
import com.example.demo.repository.OfferRepository;
import com.example.demo.repository.TransactionRepository;
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
    
    /**
     * Handle payment completion - lock offers and create transaction for rating
     */
    @Transactional
    public void handlePaymentCompletion(Payment payment) {
        try {
            log.info("Handling payment completion for payment ID: {}", payment.getId());
            
            // If this payment is for an offer, lock the offer
            if (payment.getOfferId() != null) {
                Optional<Offer> offerOpt = offerRepository.findById(payment.getOfferId());
                if (offerOpt.isPresent()) {
                    Offer offer = offerOpt.get();
                    
                    // Update offer status to COMPLETED to prevent further purchases
                    offer.setStatus(OfferStatus.COMPLETED);
                    offer.setUpdatedAt(LocalDateTime.now());
                    offerRepository.save(offer);
                    
                    log.info("Offer {} locked as COMPLETED after payment", offer.getId());
                    
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
            .map(offer -> offer.getStatus() != OfferStatus.COMPLETED)
            .orElse(false);
    }
}
