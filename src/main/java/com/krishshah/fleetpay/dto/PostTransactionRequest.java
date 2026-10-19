package com.krishshah.fleetpay.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PostTransactionRequest(
        @NotNull Long cardId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Size(max = 80) String merchant) { }
