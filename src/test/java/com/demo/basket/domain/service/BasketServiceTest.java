package com.demo.basket.domain.service;

import com.demo.basket.domain.exception.DomainException;
import com.demo.basket.domain.model.Offer;
import com.demo.basket.domain.model.Basket;
import com.demo.basket.domain.model.KeepOption;
import com.demo.basket.domain.spi.BasketPersistencePort;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketServiceTest {

    @Mock
    BasketPersistencePort persistencePort;

    @InjectMocks
    BasketService basketService;

    @Test
    void mergeBasketsShouldThrowIAEWhenCustomerIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> basketService.mergeBaskets(null, null, KeepOption.AUTHENTICATED));
    }

    @Test
    void mergeBasketsShouldThrowIAEWhenCustomerIdIsEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> basketService.mergeBaskets("", null, KeepOption.AUTHENTICATED));
    }

    static Stream<Arguments> invalidSessionIds() {
        return Stream.of(
                Arguments.of("Session id is null", null),
                Arguments.of("Session id is empty", "")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidSessionIds")
    void mergeBasketsShouldThrowIAEWhenSessionIdIsInvalid(String scenarioName, String sessionId) {
        assertThrows(IllegalArgumentException.class,
                () -> basketService.mergeBaskets("customerId", sessionId, KeepOption.AUTHENTICATED));
    }

    @Test
    void mergeBasketsShouldThrowIAEWhenKeepOptionIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> basketService.mergeBaskets("customerId", "sessionId", null));
    }

    @Test
    void mergeBasketsShouldThrowDEWhenAuthenticatedBasketIsNotFound() {
        String customerId = "customerId";
        String sessionId = "sessionId";
        when(persistencePort.find(customerId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> basketService.mergeBaskets(customerId, sessionId, KeepOption.AUTHENTICATED));
    }


    @Test
    void mergeBasketsShouldReturnAuthenticatedBasketWhenKeepOptionIsEqualToAUTHENTICATED() {
        // Given
        String customerId = "customerId";
        String sessionId = "sessionId";
        Basket authenticatedBasket = createBasket(customerId, "AUTHENTICATED-FSID","123456", 2);
        when(persistencePort.find(customerId)).thenReturn(Optional.of(authenticatedBasket));

        // When
        Basket mergedBasket = basketService.mergeBaskets(customerId, sessionId, KeepOption.AUTHENTICATED);

        // Then
        Basket expectedBasket = createBasket(customerId, "AUTHENTICATED-FSID","123456", 2);
        Assertions.assertThat(mergedBasket).isEqualTo(expectedBasket);
    }

    @Test
    void mergeBasketsShouldDeleteAnonymousBasketWhenKeepOptionIsEqualToAUTHENTICATED() {
        // Given
        String customerId = "customerId";
        String sessionId = "sessionId";
        Basket authenticatedBasket = createBasket(customerId, "AUTHENTICATED-FSID", "123456", 2);
        when(persistencePort.find(customerId)).thenReturn(Optional.of(authenticatedBasket));

        // When
        basketService.mergeBaskets(customerId, sessionId, KeepOption.AUTHENTICATED);

        // Then
        Mockito.verify(persistencePort).delete(sessionId);
    }

    @Test
    void mergeBasketsShouldThrowDEWhenKeepOptionIsEqualToAUTHENTICATEDAndAnonymousBasketIsNotFound() {
        String customerId = "customerId";
        String sessionId = "sessionId";

        when(persistencePort.find(sessionId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> basketService.mergeBaskets(customerId, sessionId, KeepOption.ANONYMOUS));
    }

    @Test
    void mergeBasketsShouldReplaceAuthenticatedBasketContentByAnonymousOneAndReturnResultWhenKeepOptionIsEqualToANONYMOUS() {
        // Given
        String customerId = "customerId";
        String sessionId = "sessionId";
        when(persistencePort.find(sessionId))
                .thenReturn(Optional.of(createBasket(sessionId, "ANONYMOUS-FSID", "563214", 3)));

        // When
        Basket mergedBasket = basketService.mergeBaskets(customerId, sessionId, KeepOption.ANONYMOUS);

        // Then
        Basket expectedBasket = createBasket(customerId, "ANONYMOUS-FSID", "563214", 3);
        Assertions.assertThat(mergedBasket).isEqualTo(expectedBasket);
    }

    @Test
    void mergeBasketsShouldReplaceAuthenticatedBasketContentByAnonymousOneAndSaveAuthenticatedBasketResultWhenKeepOptionIsEqualToANONYMOUS() {
        // Given
        String customerId = "customerId";
        String sessionId = "sessionId";
        when(persistencePort.find(sessionId))
                .thenReturn(Optional.of(createBasket(sessionId, "ANONYMOUS-FSID", "563214", 3)));

        // When
        basketService.mergeBaskets(customerId, sessionId, KeepOption.ANONYMOUS);

        // Then
        Basket savedBasket = createBasket(customerId, "ANONYMOUS-FSID", "563214", 3);
        verify(persistencePort).save(refEq(savedBasket));
    }

    @Test
    void mergeBasketsShouldDeleteAnonymousBasketWhenKeepOptionIsEqualToANONYMOUS() {
        // Given
        String customerId = "customerId";
        String sessionId = "sessionId";
        when(persistencePort.find(sessionId))
                .thenReturn(Optional.of(createBasket(sessionId, "ANONYMOUS-FSID", "563214", 3)));

        // When
        basketService.mergeBaskets(customerId, sessionId, KeepOption.ANONYMOUS);

        // Then
        Mockito.verify(persistencePort).delete(sessionId);
    }

    private Basket createBasket(String customerId, String facilityServiceId, String offerGtin, int offerQuantity) {
        return Basket.builder()
                .customerId(customerId)
                .facilityServiceId(facilityServiceId)
                .offers(Collections.singletonList(Offer.builder().gtin(offerGtin).quantity(offerQuantity).build()))
                .build();
    }

}