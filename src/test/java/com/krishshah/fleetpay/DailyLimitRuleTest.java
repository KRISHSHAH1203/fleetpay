package com.krishshah.fleetpay;

import com.krishshah.fleetpay.domain.Card;
import com.krishshah.fleetpay.service.DailyLimitRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test: no Spring context, runs in milliseconds. */
class DailyLimitRuleTest {

    private final DailyLimitRule rule = new DailyLimitRule();
    private final Card card = new Card("Asha", "1234", new BigDecimal("500.00"));

    @Test
    void allowsSpendUpToTheLimit() {
        assertThat(rule.check(card, new BigDecimal("100.00"), new BigDecimal("400.00"), "Fuel")).isEmpty();
    }

    @Test
    void declinesSpendOverTheLimit() {
        assertThat(rule.check(card, new BigDecimal("100.01"), new BigDecimal("400.00"), "Fuel"))
                .hasValueSatisfying(reason -> assertThat(reason).contains("Daily limit"));
    }
}
