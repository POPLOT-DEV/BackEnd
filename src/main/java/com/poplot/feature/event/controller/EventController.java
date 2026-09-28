package com.poplot.feature.event.controller;

import com.poplot.feature.event.dto.response.EventDetailResponse;
import com.poplot.feature.event.dto.response.EventListResponse;
import com.poplot.feature.event.dto.response.ReservationDatesResponse;
import com.poplot.feature.event.dto.response.TimeSlotsResponse;
import com.poplot.feature.event.service.EventQueryService;
import com.poplot.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventController {
    private final EventQueryService eventQueryService;

    @GetMapping
    public ApiResponse<EventListResponse> getEvents(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(eventQueryService.getEvents(page, size));
    }

    @GetMapping("/{eventPublicId}")
    public ApiResponse<EventDetailResponse> getEvent(@PathVariable String eventPublicId) {
        return ApiResponse.success(eventQueryService.getEvent(eventPublicId));
    }

    @GetMapping("/{eventPublicId}/reservation-dates")
    public ApiResponse<ReservationDatesResponse> getReservationDates(@PathVariable String eventPublicId) {
        return ApiResponse.success(eventQueryService.getReservationDates(eventPublicId));
    }

    @GetMapping("/{eventPublicId}/timeslots")
    public ApiResponse<TimeSlotsResponse> getTimeSlots(
        @PathVariable String eventPublicId,
        @RequestParam(required = false) String date
    ) {
        return ApiResponse.success(eventQueryService.getTimeSlots(eventPublicId, date));
    }
}
