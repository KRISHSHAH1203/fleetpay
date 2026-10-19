package com.krishshah.fleetpay.web;

import com.krishshah.fleetpay.dto.CardResponse;
import com.krishshah.fleetpay.dto.CreateCardRequest;
import com.krishshah.fleetpay.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@Valid @RequestBody CreateCardRequest req) {
        return CardResponse.from(cardService.create(req));
    }

    @GetMapping("/{id}")
    public CardResponse get(@PathVariable Long id) {
        return CardResponse.from(cardService.get(id));
    }

    @PostMapping("/{id}/deactivate")
    public CardResponse deactivate(@PathVariable Long id) {
        return CardResponse.from(cardService.deactivate(id));
    }
}
