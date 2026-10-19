package com.krishshah.fleetpay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end through HTTP, JPA and an in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private long cardId;

    @BeforeEach
    void createCard() throws Exception {
        String last4 = String.valueOf(1000 + (int) (Math.random() * 8999));
        String body = mvc.perform(post("/api/cards").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverName\":\"Ravi\",\"last4\":\"" + last4 + "\",\"dailyLimit\":300.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        cardId = json.readTree(body).get("id").asLong();
    }

    private String txn(String amount) {
        return "{\"cardId\":" + cardId + ",\"amount\":" + amount + ",\"merchant\":\"Shell Fuel\"}";
    }

    @Test
    void approvesTransactionWithinLimit() throws Exception {
        mvc.perform(post("/api/transactions").header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(txn("120.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void replayingTheSameKeyDoesNotChargeTwice() throws Exception {
        String key = UUID.randomUUID().toString();
        String first = mvc.perform(post("/api/transactions").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(txn("200.00")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String second = mvc.perform(post("/api/transactions").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(txn("200.00")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode a = json.readTree(first), b = json.readTree(second);
        org.assertj.core.api.Assertions.assertThat(b.get("id")).isEqualTo(a.get("id"));

        mvc.perform(get("/api/cards/" + cardId + "/transactions"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void declinesWhenDailyLimitWouldBeExceeded() throws Exception {
        mvc.perform(post("/api/transactions").header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(txn("250.00")))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(post("/api/transactions").header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(txn("60.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").exists());
    }

    @Test
    void declinesOnInactiveCard() throws Exception {
        mvc.perform(post("/api/cards/" + cardId + "/deactivate")).andExpect(status().isOk());
        mvc.perform(post("/api/transactions").header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(txn("10.00")))
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("Card is inactive"));
    }

    @Test
    void rejectsInvalidInputAndMissingKey() throws Exception {
        mvc.perform(post("/api/transactions").header("Idempotency-Key", "k1")
                        .contentType(MediaType.APPLICATION_JSON).content(txn("-5")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON).content(txn("5")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCardReturns404() throws Exception {
        mvc.perform(get("/api/cards/999999/transactions")).andExpect(status().isNotFound());
    }
}
