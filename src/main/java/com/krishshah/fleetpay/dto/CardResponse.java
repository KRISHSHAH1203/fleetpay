package com.krishshah.fleetpay.dto;

import com.krishshah.fleetpay.domain.Card;
import java.math.BigDecimal;

public record CardResponse(Long id, String driverName, String last4, BigDecimal dailyLimit, boolean active) {
    public static CardResponse from(Card c) {
        return new CardResponse(c.getId(), c.getDriverName(), c.getLast4(), c.getDailyLimit(), c.isActive());
    }
}
