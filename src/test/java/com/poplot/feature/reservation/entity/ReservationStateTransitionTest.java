package com.poplot.feature.reservation.entity;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.reservation.enums.ReservationStatus;
import com.poplot.feature.store.entity.Store;
import com.poplot.feature.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationStateTransitionTest {
    private static final Instant DEADLINE = Instant.parse("2026-10-01T00:00:00Z");

    @Test
    @DisplayName("reservation can be confirmed exactly at its hold expiration time")
    void confirmsAtExpirationBoundary() {
        Reservation reservation = createReservation(DEADLINE);

        reservation.confirm(DEADLINE);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getConfirmedAt()).isEqualTo(DEADLINE);
    }

    @Test
    @DisplayName("reservation can be canceled exactly at its cancellation deadline")
    void cancelsAtCancellationBoundary() {
        Reservation reservation = createReservation(DEADLINE.plusSeconds(60));

        reservation.cancel(DEADLINE, DEADLINE);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(reservation.getCanceledAt()).isEqualTo(DEADLINE);
    }

    private Reservation createReservation(Instant holdExpiresAt) {
        Store store = new Store("store");
        Event event = new Event(
            store,
            "event",
            DEADLINE.minusSeconds(3_600),
            DEADLINE.plusSeconds(3_600),
            LocalDate.of(2026, 10, 2),
            LocalDate.of(2026, 10, 3),
            4,
            10
        );
        TimeSlot timeSlot = new TimeSlot(
            event,
            LocalDate.of(2026, 10, 2),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            10
        );
        return new Reservation(User.create("owner@example.com", "owner"), timeSlot, "R-1", 2, holdExpiresAt);
    }
}
