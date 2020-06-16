package com.demo.basket.domain.service;

import com.demo.basket.domain.exception.DomainException;
import com.demo.basket.domain.model.Basket;
import com.demo.basket.domain.model.KeepOption;
import com.demo.basket.domain.spi.BasketPersistencePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class BasketService {

    @Autowired
    private BasketPersistencePort persistencePort;

    public Basket mergeBaskets(String customerId, String sessionId, KeepOption keep) {
        Assert.hasText(customerId, "customerId must be not empty");
        Assert.hasText(sessionId, "customerId must be not empty");
        Assert.notNull(keep, "keepOption must be not null");

        Basket mergedBasket = null;
        switch (keep) {
            case AUTHENTICATED:
                mergedBasket = getBasket(customerId);
                break;
            case ANONYMOUS:
                mergedBasket = getBasket(sessionId).toBuilder().customerId(customerId).build();
                persistencePort.save(mergedBasket);
        }

        persistencePort.delete(sessionId);
        return mergedBasket;
    }

    private Basket getBasket(String id) {
        return persistencePort.find(id).orElseThrow(DomainException::new);
    }
}
