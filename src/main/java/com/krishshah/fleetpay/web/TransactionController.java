package com.krishshah.fleetpay.web;

import com.krishshah.fleetpay.dto.PostTransactionRequest;
import com.krishshah.fleetpay.dto.TransactionResponse;
import com.krishshah.fleetpay.repository.TransactionRepository;
import com.krishshah.fleetpay.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    /** 201 on first post, 200 with the original body when the same Idempotency-Key is replayed. */
    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> post(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                    @Valid @RequestBody PostTransactionRequest req) {
        TransactionService.PostResult result = service.post(idempotencyKey, req);
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.transaction());
    }

    @GetMapping("/cards/{cardId}/transactions")
    public Page<TransactionResponse> history(@PathVariable Long cardId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return service.history(cardId, PageRequest.of(page, safeSize));
    }

    @GetMapping("/reports/spend")
    public List<TransactionRepository.SpendSummary> spend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return service.spendReport(from, to);
    }
}
