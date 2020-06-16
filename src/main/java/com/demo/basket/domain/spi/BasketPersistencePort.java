package com.demo.basket.domain.spi;

import com.demo.basket.domain.model.Basket;

import java.util.Optional;

public interface BasketPersistencePort {
    Optional<Basket> find(String customerId);
    Basket save(Basket basket);
    void delete(String customerId);
}
