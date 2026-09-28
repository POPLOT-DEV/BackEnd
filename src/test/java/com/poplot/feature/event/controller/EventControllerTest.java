package com.poplot.feature.event.controller;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.TimeSlotStatus;
import com.poplot.feature.store.entity.Store;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("event list is public, paged, and excludes drafts")
    void eventListIsPublicAndExcludesDrafts() throws Exception {
        Event visible = persistEvent("visible", EventStatus.SCHEDULED, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
        persistEvent("draft", EventStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3));

        mockMvc.perform(get("/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.events.length()").value(1))
            .andExpect(jsonPath("$.data.events[0].eventPublicId").value(visible.getPublicId().toString()))
            .andExpect(jsonPath("$.data.events[0].name").value("visible"))
            .andExpect(jsonPath("$.data.events[0].bannerImgUrl").value("https://example.com/banner.jpg"))
            .andExpect(jsonPath("$.data.events[0].status").value("SCHEDULED"))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(10))
            .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("event detail contains the store and event information")
    void eventDetailContainsStoreInformation() throws Exception {
        Event event = persistEvent("detail", EventStatus.RESERVATION_OPEN, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

        mockMvc.perform(get("/events/{eventPublicId}", event.getPublicId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("detail"))
            .andExpect(jsonPath("$.data.description").value("event description"))
            .andExpect(jsonPath("$.data.store.name").value("POPLOT store"))
            .andExpect(jsonPath("$.data.store.address").value("Seoul"));
    }

    @Test
    @DisplayName("reservation dates indicate whether each date has an available slot")
    void reservationDatesReflectSlotAvailability() throws Exception {
        Event event = persistEvent("dates", EventStatus.RESERVATION_OPEN, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
        persistTimeSlot(event, LocalDate.of(2026, 10, 1), LocalTime.of(10, 0), 20, 20, TimeSlotStatus.OPEN);
        persistTimeSlot(event, LocalDate.of(2026, 10, 2), LocalTime.of(10, 0), 20, 0, TimeSlotStatus.OPEN);
        persistTimeSlot(event, LocalDate.of(2026, 10, 4), LocalTime.of(10, 0), 20, 0, TimeSlotStatus.OPEN);

        mockMvc.perform(get("/events/{eventPublicId}/reservation-dates", event.getPublicId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.dates[0].date").value("2026-10-01"))
            .andExpect(jsonPath("$.data.dates[0].available").value(false))
            .andExpect(jsonPath("$.data.dates[1].date").value("2026-10-02"))
            .andExpect(jsonPath("$.data.dates[1].available").value(true))
            .andExpect(jsonPath("$.data.dates.length()").value(2));
    }

    @Test
    @DisplayName("time slots expose calculated remaining counts and display statuses")
    void timeSlotsExposeCalculatedStatuses() throws Exception {
        Event event = persistEvent("slots", EventStatus.RESERVATION_OPEN, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
        LocalDate date = LocalDate.of(2026, 10, 2);
        persistTimeSlot(event, date, LocalTime.of(10, 0), 20, 0, TimeSlotStatus.OPEN);
        persistTimeSlot(event, date, LocalTime.of(11, 0), 20, 16, TimeSlotStatus.OPEN);
        persistTimeSlot(event, date, LocalTime.of(12, 0), 20, 20, TimeSlotStatus.OPEN);
        persistTimeSlot(event, date, LocalTime.of(13, 0), 20, 0, TimeSlotStatus.CLOSED);

        mockMvc.perform(get("/events/{eventPublicId}/timeslots", event.getPublicId()).queryParam("date", date.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.timeSlots[0].remainingCount").value(20))
            .andExpect(jsonPath("$.data.timeSlots[0].status").value("AVAILABLE"))
            .andExpect(jsonPath("$.data.timeSlots[1].remainingCount").value(4))
            .andExpect(jsonPath("$.data.timeSlots[1].status").value("LOW_STOCK"))
            .andExpect(jsonPath("$.data.timeSlots[2].remainingCount").value(0))
            .andExpect(jsonPath("$.data.timeSlots[2].status").value("SOLD_OUT"))
            .andExpect(jsonPath("$.data.timeSlots[3].status").value("CLOSED"));
    }

    @Test
    @DisplayName("invalid pagination and reservation dates return contract errors")
    void invalidRequestsReturnContractErrors() throws Exception {
        Event event = persistEvent("validation", EventStatus.SCHEDULED, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

        mockMvc.perform(get("/events").queryParam("page", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_400_01"));
        mockMvc.perform(get("/events/{eventPublicId}/timeslots", event.getPublicId()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("EVENT_400_01"));
        mockMvc.perform(get("/events/{eventPublicId}/timeslots", event.getPublicId()).queryParam("date", "2026-10-10"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("EVENT_400_02"));
    }

    @Test
    @DisplayName("missing and draft events return distinct not-found errors")
    void missingAndDraftEventsReturnDistinctErrors() throws Exception {
        Event draft = persistEvent("draft", EventStatus.DRAFT, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

        mockMvc.perform(get("/events/{eventPublicId}", UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("EVENT_404_01"));
        mockMvc.perform(get("/events/{eventPublicId}", draft.getPublicId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("EVENT_404_02"));
    }

    private Event persistEvent(String title, EventStatus status, LocalDate startDate, LocalDate endDate) {
        Store store = new Store("POPLOT store");
        ReflectionTestUtils.setField(store, "location", "Seoul");
        ReflectionTestUtils.setField(store, "bannerImgUrl", "https://example.com/banner.jpg");
        entityManager.persist(store);
        Event event = new Event(store, title, Instant.now().minusSeconds(3_600), Instant.now().plusSeconds(3_600), startDate, endDate, 4, 10);
        ReflectionTestUtils.setField(event, "description", "event description");
        ReflectionTestUtils.setField(event, "status", status);
        entityManager.persist(event);
        entityManager.flush();
        return event;
    }

    private void persistTimeSlot(Event event, LocalDate date, LocalTime startTime, int capacity, int reservedCount, TimeSlotStatus status) {
        TimeSlot timeSlot = new TimeSlot(event, date, startTime, startTime.plusHours(1), capacity);
        ReflectionTestUtils.setField(timeSlot, "reservedCount", reservedCount);
        ReflectionTestUtils.setField(timeSlot, "status", status);
        entityManager.persist(timeSlot);
        entityManager.flush();
    }
}
