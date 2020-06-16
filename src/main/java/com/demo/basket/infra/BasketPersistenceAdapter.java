package com.demo.basket.infra;

import com.demo.basket.domain.model.Basket;
import com.demo.basket.domain.spi.BasketPersistencePort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class BasketPersistenceAdapter implements BasketPersistencePort {
    @Override
    public Optional<Basket> find(String customerId) {
        return Optional.empty();
    }

    @Override
    public Basket save(Basket basket) {
        return null;
    }

    @Override
    public void delete(String customerId) {

    }
}
