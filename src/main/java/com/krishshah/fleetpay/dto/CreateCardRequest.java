package com.krishshah.fleetpay.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateCardRequest(
        @NotBlank @Size(max = 100) String driverName,
        @NotBlank @Pattern(regexp = "\\d{4}", message = "last4 must be 4 digits") String last4,
        @NotNull @DecimalMin(value = "0.01") BigDecimal dailyLimit) { }
