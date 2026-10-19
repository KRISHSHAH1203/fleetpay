package com.krishshah.fleetpay.service;

import com.krishshah.fleetpay.domain.Card;
import com.krishshah.fleetpay.domain.Transaction;
import com.krishshah.fleetpay.domain.TransactionStatus;
import com.krishshah.fleetpay.dto.PostTransactionRequest;
import com.krishshah.fleetpay.dto.TransactionResponse;
import com.krishshah.fleetpay.exception.NotFoundException;
import com.krishshah.fleetpay.repository.CardRepository;
import com.krishshah.fleetpay.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final CardRepository cards;
    private final TransactionRepository transactions;
    private final List<SpendRule> rules;
    private final Clock clock;

    public TransactionService(CardRepository cards, TransactionRepository transactions,
                              List<SpendRule> rules, Clock clock) {
        this.cards = cards;
        this.transactions = transactions;
        this.rules = rules;
        this.clock = clock;
    }

    /** Result tells the controller whether this was a replay of an earlier request. */
    public record PostResult(TransactionResponse transaction, boolean replayed) { }

    /**
     * Posts a payment exactly once per idempotency key.
     * 1. If the key was seen before, return the original result (no second charge).
     * 2. Lock the card row so concurrent posts are serialized.
     * 3. Run every SpendRule; the first failure declines the transaction.
     */
    @Transactional
    public PostResult post(String idempotencyKey, PostTransactionRequest req) {
        Optional<Transaction> existing = transactions.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new PostResult(TransactionResponse.from(existing.get()), true);
        }

        Card card = cards.findByIdForUpdate(req.cardId())
                .orElseThrow(() -> new NotFoundException("Card " + req.cardId() + " not found"));

        Instant startOfDay = LocalDate.now(clock).atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal spent = transactions.sumAmountSince(card.getId(), TransactionStatus.APPROVED, startOfDay);
        BigDecimal spentToday = spent == null ? BigDecimal.ZERO : spent;

        Optional<String> declineReason = rules.stream()
                .map(r -> r.check(card, req.amount(), spentToday, req.merchant()))
                .flatMap(Optional::stream)
                .findFirst();

        Transaction txn = declineReason
                .map(reason -> Transaction.declined(card, idempotencyKey, req.amount(), req.merchant(), reason))
                .orElseGet(() -> Transaction.approved(card, idempotencyKey, req.amount(), req.merchant()));

        return new PostResult(TransactionResponse.from(transactions.save(txn)), false);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> history(Long cardId, Pageable pageable) {
        if (!cards.existsById(cardId)) {
            throw new NotFoundException("Card " + cardId + " not found");
        }
        // map inside the transaction so lazy fields are safe to read
        return transactions.findByCardIdOrderByCreatedAtDesc(cardId, pageable).map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TransactionRepository.SpendSummary> spendReport(Instant from, Instant to) {
        return transactions.spendReport(from, to);
    }
}
