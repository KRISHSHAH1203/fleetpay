package com.krishshah.fleetpay.service;

import com.krishshah.fleetpay.domain.Card;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Order(2)
public class DailyLimitRule implements SpendRule {
    @Override
    public Optional<String> check(Card card, BigDecimal amount, BigDecimal spentToday, String merchant) {
        BigDecimal after = spentToday.add(amount);
        if (after.compareTo(card.getDailyLimit()) > 0) {
            return Optional.of("Daily limit of " + card.getDailyLimit() + " exceeded");
        }
        return Optional.empty();
    }
}
