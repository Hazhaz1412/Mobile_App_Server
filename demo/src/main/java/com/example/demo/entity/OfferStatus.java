package com.example.demo.entity;

public enum OfferStatus {
    PENDING,    // Offer made, waiting for response
    ACCEPTED,   // Seller accepted the offer
    REJECTED,   // Seller rejected the offer
    COUNTERED,  // Seller made a counter offer
    EXPIRED,    // Offer expired without response
    WITHDRAWN,  // Buyer withdrew the offer
    COMPLETED   // Payment completed, offer locked
}
