package com.poplot.global.common.entity

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseCreatedTimeEntity {
    @CreatedDate
    @jakarta.persistence.Column(nullable = false, updatable = false)
    lateinit var createdAt: Instant
        protected set
}

@MappedSuperclass
abstract class BaseTimeEntity : BaseCreatedTimeEntity() {
    @LastModifiedDate
    @jakarta.persistence.Column(nullable = false)
    lateinit var updatedAt: Instant
        protected set
}
