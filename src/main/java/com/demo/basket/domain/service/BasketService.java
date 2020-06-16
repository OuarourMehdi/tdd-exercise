package com.demo.basket.domain.service;

import com.demo.basket.domain.spi.BasketPersistencePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BasketService {

    @Autowired
    private BasketPersistencePort persistencePort;

}
