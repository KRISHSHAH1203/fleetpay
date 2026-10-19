package com.krishshah.fleetpay.service;

import com.krishshah.fleetpay.domain.Card;
import com.krishshah.fleetpay.dto.CreateCardRequest;
import com.krishshah.fleetpay.exception.ConflictException;
import com.krishshah.fleetpay.exception.NotFoundException;
import com.krishshah.fleetpay.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

    private final CardRepository cards;

    public CardService(CardRepository cards) {
        this.cards = cards;
    }

    @Transactional
    public Card create(CreateCardRequest req) {
        if (cards.existsByLast4(req.last4())) {
            throw new ConflictException("A card ending in " + req.last4() + " already exists");
        }
        return cards.save(new Card(req.driverName(), req.last4(), req.dailyLimit()));
    }

    @Transactional(readOnly = true)
    public Card get(Long id) {
        return cards.findById(id).orElseThrow(() -> new NotFoundException("Card " + id + " not found"));
    }

    @Transactional
    public Card deactivate(Long id) {
        Card card = get(id);
        card.deactivate();
        return card;
    }
}
