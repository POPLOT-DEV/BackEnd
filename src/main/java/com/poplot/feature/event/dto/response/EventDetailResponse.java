package com.poplot.feature.event.dto.response;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EventDetailResponse(
    UUID eventPublicId,
    String name,
    String description,
    String bannerImgUrl,
    LocalDate startDate,
    LocalDate endDate,
    Instant reservationOpenAt,
    Instant reservationCloseAt,
    EventStatus status,
    StoreResponse store
) {
    public static EventDetailResponse from(Event event) {
        return new EventDetailResponse(event.getPublicId(), event.getTitle(), event.getDescription(), event.getStore().getBannerImgUrl(), event.getStartDate(),
            event.getEndDate(), event.getReservationOpenAt(), event.getReservationCloseAt(), event.getStatus(), StoreResponse.from(event));
    }

    public record StoreResponse(UUID storePublicId, String name, String address) {
        private static StoreResponse from(Event event) {
            return new StoreResponse(event.getStore().getPublicId(), event.getStore().getName(), event.getStore().getLocation());
        }
    }
}
