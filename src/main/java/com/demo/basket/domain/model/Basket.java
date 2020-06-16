package com.demo.basket.domain.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;

@Data
@Builder(toBuilder = true)
public class Basket {
    private String customerId;
    private String facilityServiceId;
    private ArrayList<Offer> offers;
}
