package com.krishshah.fleetpay.service;

import com.krishshah.fleetpay.domain.Card;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * One check a transaction must pass. Returns a decline reason, or empty if it passes.
 * New rules (merchant blocklist, fuel-only cards) plug in as new beans with no change
 * to TransactionService: strategy pattern.
 */
public interface SpendRule {
    Optional<String> check(Card card, BigDecimal amount, BigDecimal spentToday, String merchant);
}
