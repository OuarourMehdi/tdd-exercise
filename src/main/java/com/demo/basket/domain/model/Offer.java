package com.demo.basket.domain.model;

import lombok.Builder;
import lombok.Data;
import org.joda.money.BigMoney;

@Data
@Builder
public class Offer {
    private String gtin;
    private long quantity;
    private BigMoney unitPrice;
}
