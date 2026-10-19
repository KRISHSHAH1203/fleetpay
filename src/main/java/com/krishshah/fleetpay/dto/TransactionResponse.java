package com.krishshah.fleetpay.dto;

import com.krishshah.fleetpay.domain.Transaction;
import com.krishshah.fleetpay.domain.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(Long id, Long cardId, BigDecimal amount, String merchant,
                                  TransactionStatus status, String declineReason, Instant createdAt) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getCard().getId(), t.getAmount(), t.getMerchant(),
                t.getStatus(), t.getDeclineReason(), t.getCreatedAt());
    }
}
