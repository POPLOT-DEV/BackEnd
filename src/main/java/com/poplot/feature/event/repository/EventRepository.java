package com.poplot.feature.event.repository;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, Long> {
    @EntityGraph(attributePaths = "store")
    Page<Event> findByStatusNot(EventStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "store")
    Optional<Event> findByPublicId(UUID publicId);
}
