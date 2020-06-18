package com.demo.basket.domain.service;

import com.demo.basket.domain.exception.DomainException;
import com.demo.basket.domain.model.Basket;
import com.demo.basket.domain.model.KeepOption;
import com.demo.basket.domain.model.Offer;
import com.demo.basket.domain.spi.BasketPersistencePort;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
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

    @Nested
    @DisplayName("Merge AUTHENTICATED et ANONYMOUS baskets during connection tests")
    class MergeBasketTest {

        @Test
        void shouldThrowIAEWhenCustomerIdIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> basketService.mergeBaskets(null, null, KeepOption.AUTHENTICATED, false));
        }

        @Test
        void shouldThrowIAEWhenCustomerIdIsEmpty() {
            assertThrows(IllegalArgumentException.class,
                    () -> basketService.mergeBaskets("", null, KeepOption.AUTHENTICATED, false));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.demo.basket.domain.service.BasketServiceTest#invalidSessionIds")
        void shouldThrowIAEWhenSessionIdIsInvalid(String scenarioName, String sessionId) {
            assertThrows(IllegalArgumentException.class,
                    () -> basketService.mergeBaskets("customerId", sessionId, KeepOption.AUTHENTICATED, false));
        }

        @Test
        void shouldThrowIAEWhenKeepOptionIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> basketService.mergeBaskets("customerId", "sessionId", null, false));
        }

        @Test
        void shouldThrowDEWhenAuthenticatedBasketIsNotFound() {
            String customerId = "customerId";
            String sessionId = "sessionId";
            when(persistencePort.find(customerId)).thenReturn(Optional.empty());

            assertThrows(DomainException.class, () -> basketService.mergeBaskets(customerId, sessionId, KeepOption.AUTHENTICATED, false));
        }

        @Test
        void shouldReturnAuthenticatedBasketWhenKeepOptionIsEqualToAUTHENTICATED() {
            // Given
            String customerId = "customerId";
            String sessionId = "sessionId";
            Basket authenticatedBasket = createBasket(customerId, "AUTHENTICATED-FSID",createOffer("123456", 2));
            when(persistencePort.find(customerId)).thenReturn(Optional.of(authenticatedBasket));

            // When
            Basket mergedBasket = basketService.mergeBaskets(customerId, sessionId, KeepOption.AUTHENTICATED, false);

            // Then
            Basket expectedBasket = createBasket(customerId, "AUTHENTICATED-FSID",createOffer("123456", 2));
            Assertions.assertThat(mergedBasket).isEqualTo(expectedBasket);
        }

        @Test
        void shouldDeleteAnonymousBasketWhenKeepOptionIsEqualToAUTHENTICATED() {
            // Given
            String customerId = "customerId";
            String sessionId = "sessionId";
            Basket authenticatedBasket = createBasket(customerId, "AUTHENTICATED-FSID", createOffer("123456", 2));
            when(persistencePort.find(customerId)).thenReturn(Optional.of(authenticatedBasket));

            // When
            basketService.mergeBaskets(customerId, sessionId, KeepOption.AUTHENTICATED, false);

            // Then
            Mockito.verify(persistencePort).delete(sessionId);
        }

        @Test
        void shouldThrowDEWhenKeepOptionIsEqualToAUTHENTICATEDAndAnonymousBasketIsNotFound() {
            String customerId = "customerId";
            String sessionId = "sessionId";

            when(persistencePort.find(sessionId)).thenReturn(Optional.empty());

            assertThrows(DomainException.class, () -> basketService.mergeBaskets(customerId, sessionId, KeepOption.ANONYMOUS, false));
        }

        @Test
        void shouldReplaceAuthenticatedBasketContentByAnonymousOneAndReturnResultWhenKeepOptionIsEqualToANONYMOUS() {
            // Given
            String customerId = "customerId";
            String sessionId = "sessionId";
            when(persistencePort.find(sessionId))
                    .thenReturn(Optional.of(createBasket(sessionId, "ANONYMOUS-FSID", createOffer("563214", 3))));

            // When
            Basket mergedBasket = basketService.mergeBaskets(customerId, sessionId, KeepOption.ANONYMOUS, false);

            // Then
            Basket expectedBasket = createBasket(customerId, "ANONYMOUS-FSID", createOffer("563214", 3));
            Assertions.assertThat(mergedBasket).isEqualTo(expectedBasket);
        }

        @Test
        void shouldReplaceAuthenticatedBasketContentByAnonymousOneAndSaveAuthenticatedBasketResultWhenKeepOptionIsEqualToANONYMOUS() {
            // Given
            String customerId = "customerId";
            String sessionId = "sessionId";
            when(persistencePort.find(sessionId))
                    .thenReturn(Optional.of(createBasket(sessionId, "ANONYMOUS-FSID", createOffer("563214", 3))));

            // When
            basketService.mergeBaskets(customerId, sessionId, KeepOption.ANONYMOUS, false);

            // Then
            Basket savedBasket = createBasket(customerId, "ANONYMOUS-FSID", createOffer("563214", 3));
            verify(persistencePort).save(refEq(savedBasket));
        }

        @Test
        void shouldDeleteAnonymousBasketWhenKeepOptionIsEqualToANONYMOUS() {
            // Given
            String customerId = "customerId";
            String sessionId = "sessionId";
            when(persistencePort.find(sessionId))
                    .thenReturn(Optional.of(createBasket(sessionId, "ANONYMOUS-FSID", createOffer("563214", 3))));

            // When
            basketService.mergeBaskets(customerId, sessionId, KeepOption.ANONYMOUS, false);

            // Then
            Mockito.verify(persistencePort).delete(sessionId);
        }

        @Test
        void shouldMergeOffersAndReturnAuthenticatedBasketWithMergedOffersWhenKeepOptionIsEqualToAUTHENTICATEDAndMergeOffersOptionIsTrueAndOffersAreDifferent() {
            // Given
            String customerId = "customerId";
            String sessionId = "sessionId";
            when(persistencePort.find(customerId))
                    .thenReturn(Optional.of(createBasket(customerId, "AUTHENTICATED-FSID", createOffer("123456", 1))));
            when(persistencePort.find(sessionId))
                    .thenReturn(Optional.of(createBasket(sessionId, "ANONYMOUS-FSID", createOffer("563214", 3))));

            // When
            Basket mergedBasket = basketService.mergeBaskets(customerId, sessionId, KeepOption.AUTHENTICATED, true);

            // Then
            Basket expectedBasket = createBasket("customerId",
                    "AUTHENTICATED-FSID",
                    createOffer("123456", 1),
                    createOffer("563214", 3));
            Assertions.assertThat(mergedBasket).isEqualTo(expectedBasket);
        }

        @Test
        void shouldMergeOffersAndReturnAuthenticatedBasketWithMergedOffersWhenKeepOptionIsEqualToAUTHENTICATEDAndMergeOffersOptionIsTrueAndSameOffers() {
            // Given
            String customerId = "customerId";
            String sessionId = "sessionId";
            when(persistencePort.find(customerId))
                    .thenReturn(Optional.of(createBasket(customerId, "AUTHENTICATED-FSID", createOffer("123456", 1), createOffer("563214", 2))));
            when(persistencePort.find(sessionId))
                    .thenReturn(Optional.of(createBasket(sessionId, "ANONYMOUS-FSID", createOffer("563214", 3))));

            // When
            Basket mergedBasket = basketService.mergeBaskets(customerId, sessionId, KeepOption.AUTHENTICATED, true);

            // Then
            Basket expectedBasket = createBasket("customerId",
                    "AUTHENTICATED-FSID",
                    createOffer("123456", 1),
                    createOffer("563214", 2));
            Assertions.assertThat(mergedBasket).isEqualTo(expectedBasket);
        }

        @Test
        void shouldMergeOffersAndReturnMergedBasketWhenKeepOptionIsEqualToANONYMOUSAndMergeOffersOptionIsTrue() {
            // Given
            String customerId = "customerId";
            String sessionId = "sessionId";
            when(persistencePort.find(customerId))
                    .thenReturn(Optional.of(createBasket(customerId, "AUTHENTICATED-FSID", createOffer("123456", 1))));
            when(persistencePort.find(sessionId))
                    .thenReturn(Optional.of(createBasket(sessionId, "ANONYMOUS-FSID", createOffer("563214", 3))));

            // When
            Basket mergedBasket = basketService.mergeBaskets(customerId, sessionId, KeepOption.ANONYMOUS, true);

            // Then
            Basket expectedBasket = createBasket("customerId",
                    "ANONYMOUS-FSID",
                    createOffer("563214", 3),
                    createOffer("123456", 1));
            Assertions.assertThat(mergedBasket).isEqualTo(expectedBasket);
        }

        @Test
        void shouldMergeOffersAndReturnMergedBasketWhenKeepOptionIsEqualToANONYMOUSAndMergeOffersOptionIsTrueAndSameOffers() {
            // Given
            String customerId = "customerId";
            String sessionId = "sessionId";
            when(persistencePort.find(customerId))
                    .thenReturn(Optional.of(createBasket(customerId, "AUTHENTICATED-FSID", createOffer("123456", 1))));
            when(persistencePort.find(sessionId))
                    .thenReturn(Optional.of(createBasket(sessionId, "ANONYMOUS-FSID", createOffer("123456", 5), createOffer("563214", 3))));

            // When
            Basket mergedBasket = basketService.mergeBaskets(customerId, sessionId, KeepOption.ANONYMOUS, true);

            // Then
            Basket expectedBasket = createBasket("customerId",
                    "ANONYMOUS-FSID",
                    createOffer("123456", 5),
                    createOffer("563214", 3));
            Assertions.assertThat(mergedBasket).isEqualTo(expectedBasket);
        }
    }

    static Stream<Arguments> invalidSessionIds() {
        return Stream.of(
                Arguments.of("Session id is null", null),
                Arguments.of("Session id is empty", "")
        );
    }

    static Basket createBasket(String customerId, String facilityServiceId, Offer... offers) {
        return Basket.builder()
                .customerId(customerId)
                .facilityServiceId(facilityServiceId)
                .offers(new ArrayList<>(Arrays.asList(offers)))
                .build();
    }

    static Offer createOffer(String gtin, long quantity) {
        return Offer.builder().gtin(gtin).quantity(quantity).build();
    }

}