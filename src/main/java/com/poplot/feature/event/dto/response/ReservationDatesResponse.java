package com.poplot.feature.event.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ReservationDatesResponse(List<ReservationDateResponse> dates) {
    public record ReservationDateResponse(LocalDate date, boolean available) {
    }
}
