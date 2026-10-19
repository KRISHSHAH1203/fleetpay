package com.krishshah.fleetpay.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single card transaction. The idempotency key is unique, so posting the same
 * payment event twice can never create two rows.
 */
@Entity
@Table(name = "transactions",
       indexes = {
           @Index(name = "idx_txn_card_created", columnList = "card_id, created_at"),
           @Index(name = "idx_txn_status", columnList = "status")
       },
       uniqueConstraints = @UniqueConstraint(name = "uk_txn_idempotency", columnNames = "idempotency_key"))
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 80)
    private String merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status;

    @Column(length = 120)
    private String declineReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Transaction() { }

    private Transaction(Card card, String idempotencyKey, BigDecimal amount, String merchant,
                        TransactionStatus status, String declineReason) {
        this.card = card;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.merchant = merchant;
        this.status = status;
        this.declineReason = declineReason;
    }

    public static Transaction approved(Card card, String key, BigDecimal amount, String merchant) {
        return new Transaction(card, key, amount, merchant, TransactionStatus.APPROVED, null);
    }

    public static Transaction declined(Card card, String key, BigDecimal amount, String merchant, String reason) {
        return new Transaction(card, key, amount, merchant, TransactionStatus.DECLINED, reason);
    }

    public Long getId() { return id; }
    public Card getCard() { return card; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getAmount() { return amount; }
    public String getMerchant() { return merchant; }
    public TransactionStatus getStatus() { return status; }
    public String getDeclineReason() { return declineReason; }
    public Instant getCreatedAt() { return createdAt; }
}
