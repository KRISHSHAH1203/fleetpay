package com.krishshah.fleetpay.service;

import com.krishshah.fleetpay.domain.Card;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Order(1)
public class ActiveCardRule implements SpendRule {
    @Override
    public Optional<String> check(Card card, BigDecimal amount, BigDecimal spentToday, String merchant) {
        return card.isActive() ? Optional.empty() : Optional.of("Card is inactive");
    }
}
