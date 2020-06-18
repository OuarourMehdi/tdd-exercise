package com.demo.basket.domain.service;

import com.demo.basket.domain.exception.DomainException;
import com.demo.basket.domain.model.Basket;
import com.demo.basket.domain.model.KeepOption;
import com.demo.basket.domain.model.Offer;
import com.demo.basket.domain.spi.BasketPersistencePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BasketService {

    @Autowired
    private BasketPersistencePort persistencePort;

    public Basket mergeBaskets(String customerId, String sessionId, KeepOption keep, boolean mergeOffers) {
        Assert.hasText(customerId, "customerId must be not empty");
        Assert.hasText(sessionId, "customerId must be not empty");
        Assert.notNull(keep, "keepOption must be not null");

        Basket mergedBasket = null;
        switch (keep) {
            case AUTHENTICATED:
                mergedBasket = getBasket(customerId);
                if(mergeOffers) {
                    Basket anonymousBasket = getBasket(sessionId);
                    addNonExistentOfferToBasket(anonymousBasket.getOffers()).apply(mergedBasket);
                }
                break;
            case ANONYMOUS:
                mergedBasket = getBasket(sessionId).toBuilder().customerId(customerId).build();
                if(mergeOffers) {
                    Basket authenticatedBasket = getBasket(customerId);
                    addNonExistentOfferToBasket(authenticatedBasket.getOffers()).apply(mergedBasket);
                }
                

                persistencePort.save(mergedBasket);
        }

        persistencePort.delete(sessionId);
        return mergedBasket;
    }

    private static Function<Basket, Basket> addNonExistentOfferToBasket(List<Offer> offersToAdd) {
        return basket -> {
            Set<String> existentOffersGtins = Optional.ofNullable(basket.getOffers())
                    .map(existentOffers -> existentOffers.stream()
                            .map(Offer::getGtin)
                            .collect(Collectors.toSet()))
                    .orElse(new HashSet<>());

            offersToAdd.forEach(offerToAdd -> {
                if(!existentOffersGtins.contains(offerToAdd.getGtin())) {
                    basket.getOffers().add(offerToAdd);
                }
            });
            return basket;
        };
    }

    private Basket getBasket(String id) {
        return persistencePort.find(id).orElseThrow(DomainException::new);
    }
}
