package com.example.demo.entity;

public enum TransactionStatus {
    PENDING,      // Transaction initiated but not completed
    COMPLETED,    // Transaction successfully completed
    CANCELLED,    // Transaction was cancelled
    DISPUTED      // Transaction is under dispute
}
