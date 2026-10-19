package com.krishshah.fleetpay.repository;

import com.krishshah.fleetpay.domain.Transaction;
import com.krishshah.fleetpay.domain.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /** Uses idx_txn_card_created: approved spend on a card since a point in time (null if none). */
    @Query("""
           select sum(t.amount) from Transaction t
           where t.card.id = :cardId and t.status = :status and t.createdAt >= :since
           """)
    BigDecimal sumAmountSince(@Param("cardId") Long cardId,
                              @Param("status") TransactionStatus status,
                              @Param("since") Instant since);

    Page<Transaction> findByCardIdOrderByCreatedAtDesc(Long cardId, Pageable pageable);

    /** Spend report: one row per card with total approved amount and count. */
    @Query("""
           select t.card.id as cardId, t.card.driverName as driverName,
                  sum(t.amount) as totalSpend, count(t) as transactionCount
           from Transaction t
           where t.status = com.krishshah.fleetpay.domain.TransactionStatus.APPROVED
             and t.createdAt between :from and :to
           group by t.card.id, t.card.driverName
           order by sum(t.amount) desc
           """)
    List<SpendSummary> spendReport(@Param("from") Instant from, @Param("to") Instant to);

    interface SpendSummary {
        Long getCardId();
        String getDriverName();
        BigDecimal getTotalSpend();
        Long getTransactionCount();
    }
}
