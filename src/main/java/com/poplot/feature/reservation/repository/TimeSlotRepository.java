package com.poplot.feature.reservation.repository;

import com.poplot.feature.reservation.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByEventIdAndDateBetweenOrderByDateAscStartTimeAsc(Long eventId, LocalDate startDate, LocalDate endDate);

    List<TimeSlot> findByEventIdAndDateOrderByStartTimeAsc(Long eventId, LocalDate date);
}
