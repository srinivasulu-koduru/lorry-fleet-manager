package com.lorryowner.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "finance_transactions", indexes = {
        @Index(name = "idx_ft_owner_date", columnList = "owner_id, transaction_date"),
        @Index(name = "idx_ft_owner_type_date", columnList = "owner_id, transaction_type, transaction_date"),
        @Index(name = "idx_ft_lorry_date", columnList = "lorry_id, transaction_date")
})
public class FinanceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lorry_id")
    private Lorry lorry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType; // "INCOME" or "EXPENSE"

    @Column(name = "category", nullable = false, length = 50)
    private String category; // "FREIGHT", "ADVANCE", "DIESEL", "TOLL", "DRIVER_SALARY", "MAINTENANCE", "OTHER"

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus = "RECEIVED"; // "RECEIVED", "PENDING"

    @Column(name = "payment_mode", length = 50)
    private String paymentMode; // "BANK_TRANSFER", "CASH", "UPI", "CHEQUE", "FUEL_CARD"

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FinanceTransaction() {}

    public FinanceTransaction(
            User owner,
            Lorry lorry,
            Trip trip,
            String transactionType,
            String category,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            String paymentStatus,
            String paymentMode,
            String referenceNumber
    ) {
        this.owner = owner;
        this.lorry = lorry;
        this.trip = trip;
        this.transactionType = transactionType;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.paymentStatus = paymentStatus != null ? paymentStatus : "RECEIVED";
        this.paymentMode = paymentMode;
        this.referenceNumber = referenceNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Lorry getLorry() {
        return lorry;
    }

    public void setLorry(Lorry lorry) {
        this.lorry = lorry;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
