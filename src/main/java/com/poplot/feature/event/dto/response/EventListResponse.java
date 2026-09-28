package com.poplot.feature.event.dto.response;

import com.poplot.feature.event.entity.Event;
import org.springframework.data.domain.Page;

import java.util.List;

public record EventListResponse(List<EventSummaryResponse> events, int page, int size, boolean hasNext) {
    public static EventListResponse from(Page<Event> events) {
        return new EventListResponse(events.getContent().stream().map(EventSummaryResponse::from).toList(), events.getNumber(), events.getSize(), events.hasNext());
    }
}
