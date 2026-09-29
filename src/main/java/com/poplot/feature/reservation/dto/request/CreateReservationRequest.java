package com.poplot.feature.reservation.dto.request;

public record CreateReservationRequest(String timeSlotPublicId, Integer partySize) {
}
