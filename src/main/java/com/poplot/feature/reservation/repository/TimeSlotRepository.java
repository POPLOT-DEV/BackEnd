package com.poplot.feature.reservation.repository;

import com.poplot.feature.reservation.entity.TimeSlot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    @EntityGraph(attributePaths = "event")
    Optional<TimeSlot> findByPublicId(UUID publicId);

    List<TimeSlot> findByEventIdAndDateBetweenOrderByDateAscStartTimeAsc(
        Long eventId,
        LocalDate startDate,
        LocalDate endDate
    );

    List<TimeSlot> findByEventIdAndDateOrderByStartTimeAsc(Long eventId, LocalDate date);
}
