package com.poplot.feature.event.service;

import com.poplot.feature.event.dto.response.EventDetailResponse;
import com.poplot.feature.event.dto.response.EventListResponse;
import com.poplot.feature.event.dto.response.ReservationDatesResponse;
import com.poplot.feature.event.dto.response.ReservationDatesResponse.ReservationDateResponse;
import com.poplot.feature.event.dto.response.TimeSlotsResponse;
import com.poplot.feature.event.dto.response.TimeSlotsResponse.TimeSlotResponse;
import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.event.exception.EventErrorCode;
import com.poplot.feature.event.repository.EventRepository;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.TimeSlotDisplayStatus;
import com.poplot.feature.reservation.repository.TimeSlotRepository;
import com.poplot.global.exception.ErrorCode;
import com.poplot.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryService {
    private static final int MAX_PAGE_SIZE = 100;

    private final EventRepository eventRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TimeSlotAvailabilityCalculator availabilityCalculator;

    public EventListResponse getEvents(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new GeneralException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("id")));
        Page<Event> events = eventRepository.findByStatusNot(EventStatus.DRAFT, pageable);
        return EventListResponse.from(events);
    }

    public EventDetailResponse getEvent(String eventPublicId) {
        return EventDetailResponse.from(getPublicEvent(eventPublicId));
    }

    public ReservationDatesResponse getReservationDates(String eventPublicId) {
        Event event = getPublicEvent(eventPublicId);
        Instant now = Instant.now();
        Map<LocalDate, Boolean> availabilityByDate = new LinkedHashMap<>();
        for (TimeSlot timeSlot : timeSlotRepository.findByEventIdAndDateBetweenOrderByDateAscStartTimeAsc(
            event.getId(), event.getStartDate(), event.getEndDate())) {
            boolean available = isAvailable(availabilityCalculator.calculate(event, timeSlot, now));
            availabilityByDate.merge(timeSlot.getDate(), available, Boolean::logicalOr);
        }
        List<ReservationDateResponse> dates = availabilityByDate.entrySet().stream()
            .map(entry -> new ReservationDateResponse(entry.getKey(), entry.getValue()))
            .toList();
        return new ReservationDatesResponse(dates);
    }

    public TimeSlotsResponse getTimeSlots(String eventPublicId, String dateValue) {
        Event event = getPublicEvent(eventPublicId);
        LocalDate date = parseAndValidateDate(dateValue, event);
        Instant now = Instant.now();
        List<TimeSlotResponse> timeSlots = timeSlotRepository.findByEventIdAndDateOrderByStartTimeAsc(event.getId(), date).stream()
            .map(timeSlot -> TimeSlotResponse.of(timeSlot, availabilityCalculator.calculate(event, timeSlot, now)))
            .toList();
        return new TimeSlotsResponse(date, timeSlots);
    }

    private Event getPublicEvent(String eventPublicId) {
        UUID publicId;
        try {
            publicId = UUID.fromString(eventPublicId);
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(EventErrorCode.EVENT_NOT_FOUND);
        }
        Event event = eventRepository.findByPublicId(publicId).orElseThrow(() -> new GeneralException(EventErrorCode.EVENT_NOT_FOUND));
        if (event.getStatus() == EventStatus.DRAFT) {
            throw new GeneralException(EventErrorCode.EVENT_NOT_PUBLIC);
        }
        return event;
    }

    private LocalDate parseAndValidateDate(String dateValue, Event event) {
        if (dateValue == null || dateValue.isBlank()) {
            throw new GeneralException(EventErrorCode.RESERVATION_DATE_REQUIRED);
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateValue);
        } catch (DateTimeParseException exception) {
            throw new GeneralException(EventErrorCode.INVALID_RESERVATION_DATE);
        }
        if (date.isBefore(event.getStartDate()) || date.isAfter(event.getEndDate())) {
            throw new GeneralException(EventErrorCode.INVALID_RESERVATION_DATE);
        }
        return date;
    }

    private boolean isAvailable(TimeSlotDisplayStatus status) {
        return status == TimeSlotDisplayStatus.AVAILABLE || status == TimeSlotDisplayStatus.LOW_STOCK;
    }
}
