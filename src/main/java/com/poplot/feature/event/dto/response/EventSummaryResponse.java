package com.poplot.feature.event.dto.response;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EventSummaryResponse(
    UUID eventPublicId,
    String name,
    String bannerImgUrl,
    LocalDate startDate,
    LocalDate endDate,
    Instant reservationOpenAt,
    Instant reservationCloseAt,
    EventStatus status
) {
    public static EventSummaryResponse from(Event event) {
        return new EventSummaryResponse(event.getPublicId(), event.getTitle(), event.getStore().getBannerImgUrl(), event.getStartDate(), event.getEndDate(),
            event.getReservationOpenAt(), event.getReservationCloseAt(), event.getStatus());
    }
}
